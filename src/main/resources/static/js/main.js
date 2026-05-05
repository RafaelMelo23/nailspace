const RESERVED_PATHS = new Set([
    'api', 'js', 'css', 'assets', 'pages', 'favicon.svg', 'favicon.ico', 'error', 'uploads', 'public', 'swagger-ui', 'v3', 'agendar', 'entrar', 'cadastro', 'perfil', 'offline', 'redefinir-senha', 'admin', 'profissional'
]);

const getTenantIdFromUrl = () => {
    const pathParts = window.location.pathname.split('/');
    const firstPart = pathParts[1];
    if (!firstPart || RESERVED_PATHS.has(firstPart.toLowerCase())) {
        return null;
    }
    return firstPart;
};

window._originalFetch = window.fetch;
window.fetch = async (url, options = {}) => {
    options.headers = options.headers || {};
    
    let token = Auth.getToken();
    const isAuthPath = typeof url === 'string' && (url.includes('/api/v1/auth/') || url.includes('/api/v1/webhook'));
    

    if (token && Auth.isTokenExpired() && !isAuthPath) {
        await Auth.refreshToken();
        token = Auth.getToken();
    }

    const tenantId = App.getTenantId();
    if (tenantId && !options.headers['X-Tenant-Id']) {
        options.headers['X-Tenant-Id'] = tenantId;
    }

    if (token && !options.headers['Authorization']) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }
    const method = (options.method || 'GET').toUpperCase();
    
    if (method === 'DELETE' && Auth.isDemo()) {
        UI.showToast('Ação bloqueada: Exclusões não são permitidas no modo demonstração.', 'error');
        return new Response(JSON.stringify({ message: 'Exclusão não permitida no modo demo.' }), {
            status: 403,
            headers: { 'Content-Type': 'application/json' }
        });
    }

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

    if (response.headers.get('X-Salon-State') === 'CLOSED' && window.location.pathname !== '/offline') {
        App.navigate('/offline');
    }

    if ((response.status === 401 || response.status === 403) && !isAuthPath) {
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
            if (method !== 'GET' || response.status === 401) {
                Auth.logout();
            }
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
    activeTenant: null,
    salon: null,
    currentPath: null,
    templateCache: new Map(),

    init: async function() {
        if (this.initialized) return;
        this.initialized = true;
        
        this.activeTenant = this.getTenantId();

        if (Auth.getToken() && Auth.isTokenExpired()) {
            await Auth.refreshToken();
        }

        const themePromise = this.initTheme();
        const routingPromise = this.handleRouting(true);
        
        await Promise.all([themePromise, routingPromise]);

        if (this.tenantError) {
             const appContent = document.getElementById('app-content');
             if (appContent) {
                 appContent.innerHTML = '<div class="container" style="padding: 50px; text-align: center;"><h2>URL de acesso inválida</h2><p>Certifique-se de acessar pelo link correto do salão.</p></div>';
             }
             return;
        }
        this.checkAuth();
        if (typeof NotificationService !== 'undefined') {
            NotificationService.init();
        }
        window.addEventListener('popstate', () => this.handleRouting());
    },

    getTenantId: function() {
        if (this.activeTenant) return this.activeTenant;

        const urlTenant = getTenantIdFromUrl();
        if (urlTenant) {
            this.activeTenant = urlTenant;
            return urlTenant;
        }

        const tokenTenant = Auth.getTenantId();
        if (tokenTenant) {
            this.activeTenant = tokenTenant;
            return tokenTenant;
        }

        return null;
    },

    navigate: function(path) {
        const tenantId = this.getTenantId();
        let fullPath;
        if (tenantId && !path.startsWith(`/${tenantId}/`) && path !== `/${tenantId}`) {
            const cleanPath = path.startsWith('/') ? path : `/${path}`;
            fullPath = `/${tenantId}${cleanPath}`;
        } else {
            fullPath = path;
        }
        window.history.pushState({}, '', fullPath);
        this.handleRouting();
    },

    handleRouting: async function(isInitial = false) {
        if (this.tenantError) return;
        const tenantId = this.getTenantId();
        let path = window.location.pathname;

        if (tenantId && (path === `/${tenantId}` || path.startsWith(`/${tenantId}/`))) {
            path = path.substring(tenantId.length + 1) || '/';
            if (!path.startsWith('/')) path = '/' + path;
        }

        const fullPath = window.location.pathname + window.location.search;
        if (this.currentPath === fullPath) return;
        this.currentPath = fullPath;


        if (path === '/') {
            const redirectPath = tenantId ? `/${tenantId}/agendar` : '/agendar';
            window.history.replaceState({}, '', redirectPath);
            this.handleRouting();
            return;
        }

        if (this.salon && (isInitial || Auth.getToken())) {
            UI.renderGlobalHeader(this.salon);
        }

        const appContent = document.getElementById('app-content');
        if (!appContent) return;

        if (path.startsWith('/admin') || path.startsWith('/perfil') || path.startsWith('/profissional')) {
            if (!Auth.getToken()) {
                this.navigate(`/entrar?redirect=${path}`);
                return;
            }
            if (Auth.isTokenExpired()) {
                const refreshed = await Auth.refreshToken();
                if (!refreshed) return;
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

        const routeMap = {
            '/entrar': { template: '/pages/public/login.html', script: '/js/pages/login.js', title: 'Entrar' },
            '/cadastro': { template: '/pages/public/register.html', script: '/js/pages/register.js', title: 'Cadastro' },
            '/admin/configuracoes': { template: '/pages/admin/settings.html', script: '/js/pages/admin/settings.js', title: 'Configurações', isModule: true },
            '/admin/servicos': { template: '/pages/admin/services.html', script: '/js/pages/admin/services.js', title: 'Serviços' },
            '/': { template: '/pages/booking/index.html', script: '/js/pages/booking.js', title: 'Agendar' },
            '/agendar': { template: '/pages/booking/index.html', script: '/js/pages/booking.js', title: 'Agendar' },
            '/perfil': { template: '/pages/public/profile.html', script: '/js/pages/profile.js', title: 'Meu Perfil' },
            '/profissional/agenda': { template: '/pages/professional/schedule.html', script: '/js/pages/professional/schedule.js', title: 'Minha Agenda', isModule: true },
            '/offline': { template: '/pages/public/offline.html', script: '/js/pages/offline.js', title: 'Indisponível' }
        };

        const route = routeMap[path] || (path.startsWith('/perfil') ? routeMap['/perfil'] : routeMap['/']);
        
        templatePath = route.template;
        scriptPath = route.script;
        isModule = !!route.isModule;
        pageTitle = route.title;

        if (templatePath) {
            document.title = this.salon ? `${this.salon.tradeName} - ${pageTitle}` : pageTitle;

            if (!this.templateCache.has(templatePath) && !isInitial) {
                 appContent.innerHTML = '<div class="container" style="text-align: center; padding: 50px;"><div class="skeleton-title skeleton"></div><div class="skeleton" style="height: 200px;"></div></div>';
            }

            try {
                let html;
                if (this.templateCache.has(templatePath)) {
                    html = this.templateCache.get(templatePath);
                } else {
                    const res = await fetch(templatePath);
                    if (res.ok) {
                        html = await res.text();
                        this.templateCache.set(templatePath, html);
                    } else if (res.status === 400) {
                        this.tenantError = true;
                        appContent.innerHTML = '<div class="container" style="padding: 50px; text-align: center;"><h2>URL de acesso inválida</h2><p>Certifique-se de acessar pelo link correto do salão.</p></div>';
                        return;
                    }
                }

                if (html) {
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    const snippetContent = doc.querySelector('main') || doc.body;

                    const styles = doc.querySelectorAll('link[rel="stylesheet"]');
                    const stylePromises = Array.from(styles).map(s => {
                        const href = s.getAttribute('href');
                        if (!document.querySelector(`link[href="${href}"]`)) {
                            return new Promise(resolve => {
                                const newLink = document.createElement('link');
                                newLink.rel = 'stylesheet';
                                newLink.href = href;
                                newLink.setAttribute('data-page-style', 'true');
                                newLink.onload = resolve;
                                newLink.onerror = resolve;
                                document.head.appendChild(newLink);
                            });
                        }
                        return Promise.resolve();
                    });

                    await Promise.all(stylePromises);

                    await new Promise(resolve => {
                        requestAnimationFrame(() => {
                            const currentHrefs = Array.from(styles).map(s => s.getAttribute('href'));
                            document.querySelectorAll('link[data-page-style="true"]').forEach(el => {
                                if (!currentHrefs.includes(el.getAttribute('href'))) el.remove();
                            });

                            appContent.innerHTML = snippetContent.innerHTML;
                            this.applyBranding();
                            requestAnimationFrame(() => resolve());
                        });
                    });

                    if (scriptPath) {
                        await this.loadScript(scriptPath, isModule);
                        this.initPage(path);
                    }
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
        if (path.startsWith('/perfil') && typeof initProfile === 'function') initProfile();
        if (path === '/profissional/agenda' && typeof professionalScheduleApp !== 'undefined') professionalScheduleApp.init();
        if (path === '/offline' && typeof initOffline === 'function') initOffline();
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
            script.defer = true;
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
                <div class="modal-content modal-sm fade-in">
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
                const payload = Auth.getPayload();
                const res = await fetch('/api/v1/user/password', {
                    method: 'PATCH',
                    headers: { 
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${Auth.getToken()}`
                    },
                    body: JSON.stringify({
                        email: payload.email,
                        newPassword: newPassword
                    })
                });
                if (res.ok) {
                    Toast.success('Senha alterada com sucesso! Faça login novamente.');
                    setTimeout(() => Auth.logout(), 2000);
                } else {
                    const err = await res.json();
                    Toast.error(err.message || err.messages?.[0] || 'Erro ao alterar senha.');
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
