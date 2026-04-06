export const SalonModule = {
    loadSalonProfile: async function() {
        try {
            const res = await fetch('/api/v1/admin/salon/profile');
            if (res.ok) {
                const salon = await res.json();
                const form = document.getElementById('salon-profile-form');
                if (!form) return;

                const setVal = (name, val) => {
                    const el = form.querySelector(`[name="${name}"]`);
                    if (el) el.value = val || '';
                };

                setVal('tradeName', salon.tradeName);
                setVal('slogan', salon.slogan);
                setVal('comercialPhone', salon.comercialPhone);
                setVal('fullAddress', salon.fullAddress);
                setVal('socialMediaLink', salon.socialMediaLink);
                setVal('zoneId', salon.zoneId || 'America/Sao_Paulo');
                setVal('appointmentBufferMinutes', salon.appointmentBufferMinutes);
                setVal('standardBookingWindow', salon.standardBookingWindow || 30);
                setVal('warningMessage', salon.warningMessage);
                setVal('loyalClientBookingWindowDays', salon.loyalClientBookingWindowDays || 60);

                const color = salon.primaryColor || '#E91E63';
                const colorPicker = form.querySelector('[name="primaryColor"]');
                if (colorPicker) colorPicker.value = color;
                const colorText = form.querySelector('.color-text');
                if (colorText) colorText.value = color.toUpperCase();

                const status = salon.status || 'OPEN';
                const statusEl = form.querySelector('[name="status"]');
                if (statusEl) {
                    statusEl.value = status;
                    this.handleStatusChange(status);
                }

                const loyalCheckbox = form.querySelector('[name="isLoyalClientelePrioritized"]');
                if (loyalCheckbox) {
                    loyalCheckbox.checked = !!salon.isLoyalClientelePrioritized;
                    this.toggleLoyalWindow(loyalCheckbox.checked);
                }

                if (typeof NotificationService !== 'undefined' && salon.connectionState) {
                    NotificationService.updateStatusUI(salon.connectionState);
                }
            }
        } catch (e) {
        }
    },

    handleStatusChange: function(value) {
        const group = document.getElementById('warning-message-group');
        if (!group) return;
        if (value === 'CLOSED_TEMPORARY') {
            group.classList.remove('hidden');
        } else {
            group.classList.add('hidden');
        }
    },

    toggleLoyalWindow: function(checked) {
        const group = document.getElementById('loyal-window-group');
        if (!group) return;
        if (checked) {
            group.classList.remove('hidden');
        } else {
            group.classList.add('hidden');
        }
    },

    handleSaveProfile: async function(event) {
        event.preventDefault();
        const form = event.target;
        const btn = form.querySelector('button[type="submit"]');
        const formData = new FormData(form);
        const data = Object.fromEntries(formData.entries());

        data.appointmentBufferMinutes = parseInt(data.appointmentBufferMinutes) || 0;
        data.standardBookingWindow = parseInt(data.standardBookingWindow) || 30;
        const loyalCheckbox = form.querySelector('#isLoyalClientelePrioritized');
        data.isLoyalClientelePrioritized = loyalCheckbox ? loyalCheckbox.checked : false;
        data.loyalClientBookingWindowDays = parseInt(data.loyalClientBookingWindowDays) || 60;

        this.setLoading(btn, true);
        try {
            const response = await fetch('/api/v1/admin/salon/profile', {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                Toast.success('Configurações salvas com sucesso!');
                await this.loadSalonProfile();
                if (typeof App !== 'undefined' && App.initTheme) {
                    await App.initTheme();
                }
            } else {
                const err = await response.json();
                Toast.error(err.message || 'Erro ao salvar configurações.');
            }
        } catch (e) {
            Toast.error('Erro de conexão ao salvar.');
        } finally {
            this.setLoading(btn, false);
        }
    },

    setupColorPicker: function() {
        const picker = document.querySelector('input[name="primaryColor"]');
        const text = document.querySelector('.color-text');
        if (picker && text) {
            picker.addEventListener('input', (e) => {
                text.value = e.target.value.toUpperCase();
            });
        }
    }
};