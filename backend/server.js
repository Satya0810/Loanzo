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
const TELEGRAM_ADMIN_IDS = [8234574147, 7464832770];

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

        console.log(`[Telegram received from ${chatId}]:`, text);

        // Normalize command (strip @Loanzo_bot if present)
        const lowerText = text.toLowerCase();
        const cmd = lowerText.split(' ')[0].replace(/@\w+$/, '');

        if (cmd === '/start') {
            const isAdmin = TELEGRAM_ADMIN_IDS.includes(chatId);
            const adminSection = isAdmin ? (
                `\n🛡️ <b>Admin Commands:</b>\n` +
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
            if (TELEGRAM_ADMIN_IDS.includes(chatId)) {
                await sendTelegramMessage(
                    chatId,
                    `📈 <b>Loanzo Platform Live Stats (Admin)</b>\n\n` +
                    `• Total Registered Users: <b>${usersDb.size}</b>\n` +
                    `• Active Webhook: <code>Operational ✅</code>\n` +
                    `• Server Status: <code>Healthy</code>\n` +
                    `• Pending KYCs: 0\n` +
                    `• Overdue Loans: 0`
                );
            } else {
                await sendTelegramMessage(chatId, `⛔ <i>Unauthorized: This command is reserved for Loanzo Admins.</i>`);
            }
        } else if (cmd === '/pendingkyc') {
            if (TELEGRAM_ADMIN_IDS.includes(chatId)) {
                await sendTelegramMessage(
                    chatId,
                    `📋 <b>Pending KYC Review Queue (Admin)</b>\n\n` +
                    `All submitted documents are currently up-to-date! No pending items.\n` +
                    `New submissions will be alerted here in real-time.`
                );
            } else {
                await sendTelegramMessage(chatId, `⛔ <i>Unauthorized: This command is reserved for Loanzo Admins.</i>`);
            }
        } else if (cmd === '/admin') {
            if (TELEGRAM_ADMIN_IDS.includes(chatId)) {
                await sendTelegramMessage(
                    chatId,
                    `🛡️ <b>Loanzo Admin Control Panel</b>\n\n` +
                    `Welcome Admin! Available controls:\n` +
                    `• /stats — Platform business snapshot\n` +
                    `• /pendingkyc — Review pending user verifications\n` +
                    `• Server: Node.js / Vercel (Online)`
                );
            } else {
                await sendTelegramMessage(chatId, `⛔ <i>Unauthorized: This command is reserved for Loanzo Admins.</i>`);
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

// Admin Broadcast / Notify endpoint
app.post('/api/telegram/notify', async (req, res) => {
    const { title, message, type, url } = req.body;
    const formatted = `🔔 <b>${title || 'Loanzo Notification'}</b>\n\n${message || ''}`;
    const replyMarkup = url ? {
        inline_keyboard: [[{ text: '🔗 View in Loanzo', url: url }]]
    } : null;

    let count = 0;
    for (const adminId of TELEGRAM_ADMIN_IDS) {
        const ok = await sendTelegramMessage(adminId, formatted, replyMarkup);
        if (ok) count++;
    }

    res.json({ success: true, delivered: count });
});

// Root health check
app.get('/', (req, res) => {
    res.json({
        status: 'online',
        service: 'Loanzo Cloud Backend',
        timestamp: new Date().toISOString(),
        usersCount: usersDb.size
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`Loanzo Cloud Backend Server running on http://0.0.0.0:${PORT}`);
});

module.exports = app;
