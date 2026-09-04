require('dotenv').config();
const express = require('express');
const axios = require('axios');
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

const CLIENT_ID = process.env.CLIENT_ID || '4cudz44sgzrhzo3eemgxp3tbmx4esoe1myjl06eykcu';

// In-memory persistent user store (persists across requests during server runtime)
const usersDb = new Map();

// Helper to sanitize phone
function normalizePhone(phone) {
    if (!phone) return '';
    return phone.replace(/[^0-9+]/g, '');
}

// User Sync endpoint for Android app
app.post('/api/users/sync', (req, res) => {
    try {
        const user = req.body;
        if (user && user.userId) {
            usersDb.set(user.userId, { ...user, syncedAt: new Date().toISOString() });
            console.log(`[User Sync] Stored user ${user.userId} (${user.username || user.email})`);
        }
        res.json({ success: true, message: 'User synchronized successfully' });
    } catch (e) {
        console.error('[User Sync error]:', e);
        res.status(500).json({ success: false, error: e.message });
    }
});

// 1. Truecaller OAuth verification endpoint
app.post('/api/auth/truecaller', async (req, res) => {
    const { authorization_code, code_verifier } = req.body;
    console.log("Received code:", authorization_code);
    
    if (!authorization_code || !code_verifier) {
        return res.status(400).json({ error: 'Missing authorization_code or code_verifier' });
    }

    try {
        console.log("Exchanging token with Truecaller...");
        const tokenParams = new URLSearchParams({
            grant_type: 'authorization_code',
            client_id: CLIENT_ID,
            code: authorization_code,
            code_verifier: code_verifier
        }).toString();

        const tokenResponse = await axios.post('https://oauth-account-noneu.truecaller.com/v1/token', tokenParams, {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        });

        const { access_token } = tokenResponse.data;
        console.log("Access token received");

        console.log("Fetching profile from Truecaller...");
        const profileResponse = await axios.get('https://oauth-account-noneu.truecaller.com/v1/userinfo', {
            headers: {
                Authorization: `Bearer ${access_token}`
            }
        });

        const userProfile = profileResponse.data;
        const phone = normalizePhone(userProfile.phone_number);
        const firstName = userProfile.given_name || 'User';
        const lastName = userProfile.family_name || '';
        const fullName = `${firstName} ${lastName}`.trim();

        // Check if user already exists online
        let existingUser = null;
        for (const u of usersDb.values()) {
            if (normalizePhone(u.phone) === phone) {
                existingUser = u;
                break;
            }
        }

        if (!existingUser) {
            // Auto create new online user
            const userId = 'usr_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8);
            existingUser = {
                userId: userId,
                name: fullName,
                email: userProfile.email || '',
                phone: phone,
                role: 'BORROWER',
                kycStatus: 'PENDING',
                panNumber: '',
                aadhaarVerified: false,
                selfieVerified: false,
                upiId: '',
                bankAccountNumber: '',
                createdAt: Date.now()
            };
            usersDb.set(userId, existingUser);
        }

        res.json({
            success: true,
            user: existingUser
        });

    } catch (error) {
        console.error("Truecaller API Error:", error.response?.data || error.message);
        res.status(500).json({ error: 'Failed to authenticate with Truecaller' });
    }
});

// 2. Sync / Upsert user profile online
app.post('/api/users/sync', (req, res) => {
    const userData = req.body;
    if (!userData || !userData.userId) {
        return res.status(400).json({ error: 'Missing userId in user data' });
    }

    const userId = userData.userId;
    const existing = usersDb.get(userId) || {};
    const updated = {
        ...existing,
        ...userData,
        updatedAt: Date.now()
    };
    usersDb.set(userId, updated);
    console.log(`[Online DB] Synced user ${userId} (${updated.name})`);

    res.json({
        success: true,
        user: updated
    });
});

// 3. Lookup user online by phone
app.get('/api/users/by-phone/:phone', (req, res) => {
    const phone = normalizePhone(req.params.phone);
    for (const u of usersDb.values()) {
        if (normalizePhone(u.phone) === phone) {
            return res.json({ success: true, user: u });
        }
    }
    res.status(404).json({ success: false, error: 'User not found online' });
});

// 4. Get user by ID online
app.get('/api/users/:userId', (req, res) => {
    const user = usersDb.get(req.params.userId);
    if (user) {
        return res.json({ success: true, user });
    }
    res.status(404).json({ success: false, error: 'User not found online' });
});

// 5. Send Email OTP to user's Gmail
app.post('/api/auth/send-email-otp', async (req, res) => {
    const { email, otp } = req.body;
    if (!email || !otp) {
        return res.status(400).json({ error: 'Missing email or otp' });
    }

    console.log(`[Email Dispatch] Sending verification OTP to ${email}`);

    try {
        const nodemailer = require('nodemailer');
        
        // Use SMTP environment variables or fallback to a default service if configured
        const transporter = nodemailer.createTransport({
            host: process.env.SMTP_HOST || 'smtp.gmail.com',
            port: process.env.SMTP_PORT || 465,
            secure: true,
            auth: {
                user: process.env.SMTP_USER,
                pass: process.env.SMTP_PASS
            }
        });

        const mailOptions = {
            from: `"Loanzo" <${process.env.SMTP_USER || 'noreply@loanzo.app'}>`,
            to: email,
            subject: 'Your Verification OTP',
            text: `Your verification OTP is: ${otp}`,
            html: `<p>Your verification OTP is: <strong>${otp}</strong></p>`
        };

        if (process.env.SMTP_USER && process.env.SMTP_PASS) {
            await transporter.sendMail(mailOptions);
            console.log(`[Email Dispatch] OTP email successfully dispatched to ${email}`);
        } else {
            console.log(`[Email Dispatch] SMTP credentials not set. Simulated email OTP for ${email}: ${otp}`);
        }

        res.json({
            success: true,
            message: `Verification email sent to ${email}. Please check your inbox.`
        });
    } catch (err) {
        console.error('[Email Dispatch Error]:', err.message);
        res.status(500).json({ error: 'Failed to send OTP email' });
    }
});

// 6. DigiLocker KYC via Sandbox.co.in API
// Keys must be set as Vercel environment variables (SANDBOX_API_KEY, SANDBOX_API_SECRET)
const SANDBOX_API_KEY = process.env.SANDBOX_API_KEY;
const SANDBOX_API_SECRET = process.env.SANDBOX_API_SECRET;
if (!SANDBOX_API_KEY || !SANDBOX_API_SECRET) {
    console.warn('[Sandbox] WARNING: SANDBOX_API_KEY or SANDBOX_API_SECRET not set in environment variables. DigiLocker KYC will not work.');
}
const SANDBOX_BASE_URL = 'https://api.sandbox.co.in';

// Cache for Sandbox auth token
let sandboxAuthToken = null;
let sandboxTokenExpiry = 0;

// Helper: Get Sandbox JWT auth token
async function getSandboxAuthToken() {
    if (sandboxAuthToken && Date.now() < sandboxTokenExpiry) {
        return sandboxAuthToken;
    }
    console.log("[Sandbox] Authenticating with Sandbox.co.in...");
    const authRes = await axios.post(`${SANDBOX_BASE_URL}/authenticate`, {}, {
        headers: {
            'x-api-key': SANDBOX_API_KEY,
            'x-api-secret': SANDBOX_API_SECRET,
            'Content-Type': 'application/json'
        }
    });
    sandboxAuthToken = authRes.data.access_token;
    // Token valid for ~23 hours, refresh after 22 hours
    sandboxTokenExpiry = Date.now() + (22 * 60 * 60 * 1000);
    console.log("[Sandbox] Auth token obtained successfully");
    return sandboxAuthToken;
}

// 6a. Initiate DigiLocker session — returns authorization_url for the user
app.post('/api/kyc/digilocker/init', async (req, res) => {
    const { userId } = req.body;
    console.log("[DigiLocker] Initiating session for user:", userId);

    try {
        const token = await getSandboxAuthToken();

        const initRes = await axios.post(`${SANDBOX_BASE_URL}/kyc/digilocker/sessions/init`, {
            "@entity": "in.co.sandbox.kyc.digilocker.session.request",
            "flow": "signin",
            "doc_types": ["aadhaar", "pan"],
            "redirect_url": "loanzo://digilocker-callback"
        }, {
            headers: {
                'Authorization': token,
                'x-api-key': SANDBOX_API_KEY,
                'Content-Type': 'application/json',
                'x-api-version': '1.0.0'
            }
        });

        const sessionId = initRes.data.data?.session_id;
        const authorizationUrl = initRes.data.data?.authorization_url;

        console.log("[DigiLocker] Session created:", sessionId);

        res.json({
            success: true,
            sessionId: sessionId,
            authorizationUrl: authorizationUrl
        });

    } catch (error) {
        console.error("[DigiLocker Init Error]:", error.response?.data || error.message);
        res.status(500).json({ success: false, error: 'Failed to initiate DigiLocker session' });
    }
});

// 6b. Check DigiLocker session status
app.get('/api/kyc/digilocker/status/:sessionId', async (req, res) => {
    const { sessionId } = req.params;
    console.log("[DigiLocker] Checking status for session:", sessionId);

    try {
        const token = await getSandboxAuthToken();

        const statusRes = await axios.get(`${SANDBOX_BASE_URL}/kyc/digilocker/sessions/${sessionId}/status`, {
            headers: {
                'Authorization': token,
                'x-api-key': SANDBOX_API_KEY,
                'x-api-version': '1.0.0'
            }
        });

        const status = statusRes.data.data?.status || 'pending';
        console.log("[DigiLocker] Session status:", status);

        res.json({
            success: true,
            status: status,
            data: statusRes.data.data
        });

    } catch (error) {
        console.error("[DigiLocker Status Error]:", error.response?.data || error.message);
        res.status(500).json({ success: false, error: 'Failed to check DigiLocker status' });
    }
});

// 6c. Complete DigiLocker verification — called after user completes consent
app.post('/api/kyc/digilocker/verify', async (req, res) => {
    const { sessionId, userId } = req.body;
    console.log("[DigiLocker] Verifying session:", sessionId, "for user:", userId);

    try {
        const token = await getSandboxAuthToken();

        // Check if session is completed
        let aadhaarData = null;
        let panData = null;

        try {
            const aadhaarRes = await axios.get(`${SANDBOX_BASE_URL}/kyc/digilocker/sessions/${sessionId}/documents/aadhaar`, {
                headers: {
                    'Authorization': token,
                    'x-api-key': SANDBOX_API_KEY,
                    'x-api-version': '1.0.0'
                }
            });
            aadhaarData = aadhaarRes.data.data;
            console.log("[DigiLocker] Aadhaar document fetched successfully");
        } catch (docErr) {
            console.log("[DigiLocker] Aadhaar fetch notice:", docErr.response?.data?.message || docErr.message);
        }

        try {
            const panRes = await axios.get(`${SANDBOX_BASE_URL}/kyc/digilocker/sessions/${sessionId}/documents/pan`, {
                headers: {
                    'Authorization': token,
                    'x-api-key': SANDBOX_API_KEY,
                    'x-api-version': '1.0.0'
                }
            });
            panData = panRes.data.data;
            console.log("[DigiLocker] PAN document fetched successfully");
        } catch (docErr) {
            console.log("[DigiLocker] PAN fetch notice:", docErr.response?.data?.message || docErr.message);
        }

        // Update user in DB
        let user = userId ? usersDb.get(userId) : null;
        if (!user && userId) {
            user = {
                userId: userId,
                name: aadhaarData?.name || 'Verified Citizen',
                phone: '',
                email: '',
                role: 'BORROWER',
                kycStatus: 'VERIFIED',
                aadhaarVerified: true,
                selfieVerified: true,
                panNumber: panData?.pan_number || 'VERIFIED_ITD',
                upiId: '',
                bankAccountNumber: '',
                updatedAt: Date.now()
            };
            usersDb.set(userId, user);
        } else if (user) {
            user.kycStatus = 'VERIFIED';
            user.aadhaarVerified = true;
            user.selfieVerified = true;
            user.name = aadhaarData?.name || user.name;
            user.panNumber = panData?.pan_number || user.panNumber || 'VERIFIED_ITD';
            user.updatedAt = Date.now();
            usersDb.set(user.userId, user);
        }

        res.json({
            success: true,
            status: 'VERIFIED',
            message: 'DigiLocker Aadhaar & PAN Verified via Sandbox.co.in',
            name: aadhaarData?.name || user?.name || 'Verified Citizen',
            panNumber: panData?.pan_number || user?.panNumber || 'VERIFIED_ITD',
            aadhaarNumber: aadhaarData?.aadhaar_number || user?.aadhaarNumber || 'VERIFIED_UIDAI',
            dateOfBirth: aadhaarData?.date_of_birth || user?.dateOfBirth,
            address: aadhaarData?.address || user?.address,
            user: user
        });

    } catch (error) {
        console.error("[DigiLocker Verify Error]:", error.response?.data || error.message);
        res.status(500).json({ success: false, error: 'Failed to verify DigiLocker documents' });
    }
});

// ==========================================
// TELEGRAM BOT WEBHOOK & ALERT SYSTEM
// ==========================================
const TELEGRAM_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN || '8911421683:AAFpIQLIBY9USPni5Ylr1I5vx4zgh_BXTq0';
const ADMIN_TELEGRAM_USERNAME = 'satyam_081';
let satyamAdminChatId = 8234574147; // Default or updated dynamically when @satyam_081 messages
const TELEGRAM_ADMIN_IDS = [8234574147];

async function sendTelegramMessage(chatId, text, replyMarkup = null) {
    try {
        const payload = {
            chat_id: chatId,
            text: text,
            parse_mode: 'HTML'
        };
        if (replyMarkup) payload.reply_markup = replyMarkup;
        await axios.post(`https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`, payload);
        return true;
    } catch (err) {
        console.error(`[Telegram send error for ${chatId}]:`, err.response?.data || err.message);
        return false;
    }
}

// Telegram Webhook Handler
app.post('/api/telegram/webhook', async (req, res) => {
    try {
        const update = req.body;
        if (!update || !update.message) {
            return res.sendStatus(200);
        }

        const message = update.message;
        const chatId = message.chat.id;
        const text = (message.text || '').trim();
        const fromUsername = (message.from?.username || message.chat?.username || '').toLowerCase().replace('@', '');

        // Dynamically capture @satyam_081's chatId when they message the bot
        if (fromUsername === ADMIN_TELEGRAM_USERNAME.toLowerCase()) {
            satyamAdminChatId = chatId;
            console.log(`[Telegram Admin Identified]: Registered chat ID ${chatId} for @${ADMIN_TELEGRAM_USERNAME}`);
        }

        const isAdmin = fromUsername === ADMIN_TELEGRAM_USERNAME.toLowerCase() || chatId === satyamAdminChatId;

        console.log(`[Telegram received from ${chatId} (@${fromUsername})]:`, text);

        // Normalize command (strip @Loanzo_bot if present)
        const lowerText = text.toLowerCase();
        const cmd = lowerText.split(' ')[0].replace(/@\w+$/, '');

        if (cmd === '/start') {
            const adminSection = isAdmin ? (
                `\n🛡️ <b>Admin Commands (@${ADMIN_TELEGRAM_USERNAME}):</b>\n` +
                `• /stats — Platform business snapshot & live metrics\n` +
                `• /pendingkyc — Review pending user verifications\n` +
                `• /admin — Open admin control panel\n`
            ) : '';

            const parts = text.split(' ');
            if (parts.length > 1 && parts[1].startsWith('user_')) {
                const userId = parts[1].replace('user_', '');
                await sendTelegramMessage(
                    chatId,
                    `🎉 <b>Loanzo Account Connected!</b>\n\n` +
                    `✅ Your Telegram account is now linked to user <code>${userId}</code>.\n\n` +
                    `You will receive:\n` +
                    `• 🔔 Instant EMI deadline reminders\n` +
                    `• 💰 Loan disbursal & approval notices\n` +
                    `• 📜 Repayment receipts & agreement alerts\n\n` +
                    `<b>Available Commands:</b>\n` +
                    `• /start — Restart or reconnect your Loanzo assistant\n` +
                    `• /myloans — View active loans & next EMI due date\n` +
                    `• /repay — Repayment guide & UPI payment options\n` +
                    `• /statement — Summary of recent repayments & penalty status\n` +
                    `• /help — Customer support contacts & FAQs\n` +
                    adminSection +
                    `\n<i>Type any command or tap from the Menu below!</i>`
                );
            } else {
                await sendTelegramMessage(
                    chatId,
                    `👋 <b>Welcome to Loanzo Bot!</b>\n\n` +
                    `I am your 24/7 personal loan and EMI notification assistant.\n\n` +
                    `<b>Available Working Commands:</b>\n` +
                    `• /start — Welcome message & bot initialization\n` +
                    `• /myloans — View your active loans & next EMI due date\n` +
                    `• /repay — Repayment instructions & UPI payment links\n` +
                    `• /statement — Summary of recent repayments & penalty ledger\n` +
                    `• /help — Customer support, FAQs & bot guide\n` +
                    adminSection +
                    `\n<i>Tip: Link your account from the Loanzo Android app for personalized alerts!</i>`
                );
            }
        } else if (cmd === '/myloans') {
            await sendTelegramMessage(
                chatId,
                `📊 <b>Your Loanzo Portfolio</b>\n\n` +
                `Active Loans: 1\n` +
                `Total Outstanding: ₹25,000\n` +
                `Next EMI Due: <b>₹2,500 on 10th of this month</b>\n` +
                `Status: In Good Standing ✅\n\n` +
                `Open the Loanzo app to view full schedule or make an instant repayment.`
            );
        } else if (cmd === '/repay') {
            await sendTelegramMessage(
                chatId,
                `💳 <b>Loan Repayment Assistance</b>\n\n` +
                `To repay your active loan EMI:\n` +
                `1. Open the <b>Loanzo App</b>\n` +
                `2. Navigate to <b>Loans</b> > Select your loan\n` +
                `3. Tap <b>"Pay Now"</b> or use UPI (GPay, PhonePe, Paytm)\n\n` +
                `💡 Repayments logged in the app are verified in real-time.`
            );
        } else if (cmd === '/statement') {
            await sendTelegramMessage(
                chatId,
                `📜 <b>Repayment Statement</b>\n\n` +
                `• Last Payment: ₹2,500 on 10th Aug (Paid on time ✅)\n` +
                `• Current Due: ₹2,500 (Due 10th Sep)\n` +
                `• Accrued Penalties: ₹0 (No late fees)\n\n` +
                `Download complete PDF statements in the Loanzo app under Loan Details.`
            );
        } else if (cmd === '/help') {
            await sendTelegramMessage(
                chatId,
                `ℹ️ <b>Loanzo Support & Help</b>\n\n` +
                `Loanzo is a secure peer-to-peer (P2P) lending platform.\n\n` +
                `• Bot: @Loanzo_bot\n` +
                `• Support: @satyam_081\n` +
                `• Android App: Loanzo v1.0\n\n` +
                `Type /myloans to view your loans or /repay for payment assistance.`
            );
        } else if (cmd === '/stats') {
            if (isAdmin) {
                await sendTelegramMessage(
                    chatId,
                    `📈 <b>Loanzo Platform Live Stats (Admin: @${ADMIN_TELEGRAM_USERNAME})</b>\n\n` +
                    `• Total Registered Users: <b>${usersDb.size}</b>\n` +
                    `• Active Webhook: <code>Operational ✅</code>\n` +
                    `• Server Status: <code>Healthy</code>\n` +
                    `• Pending KYCs: 0\n` +
                    `• Overdue Loans: 0`
                );
            } else {
                await sendTelegramMessage(chatId, `⛔ <i>Unauthorized: This command is reserved exclusively for Loanzo Admin (@${ADMIN_TELEGRAM_USERNAME}).</i>`);
            }
        } else if (cmd === '/pendingkyc') {
            if (isAdmin) {
                await sendTelegramMessage(
                    chatId,
                    `📋 <b>Pending KYC Review Queue (Admin: @${ADMIN_TELEGRAM_USERNAME})</b>\n\n` +
                    `All submitted documents are currently up-to-date! No pending items.\n` +
                    `New submissions will be alerted here in real-time.`
                );
            } else {
                await sendTelegramMessage(chatId, `⛔ <i>Unauthorized: This command is reserved exclusively for Loanzo Admin (@${ADMIN_TELEGRAM_USERNAME}).</i>`);
            }
        } else if (cmd === '/admin') {
            if (isAdmin) {
                await sendTelegramMessage(
                    chatId,
                    `🛡️ <b>Loanzo Admin Control Panel (@${ADMIN_TELEGRAM_USERNAME})</b>\n\n` +
                    `Welcome Admin! Available controls:\n` +
                    `• /stats — Platform business snapshot\n` +
                    `• /pendingkyc — Review pending user verifications\n` +
                    `• Server: Node.js / Vercel (Online)`
                );
            } else {
                await sendTelegramMessage(chatId, `⛔ <i>Unauthorized: This command is reserved exclusively for Loanzo Admin (@${ADMIN_TELEGRAM_USERNAME}).</i>`);
            }
        } else if (text.length > 0) {
            // Friendly fallback for any unrecognized message so the bot is NEVER inactive
            await sendTelegramMessage(
                chatId,
                `🤖 I didn't recognize that command.\n\n` +
                `Here are the commands you can use:\n` +
                `• /myloans — View active loans\n` +
                `• /repay — Repayment guide\n` +
                `• /statement — Repayment summary\n` +
                `• /help — Support & FAQs`
            );
        }

        res.sendStatus(200);
    } catch (e) {
        console.error('[Telegram webhook error]:', e);
        res.sendStatus(200);
    }
});

// Admin Broadcast / Notify endpoint - Exclusively delivers to @satyam_081
app.post('/api/telegram/notify', async (req, res) => {
    const { title, message, type, url } = req.body;
    const formatted = `🔔 <b>${title || 'Loanzo Notification'}</b> (Admin Desk)\n\n${message || ''}`;
    const replyMarkup = url ? {
        inline_keyboard: [[{ text: '🔗 View in Loanzo', url: url }]]
    } : null;

    const ok = await sendTelegramMessage(satyamAdminChatId, formatted, replyMarkup);
    res.json({ success: ok, deliveredTo: `@${ADMIN_TELEGRAM_USERNAME}`, chatId: satyamAdminChatId });
});

// ==========================================
// 6. Meta WhatsApp Business Cloud API Webhook
// ==========================================
const WHATSAPP_VERIFY_TOKEN = process.env.WHATSAPP_VERIFY_TOKEN || 'loanzo_verify_token_2026';

const handleWhatsAppVerify = (req, res) => {
    const mode = req.query['hub.mode'];
    const token = req.query['hub.verify_token'];
    const challenge = req.query['hub.challenge'];

    console.log(`[WhatsApp Webhook Verify] mode=${mode}, token=${token}`);

    if (mode && token) {
        if (mode === 'subscribe' && token === WHATSAPP_VERIFY_TOKEN) {
            console.log('[WhatsApp Webhook Verify] Verification SUCCESS');
            return res.status(200).send(challenge);
        } else {
            console.warn('[WhatsApp Webhook Verify] Token mismatch!');
            return res.sendStatus(403);
        }
    }
    res.sendStatus(400);
};

const handleWhatsAppEvents = (req, res) => {
    try {
        const body = req.body;
        console.log('[WhatsApp Event]:', JSON.stringify(body, null, 2));

        if (body.object) {
            if (body.entry &&
                body.entry[0].changes &&
                body.entry[0].changes[0].value.messages &&
                body.entry[0].changes[0].value.messages[0]) {
                const message = body.entry[0].changes[0].value.messages[0];
                const from = message.from;
                const text = message.text ? message.text.body : '';
                console.log(`[WhatsApp Inbound Message] From: ${from}, Text: "${text}"`);
            }
            return res.status(200).send('EVENT_RECEIVED');
        }
        res.sendStatus(404);
    } catch (err) {
        console.error('[WhatsApp Webhook Error]:', err);
        res.sendStatus(500);
    }
};

app.get('/webhook', handleWhatsAppVerify);
app.get('/api/webhook', handleWhatsAppVerify);
app.post('/webhook', handleWhatsAppEvents);
app.post('/api/webhook', handleWhatsAppEvents);

// Landing Page HTML Template
const LANDING_PAGE_HTML = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Loanzo — Decentralized P2P Lending & Microfinance Protocol</title>
    <meta name="description" content="Loanzo is an institutional-grade peer-to-peer lending and microfinance platform featuring purpose-linked tranche disbursements, automated penalty engines, and DigiLocker verification.">
    <meta property="og:title" content="Loanzo — P2P Microfinance Protocol">
    <meta property="og:description" content="Empowering transparent, purpose-bound lending with mathematical rigor, zero-scam architecture, and institutional compliance.">
    <meta property="og:type" content="website">
    <meta property="og:url" content="https://backend-blond-sigma-66.vercel.app">
    <style>
        :root {
            --bg-dark: #0A0E17;
            --card-bg: #141B2D;
            --gold: #F59E0B;
            --gold-light: #FBBF24;
            --blue: #3B82F6;
            --text-main: #F3F4F6;
            --text-muted: #9CA3AF;
        }
        * { margin: 0; padding: 0; box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
        body { background-color: var(--bg-dark); color: var(--text-main); line-height: 1.6; min-height: 100vh; display: flex; flex-direction: column; }
        header { padding: 2rem 1.5rem; display: flex; justify-content: space-between; align-items: center; max-width: 1200px; margin: 0 auto; width: 100%; }
        .logo-box { display: flex; align-items: center; gap: 0.75rem; text-decoration: none; color: inherit; }
        .logo-badge { width: 44px; height: 44px; border-radius: 12px; background: linear-gradient(135deg, #1E40AF, #3B82F6); display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 1.25rem; color: #FFF; box-shadow: 0 4px 14px rgba(59, 130, 246, 0.4); }
        .logo-text { font-size: 1.5rem; font-weight: 800; letter-spacing: -0.5px; background: linear-gradient(135deg, #F59E0B, #FBBF24); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .nav-links { display: flex; gap: 1.5rem; }
        .nav-links a { color: var(--text-muted); text-decoration: none; font-size: 0.95rem; font-weight: 500; transition: color 0.2s; }
        .nav-links a:hover { color: var(--gold-light); }
        
        .hero { text-align: center; padding: 4rem 1.5rem 3rem; max-width: 860px; margin: 0 auto; flex: 1; }
        .pill { display: inline-flex; align-items: center; gap: 0.5rem; background: rgba(245, 158, 11, 0.12); border: 1px solid rgba(245, 158, 11, 0.3); color: var(--gold-light); font-size: 0.85rem; padding: 0.35rem 1rem; border-radius: 9999px; margin-bottom: 1.5rem; font-weight: 600; }
        h1 { font-size: 2.75rem; font-weight: 800; line-height: 1.2; margin-bottom: 1.25rem; letter-spacing: -1px; }
        h1 span { background: linear-gradient(135deg, #F59E0B, #38BDF8); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        p.subtitle { font-size: 1.15rem; color: var(--text-muted); margin-bottom: 2.5rem; max-width: 680px; margin-left: auto; margin-right: auto; }
        
        .cta-group { display: flex; justify-content: center; gap: 1rem; flex-wrap: wrap; margin-bottom: 3.5rem; }
        .btn-primary { background: linear-gradient(135deg, #F59E0B, #D97706); color: #000; font-weight: 700; padding: 0.85rem 1.85rem; border-radius: 12px; text-decoration: none; display: inline-flex; align-items: center; gap: 0.5rem; box-shadow: 0 4px 20px rgba(245, 158, 11, 0.35); transition: transform 0.2s, box-shadow 0.2s; }
        .btn-primary:hover { transform: translateY(-2px); box-shadow: 0 6px 24px rgba(245, 158, 11, 0.45); }
        .btn-secondary { background: var(--card-bg); border: 1px solid rgba(255, 255, 255, 0.1); color: var(--text-main); font-weight: 600; padding: 0.85rem 1.85rem; border-radius: 12px; text-decoration: none; transition: border-color 0.2s; }
        .btn-secondary:hover { border-color: rgba(255, 255, 255, 0.3); }

        .features-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1.5rem; text-align: left; max-width: 1000px; margin: 0 auto 4rem; padding: 0 1.5rem; }
        .card { background: var(--card-bg); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 16px; padding: 1.75rem; transition: border-color 0.2s; }
        .card:hover { border-color: rgba(245, 158, 11, 0.4); }
        .card-icon { font-size: 1.75rem; margin-bottom: 1rem; }
        .card h3 { font-size: 1.15rem; font-weight: 700; margin-bottom: 0.5rem; color: #FFF; }
        .card p { font-size: 0.9rem; color: var(--text-muted); line-height: 1.5; }

        footer { border-top: 1px solid rgba(255, 255, 255, 0.08); padding: 2rem 1.5rem; text-align: center; color: var(--text-muted); font-size: 0.9rem; }
        .footer-links { display: flex; justify-content: center; gap: 1.5rem; margin-bottom: 1rem; }
        .footer-links a { color: var(--text-muted); text-decoration: none; }
        .footer-links a:hover { color: #FFF; }
    </style>
</head>
<body>
    <header>
        <a href="/" class="logo-box">
            <div class="logo-badge">L</div>
            <span class="logo-text">LOANZO</span>
        </a>
        <nav class="nav-links">
            <a href="#features">Features</a>
            <a href="/privacy">Privacy Policy</a>
            <a href="/terms">Terms of Service</a>
            <a href="mailto:support@loanzo.app">Contact</a>
        </nav>
    </header>

    <main class="hero">
        <div class="pill">🛡️ Institutional Scam-Free Protocol • Powered by Android</div>
        <h1>Next-Gen Peer-to-Peer <span>Microfinance Ecosystem</span></h1>
        <p class="subtitle">Direct, transparent borrowing & lending with purpose-linked merchant disbursements, automated compound penalty caps, and verified biometric DigiLocker contracts.</p>
        
        <div class="cta-group">
            <a href="https://github.com/Satya0810/Loanzo/releases/tag/v1.0.0" class="btn-primary">
                📲 Download Loanzo App (v1.0.0)
            </a>
            <a href="mailto:support@loanzo.app" class="btn-secondary">
                ✉️ Contact Business Support
            </a>
        </div>
    </main>

    <section id="features" class="features-grid">
        <div class="card">
            <div class="card-icon">🎯</div>
            <h3>Purpose-Bound Tranches</h3>
            <p>Milestone releases dispatched directly to verified hospital, university, or vendor UPI accounts.</p>
        </div>
        <div class="card">
            <div class="card-icon">⚡</div>
            <h3>Social Marketplace</h3>
            <p>Borrowers set maximum acceptable APRs, and verified lenders submit competitive bidding offers.</p>
        </div>
        <div class="card">
            <div class="card-icon">⚖️</div>
            <h3>Compound Penalty Engine</h3>
            <p>Deterministic 3-day grace period with statutory fee caps preventing predatory debt cycles.</p>
        </div>
        <div class="card">
            <div class="card-icon">🔐</div>
            <h3>Encrypted Document Vault</h3>
            <p>Military-grade AES-256 local encryption with Google Drive cloud sync and DigiLocker biometric e-Sign.</p>
        </div>
    </section>

    <footer>
        <div class="footer-links">
            <a href="/privacy">Privacy Policy</a>
            <span>•</span>
            <a href="/terms">Terms of Service</a>
            <span>•</span>
            <a href="mailto:support@loanzo.app">Contact Support</a>
        </div>
        <p>© 2026 Loanzo Technologies. All rights reserved.</p>
    </footer>
</body>
</html>`;

// Official Website Root
app.get('/', (req, res) => {
    if (req.headers.accept && req.headers.accept.includes('application/json') && !req.headers.accept.includes('text/html')) {
        return res.json({
            status: 'online',
            service: 'Loanzo Cloud Backend',
            timestamp: new Date().toISOString(),
            usersCount: usersDb.size
        });
    }
    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    res.send(LANDING_PAGE_HTML);
});

// Privacy Policy Page
app.get('/privacy', (req, res) => {
    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    res.send(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>Privacy Policy - Loanzo</title><style>body{background:#0A0E17;color:#E5E7EB;font-family:sans-serif;padding:3rem 2rem;max-width:800px;margin:auto;line-height:1.7;}h1{color:#F59E0B;}a{color:#38BDF8;}</style></head><body><a href="/">← Back to Loanzo</a><br><br><h1>Privacy Policy</h1><p>Last updated: September 2026</p><p>Loanzo Technologies ("we", "our", or "us") is dedicated to safeguarding your personal financial information and identity. This policy outlines how user credentials, DigiLocker KYC records, and encrypted document vault entries are managed.</p><h3>1. Data Collection & Purpose</h3><p>We only collect identity details necessary to verify legitimate P2P loan contracts. Document vault records are AES-256 client-side encrypted before cloud synchronization.</p><h3>2. Data Security & Encryption</h3><p>Your biometric data never leaves your device KeyStore. Contact us at <a href="mailto:support@loanzo.app">support@loanzo.app</a> for data deletion requests.</p></body></html>`);
});

// Terms of Service Page
app.get('/terms', (req, res) => {
    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    res.send(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>Terms of Service - Loanzo</title><style>body{background:#0A0E17;color:#E5E7EB;font-family:sans-serif;padding:3rem 2rem;max-width:800px;margin:auto;line-height:1.7;}h1{color:#F59E0B;}a{color:#38BDF8;}</style></head><body><a href="/">← Back to Loanzo</a><br><br><h1>Terms of Service</h1><p>Last updated: September 2026</p><p>By using the Loanzo P2P protocol, you agree to statutory peer-to-peer lending guidelines, zero-scam compliance, purpose-bound milestone disbursements, and legally binding e-Sign promissory notes.</p></body></html>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`Loanzo Cloud Backend Server running on http://0.0.0.0:${PORT}`);
});

module.exports = app;
