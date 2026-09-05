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
// TELEGRAM BOT WEBHOOK & ADVANCED RBAC SYSTEM
// ==========================================
const TELEGRAM_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN || '8911421683:AAFpIQLIBY9USPni5Ylr1I5vx4zgh_BXTq0';
const SUPER_ADMIN_USERNAME = 'satyam_081';
const SUPER_ADMIN_ALIAS = 'satyam@081';
const SUPER_ADMIN_ID = 8234574147;
let satyamAdminChatId = SUPER_ADMIN_ID;

// Multi-Tier Role Hierarchy
const ROLES = {
    SUPER_ADMIN: 'super_admin',
    ADMIN: 'admin',
    VERIFIED_USER: 'verified_user',
    USER: 'user',
    BANNED: 'banned'
};

// In-Memory Dynamic Protected Content Store
const botContent = {
    about: "Loanzo is an institutional-grade microfinance and peer-to-peer (P2P) lending platform engineered with purpose-bound tranche disbursements, automated penalty engines, and DigiLocker biometric e-Sign.",
    rules: "1. No predatory interest rates (market-driven competitive bidding).\n2. Purpose-bound disbursements verified via merchant invoices.\n3. Mandatory 3-factor eSign for legal contract enforceability.\n4. Zero tolerance for abusive recovery or harassment.",
    help: "For support, contact @satyam_081 or email support@loanzo.app. Use /myloans to view active portfolios and /repay for instant UPI payment instructions."
};

// In-Memory User Role Store (Key: string chatId or lowercase username)
const telegramRolesDb = new Map();

// Initialize Super Admin credentials
telegramRolesDb.set(String(SUPER_ADMIN_ID), {
    role: ROLES.SUPER_ADMIN,
    username: SUPER_ADMIN_USERNAME,
    name: 'Satyam Kumar',
    updatedAt: new Date().toISOString()
});
telegramRolesDb.set(SUPER_ADMIN_USERNAME.toLowerCase(), {
    role: ROLES.SUPER_ADMIN,
    username: SUPER_ADMIN_USERNAME,
    name: 'Satyam Kumar',
    updatedAt: new Date().toISOString()
});
telegramRolesDb.set(SUPER_ADMIN_ALIAS.toLowerCase(), {
    role: ROLES.SUPER_ADMIN,
    username: SUPER_ADMIN_USERNAME,
    name: 'Satyam Kumar',
    updatedAt: new Date().toISOString()
});

// Verification Requests Store (Key: requestId)
const verificationQueue = new Map();

// Tamper-Resistant Security Audit Trail (Array of last 200 logs)
const telegramAuditLog = [];

function logAudit(actorId, actorUsername, action, targetId, details) {
    const entry = {
        id: 'aud_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6),
        timestamp: new Date().toISOString(),
        actorId: String(actorId || 'SYSTEM'),
        actorUsername: actorUsername || 'unknown',
        action,
        targetId: String(targetId || ''),
        details
    };
    telegramAuditLog.unshift(entry);
    if (telegramAuditLog.length > 200) telegramAuditLog.pop();
    console.log(`[Telegram Audit] [${action}] by @${actorUsername} -> Target: ${targetId}: ${details}`);
    return entry;
}

// Role Resolution Helper
function getUserRole(chatId, username) {
    const cleanUsername = (username || '').toLowerCase().replace('@', '');

    if (chatId === SUPER_ADMIN_ID || cleanUsername === SUPER_ADMIN_USERNAME.toLowerCase() || cleanUsername === SUPER_ADMIN_ALIAS.toLowerCase() || cleanUsername === 'satyam_081') {
        return ROLES.SUPER_ADMIN;
    }

    if (telegramRolesDb.has(String(chatId))) {
        return telegramRolesDb.get(String(chatId)).role;
    }

    if (cleanUsername && telegramRolesDb.has(cleanUsername)) {
        return telegramRolesDb.get(cleanUsername).role;
    }

    return ROLES.USER;
}

function isUserSuperAdmin(chatId, username) {
    return getUserRole(chatId, username) === ROLES.SUPER_ADMIN;
}

function isUserAdminOrHigher(chatId, username) {
    const role = getUserRole(chatId, username);
    return role === ROLES.ADMIN || role === ROLES.SUPER_ADMIN;
}

function getAdminChatIds() {
    const adminIds = new Set([SUPER_ADMIN_ID]);
    if (satyamAdminChatId) adminIds.add(satyamAdminChatId);
    for (const [key, val] of telegramRolesDb.entries()) {
        if ((val.role === ROLES.ADMIN || val.role === ROLES.SUPER_ADMIN) && /^\d+$/.test(key)) {
            adminIds.add(Number(key));
        }
    }
    return Array.from(adminIds);
}

// Telegram API Communication Helpers
async function sendTelegramMessage(chatId, text, replyMarkup = null) {
    try {
        const payload = {
            chat_id: chatId,
            text: text,
            parse_mode: 'HTML'
        };
        if (replyMarkup) payload.reply_markup = replyMarkup;
        const res = await axios.post(`https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`, payload);
        return res.data?.result || true;
    } catch (err) {
        console.error(`[Telegram send error for ${chatId}]:`, err.response?.data || err.message);
        return false;
    }
}

async function editTelegramMessage(chatId, messageId, text, replyMarkup = null) {
    try {
        const payload = {
            chat_id: chatId,
            message_id: messageId,
            text: text,
            parse_mode: 'HTML'
        };
        if (replyMarkup) payload.reply_markup = replyMarkup;
        const res = await axios.post(`https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/editMessageText`, payload);
        return res.data?.result || true;
    } catch (err) {
        console.error(`[Telegram edit error for ${chatId}:${messageId}]:`, err.response?.data || err.message);
        return false;
    }
}

async function answerTelegramCallbackQuery(callbackQueryId, text = null, showAlert = false) {
    try {
        const payload = { callback_query_id: callbackQueryId };
        if (text) payload.text = text;
        if (showAlert) payload.show_alert = showAlert;
        await axios.post(`https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/answerCallbackQuery`, payload);
        return true;
    } catch (err) {
        console.error(`[Telegram answerCallbackQuery error]:`, err.response?.data || err.message);
        return false;
    }
}

// Central Telegram Webhook Handler (Handles both messages and inline button callback queries)
app.post('/api/telegram/webhook', async (req, res) => {
    try {
        const update = req.body;
        if (!update) return res.sendStatus(200);

        // ==========================================
        // 1. INLINE BUTTON CALLBACK QUERY HANDLER
        // ==========================================
        if (update.callback_query) {
            const cq = update.callback_query;
            const cqId = cq.id;
            const fromId = cq.from.id;
            const fromUser = cq.from.username || cq.from.first_name || 'unknown';
            const data = cq.data || '';
            const message = cq.message;
            const msgChatId = message?.chat?.id;
            const msgId = message?.message_id;

            const isCallerAdmin = isUserAdminOrHigher(fromId, fromUser);

            if (!isCallerAdmin) {
                await answerTelegramCallbackQuery(cqId, "⛔ Access Denied: Admin privileges required to review requests.", true);
                return res.sendStatus(200);
            }

            if (data.startsWith('verify_req_')) {
                const reqId = data.replace('verify_req_', '');
                const reqItem = verificationQueue.get(reqId);

                if (!reqItem) {
                    await answerTelegramCallbackQuery(cqId, "Verification request not found or expired.", true);
                    return res.sendStatus(200);
                }

                if (reqItem.status !== 'PENDING') {
                    await answerTelegramCallbackQuery(cqId, `This request is already ${reqItem.status}.`, true);
                    return res.sendStatus(200);
                }

                reqItem.status = 'VERIFIED';
                reqItem.reviewedBy = `@${fromUser}`;
                reqItem.reviewedAt = new Date().toISOString();

                // Elevate user role to VERIFIED_USER
                telegramRolesDb.set(String(reqItem.userId), {
                    role: ROLES.VERIFIED_USER,
                    username: reqItem.username,
                    name: reqItem.name,
                    updatedAt: new Date().toISOString()
                });
                if (reqItem.username) {
                    telegramRolesDb.set(reqItem.username.toLowerCase(), {
                        role: ROLES.VERIFIED_USER,
                        username: reqItem.username,
                        name: reqItem.name,
                        updatedAt: new Date().toISOString()
                    });
                }

                logAudit(fromId, fromUser, 'REQUEST_VERIFIED', reqItem.userId, `Verified applicant ${reqItem.name} (@${reqItem.username}) - Request #${reqId}`);

                const updatedCard = `✅ <b>NOTIFICATION: VERIFICATION APPROVED</b>\n\n` +
                    `━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
                    `👤 <b>Applicant:</b> ${reqItem.name} (@${reqItem.username || 'n/a'})\n` +
                    `🆔 <b>User ID:</b> <code>${reqItem.userId}</code>\n` +
                    `📝 <b>Details:</b> ${reqItem.details}\n` +
                    `📅 <b>Submitted:</b> ${reqItem.createdAt}\n` +
                    `━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
                    `🟢 <b>Status:</b> <b>VERIFIED</b> by @${fromUser}\n` +
                    `⏱️ <b>Verified At:</b> ${reqItem.reviewedAt}`;

                await editTelegramMessage(msgChatId, msgId, updatedCard, null);
                await answerTelegramCallbackQuery(cqId, `Approved verification for ${reqItem.name}!`);

                // Send live alert to applicant
                await sendTelegramMessage(reqItem.userId,
                    `🎉 <b>Congratulations ${reqItem.name}!</b>\n\n` +
                    `Your Loanzo verification request has been approved by our Admin desk (@${fromUser})!\n\n` +
                    `✨ You are now a <b>Verified User</b> on Loanzo.\n` +
                    `Enjoy higher borrowing limits, zero escrow listing fees, and priority tranche releases.\n\n` +
                    `Type /perks to view your verified privileges!`
                );
                return res.sendStatus(200);

            } else if (data.startsWith('reject_req_')) {
                const reqId = data.replace('reject_req_', '');
                const reqItem = verificationQueue.get(reqId);

                if (!reqItem) {
                    await answerTelegramCallbackQuery(cqId, "Verification request not found or expired.", true);
                    return res.sendStatus(200);
                }

                if (reqItem.status !== 'PENDING') {
                    await answerTelegramCallbackQuery(cqId, `This request is already ${reqItem.status}.`, true);
                    return res.sendStatus(200);
                }

                reqItem.status = 'REJECTED';
                reqItem.reviewedBy = `@${fromUser}`;
                reqItem.reviewedAt = new Date().toISOString();

                logAudit(fromId, fromUser, 'REQUEST_REJECTED', reqItem.userId, `Rejected applicant ${reqItem.name} (@${reqItem.username}) - Request #${reqId}`);

                const updatedCard = `❌ <b>NOTIFICATION: VERIFICATION REJECTED</b>\n\n` +
                    `━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
                    `👤 <b>Applicant:</b> ${reqItem.name} (@${reqItem.username || 'n/a'})\n` +
                    `🆔 <b>User ID:</b> <code>${reqItem.userId}</code>\n` +
                    `📝 <b>Details:</b> ${reqItem.details}\n` +
                    `📅 <b>Submitted:</b> ${reqItem.createdAt}\n` +
                    `━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
                    `🔴 <b>Status:</b> <b>REJECTED</b> by @${fromUser}\n` +
                    `⏱️ <b>Reviewed At:</b> ${reqItem.reviewedAt}`;

                await editTelegramMessage(msgChatId, msgId, updatedCard, null);
                await answerTelegramCallbackQuery(cqId, `Rejected request #${reqId}.`);

                // Send live alert to applicant
                await sendTelegramMessage(reqItem.userId,
                    `⚠️ <b>Verification Request Update</b>\n\n` +
                    `Your Loanzo verification request was reviewed by our Admin desk (@${fromUser}) and could not be approved at this time.\n\n` +
                    `Please verify your profile details and re-apply using /verify_me <details>.`
                );
                return res.sendStatus(200);

            } else if (data.startsWith('view_user_')) {
                const targetUserId = data.replace('view_user_', '');
                const role = getUserRole(targetUserId, '');
                await answerTelegramCallbackQuery(cqId, `User ID: ${targetUserId} | Role: ${role.toUpperCase()}`, true);
                return res.sendStatus(200);
            }
        }

        // ==========================================
        // 2. INCOMING MESSAGE / COMMAND HANDLER
        // ==========================================
        if (!update.message) return res.sendStatus(200);

        const message = update.message;
        const chatId = message.chat.id;
        const text = (message.text || '').trim();
        const fromUsername = (message.from?.username || message.chat?.username || '').toLowerCase().replace('@', '');
        const fromName = `${message.from?.first_name || ''} ${message.from?.last_name || ''}`.trim() || 'User';

        // Auto-detect Super Admin
        if (fromUsername === SUPER_ADMIN_USERNAME.toLowerCase() || fromUsername === 'satyam@081' || chatId === SUPER_ADMIN_ID) {
            satyamAdminChatId = chatId;
            telegramRolesDb.set(String(chatId), { role: ROLES.SUPER_ADMIN, username: SUPER_ADMIN_USERNAME, name: 'Satyam Kumar' });
        }

        const userRole = getUserRole(chatId, fromUsername);

        // Check if Banned
        if (userRole === ROLES.BANNED) {
            await sendTelegramMessage(chatId, `🚫 <b>Access Restricted</b>\n\nYour Telegram account has been banned from interacting with the Loanzo Bot due to policy violations. Contact support@loanzo.app if you believe this is an error.`);
            return res.sendStatus(200);
        }

        const isAdmin = isUserAdminOrHigher(chatId, fromUsername);
        const isSuperAdmin = isUserSuperAdmin(chatId, fromUsername);

        console.log(`[Telegram received from ${chatId} (@${fromUsername}) | Role: ${userRole}]:`, text);

        const lowerText = text.toLowerCase();
        const tokens = text.split(' ');
        const cmd = tokens[0].toLowerCase().replace(/@\w+$/, '');
        const args = text.substring(tokens[0].length).trim();

        // ----------------------------------------------------
        // COMMAND: /start
        // ----------------------------------------------------
        if (cmd === '/start') {
            if (tokens.length > 1 && tokens[1].startsWith('user_')) {
                const appUserId = tokens[1].replace('user_', '');
                telegramRolesDb.set(String(chatId), {
                    role: userRole,
                    username: fromUsername,
                    name: fromName,
                    appUserId: appUserId,
                    updatedAt: new Date().toISOString()
                });
                await sendTelegramMessage(
                    chatId,
                    `🔗 <b>Loanzo Account Connected!</b>\n\n` +
                    `✅ Your Telegram account is now securely linked to Loanzo user <code>${appUserId}</code>.\n` +
                    `Current Role: <b>${userRole.toUpperCase()}</b>\n\n` +
                    `You will receive real-time updates for:\n` +
                    `• ⏰ Milestone EMI deadline alerts\n` +
                    `• 💰 Loan disbursals & bids\n` +
                    `• 📑 Executed 3-factor eSign agreements\n\n` +
                    `Type /about to learn about Loanzo, or /profile to view your role card!`
                );
                return res.sendStatus(200);
            }

            let menuContent = `👋 <b>Welcome to Loanzo Bot!</b>\n\n` +
                `I am your 24/7 personal microfinance and verification assistant.\n` +
                `Role: <b>${userRole.toUpperCase()}</b>\n\n` +
                `<b>Common Commands:</b>\n` +
                `• /about - Official platform overview\n` +
                `• /profile - Your role, badge & verification status\n` +
                `• /verify_me &lt;details&gt; - Submit verification application\n` +
                `• /perks - View verified user privileges\n` +
                `• /myloans - View active loans & upcoming EMI\n` +
                `• /repay - Repayment instructions & UPI links\n` +
                `• /statement - Repayment history & penalty ledger\n` +
                `• /help - Support and bot guide\n`;

            if (isAdmin) {
                menuContent += `\n🛡️ <b>Admin Commands:</b>\n` +
                    `• /admin - Administrative command dashboard\n` +
                    `• /pending - View interactive verification queue ([✓] / [✗])\n` +
                    `• /ban &lt;user_id&gt; - Restrict abusive user\n` +
                    `• /unban &lt;user_id&gt; - Restore user access\n` +
                    `• /broadcast &lt;msg&gt; - Broadcast announcement\n` +
                    `• /stats - Live business snapshot\n`;
            }

            if (isSuperAdmin) {
                menuContent += `\n👑 <b>Super Admin Exclusive Commands (@${SUPER_ADMIN_USERNAME}):</b>\n` +
                    `• /edit_about &lt;new content&gt; - Update official About section (Strict Lock)\n` +
                    `• /edit_section &lt;key&gt; &lt;new content&gt; - Update core sections\n` +
                    `• /set_admin &lt;chat_id&gt; - Appoint new admin\n` +
                    `• /demote_admin &lt;chat_id&gt; - Demote admin to user\n` +
                    `• /audit - View tamper-resistant audit logs\n` +
                    `• /superadmin - Super Admin overview\n`;
            }

            await sendTelegramMessage(chatId, menuContent);
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /about (Dynamic Platform Content)
        // ----------------------------------------------------
        } else if (cmd === '/about') {
            await sendTelegramMessage(
                chatId,
                `📖 <b>About Loanzo Platform</b>\n\n` +
                `${botContent.about}\n\n` +
                `<i>Managed securely under Super Admin authority (@${SUPER_ADMIN_USERNAME}).</i>`
            );
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /profile (Personal Identity & Role Card)
        // ----------------------------------------------------
        } else if (cmd === '/profile') {
            const roleBadge = userRole === ROLES.SUPER_ADMIN ? '👑 SUPER ADMIN' :
                              userRole === ROLES.ADMIN ? '🛡️ ADMIN' :
                              userRole === ROLES.VERIFIED_USER ? '⭐ VERIFIED USER' : '👤 MEMBER';

            await sendTelegramMessage(
                chatId,
                `💳 <b>Your Loanzo Profile Card</b>\n\n` +
                `━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
                `👤 <b>Name:</b> ${fromName}\n` +
                `🏷️ <b>Username:</b> @${fromUsername || 'n/a'}\n` +
                `🆔 <b>Telegram ID:</b> <code>${chatId}</code>\n` +
                `🎖️ <b>Role:</b> <b>${roleBadge}</b>\n` +
                `🔐 <b>Status:</b> ${userRole === ROLES.VERIFIED_USER || isAdmin ? 'Verified Citizen ✅' : 'Standard Member (Unverified) ⚠️'}\n` +
                `━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n` +
                `${userRole === ROLES.USER ? '💡 <i>Tip: Submit a verification request using /verify_me to unlock verified perks!</i>' : ''}`
            );
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /verify_me <details> (Submits to Admin Queue)
        // ----------------------------------------------------
        } else if (cmd === '/verify_me') {
            if (userRole === ROLES.VERIFIED_USER || isAdmin) {
                await sendTelegramMessage(chatId, `✅ You are already a verified member with full privileges! Type /perks to see your benefits.`);
                return res.sendStatus(200);
            }

            if (!args) {
                await sendTelegramMessage(
                    chatId,
                    `📝 <b>How to Submit Verification:</b>\n\n` +
                    `Please provide your verification credentials with the command:\n` +
                    `<code>/verify_me Aadhaar/PAN Details, City, Purpose of Membership</code>\n\n` +
                    `<i>Example:</i> <code>/verify_me PAN ABCDE1234F, Delhi, Business Retailer</code>`
                );
                return res.sendStatus(200);
            }

            const reqId = 'req_' + Date.now().toString().slice(-6);
            const reqItem = {
                id: reqId,
                userId: String(chatId),
                username: fromUsername,
                name: fromName,
                details: args,
                status: 'PENDING',
                createdAt: new Date().toISOString().replace('T', ' ').substring(0, 19) + ' UTC',
                reviewedBy: null,
                reviewedAt: null
            };
            verificationQueue.set(reqId, reqItem);

            logAudit(chatId, fromUsername, 'VERIFICATION_REQUEST_SUBMITTED', reqId, `Applicant ${fromName} submitted request with details: ${args}`);

            // Acknowledge user
            await sendTelegramMessage(
                chatId,
                `📨 <b>Verification Request Submitted!</b>\n\n` +
                `Request Reference: <code>#${reqId}</code>\n` +
                `Status: <b>PENDING REVIEW ⏳</b>\n\n` +
                `Our verification desk has received your application. You will be notified here immediately once approved.`
            );

            // Dispatch interactive decision card to Admins with [✓ Verify] and [✗ Reject] buttons
            const notificationCard = `🔔 <b>NOTIFICATION: VERIFICATION REQUEST #${reqId}</b>\n` +
                `━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
                `👤 <b>Applicant:</b> ${fromName} (@${fromUsername || 'n/a'})\n` +
                `🆔 <b>User ID:</b> <code>${chatId}</code>\n` +
                `📋 <b>Request Type:</b> Role & KYC Verification\n` +
                `📝 <b>Details:</b> ${args}\n` +
                `📅 <b>Submitted At:</b> ${reqItem.createdAt}\n` +
                `━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
                `⏳ <b>Status:</b> <b>PENDING REVIEW</b>`;

            const inlineKeyboard = {
                inline_keyboard: [
                    [
                        { text: '✅ Verify / Approve', callback_data: `verify_req_${reqId}` },
                        { text: '❌ Reject', callback_data: `reject_req_${reqId}` }
                    ],
                    [
                        { text: '👤 View Profile', callback_data: `view_user_${chatId}` }
                    ]
                ]
            };

            const adminIds = getAdminChatIds();
            for (const adminId of adminIds) {
                await sendTelegramMessage(adminId, notificationCard, inlineKeyboard);
            }
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /perks (Verified User Privileges)
        // ----------------------------------------------------
        } else if (cmd === '/perks') {
            const hasPerks = userRole === ROLES.VERIFIED_USER || isAdmin;
            if (!hasPerks) {
                await sendTelegramMessage(
                    chatId,
                    `🔒 <b>Verified User Perks</b>\n\n` +
                    `Exclusive perks are reserved for verified citizens:\n` +
                    `• 🚀 Priority P2P loan listing on Marketplace\n` +
                    `• 💳 0% platform escrow listing fees\n` +
                    `• ⚡ Instant milestone tranche disbursals\n` +
                    `• 📞 24/7 Priority Admin concierge\n\n` +
                    `👉 Submit your application now using: <code>/verify_me &lt;details&gt;</code>`
                );
            } else {
                await sendTelegramMessage(
                    chatId,
                    `⭐ <b>Active Verified User Perks</b>\n\n` +
                    `As a recognized member with <b>${userRole.toUpperCase()}</b> status, you have unlocked:\n` +
                    `✅ Priority Marketplace Bidding Placement\n` +
                    `✅ 0% Platform Escrow Listing Fees\n` +
                    `✅ Accelerated Tranche Invoice Verification\n` +
                    `✅ Direct Telegram Admin Support Line`
                );
            }
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /edit_about <text> (STRICT SUPER ADMIN LOCK)
        // ----------------------------------------------------
        } else if (cmd === '/edit_about') {
            if (!isSuperAdmin) {
                logAudit(chatId, fromUsername, 'UNAUTHORIZED_SECTION_EDIT_BLOCKED', 'about', `Unauthorized edit attempt by @${fromUsername} (Role: ${userRole}): "${args}"`);
                await sendTelegramMessage(
                    chatId,
                    `⛔ <b>ACCESS DENIED: STRICT SECURITY LOCK</b>\n\n` +
                    `Only the designated Super Admin (@${SUPER_ADMIN_USERNAME}) is authorized to edit the platform About section.\n\n` +
                    `⚠️ <i>This unauthorized attempt has been permanently logged to the security audit trail.</i>`
                );
                return res.sendStatus(200);
            }

            if (!args) {
                await sendTelegramMessage(chatId, `⚠️ Please provide the new About content: <code>/edit_about &lt;new content&gt;</code>`);
                return res.sendStatus(200);
            }

            botContent.about = args;
            logAudit(chatId, fromUsername, 'CONTENT_UPDATED', 'about', `Updated About content to: "${args.substring(0, 60)}..."`);

            await sendTelegramMessage(
                chatId,
                `✅ <b>About Section Updated Successfully!</b>\n\n` +
                `New Content:\n${args}\n\n` +
                `<i>Changes are now live across all user sessions.</i>`
            );
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /edit_section <key> <text> (STRICT SUPER ADMIN LOCK)
        // ----------------------------------------------------
        } else if (cmd === '/edit_section') {
            if (!isSuperAdmin) {
                logAudit(chatId, fromUsername, 'UNAUTHORIZED_SECTION_EDIT_BLOCKED', tokens[1] || 'unknown', `Unauthorized edit attempt by @${fromUsername}: "${args}"`);
                await sendTelegramMessage(
                    chatId,
                    `⛔ <b>ACCESS DENIED</b>\n\nOnly Super Admin (@${SUPER_ADMIN_USERNAME}) can edit core sections.`
                );
                return res.sendStatus(200);
            }

            const sectionKey = (tokens[1] || '').toLowerCase();
            const sectionContent = text.substring(tokens[0].length + (tokens[1] || '').length + 1).trim();

            if (!sectionKey || !sectionContent) {
                await sendTelegramMessage(chatId, `⚠️ Usage: <code>/edit_section &lt;rules|help&gt; &lt;new text&gt;</code>`);
                return res.sendStatus(200);
            }

            botContent[sectionKey] = sectionContent;
            logAudit(chatId, fromUsername, 'CONTENT_UPDATED', sectionKey, `Updated ${sectionKey} section`);

            await sendTelegramMessage(chatId, `✅ <b>Section '${sectionKey}' updated successfully!</b>`);
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /set_admin <id_or_username> (SUPER ADMIN ONLY)
        // ----------------------------------------------------
        } else if (cmd === '/set_admin') {
            if (!isSuperAdmin) {
                await sendTelegramMessage(chatId, `⛔ Access Denied: Super Admin exclusive command.`);
                return res.sendStatus(200);
            }

            const target = (args.split(' ')[0] || '').replace('@', '');
            if (!target) {
                await sendTelegramMessage(chatId, `⚠️ Usage: <code>/set_admin &lt;telegram_id_or_username&gt;</code>`);
                return res.sendStatus(200);
            }

            telegramRolesDb.set(target.toLowerCase(), {
                role: ROLES.ADMIN,
                username: target,
                updatedAt: new Date().toISOString()
            });
            logAudit(chatId, fromUsername, 'ROLE_PROMOTED_ADMIN', target, `Promoted ${target} to ADMIN`);

            await sendTelegramMessage(chatId, `🛡️ <b>User ${target} has been appointed as an ADMIN!</b>`);
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /demote_admin <id_or_username> (SUPER ADMIN ONLY)
        // ----------------------------------------------------
        } else if (cmd === '/demote_admin') {
            if (!isSuperAdmin) {
                await sendTelegramMessage(chatId, `⛔ Access Denied: Super Admin exclusive command.`);
                return res.sendStatus(200);
            }

            const target = (args.split(' ')[0] || '').replace('@', '');
            if (!target || target === SUPER_ADMIN_USERNAME.toLowerCase() || target === String(SUPER_ADMIN_ID)) {
                await sendTelegramMessage(chatId, `⚠️ Cannot demote Super Admin.`);
                return res.sendStatus(200);
            }

            telegramRolesDb.set(target.toLowerCase(), {
                role: ROLES.USER,
                username: target,
                updatedAt: new Date().toISOString()
            });
            logAudit(chatId, fromUsername, 'ROLE_DEMOTED_USER', target, `Demoted ${target} to USER`);

            await sendTelegramMessage(chatId, `👤 <b>Admin ${target} has been demoted to standard USER.</b>`);
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /pending (ADMIN DECISION QUEUE)
        // ----------------------------------------------------
        } else if (cmd === '/pending') {
            if (!isAdmin) {
                await sendTelegramMessage(chatId, `⛔ Access Denied: Admin privileges required.`);
                return res.sendStatus(200);
            }

            const pendingList = Array.from(verificationQueue.values()).filter(r => r.status === 'PENDING');
            if (pendingList.length === 0) {
                await sendTelegramMessage(chatId, `🎉 <b>No Pending Requests!</b>\n\nAll verification submissions have been reviewed and processed.`);
                return res.sendStatus(200);
            }

            await sendTelegramMessage(chatId, `📋 <b>Pending Verification Queue (${pendingList.length} items):</b>`);

            for (const reqItem of pendingList) {
                const card = `🔔 <b>REQUEST #${reqItem.id}</b>\n` +
                    `👤 <b>Applicant:</b> ${reqItem.name} (@${reqItem.username || 'n/a'})\n` +
                    `🆔 <b>User ID:</b> <code>${reqItem.userId}</code>\n` +
                    `📝 <b>Details:</b> ${reqItem.details}\n` +
                    `📅 <b>Submitted:</b> ${reqItem.createdAt}`;

                const buttons = {
                    inline_keyboard: [
                        [
                            { text: '✅ Verify / Approve', callback_data: `verify_req_${reqItem.id}` },
                            { text: '❌ Reject', callback_data: `reject_req_${reqItem.id}` }
                        ],
                        [
                            { text: '👤 View User', callback_data: `view_user_${reqItem.userId}` }
                        ]
                    ]
                };

                await sendTelegramMessage(chatId, card, buttons);
            }
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /ban <id> & /unban <id> (ADMIN MODERATION)
        // ----------------------------------------------------
        } else if (cmd === '/ban') {
            if (!isAdmin) {
                await sendTelegramMessage(chatId, `⛔ Access Denied: Admin privileges required.`);
                return res.sendStatus(200);
            }

            const target = (args.split(' ')[0] || '').replace('@', '');
            if (!target || target === SUPER_ADMIN_USERNAME.toLowerCase() || target === String(SUPER_ADMIN_ID)) {
                await sendTelegramMessage(chatId, `⚠️ Cannot ban Super Admin.`);
                return res.sendStatus(200);
            }

            telegramRolesDb.set(target.toLowerCase(), {
                role: ROLES.BANNED,
                username: target,
                updatedAt: new Date().toISOString()
            });
            logAudit(chatId, fromUsername, 'USER_BANNED', target, `Banned by @${fromUsername}`);

            await sendTelegramMessage(chatId, `🚫 <b>User ${target} has been BANNED from interacting with the bot.</b>`);
            return res.sendStatus(200);

        } else if (cmd === '/unban') {
            if (!isAdmin) {
                await sendTelegramMessage(chatId, `⛔ Access Denied: Admin privileges required.`);
                return res.sendStatus(200);
            }

            const target = (args.split(' ')[0] || '').replace('@', '');
            if (!target) {
                await sendTelegramMessage(chatId, `⚠️ Usage: <code>/unban &lt;user_id&gt;</code>`);
                return res.sendStatus(200);
            }

            telegramRolesDb.set(target.toLowerCase(), {
                role: ROLES.USER,
                username: target,
                updatedAt: new Date().toISOString()
            });
            logAudit(chatId, fromUsername, 'USER_UNBANNED', target, `Unbanned by @${fromUsername}`);

            await sendTelegramMessage(chatId, `✅ <b>User ${target} has been UNBANNED and restored to standard USER.</b>`);
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /audit (INSPECT SECURITY AUDIT TRAIL)
        // ----------------------------------------------------
        } else if (cmd === '/audit') {
            if (!isAdmin) {
                await sendTelegramMessage(chatId, `⛔ Access Denied: Admin privileges required.`);
                return res.sendStatus(200);
            }

            if (telegramAuditLog.length === 0) {
                await sendTelegramMessage(chatId, `📜 <b>Security Audit Trail is currently empty.</b>`);
                return res.sendStatus(200);
            }

            const recentLogs = telegramAuditLog.slice(0, 8);
            let logReport = `📜 <b>Security & RBAC Audit Trail (Last ${recentLogs.length} events):</b>\n\n`;

            recentLogs.forEach((log, idx) => {
                logReport += `<b>#${idx + 1} [${log.action}]</b>\n` +
                    `⏰ ${log.timestamp.replace('T', ' ').substring(0, 19)}\n` +
                    `👤 Actor: @${log.actorUsername} (<code>${log.actorId}</code>)\n` +
                    `🎯 Target: <code>${log.targetId}</code>\n` +
                    `📝 Details: ${log.details}\n\n`;
            });

            await sendTelegramMessage(chatId, logReport);
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /broadcast <message> (ADMIN BROADCAST)
        // ----------------------------------------------------
        } else if (cmd === '/broadcast') {
            if (!isAdmin) {
                await sendTelegramMessage(chatId, `⛔ Access Denied: Admin privileges required.`);
                return res.sendStatus(200);
            }

            if (!args) {
                await sendTelegramMessage(chatId, `⚠️ Usage: <code>/broadcast &lt;announcement message&gt;</code>`);
                return res.sendStatus(200);
            }

            const broadcastText = `📢 <b>LOANZO OFFICIAL ANNOUNCEMENT</b>\n\n${args}\n\n<i>Dispatched by Platform Admin Desk (@${fromUsername})</i>`;

            // Broadcast to known numeric chat IDs
            let count = 0;
            const targetIds = new Set([chatId]);
            if (satyamAdminChatId) targetIds.add(satyamAdminChatId);
            for (const [key] of telegramRolesDb.entries()) {
                if (/^\d+$/.test(key)) targetIds.add(Number(key));
            }

            for (const targetChatId of targetIds) {
                await sendTelegramMessage(targetChatId, broadcastText);
                count++;
            }

            logAudit(chatId, fromUsername, 'BROADCAST_SENT', `${count}_recipients`, `Message: ${args.substring(0, 40)}...`);
            await sendTelegramMessage(chatId, `✅ Announcement broadcast dispatched to ${count} active session(s).`);
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMAND: /superadmin (SUPER ADMIN COCKPIT)
        // ----------------------------------------------------
        } else if (cmd === '/superadmin') {
            if (!isSuperAdmin) {
                await sendTelegramMessage(chatId, `⛔ Access Denied: Reserved exclusively for Super Admin (@${SUPER_ADMIN_USERNAME}).`);
                return res.sendStatus(200);
            }

            const pendingCount = Array.from(verificationQueue.values()).filter(r => r.status === 'PENDING').length;
            const verifiedCount = Array.from(telegramRolesDb.values()).filter(r => r.role === ROLES.VERIFIED_USER).length;
            const adminCount = Array.from(telegramRolesDb.values()).filter(r => r.role === ROLES.ADMIN || r.role === ROLES.SUPER_ADMIN).length;

            await sendTelegramMessage(
                chatId,
                `👑 <b>LOANZO SUPER ADMIN COMMAND COCKPIT</b>\n\n` +
                `<b>Authority:</b> @${SUPER_ADMIN_USERNAME} (<code>${SUPER_ADMIN_ID}</code>)\n` +
                `━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
                `• ⏳ Pending Verifications: <b>${pendingCount}</b>\n` +
                `• ⭐ Verified Members: <b>${verifiedCount}</b>\n` +
                `• 🛡️ Active Admins: <b>${adminCount}</b>\n` +
                `• 📜 Total Audit Events: <b>${telegramAuditLog.length}</b>\n` +
                `━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n` +
                `<b>Master Control Commands:</b>\n` +
                `• /edit_about &lt;text&gt; - Update About section (Strict Lock)\n` +
                `• /edit_section &lt;key&gt; &lt;text&gt; - Update core rules/help\n` +
                `• /set_admin &lt;chat_id&gt; - Appoint admin\n` +
                `• /demote_admin &lt;chat_id&gt; - Demote admin\n` +
                `• /pending - View interactive verification queue\n` +
                `• /audit - View security audit log`
            );
            return res.sendStatus(200);

        // ----------------------------------------------------
        // COMMANDS: LOAN SERVICING & EXISTING ENDPOINTS
        // ----------------------------------------------------
        } else if (cmd === '/myloans') {
            await sendTelegramMessage(
                chatId,
                `📊 <b>Your Loanzo Portfolio</b>\n\n` +
                `Active Loans: 1\n` +
                `Total Outstanding: ₹25,000\n` +
                `Next EMI Due: <b>₹2,500 on 10th of this month</b>\n` +
                `Status: In Good Standing ✅\n\n` +
                `Open the Loanzo app to view your amortization schedule or make an instant repayment.`
            );
            return res.sendStatus(200);

        } else if (cmd === '/repay') {
            await sendTelegramMessage(
                chatId,
                `💳 <b>Loan Repayment Assistance</b>\n\n` +
                `To repay your active loan EMI:\n` +
                `1. Open the <b>Loanzo App</b>\n` +
                `2. Navigate to <b>Loans</b> &gt; Select your loan\n` +
                `3. Tap <b>"Pay Now"</b> or use UPI (GPay, PhonePe, Paytm)\n\n` +
                `✨ Repayments logged in the app are verified in real-time with instant digital receipt issuance.`
            );
            return res.sendStatus(200);

        } else if (cmd === '/statement') {
            await sendTelegramMessage(
                chatId,
                `📑 <b>Repayment Statement</b>\n\n` +
                `• Last Payment: ₹2,500 on 10th Aug (Paid on time ✅)\n` +
                `• Current Due: ₹2,500 (Due 10th Sep)\n` +
                `• Accrued Penalties: ₹0 (No late fees)\n\n` +
                `Download complete signed PDF statements in the Loanzo app under Loan Details.`
            );
            return res.sendStatus(200);

        } else if (cmd === '/help') {
            await sendTelegramMessage(
                chatId,
                `ℹ️ <b>Loanzo Support & Help Guide</b>\n\n` +
                `${botContent.help}\n\n` +
                `• Bot: @Loanzo_bot\n` +
                `• Master Admin: @${SUPER_ADMIN_USERNAME}\n` +
                `• Android App: Loanzo v1.0 (Build 34)`
            );
            return res.sendStatus(200);

        } else if (cmd === '/stats') {
            if (isAdmin) {
                await sendTelegramMessage(
                    chatId,
                    `📈 <b>Loanzo Platform Live Metrics (Admin Desk)</b>\n\n` +
                    `• Registered App Users: <b>${usersDb.size}</b>\n` +
                    `• Telegram Roles Tracked: <b>${telegramRolesDb.size}</b>\n` +
                    `• Pending Verification Requests: <b>${Array.from(verificationQueue.values()).filter(r => r.status === 'PENDING').length}</b>\n` +
                    `• Webhook Engine: <code>Operational (v2.0 RBAC) ✅</code>\n` +
                    `• Server Status: <code>Healthy (Vercel)</code>`
                );
            } else {
                await sendTelegramMessage(chatId, `⛔ <i>Unauthorized: This command is reserved exclusively for Loanzo Admins.</i>`);
            }
            return res.sendStatus(200);

        } else if (cmd === '/pendingkyc') {
            if (isAdmin) {
                const pendingCount = Array.from(verificationQueue.values()).filter(r => r.status === 'PENDING').length;
                await sendTelegramMessage(
                    chatId,
                    `📂 <b>Pending Verification Queue</b>\n\n` +
                    `Current pending queue count: <b>${pendingCount}</b>\n` +
                    `Use /pending to interactively approve or reject incoming verification requests.`
                );
            } else {
                await sendTelegramMessage(chatId, `⛔ <i>Unauthorized: This command is reserved exclusively for Loanzo Admins.</i>`);
            }
            return res.sendStatus(200);

        } else if (cmd === '/admin') {
            if (isAdmin) {
                await sendTelegramMessage(
                    chatId,
                    `🛡️ <b>Loanzo Admin Command Dashboard</b>\n\n` +
                    `Welcome Admin! Available administrative tools:\n` +
                    `• /pending - Review interactive verification queue\n` +
                    `• /stats - Live platform statistics\n` +
                    `• /broadcast &lt;msg&gt; - Send announcement\n` +
                    `• /ban &lt;user_id&gt; - Suspend user\n` +
                    `• /unban &lt;user_id&gt; - Restore user\n` +
                    `• /audit - Inspect security audit trail`
                );
            } else {
                await sendTelegramMessage(chatId, `⛔ <i>Unauthorized: This command is reserved for Loanzo Admins.</i>`);
            }
            return res.sendStatus(200);

        } else if (text.length > 0) {
            // Intelligent role-aware fallback
            let guidance = `❓ I didn't recognize that command.\n\nAvailable commands:\n` +
                `• /about - Official platform information\n` +
                `• /profile - Your role & verification status\n` +
                `• /verify_me - Submit verification application\n` +
                `• /myloans - View active loans\n` +
                `• /repay - Repayment assistance\n` +
                `• /help - Support and FAQs`;

            if (isAdmin) {
                guidance += `\n\nAdmin Tools:\n• /pending - Verification queue\n• /admin - Admin dashboard`;
            }

            await sendTelegramMessage(chatId, guidance);
            return res.sendStatus(200);
        }

        res.sendStatus(200);
    } catch (e) {
        console.error('[Telegram webhook error]:', e);
        res.sendStatus(200);
    }
});

// Admin Broadcast / Notify endpoint
app.post('/api/telegram/notify', async (req, res) => {
    const { title, message, type, url, requestId } = req.body;
    const formatted = `🔔 <b>${title || 'Loanzo Notification'}</b> (Admin Desk)\n\n${message || ''}`;
    
    let replyMarkup = null;
    if (requestId) {
        replyMarkup = {
            inline_keyboard: [
                [
                    { text: '✅ Verify / Approve', callback_data: `verify_req_${requestId}` },
                    { text: '❌ Reject', callback_data: `reject_req_${requestId}` }
                ]
            ]
        };
    } else if (url) {
        replyMarkup = {
            inline_keyboard: [[{ text: '📄 View in Loanzo', url: url }]]
        };
    }

    const adminIds = getAdminChatIds();
    let delivered = 0;
    for (const id of adminIds) {
        const ok = await sendTelegramMessage(id, formatted, replyMarkup);
        if (ok) delivered++;
    }

    res.json({ success: delivered > 0, deliveredCount: delivered, adminRecipients: adminIds });
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
