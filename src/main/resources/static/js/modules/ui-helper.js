
const Toast = {
    container: null,

    init: function() {
        this.container = document.createElement('div');
        this.container.className = 'toast-container';
        document.body.appendChild(this.container);
    },

    show: function(message, type = 'error', duration = 5000) {
        if (!this.container) this.init();

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;

        const content = document.createElement('span');
        content.innerText = message;

        const closeBtn = document.createElement('button');
        closeBtn.innerHTML = '&times;';
        closeBtn.style.background = 'none';
        closeBtn.style.border = 'none';
        closeBtn.style.fontSize = '20px';
        closeBtn.style.cursor = 'pointer';
        closeBtn.onclick = () => this.remove(toast);

        toast.appendChild(content);
        toast.appendChild(closeBtn);
        this.container.appendChild(toast);

        setTimeout(() => this.remove(toast), duration);
    },

    remove: function(toast) {
        if (!toast.parentNode) return;
        toast.classList.add('toast-fade-out');
        toast.onanimationend = () => {
            if (toast.parentNode) toast.parentNode.removeChild(toast);
        };
    },

    error: function(msg) { this.show(msg, 'error'); },
    success: function(msg) { this.show(msg, 'success'); }
};

const UI = {
    setLoading: function(btn, loading, text) {
        if (loading) {
            btn.setAttribute('data-original-text', btn.innerText);
            btn.innerText = text || 'Carregando...';
            btn.disabled = true;
            btn.style.opacity = '0.7';
        } else {
            btn.innerText = btn.getAttribute('data-original-text') || text;
            btn.disabled = false;
            btn.style.opacity = '1';
        }
    },

    showToast: function(message, type) {
        Toast.show(message, type);
    },

    renderGlobalHeader: function(salon) {
        const header = document.getElementById('main-header');
        if (!header || !salon) return;

        header.innerHTML = `
            <div class="container">
                <nav class="main-nav">
                    <a href="/" class="brand-link">
                        <span class="brand-name">${salon.tradeName}</span>
                    </a>
                </nav>
            </div>
        `;
    },

    renderGlobalFooter: function(salon) {
        const footer = document.getElementById('main-footer');
        if (!footer || !salon) return;

        footer.innerHTML = `
            <div class="container">
                <div class="footer-grid">
                    <div class="footer-info">
                        <h3>${salon.tradeName}</h3>
                        <p>${salon.slogan || ''}</p>
                    </div>
                    <div class="footer-contact">
                        <div class="contact-item">
                            <span>📍</span>
                            <p>${salon.fullAddress}</p>
                        </div>
                        <div class="contact-item">
                            <span>📞</span>
                            <a href="tel:${salon.comercialPhone}">${salon.comercialPhone}</a>
                        </div>
                        ${salon.socialMediaLink ? `
                        <div class="contact-item">
                            <span>📱</span>
                            <a href="${salon.socialMediaLink}" target="_blank" rel="noopener noreferrer">Redes Sociais</a>
                        </div>
                        ` : ''}
                    </div>
                </div>
                <div class="footer-bottom">
                    <p>&copy; ${new Date().getFullYear()} ${salon.tradeName}. Todos os direitos reservados.</p>
                </div>
            </div>
        `;
    }
};
