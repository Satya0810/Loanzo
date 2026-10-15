/**
 * Loanzo Bot Long-Polling Daemon Service
 * Actively retrieves updates from Telegram API and forwards to server webhook handler.
 */
const axios = require('axios');
const path = require('path');
const fs = require('fs');

// Resolve Token securely from local config
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
    console.error('[Bot Service] FATAL: TELEGRAM_BOT_TOKEN could not be resolved from local credentials.');
    process.exit(1);
}

// Start Express Backend
const app = require('./server');
const PORT = process.env.PORT || 3000;


let lastUpdateId = 0;
let isPolling = true;

async function pollUpdates() {
    console.log(`[Bot Service] Polling loop initialized for @Loanzo_bot. Awaiting messages and actions...`);
    
    // Ensure any webhook is deleted before polling
    try {
        await axios.get(`https://api.telegram.org/bot${BOT_TOKEN}/deleteWebhook`);
    } catch (_) {}

    while (isPolling) {
        try {
            const url = `https://api.telegram.org/bot${BOT_TOKEN}/getUpdates?offset=${lastUpdateId}&timeout=25&allowed_updates=["message","callback_query"]`;
            const response = await axios.get(url, { timeout: 35000 });
            const updates = response.data?.result || [];

            for (const update of updates) {
                lastUpdateId = update.update_id + 1;
                const sender = update.message?.from?.username || update.callback_query?.from?.username || 'user';
                const text = update.message?.text || update.callback_query?.data || '[media/action]';
                console.log(`[Bot Service] Received from @${sender}: "${text}" (Update ID: ${update.update_id})`);

                try {
                    // Forward update to the webhook endpoint on our Express app
                    await axios.post(`http://127.0.0.1:${PORT}/api/telegram/webhook`, update, {
                        headers: { 'Content-Type': 'application/json' },
                        timeout: 10000
                    });
                } catch (webhookErr) {
                    console.error('[Bot Service] Error dispatching update to webhook:', webhookErr.message);
                }
            }
        } catch (err) {
            if (err.code !== 'ECONNABORTED' && !err.message.includes('timeout')) {
                console.error('[Bot Service] Polling error:', err.response?.data || err.message);
                await new Promise(r => setTimeout(r, 3000));
            }
        }
    }
}

pollUpdates().catch(err => {
    console.error('[Bot Service] Fatal error in polling loop:', err);
});
