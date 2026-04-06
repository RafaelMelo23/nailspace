
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

    confirm: function(title, message) {
        return new Promise((resolve) => {
            const modal = document.getElementById('confirm-modal');
            const titleEl = document.getElementById('confirm-modal-title');
            const msgEl = document.getElementById('confirm-modal-message');
            const okBtn = document.getElementById('confirm-modal-ok');
            const cancelBtn = document.getElementById('confirm-modal-cancel');

            if (!modal) {
                console.warn('Confirm modal not found, falling back to window.confirm');
                resolve(window.confirm(message));
                return;
            }

            titleEl.textContent = title || 'Confirmar Ação';
            msgEl.textContent = message;
            modal.classList.remove('hidden');

            const handleClose = (result) => {
                modal.classList.add('hidden');
                okBtn.onclick = null;
                cancelBtn.onclick = null;
                resolve(result);
            };

            okBtn.onclick = () => handleClose(true);
            cancelBtn.onclick = () => handleClose(false);

            modal.onclick = (e) => {
                if (e.target === modal) handleClose(false);
            };
        });
    },

    renderGlobalHeader: function(salon) {
        const header = document.getElementById('main-header');
        if (!header || !salon) return;

        const token = Auth.getToken();
        const isAdmin = Auth.hasRole('ADMIN');
        const isProfessional = Auth.hasRole('PROFESSIONAL');

        header.innerHTML = `
            <div class="container">
                <nav class="main-nav">
                    <div class="nav-left">
                        <a href="/" class="brand-link" onclick="navigate('/'); return false;">
                            <span class="brand-name">${salon.tradeName}</span>
                        </a>
                    </div>
                    
                    <button class="menu-toggle" aria-label="Abrir menu" onclick="UI.toggleMobileMenu()">
                        <span></span>
                        <span></span>
                        <span></span>
                    </button>

                    <div class="nav-links" id="nav-links">
                        ${token ? `
                            <a href="/perfil" class="nav-link" onclick="UI.closeMobileMenu(); navigate('/perfil'); return false;">Meu Perfil</a>
                            ${isProfessional ? `<a href="/profissional/agenda" class="nav-link" onclick="UI.closeMobileMenu(); navigate('/profissional/agenda'); return false;">Minha Agenda</a>` : ''}
                            ${isAdmin ? `
                                <a href="/admin/configuracoes" class="nav-link" onclick="UI.closeMobileMenu(); navigate('/admin/configuracoes'); return false;">Configurações</a>
                                <a href="/admin/servicos" class="nav-link" onclick="UI.closeMobileMenu(); navigate('/admin/servicos'); return false;">Serviços</a>
                            ` : ''}
                            <button class="btn-logout" onclick="UI.closeMobileMenu(); Auth.logout()">Sair</button>
                        ` : `
                            <a href="/entrar" class="nav-link" onclick="UI.closeMobileMenu(); navigate('/entrar'); return false;">Entrar</a>
                            <a href="/cadastro" class="btn btn-primary btn-sm" onclick="UI.closeMobileMenu(); navigate('/cadastro'); return false;">Cadastrar</a>
                        `}
                    </div>
                </nav>
            </div>
        `;
    },

    toggleMobileMenu: function() {
        const navLinks = document.getElementById('nav-links');
        const menuToggle = document.querySelector('.menu-toggle');
        if (navLinks) {
            const isActive = navLinks.classList.toggle('active');
            menuToggle.classList.toggle('active');
            document.body.style.overflow = isActive ? 'hidden' : '';
        }
    },

    closeMobileMenu: function() {
        const navLinks = document.getElementById('nav-links');
        const menuToggle = document.querySelector('.menu-toggle');
        if (navLinks && navLinks.classList.contains('active')) {
            navLinks.classList.remove('active');
            menuToggle.classList.remove('active');
            document.body.style.overflow = '';
        }
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
