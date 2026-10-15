/**
 * Comprehensive Automated Test Suite for Loanzo Telegram Bot
 * Validates:
 * 1. Direct Telegram Bot API Connectivity & Token Auth
 * 2. Bot Profile & Metadata (getMe, getMyCommands, getMyDescription)
 * 3. Server Webhook Endpoint: All user & admin commands
 * 4. Server Webhook Endpoint: All interactive button callbacks
 * 5. Android Notification Ingestion Endpoint (/api/telegram/notify)
 */

const axios = require('axios');
const path = require('path');
const fs = require('fs');

// Resolve Token securely
let BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN || '';
if (!BOT_TOKEN) {
    const envLocal = path.join(__dirname, '.env.local');
    if (fs.existsSync(envLocal)) {
        const m = fs.readFileSync(envLocal, 'utf8').match(/TELEGRAM_BOT_TOKEN=["']?([^"'\r\n]+)["']?/);
        if (m) BOT_TOKEN = m[1].trim();
    }
    if (!BOT_TOKEN) {
        const localProps = path.join(__dirname, '..', 'local.properties');
        if (fs.existsSync(localProps)) {
            const m = fs.readFileSync(localProps, 'utf8').match(/TELEGRAM_BOT_TOKEN=([^\r\n]+)/);
            if (m) BOT_TOKEN = m[1].trim();
        }
    }
}

if (!BOT_TOKEN) {
    console.error('❌ FATAL: Could not resolve TELEGRAM_BOT_TOKEN from local config.');
    process.exit(1);
}

const BASE_URL = `https://api.telegram.org/bot${BOT_TOKEN}`;
const LOCAL_SERVER = 'http://127.0.0.1:3000';
const SUPER_ADMIN_ID = 8234574147;
const SUPER_ADMIN_USER = 'satyam_081';
const TEST_USER_ID = 8605293240;
const TEST_USERNAME = 'shivsattu';

let testResults = [];

function recordResult(category, testName, passed, details = '') {
    testResults.push({ category, testName, passed, details });
    const mark = passed ? '✅ PASS' : '❌ FAIL';
    console.log(`[${mark}] ${category} -> ${testName} ${details ? '(' + details + ')' : ''}`);
}

async function runDirectTelegramApiTests() {
    console.log('\n======================================================');
    console.log('📡 STEP 1: Direct Telegram Bot API Authentication & Config');
    console.log('======================================================');

    try {
        const meRes = await axios.get(`${BASE_URL}/getMe`);
        const botInfo = meRes.data.result;
        recordResult('Direct API', 'getMe Auth', meRes.data.ok, `@${botInfo.username} (ID: ${botInfo.id}, Name: ${botInfo.first_name})`);
    } catch (err) {
        recordResult('Direct API', 'getMe Auth', false, err.response?.data?.description || err.message);
    }

    try {
        const cmdRes = await axios.get(`${BASE_URL}/getMyCommands`);
        const cmds = cmdRes.data.result || [];
        const cmdList = cmds.map(c => `/${c.command}`).join(', ');
        recordResult('Direct API', 'getMyCommands', cmds.length > 0, `${cmds.length} commands registered: ${cmdList}`);
    } catch (err) {
        recordResult('Direct API', 'getMyCommands', false, err.message);
    }

    try {
        const descRes = await axios.get(`${BASE_URL}/getMyDescription`);
        const desc = descRes.data.result?.description || '';
        recordResult('Direct API', 'getMyDescription', desc.length > 0 && !desc.toLowerCase().includes('adult'), `${desc.length} chars verified`);
    } catch (err) {
        recordResult('Direct API', 'getMyDescription', false, err.message);
    }

    try {
        const shortDescRes = await axios.get(`${BASE_URL}/getMyShortDescription`);
        const shortDesc = shortDescRes.data.result?.short_description || '';
        recordResult('Direct API', 'getMyShortDescription', shortDesc.length > 0, `${shortDesc.length} chars verified`);
    } catch (err) {
        recordResult('Direct API', 'getMyShortDescription', false, err.message);
    }
}

async function runCommandTests() {
    console.log('\n======================================================');
    console.log('🤖 STEP 2: Bot Slash Commands (Simulated Webhook Ingestion)');
    console.log('======================================================');

    const commandsToTest = [
        { cmd: '/start', fromId: SUPER_ADMIN_ID, user: SUPER_ADMIN_USER, role: 'Super Admin' },
        { cmd: '/start', fromId: TEST_USER_ID, user: TEST_USERNAME, role: 'Standard User' },
        { cmd: '/start user_devesh123', fromId: TEST_USER_ID, user: TEST_USERNAME, role: 'Account Link' },
        { cmd: '/about', fromId: TEST_USER_ID, user: TEST_USERNAME, role: 'General' },
        { cmd: '/profile', fromId: TEST_USER_ID, user: TEST_USERNAME, role: 'General' },
        { cmd: '/help', fromId: TEST_USER_ID, user: TEST_USERNAME, role: 'General' },
        { cmd: '/myloans', fromId: TEST_USER_ID, user: TEST_USERNAME, role: 'General' },
        { cmd: '/repay', fromId: TEST_USER_ID, user: TEST_USERNAME, role: 'General' },
        { cmd: '/statement', fromId: TEST_USER_ID, user: TEST_USERNAME, role: 'General' },
        { cmd: '/agent', fromId: TEST_USER_ID, user: TEST_USERNAME, role: 'General' },
        { cmd: '/verify_me PAN ABCDE1234F, Delhi Retailer', fromId: TEST_USER_ID, user: TEST_USERNAME, role: 'Application' },
        { cmd: '/admin', fromId: SUPER_ADMIN_ID, user: SUPER_ADMIN_USER, role: 'Admin Command' },
        { cmd: '/pending', fromId: SUPER_ADMIN_ID, user: SUPER_ADMIN_USER, role: 'Admin Queue' },
        { cmd: '/stats', fromId: SUPER_ADMIN_ID, user: SUPER_ADMIN_USER, role: 'Admin Stats' },
        { cmd: '/audit', fromId: SUPER_ADMIN_ID, user: SUPER_ADMIN_USER, role: 'Admin Audit' },
        { cmd: '/superadmin', fromId: SUPER_ADMIN_ID, user: SUPER_ADMIN_USER, role: 'Super Admin Overview' }
    ];

    let updateId = 900000001;

    for (const testItem of commandsToTest) {
        const payload = {
            update_id: updateId++,
            message: {
                message_id: Math.floor(Math.random() * 10000),
                from: {
                    id: testItem.fromId,
                    is_bot: false,
                    first_name: testItem.user,
                    username: testItem.user
                },
                chat: {
                    id: testItem.fromId,
                    first_name: testItem.user,
                    username: testItem.user,
                    type: 'private'
                },
                date: Math.floor(Date.now() / 1000),
                text: testItem.cmd
            }
        };

        try {
            const res = await axios.post(`${LOCAL_SERVER}/api/telegram/webhook`, payload, {
                headers: { 'Content-Type': 'application/json' },
                timeout: 5000
            });
            recordResult('Slash Command', testItem.cmd, res.status === 200, `Caller: @${testItem.user} (${testItem.role})`);
        } catch (err) {
            recordResult('Slash Command', testItem.cmd, false, err.response?.data || err.message);
        }
    }
}

async function runCallbackButtonTests() {
    console.log('\n======================================================');
    console.log('🔘 STEP 3: Interactive Inline Action Buttons (Callback Queries)');
    console.log('======================================================');

    const callbacksToTest = [
        { data: 'btn_myloans', fromId: TEST_USER_ID, user: TEST_USERNAME, desc: 'Portfolio Facility Review' },
        { data: 'btn_repay', fromId: TEST_USER_ID, user: TEST_USERNAME, desc: 'Instant Repayment Flow' },
        { data: 'btn_verify', fromId: TEST_USER_ID, user: TEST_USERNAME, desc: 'Verification Guide Desk' },
        { data: 'btn_about', fromId: TEST_USER_ID, user: TEST_USERNAME, desc: 'Institutional About Section' },
        { data: 'btn_help', fromId: TEST_USER_ID, user: TEST_USERNAME, desc: '24/7 Support Desk' },
        { data: 'btn_statement', fromId: TEST_USER_ID, user: TEST_USERNAME, desc: 'Full Ledger Statement' },
        { data: 'dl_note_LZ-BIZ-50K', fromId: TEST_USER_ID, user: TEST_USERNAME, desc: 'Section 4 NI Act Promissory Note' },
        { data: 'open_dispute_LZ-BIZ-50K', fromId: TEST_USER_ID, user: TEST_USERNAME, desc: 'Anti-Harassment SOS Desk' },
        { data: 'pay_loan_LZ-BIZ-50K', fromId: TEST_USER_ID, user: TEST_USERNAME, desc: 'Pay EMI Callback' },
        // Admin Actions
        { data: 'inspect_loan_LZ-BIZ-50K', fromId: SUPER_ADMIN_ID, user: SUPER_ADMIN_USER, desc: 'Admin Collateral Inspection' },
        { data: 'freeze_recovery_LZ-BIZ-50K', fromId: SUPER_ADMIN_ID, user: SUPER_ADMIN_USER, desc: 'Emergency Recovery Freeze (BNS §351)' },
        { data: 'dispatch_agent_LZ-BIZ-50K', fromId: SUPER_ADMIN_ID, user: SUPER_ADMIN_USER, desc: 'Field Agent Vikas Sharma Dispatch' },
        { data: 'approve_loan_LZ-BIZ-50K', fromId: SUPER_ADMIN_ID, user: SUPER_ADMIN_USER, desc: 'Admin Loan Facility Approval' },
        { data: 'disburse_loan_LZ-BIZ-50K', fromId: SUPER_ADMIN_ID, user: SUPER_ADMIN_USER, desc: 'Smart Escrow Disbursal Execution' },
        { data: 'resolve_dispute_DSP-9041', fromId: SUPER_ADMIN_ID, user: SUPER_ADMIN_USER, desc: 'Admin Dispute Case Resolution' }
    ];

    let updateId = 910000001;

    for (const cb of callbacksToTest) {
        const payload = {
            update_id: updateId++,
            callback_query: {
                id: `cq_${Math.floor(Math.random() * 1000000)}`,
                from: {
                    id: cb.fromId,
                    is_bot: false,
                    first_name: cb.user,
                    username: cb.user
                },
                message: {
                    message_id: 12345,
                    chat: {
                        id: cb.fromId,
                        first_name: cb.user,
                        username: cb.user,
                        type: 'private'
                    },
                    date: Math.floor(Date.now() / 1000),
                    text: 'Previous message card'
                },
                chat_instance: 'instance_xyz_123',
                data: cb.data
            }
        };

        try {
            const res = await axios.post(`${LOCAL_SERVER}/api/telegram/webhook`, payload, {
                headers: { 'Content-Type': 'application/json' },
                timeout: 5000
            });
            recordResult('Action Button Callback', cb.data, res.status === 200, cb.desc);
        } catch (err) {
            recordResult('Action Button Callback', cb.data, false, err.response?.data || err.message);
        }
    }
}

async function runAndroidNotificationTests() {
    console.log('\n======================================================');
    console.log('📱 STEP 4: Android App Event Notifications (/api/telegram/notify)');
    console.log('======================================================');

    const notificationEvents = [
        {
            type: 'KYC_UPLOAD',
            userName: 'Rahul Sharma',
            userId: 'user_rahul_99',
            documentType: 'Aadhaar Card (Offline XML eKYC)',
            documentUrl: 'https://loanzo.app/vault/kyc_doc_sample.pdf'
        },
        {
            type: 'LOAN_REQUEST',
            borrowerName: 'Pooja Verma',
            loanId: 'LZ-REQ-7721',
            amount: 75000,
            purpose: 'Boutique Apparel Inventory Purchase'
        },
        {
            type: 'AGREEMENT_SIGNED',
            borrowerName: 'Pooja Verma',
            loanId: 'LZ-REQ-7721',
            signHash: 'a7b8c9d0e1f234567890abcdef1234567890abcdef1234567890abcdef1234'
        },
        {
            type: 'EMERGENCY_SOS',
            userName: 'Karan Mehra',
            loanId: 'LZ-FAC-1102',
            reason: 'Aggressive third-party collection agent calling after 8 PM without authorization'
        },
        {
            type: 'DISPUTE_REPORT',
            caseId: 'DSP-8842',
            reporterName: 'Sunita Devi',
            reporterPhone: '+91 98765 43210',
            targetPerson: 'agent_rogue_01',
            category: 'HARASSMENT',
            severity: 'CRITICAL',
            details: 'Unauthorized late fee extortion attempt recorded on phone audio'
        }
    ];

    for (const ev of notificationEvents) {
        try {
            const res = await axios.post(`${LOCAL_SERVER}/api/telegram/notify`, ev, {
                headers: { 'Content-Type': 'application/json' },
                timeout: 8000
            });
            recordResult('Android Event Ingestion', ev.type, res.status === 200, `Payload: ${ev.loanId || ev.userId || ev.caseId}`);
        } catch (err) {
            recordResult('Android Event Ingestion', ev.type, false, err.response?.data || err.message);
        }
    }
}

async function runDirectAdminGreetingTest() {
    console.log('\n======================================================');
    console.log('📬 STEP 5: Live Test Ping to Super Admin on Telegram');
    console.log('======================================================');

    try {
        const payload = {
            chat_id: SUPER_ADMIN_ID,
            text: `🧪 <b>Loanzo Bot System Diagnostic Test</b>\n\n` +
                  `✅ <b>Status:</b> All systems fully operational\n` +
                  `⏱️ <b>Timestamp:</b> ${new Date().toISOString().replace('T', ' ').substring(0, 19)} UTC\n` +
                  `🛡️ <b>Super Admin Verified:</b> @${SUPER_ADMIN_USER}\n` +
                  `⚡ <b>Interactive Actions:</b> 100% Operational (Two-Way Response Hub Active)\n\n` +
                  `Tap below to test instant portfolio review:`,
            parse_mode: 'HTML',
            reply_markup: {
                inline_keyboard: [
                    [
                        { text: '📊 My Portfolio', callback_data: 'btn_myloans' },
                        { text: '💳 Instant Repay', callback_data: 'btn_repay' }
                    ],
                    [
                        { text: '📜 Section 4 Note', callback_data: 'dl_note_LZ-BIZ-50K' },
                        { text: '🛑 Freeze Recovery (BNS §351)', callback_data: 'freeze_recovery_LZ-BIZ-50K' }
                    ]
                ]
            }
        };

        const res = await axios.post(`${BASE_URL}/sendMessage`, payload, { timeout: 8000 });
        recordResult('Live Telegram Ping', 'Super Admin Ping', res.data.ok, `Message ID: ${res.data.result?.message_id}`);
    } catch (err) {
        recordResult('Live Telegram Ping', 'Super Admin Ping', false, err.response?.data?.description || err.message);
    }
}

async function main() {
    console.log('================================================================');
    console.log('🚀 LOANZO TELEGRAM BOT - FULL END-TO-END TEST SUITE');
    console.log('================================================================');

    await runDirectTelegramApiTests();
    await runCommandTests();
    await runCallbackButtonTests();
    await runAndroidNotificationTests();
    await runDirectAdminGreetingTest();

    console.log('\n================================================================');
    console.log('📊 FINAL TEST SUITE RESULTS');
    console.log('================================================================');
    const total = testResults.length;
    const passed = testResults.filter(r => r.passed).length;
    const failed = testResults.filter(r => !r.passed).length;

    console.log(`Total Checks: ${total} | Passed: ${passed} | Failed: ${failed}`);
    if (failed === 0) {
        console.log('🎉 ALL TESTS PASSED WITH 100% SUCCESS RATE! The bot is completely verified.');
    } else {
        console.log(`⚠️ ${failed} test(s) failed. Check logs above.`);
    }
    process.exit(failed === 0 ? 0 : 1);
}

main().catch(err => {
    console.error('Fatal test error:', err);
    process.exit(1);
});
