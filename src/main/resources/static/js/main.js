window._originalFetch = window.fetch;
window.fetch = async (url, options = {}) => {
    options.headers = options.headers || {};
    const token = Auth.getToken();
    if (token && !options.headers['Authorization']) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }
    const method = (options.method || 'GET').toUpperCase();
    if (method !== 'GET' && !options.headers['X-XSRF-TOKEN']) {
        const getCookie = (name) => {
            const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
            if (match) return match[2];
        };
        const csrfToken = getCookie('XSRF-TOKEN');
        if (csrfToken) {
            options.headers['X-XSRF-TOKEN'] = csrfToken;
        }
    }
    options.credentials = 'include';
    let response = await window._originalFetch(url, options);
    const isAuthPath = typeof url === 'string' && url.includes('/api/v1/auth/');
    if (response.status === 401 && !isAuthPath) {
        const refreshed = await Auth.refreshToken();
        if (refreshed) {
            const newToken = Auth.getToken();
            options.headers['Authorization'] = `Bearer ${newToken}`;
            if (method !== 'GET') {
                const getCookie = (name) => {
                    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
                    if (match) return match[2];
                };
                const csrfToken = getCookie('XSRF-TOKEN');
                if (csrfToken) options.headers['X-XSRF-TOKEN'] = csrfToken;
            }
            response = await window._originalFetch(url, options);
        } else {
            return response;
        }
    }
    if (!response.ok) {
        if (response.status !== 401 || isAuthPath) {
            await ErrorHandler.handle(response.clone());
        }
    }
    return response;
};

window.navigate = (path) => App.navigate(path);

const App = {
    initialized: false,
    tenantError: false,
    salon: null,
    currentPath: null,

    init: async function() {
        if (this.initialized) return;
        this.initialized = true;
        await this.initTheme();
        if (this.tenantError) {
             const appContent = document.getElementById('app-content');
             if (appContent) {
                 appContent.innerHTML = '<div class="container" style="padding: 50px; text-align: center;"><h2>URL de acesso inválida</h2><p>Certifique-se de acessar pelo link correto do salão.</p></div>';
             }
             return;
        }
        this.checkAuth();
        await this.handleRouting();
        window.addEventListener('popstate', () => this.handleRouting());
    },

    navigate: function(path) {
        window.history.pushState({}, '', path);
        this.handleRouting();
    },

    handleRouting: async function() {
        if (this.tenantError) return;
        const path = window.location.pathname;
        if (this.currentPath === path) return;
        this.currentPath = path;
        const appContent = document.getElementById('app-content');
        if (!appContent) return;
        if (path.startsWith('/admin') || path.startsWith('/perfil') || path.startsWith('/profissional')) {
            if (!Auth.getToken()) {
                this.navigate('/entrar');
                return;
            }
            if (path.startsWith('/admin') && !Auth.hasRole('ADMIN')) {
                this.navigate('/agendar');
                return;
            }
            if (path.startsWith('/profissional') && !Auth.hasRole('PROFESSIONAL')) {
                this.navigate('/agendar');
                return;
            }
        }
        let templatePath = '';
        let scriptPath = '';
        let isModule = false;
        let pageTitle = 'Agendamento';
        if (path === '/entrar') {
            templatePath = '/pages/public/login.html';
            scriptPath = '/js/pages/login.js';
            pageTitle = 'Entrar';
        } else if (path === '/cadastro') {
            templatePath = '/pages/public/register.html';
            scriptPath = '/js/pages/register.js';
            pageTitle = 'Cadastro';
        } else if (path === '/admin/configuracoes') {
            templatePath = '/pages/admin/settings.html';
            scriptPath = '/js/pages/admin/settings.js';
            isModule = true;
            pageTitle = 'Configurações';
        } else if (path === '/admin/servicos') {
            templatePath = '/pages/admin/services.html';
            scriptPath = '/js/pages/admin/services.js';
            pageTitle = 'Serviços';
        } else if (path === '/' || path === '/agendar') {
            templatePath = '/pages/booking/index.html';
            scriptPath = '/js/pages/booking.js';
            pageTitle = 'Agendar';
        } else if (path === '/perfil') {
            templatePath = '/pages/public/profile.html';
            scriptPath = '/js/pages/profile.js';
            pageTitle = 'Meu Perfil';
        } else if (path === '/profissional/agenda') {
            templatePath = '/pages/professional/schedule.html';
            scriptPath = '/js/pages/professional/schedule.js';
            isModule = true;
            pageTitle = 'Minha Agenda';
        } else {
            templatePath = '/pages/booking/index.html';
            scriptPath = '/js/pages/booking.js';
        }
        if (templatePath) {
            document.title = this.salon ? `${this.salon.tradeName} - ${pageTitle}` : pageTitle;
            appContent.innerHTML = '<div class="container" style="text-align: center; padding: 50px;"><p>Carregando...</p></div>';
            try {
                const res = await fetch(templatePath);
                if (res.ok) {
                    const html = await res.text();
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    const snippetContent = doc.querySelector('main') || doc.body;
                    appContent.innerHTML = snippetContent.innerHTML;
                    const styles = doc.querySelectorAll('link[rel="stylesheet"]');
                    styles.forEach(s => {
                        const href = s.getAttribute('href');
                        if (!document.querySelector(`link[href="${href}"]`)) {
                            const newLink = document.createElement('link');
                            newLink.rel = 'stylesheet';
                            newLink.href = href;
                            document.head.appendChild(newLink);
                        }
                    });
                    this.applyBranding();
                    if (scriptPath) {
                        await this.loadScript(scriptPath, isModule);
                        this.initPage(path);
                    }
                } else if (res.status === 400) {
                     this.tenantError = true;
                     appContent.innerHTML = '<div class="container" style="padding: 50px; text-align: center;"><h2>URL de acesso inválida</h2><p>Certifique-se de acessar pelo link correto do salão.</p></div>';
                }
            } catch (err) {
                appContent.innerHTML = '<div class="container" style="padding: 50px;">Erro ao carregar página.</div>';
            }
        }
    },

    initPage: function(path) {
        if (path === '/entrar' && typeof initLogin === 'function') initLogin();
        if (path === '/cadastro' && typeof initRegister === 'function') initRegister();
        if (path === '/admin/configuracoes' && typeof adminSettingsApp !== 'undefined') adminSettingsApp.init();
        if (path === '/admin/servicos' && typeof adminServicesApp !== 'undefined') adminServicesApp.init();
        if ((path === '/' || path === '/agendar') && typeof bookingApp !== 'undefined') bookingApp.init();
        if (path === '/perfil' && typeof initProfile === 'function') initProfile();
        if (path === '/profissional/agenda' && typeof professionalScheduleApp !== 'undefined') professionalScheduleApp.init();
    },

    loadScript: function(src, isModule = false) {
        return new Promise((resolve, reject) => {
            const existing = document.querySelector(`script[src="${src}"]`);
            if (existing) {
                 resolve();
                 return;
            }
            const script = document.createElement('script');
            script.src = src;
            script.async = true;
            if (isModule) {
                script.type = 'module';
            }
            script.onload = resolve;
            script.onerror = reject;
            document.body.appendChild(script);
        });
    },

    initTheme: async function() {
        try {
            const res = await fetch('/api/v1/salon/profile');
            if (res.ok) {
                this.salon = await res.json();
                if (this.salon.primaryColor) {
                    document.documentElement.style.setProperty('--primary', this.salon.primaryColor);
                }
                UI.renderGlobalHeader(this.salon);
                UI.renderGlobalFooter(this.salon);
            } else if (res.status === 400) {
                this.tenantError = true;
            }
        } catch (e) {
        }
    },

    applyBranding: function() {
        if (!this.salon) return;
        document.querySelectorAll('[data-salon-field]').forEach(el => {
            const field = el.getAttribute('data-salon-field');
            if (this.salon[field]) {
                el.innerText = this.salon[field];
            }
        });
    },

    checkAuth: function() {
        const payload = Auth.getPayload();
        if (payload && payload.isFirstLogin && window.location.pathname !== '/entrar') {
            this.showFirstLoginModal();
        }
    },

    showFirstLoginModal: function() {
        if (document.getElementById('first-login-modal')) return;
        const modalHtml = `
            <div id="first-login-modal" class="modal-overlay">
                <div class="modal-content fade-in" style="max-width: 400px;">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <span style="font-size: 40px;">🔒</span>
                        <h2 style="margin-top: 10px; color: var(--text-main);">Primeiro Acesso</h2>
                        <p style="font-size: 14px; color: var(--text-muted);">Para sua segurança, você deve alterar sua senha inicial para continuar.</p>
                    </div>
                    <form id="first-login-form">
                        <div class="form-group">
                            <label class="form-label">Nova Senha</label>
                            <input type="password" id="new-password" class="form-input" 
                                   placeholder="Mínimo 6 caracteres" required minlength="6">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Confirmar Nova Senha</label>
                            <input type="password" id="confirm-password" class="form-input" 
                                   placeholder="Repita a nova senha" required minlength="6">
                        </div>
                        <button type="submit" id="btn-change-pass" class="btn btn-primary btn-block">
                            Definir Nova Senha
                        </button>
                    </form>
                </div>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const form = document.getElementById('first-login-form');
        const btn = document.getElementById('btn-change-pass');
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const newPassword = document.getElementById('new-password').value;
            const confirmPassword = document.getElementById('confirm-password').value;
            if (newPassword !== confirmPassword) {
                Toast.error('As senhas não coincidem.');
                return;
            }
            btn.disabled = true;
            btn.innerText = 'Processando...';
            try {
                const res = await fetch('/api/v1/user/change-password', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'text/plain',
                        'Authorization': `Bearer ${Auth.getToken()}`
                    },
                    body: newPassword
                });
                if (res.ok) {
                    Toast.success('Senha alterada com sucesso! Faça login novamente.');
                    setTimeout(() => Auth.logout(), 2000);
                } else {
                    const err = await res.json();
                    Toast.error(err.messages?.[0] || 'Erro ao alterar senha.');
                }
            } catch (err) {
                Toast.error('Erro de conexão ao alterar senha.');
            } finally {
                btn.disabled = false;
                btn.innerText = 'Definir Nova Senha';
            }
        });
    }
};
document.addEventListener('DOMContentLoaded', () => App.init());
