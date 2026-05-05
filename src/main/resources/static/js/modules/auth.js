const Auth = {
    tokenKey: 'nails_pro_token',
    refreshPromise: null,

    getToken: function() {
        try {
            return localStorage.getItem(this.tokenKey);
        } catch (e) {
            console.error('Error accessing localStorage:', e);
            return null;
        }
    },

    setToken: function(token) {
        try {
            localStorage.setItem(this.tokenKey, token);
        } catch (e) {
            console.error('Error setting localStorage:', e);
        }
    },

    clearToken: function() {
        try {
            localStorage.removeItem(this.tokenKey);
        } catch (e) {
            console.error('Error clearing localStorage:', e);
        }
    },

    getPayload: function() {
        const token = this.getToken();
        if (!token) return null;
        try {
            const base64Url = token.split('.')[1];
            let base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');

            while (base64.length % 4) {
                base64 += '=';
            }
            
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            return JSON.parse(jsonPayload);
        } catch (e) {
            console.error('Error decoding token payload:', e);
            return null;
        }
    },

    getUserId: function() {
        const payload = this.getPayload();
        return payload ? payload.sub : null;
    },

    getUserRoles: function() {
        const payload = this.getPayload();
        if (!payload || !payload.roles) return [];
        return payload.roles;
    },

    hasRole: function(role) {
        return this.getUserRoles().includes(role);
    },

    isDemo: function() {
        const payload = this.getPayload();
        return payload && payload.tenantId === 'demo-salon-2026';
    },

    isTokenExpired: function() {
        const token = this.getToken();
        if (!token) return false;
        
        const payload = this.getPayload();
        if (!payload || !payload.exp) return true;
        
        const now = Math.floor(Date.now() / 1000);
        return payload.exp < (now + 30);
    },

    getTenantId: function() {
        const RESERVED = new Set([
            'api', 'js', 'css', 'assets', 'pages', 'favicon.svg', 'favicon.ico', 'error', 'uploads', 'public', 'swagger-ui', 'v3', 'agendar', 'entrar', 'cadastro', 'perfil', 'offline', 'redefinir-senha', 'admin', 'profissional'
        ]);
        const pathParts = window.location.pathname.split('/');
        const firstPart = pathParts[1];
        if (firstPart && !RESERVED.has(firstPart.toLowerCase())) {
            return firstPart;
        }
        const payload = this.getPayload();
        if (payload && payload.tenantId) return payload.tenantId;
        
        return null;
    },

    refreshToken: async function() {
        if (this.refreshPromise) {
            return this.refreshPromise;
        }

        const token = this.getToken();
        if (!token) return false;

        this.refreshPromise = (async () => {
            try {
                const originalFetch = window._originalFetch || window.fetch;
                const tenantId = this.getTenantId();
                const headers = { 'Content-Type': 'application/json' };

                if (tenantId) {
                    headers['X-Tenant-Id'] = tenantId;
                }

                const res = await originalFetch('/api/v1/auth/refresh', {
                    method: 'POST',
                    headers: headers,
                    credentials: 'include'
                });

                if (res.ok) {
                    const newToken = await res.text();
                    this.setToken(newToken);
                    return true;
                }
            } catch (e) {
                console.error('Refresh token error:', e);
            } finally {
                this.refreshPromise = null;
            }

            const path = window.location.pathname;
            const isProtected = path.includes('/admin') || path.includes('/perfil') || path.includes('/profissional');
            if (isProtected) {
                this.logout();
            }
            return false;
        })();

        return this.refreshPromise;
    },

    logout: async function() {
        if (typeof NotificationService !== 'undefined' && NotificationService.eventSource) {
            NotificationService.eventSource.close();
            NotificationService.eventSource = null;
        }

        const tenantId = this.getTenantId();
        const reserved = new Set(['entrar', 'cadastro', 'redefinir-senha', 'agendar', 'perfil', 'offline']);

        try {
            await fetch('/api/v1/auth/logout', { 
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
        } catch (e) {
            console.error('Logout error:', e);
        }
        
        this.clearToken();
        
        if (tenantId && !reserved.has(tenantId)) {
            window.location.href = `/${tenantId}/entrar`;
        } else {
            window.location.href = '/entrar';
        }
    }
};
