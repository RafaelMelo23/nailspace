const Auth = {
    tokenKey: 'nails_pro_token',
    refreshPromise: null,

    getToken: function() {
        return localStorage.getItem(this.tokenKey);
    },

    setToken: function(token) {
        localStorage.setItem(this.tokenKey, token);
        document.cookie = `access_token=${token}; path=/; max-age=600; SameSite=Lax; Secure`;
    },

    clearToken: function() {
        localStorage.removeItem(this.tokenKey);
        document.cookie = "access_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
    },

    getPayload: function() {
        const token = this.getToken();
        if (!token) return null;
        try {
            return JSON.parse(atob(token.split('.')[1]));
        } catch (e) {
            return null;
        }
    },

    getUserId: function() {
        const payload = this.getPayload();
        return payload ? payload.sub : null;
    },

    hasRole: function(role) {
        const payload = this.getPayload();
        if (!payload || !payload.roles) return false;
        return payload.roles.includes(role);
    },

    refreshToken: async function() {
        if (this.refreshPromise) {
            return this.refreshPromise;
        }

        this.refreshPromise = (async () => {
            const originalFetch = window._originalFetch || window.fetch;
            try {
                const res = await originalFetch('/api/v1/auth/refresh', {
                    method: 'POST',
                    credentials: 'include'
                });

                if (res.ok) {
                    const token = await res.text();
                    this.setToken(token);
                    console.log('Token refreshed successfully');
                    return true;
                }
            } catch (e) {
                console.error('Refresh token error:', e);
            } finally {
                this.refreshPromise = null;
            }

            this.logout();
            return false;
        })();

        return this.refreshPromise;
    },

    logout: function() {
        this.clearToken();
        window.location.href = '/entrar';
    }
};
