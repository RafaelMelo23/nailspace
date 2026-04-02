const profileApp = {
    user: null,
    appointments: [],
    appointmentToCancel: null,

    setupTabs: function() {
        const tabs = {
            'tab-upcoming': { section: 'section-appointments', list: 'list-upcoming' },
            'tab-history': { section: 'section-appointments', list: 'list-history' },
            'tab-settings': { section: 'section-settings' }
        };

        Object.keys(tabs).forEach(tabId => {
            document.getElementById(tabId).addEventListener('click', (e) => {
                document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
                e.target.classList.add('active');

                document.getElementById('section-appointments').classList.add('hidden');
                document.getElementById('section-settings').classList.add('hidden');

                const config = tabs[tabId];
                document.getElementById(config.section).classList.remove('hidden');

                if (config.list) {
                    document.getElementById('list-upcoming').classList.add('hidden');
                    document.getElementById('list-history').classList.add('hidden');
                    document.getElementById(config.list).classList.remove('hidden');
                }
            });
        });
    },

    loadProfile: async function() {
        try {
            const res = await fetch('/api/v1/user', {
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });

            if (res.ok) {
                this.user = await res.json();
                this.renderProfile();
            }
        } catch (error) {
            console.error('Error loading profile:', error);
        }
    },

    loadAppointments: async function() {
        try {
            const res = await fetch('/api/v1/client/appointments?size=50', {
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });

            if (res.ok) {
                const data = await res.json();
                this.appointments = data.content;
                this.renderAppointments();
            }
        } catch (error) {
            console.error('Error loading appointments:', error);
        }
    },

    renderProfile: function() {
        if (!this.user) return;

        document.getElementById('user-name').innerText = this.user.fullName.split(' ')[0];
        document.getElementById('set-name').innerText = this.user.fullName;
        document.getElementById('set-email').innerText = this.user.email;
        document.getElementById('set-phone').innerText = this.user.phoneNumber || 'Não informado';

        if (Auth.hasRole('PROFESSIONAL')) {
            const picContainer = document.getElementById('prof-pic-container');
            const picEl = document.getElementById('prof-pic');
            
            if (picContainer && picEl) {
                picContainer.classList.remove('hidden');
                if (this.user.professionalPicture) {
                    picEl.src = this.user.professionalPicture;
                } else {
                    picEl.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(this.user.fullName)}&background=FB7185&color=fff`;
                }
            }
        }
    },

    triggerPicUpload: function() {
        document.getElementById('pic-upload-input').click();
    },

    handlePicUpload: async function(event) {
        const file = event.target.files[0];
        if (!file) return;

        if (!file.type.startsWith('image/')) {
            Toast.error('Por favor, selecione uma imagem.');
            return;
        }

        if (file.size > 2 * 1024 * 1024) {
            Toast.error('A imagem deve ter no máximo 2MB.');
            return;
        }

        const reader = new FileReader();
        reader.onload = async (e) => {
            const base64 = e.target.result;
            await this.uploadPicture(base64);
        };
        reader.readAsDataURL(file);
    },

    uploadPicture: async function(base64) {
        const picEl = document.getElementById('prof-pic');
        const oldSrc = picEl ? picEl.src : '';
        
        if (picEl) picEl.style.opacity = '0.5';

        try {
            const res = await fetch('/api/v1/professional/profile/picture', {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${Auth.getToken()}`
                },
                body: JSON.stringify({ pictureBase64: base64 })
            });

            if (res.ok) {
                Toast.success('Foto de perfil atualizada!');
                await this.loadProfile();
            } else {
                if (picEl) picEl.src = oldSrc;
                const err = await res.json();
                Toast.error(err.message || 'Erro ao atualizar foto.');
            }
        } catch (error) {
            if (picEl) picEl.src = oldSrc;
            Toast.error('Erro de conexão.');
        } finally {
            if (picEl) picEl.style.opacity = '1';
        }
    },

    renderAppointments: function() {
        const listUpcoming = document.getElementById('list-upcoming');
        const listHistory = document.getElementById('list-history');

        const now = new Date();
        const upcoming = this.appointments.filter(a => new Date(a.startDate) >= now && a.status !== 'CANCELLED');
        const history = this.appointments.filter(a => new Date(a.startDate) < now || a.status === 'CANCELLED');

        listUpcoming.innerHTML = upcoming.length ? upcoming.map(a => this.createApptCard(a)).join('') : '<div class="empty-state">Você não tem agendamentos próximos.</div>';
        listHistory.innerHTML = history.length ? history.map(a => this.createApptCard(a)).join('') : '<div class="empty-state">Nenhum histórico encontrado.</div>';
    },

    createApptCard: function(a) {
        const start = new Date(a.startDate);
        const day = start.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' });
        const time = start.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
        
        const statusMap = {
            'CONFIRMED': { label: 'Confirmado', class: 'status-confirmed' },
            'PENDING': { label: 'Pendente', class: 'status-pending' },
            'CANCELLED': { label: 'Cancelado', class: 'status-cancelled' },
            'FINISHED': { label: 'Concluído', class: 'status-done' },
            'MISSED': { label: 'Faltou', class: 'status-cancelled' }
        };

        const status = statusMap[a.status] || { label: a.status, class: '' };
        const canCancel = a.status !== 'CANCELLED' && a.status !== 'FINISHED' && a.status !== 'MISSED' && start > new Date();

        return `
            <div class="appt-card">
                <div class="appt-header">
                    <div class="appt-date">
                        <span class="date-day">${day}</span>
                        <span class="date-time">${time}</span>
                    </div>
                    <span class="status-badge ${status.class}">${status.label}</span>
                </div>
                <div class="appt-body">
                    <h3>${a.mainServiceName}</h3>
                    <p>Profissional: ${a.professionalName}</p>
                    ${a.addOns && a.addOns.length ? `<p style="margin-top:4px; font-size:11px;">+ ${a.addOns.length} adicionais</p>` : ''}
                </div>
                ${canCancel ? `
                    <div class="appt-actions">
                        <button class="btn-sm btn-cancel-appt" onclick="profileApp.openCancelModal(${a.id})">Cancelar</button>
                    </div>
                ` : ''}
            </div>
        `;
    },

    openCancelModal: function(id) {
        this.appointmentToCancel = id;
        document.getElementById('modal-cancel').classList.remove('hidden');
    },

    closeCancelModal: function() {
        this.appointmentToCancel = null;
        document.getElementById('modal-cancel').classList.add('hidden');
    },

    confirmCancel: async function() {
        if (!this.appointmentToCancel) return;
        const id = this.appointmentToCancel;
        const btn = document.getElementById('btn-confirm-cancel');
        
        btn.disabled = true;
        btn.innerText = 'Cancelando...';

        try {
            const res = await fetch(`/api/v1/booking/${id}`, {
                method: 'PATCH',
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });

            if (res.ok) {
                Toast.success('Agendamento cancelado com sucesso.');
                this.closeCancelModal();
                await this.loadAppointments();
            } else {
                const err = await res.json();
                Toast.error(err.message || 'Erro ao cancelar agendamento.');
            }
        } catch (error) {
            Toast.error('Erro de conexão.');
        } finally {
            btn.disabled = false;
            btn.innerText = 'Sim, Cancelar';
        }
    },

    showEdit: function(type) {
        const overlay = document.getElementById('modal-overlay');
        const title = document.getElementById('modal-title');
        const editType = document.getElementById('edit-type');
        
        editType.value = type;
        overlay.classList.remove('hidden');

        document.getElementById('email-fields').classList.add('hidden');
        document.getElementById('phone-fields').classList.add('hidden');
        document.getElementById('password-fields').classList.add('hidden');
        document.getElementById('password-confirm-field').classList.remove('hidden');
        document.getElementById('btn-save').classList.remove('hidden');

        if (type === 'email') {
            title.innerText = 'Alterar E-mail';
            document.getElementById('email-fields').classList.remove('hidden');
            document.getElementById('new-email').value = this.user.email;
        } else if (type === 'phone') {
            title.innerText = 'Alterar Telefone';
            document.getElementById('phone-fields').classList.remove('hidden');
            document.getElementById('new-phone').value = this.user.phoneNumber || '';
        } else if (type === 'password') {
            title.innerText = 'Alterar Senha';
            document.getElementById('password-fields').classList.remove('hidden');
            document.getElementById('password-confirm-field').classList.add('hidden');
            document.getElementById('btn-save').innerText = 'Enviar Link de Recuperação';
        }
    },

    closeModal: function() {
        document.getElementById('modal-overlay').classList.add('hidden');
        document.getElementById('edit-form').reset();
        document.getElementById('btn-save').innerText = 'Salvar';
    },

    handleUpdate: async function(e) {
        e.preventDefault();
        const type = document.getElementById('edit-type').value;
        const password = document.getElementById('confirm-pass').value;
        const btn = document.getElementById('btn-save');

        if (type !== 'password' && !password) {
            Toast.error('Por favor, confirme sua senha atual.');
            return;
        }

        btn.disabled = true;
        btn.innerText = 'Processando...';

        try {
            let url = '';
            let body = {};

            if (type === 'email') {
                url = '/api/v1/user/email';
                body = { newEmail: document.getElementById('new-email').value, password };
            } else if (type === 'phone') {
                url = '/api/v1/user/phone';
                body = { newPhone: document.getElementById('new-phone').value, password };
            } else if (type === 'password') {
                url = `/api/v1/user/password/forgot?userEmail=${encodeURIComponent(this.user.email)}`;
                const res = await fetch(url, {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
                });
                if (res.ok) {
                    Toast.success('Link de recuperação enviado para seu e-mail.');
                    this.closeModal();
                } else {
                    Toast.error('Erro ao solicitar recuperação de senha.');
                }
                return;
            }

            const res = await fetch(url, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${Auth.getToken()}`
                },
                body: JSON.stringify(body)
            });

            if (res.ok) {
                Toast.success('Dados atualizados com sucesso.');
                this.closeModal();
                await this.loadProfile();
            } else {
                const err = await res.json();
                Toast.error(err.message || 'Erro ao atualizar dados.');
            }
        } catch (error) {
            Toast.error('Erro de conexão.');
        } finally {
            btn.disabled = false;
            btn.innerText = 'Salvar';
        }
    }
};

function initProfile() {
    const el = document.getElementById('user-name');
    if (!el) return;

    if (!Auth.getToken()) {
        App.navigate('/entrar');
        return;
    }

    profileApp.setupTabs();
    Promise.all([
        profileApp.loadProfile(),
        profileApp.loadAppointments()
    ]);

    const confirmBtn = document.getElementById('btn-confirm-cancel');
    if (confirmBtn) {
        confirmBtn.addEventListener('click', () => profileApp.confirmCancel());
    }
}
