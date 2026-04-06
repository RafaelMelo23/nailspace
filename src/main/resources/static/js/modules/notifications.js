window.NotificationService = {
    eventSource: null,
    whatsappStatus: 'CLOSE',

    init: function() {
        if (!Auth.getToken() || !Auth.hasRole('ADMIN')) return;
        this.subscribe();
    },

    subscribe: function() {
        if (this.eventSource) this.eventSource.close();

        const token = Auth.getToken();
        const url = `/api/v1/notifications/subscribe?token=${token}`;
        this.eventSource = new EventSource(url);

        this.eventSource.onopen = () => {
            console.log("SSE Connection opened");
        };

        this.eventSource.onmessage = (event) => {
            try {
                const payload = JSON.parse(event.data);
                this.handleNotification(payload);
            } catch (e) {
                // Some events might be plain text (like the INIT event)
                console.log("SSE message received:", event.data);
            }
        };

        this.eventSource.onerror = (err) => {
            console.error("SSE Connection Error", err);
            this.eventSource.close();
            // Reconnect after 5 seconds if still authenticated
            if (Auth.getToken()) {
                setTimeout(() => this.subscribe(), 5000);
            }
        };
    },

    handleNotification: function(payload) {
        const { sseEventType, data } = payload;

        switch (sseEventType) {
            case 'QR_CODE_UPDATE':
                this.handleQrCodeUpdate(data);
                break;
            case 'CONNECTION_UPDATE':
                this.handleConnectionUpdate(data);
                break;
            default:
                console.log("Unhandled SSE event type:", sseEventType);
        }
    },

    handleQrCodeUpdate: function(data) {
        const qrContainer = document.getElementById('whatsapp-qr-container');
        const pairingContainer = document.getElementById('whatsapp-pairing-container');
        const loading = document.getElementById('whatsapp-loading');
        const retry = document.getElementById('whatsapp-retry');
        
        if (!qrContainer) return;

        loading.classList.add('hidden');
        if (retry) retry.classList.remove('hidden');
        
        if (data.pairingCode) {
            const codeEl = document.getElementById('whatsapp-pairing-code');
            codeEl.innerText = data.pairingCode;
            pairingContainer.classList.remove('hidden');
            qrContainer.classList.add('hidden');
            this.showWhatsappPopup();
        } else if (data.code || data.base64 || data.qrcode) {
            const qrSource = data.base64 || data.qrcode || data.code;
            const imgSrc = qrSource.startsWith('data:image') ? qrSource : `data:image/png;base64,${qrSource}`;
            
            qrContainer.innerHTML = `<img src="${imgSrc}" alt="WhatsApp QR Code">`;
            qrContainer.classList.remove('hidden');
            pairingContainer.classList.add('hidden');
            this.showWhatsappPopup();
        }
    },

    handleConnectionUpdate: function(data) {
        // data might be the EvolutionConnectionState string or an object containing it
        const state = typeof data === 'string' ? data : (data.state || data.status);
        if (!state) return;

        this.whatsappStatus = state;
        this.updateStatusUI(state);

        if (state === 'OPEN') {
            Toast.success("WhatsApp conectado com sucesso!");
            this.hideWhatsappPopup();
        } else if (state === 'CLOSE') {
            // Only show toast if it was previously connecting/open
            // Toast.info("WhatsApp desconectado.");
        }
    },

    updateStatusUI: function(state) {
        const statusEls = document.querySelectorAll('#whatsapp-connection-status');
        statusEls.forEach(el => {
            const dot = el.querySelector('.status-dot');
            const text = el.querySelector('.status-text');
            
            if (!dot || !text) return;
            
            dot.className = 'status-dot';
            
            if (state === 'OPEN') {
                dot.classList.add('status-open');
                text.innerText = 'Conectado';
            } else if (state === 'CONNECTING') {
                dot.classList.add('status-connecting');
                text.innerText = 'Conectando...';
            } else {
                dot.classList.add('status-close');
                text.innerText = 'Desconectado';
            }
        });
    },

    showWhatsappPopup: function() {
        const popup = document.getElementById('whatsapp-popup');
        if (popup) popup.classList.remove('hidden');
    },

    hideWhatsappPopup: function() {
        const popup = document.getElementById('whatsapp-popup');
        if (popup) popup.classList.add('hidden');
        this.resetPopup();
    },

    resetPopup: function() {
        const qrContainer = document.getElementById('whatsapp-qr-container');
        const pairingContainer = document.getElementById('whatsapp-pairing-container');
        const loading = document.getElementById('whatsapp-loading');
        const instructions = document.getElementById('whatsapp-instructions');
        const retry = document.getElementById('whatsapp-retry');

        if (qrContainer) qrContainer.classList.add('hidden');
        if (pairingContainer) pairingContainer.classList.add('hidden');
        if (loading) loading.classList.add('hidden');
        if (instructions) instructions.classList.remove('hidden');
        if (retry) retry.classList.add('hidden');
    },

    retryConnection: function() {
        this.resetPopup();
    },

    startWhatsappConnection: async function(method) {
        const loading = document.getElementById('whatsapp-loading');
        const instructions = document.getElementById('whatsapp-instructions');
        const retry = document.getElementById('whatsapp-retry');
        
        if (loading) loading.classList.remove('hidden');
        if (instructions) instructions.classList.add('hidden');
        if (retry) retry.classList.add('hidden');

        try {
            const res = await fetch(`/api/v1/whatsapp?connectionMethod=${method}`, {
                method: 'POST'
            });

            if (!res.ok) {
                Toast.error("Erro ao iniciar conexão com WhatsApp.");
                if (loading) loading.classList.add('hidden');
                if (instructions) instructions.classList.remove('hidden');
            } else {
                this.updateStatusUI('CONNECTING');
            }
        } catch (err) {
            Toast.error("Erro de rede ao conectar WhatsApp.");
            if (loading) loading.classList.add('hidden');
            if (instructions) instructions.classList.remove('hidden');
        }
    }
};
