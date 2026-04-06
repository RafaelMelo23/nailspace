export const ProfessionalsModule = {
    loadProfessionals: async function() {
        const list = document.getElementById('professionals-list');
        if (!list) return;
        try {
            const response = await fetch('/api/v1/admin/professional');
            if (response.ok) {
                const professionals = await response.json();
                this.renderProfessionals(professionals);
            }
        } catch (error) {
            list.innerHTML = '<tr><td colspan="5" class="empty-state">Erro ao carregar profissionais.</td></tr>';
        }
    },

    renderProfessionals: function(professionals) {
        const list = document.getElementById('professionals-list');
        if (!list) return;
        if (professionals.length === 0) {
            list.innerHTML = '<tr><td colspan="6" class="empty-state">Nenhum profissional cadastrado.</td></tr>';
            return;
        }

        list.innerHTML = professionals.map(prof => `
            <tr>
                <td>
                    <div class="prof-cell">
                        <div class="prof-initials">${this.getInitials(prof.name)}</div>
                        <div>
                            <strong>${prof.name}</strong><br>
                            <span style="font-size: 0.8rem; color: #666;">${prof.isFirstLogin ? 'Pendente' : 'Profissional'}</span>
                        </div>
                    </div>
                </td>
                <td data-label="Email">${prof.email}</td>
                <td data-label="Serviços"><span class="tag">Todos</span></td>
                <td data-label="Gestão">
                    <div class="prof-btn-group">
                        <button class="btn btn-secondary btn-sm" onclick="adminSettingsApp.openSchedulesModal(${prof.id}, '${prof.name}')" title="Ver Horários">Horários</button>
                        <button class="btn btn-secondary btn-sm" onclick="adminSettingsApp.openBlocksModal(${prof.id}, '${prof.name}')" title="Ver Bloqueios">Bloqueios</button>
                    </div>
                </td>
                <td data-label="Status"><span class="badge ${prof.isActive ? 'badge-success' : 'badge-danger'}">${prof.isActive ? 'Ativo' : 'Inativo'}</span></td>
                <td data-label="Ação">
                    ${prof.isActive ?
                        `<button class="btn-outline-danger btn-sm" onclick="adminSettingsApp.handleDeactivateProfessional(${prof.id}, this)">Desativar</button>` :
                        `<button class="btn btn-secondary btn-sm" onclick="adminSettingsApp.handleActivateProfessional(${prof.id}, this)">Ativar</button>`}
                </td>
            </tr>
        `).join('');
    },

    openSchedulesModal: function(id, name) {
        document.getElementById('schedules-modal-title').textContent = `Horários: ${name}`;
        document.getElementById('schedules-modal').classList.remove('hidden');
        this.loadSchedules(id);
    },

    closeSchedulesModal: function() {
        document.getElementById('schedules-modal').classList.add('hidden');
    },

    loadSchedules: async function(id) {
        const container = document.getElementById('schedules-container');
        if (!container) return;
        try {
            const response = await fetch(`/api/v1/admin/professional/schedule/${id}`);
            if (response.ok) {
                const schedules = await response.json();
                this.renderSchedules(schedules);
            }
        } catch (error) {
            container.innerHTML = '<p class="empty-state">Erro ao carregar horários.</p>';
        }
    },

    renderSchedules: function(schedules) {
        const container = document.getElementById('schedules-container');
        if (!container) return;
        if (!schedules || schedules.length === 0) {
            container.innerHTML = '<p class="empty-state">Nenhum horário configurado.</p>';
            return;
        }

        const daysMap = {
            'MONDAY': 'Segunda', 'TUESDAY': 'Terça', 'WEDNESDAY': 'Quarta',
            'THURSDAY': 'Quinta', 'FRIDAY': 'Sexta', 'SATURDAY': 'Sábado', 'SUNDAY': 'Domingo'
        };

        const sorted = schedules.sort((a, b) => {
            const order = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
            return order.indexOf(a.dayOfWeek) - order.indexOf(b.dayOfWeek);
        });

        container.innerHTML = `
            <table class="admin-table">
                <thead><tr><th>Dia</th><th>Início</th><th>Fim</th></tr></thead>
                <tbody>
                    ${sorted.map(s => `
                        <tr>
                            <td data-label="Dia"><strong>${daysMap[s.dayOfWeek]}</strong></td>
                            <td data-label="Início">${s.startTime.substring(0, 5)}</td>
                            <td data-label="Fim">${s.endTime.substring(0, 5)}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    },

    openBlocksModal: function(id, name) {
        document.getElementById('blocks-modal-title').textContent = `Bloqueios: ${name}`;
        document.getElementById('blocks-modal').classList.remove('hidden');
        this.loadBlocks(id);
    },

    closeBlocksModal: function() {
        document.getElementById('blocks-modal').classList.add('hidden');
    },

    loadBlocks: async function(id) {
        const container = document.getElementById('blocks-container');
        if (!container) return;
        try {
            const response = await fetch(`/api/v1/admin/professional/schedule/block/${id}`);
            if (response.ok) {
                const blocks = await response.json();
                this.renderBlocks(blocks);
            }
        } catch (error) {
            container.innerHTML = '<p class="empty-state">Erro ao carregar bloqueios.</p>';
        }
    },

    renderBlocks: function(blocks) {
        const container = document.getElementById('blocks-container');
        if (!container) return;
        if (!blocks || blocks.length === 0) {
            container.innerHTML = '<p class="empty-state">Nenhum bloqueio ativo.</p>';
            return;
        }

        const formatDate = (dateStr) => {
            if (!dateStr) return '-';
            try {
                const date = new Date(dateStr);
                if (isNaN(date.getTime())) return 'Data Inválida';
                return date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
            } catch (e) {
                return 'Erro na Data';
            }
        };

        container.innerHTML = `
            <table class="admin-table">
                <thead><tr><th>Início</th><th>Fim</th><th>Motivo</th></tr></thead>
                <tbody>
                    ${blocks.map(b => `
                        <tr>
                            <td data-label="Início">${formatDate(b.startTime || b.dateAndStartTime || b.dateStartTime)}</td>
                            <td data-label="Fim">${formatDate(b.endTime || b.dateAndEndTime || b.dateEndTime)}</td>
                            <td data-label="Motivo">${b.reason || '-'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    },

    handleDeactivateProfessional: async function(id, btn) {
        const confirmed = await this.showConfirm('Desativar Profissional', 'Tem certeza que deseja desativar esta profissional?');
        if (!confirmed) return;

        this.setLoading(btn, true);
        try {
            const response = await fetch(`/api/v1/admin/professional/${id}/deactivate`, { method: 'PATCH' });
            if (response.ok) {
                Toast.success('Profissional desativada com sucesso!');
                await this.loadProfessionals();
            }
        } finally {
            this.setLoading(btn, false);
        }
    },

    handleActivateProfessional: async function(id, btn) {
        const confirmed = await this.showConfirm('Ativar Profissional', 'Deseja reativar esta profissional?');
        if (!confirmed) return;

        this.setLoading(btn, true);
        try {
            const response = await fetch(`/api/v1/admin/professional/${id}/activate`, { method: 'PATCH' });
            if (response.ok) {
                Toast.success('Profissional ativada com sucesso!');
                await this.loadProfessionals();
            }
        } finally {
            this.setLoading(btn, false);
        }
    },

    openProfessionalModal: function() {
        document.getElementById('professional-modal').classList.remove('hidden');
        this.loadServicesForModal();
    },

    closeProfessionalModal: function() {
        document.getElementById('professional-modal').classList.add('hidden');
        document.getElementById('professional-form').reset();
    },

    loadServicesForModal: async function() {
        const list = document.getElementById('services-checkbox-list');
        if (!list) return;
        try {
            const response = await fetch('/api/v1/salon/service');
            if (response.ok) {
                const services = await response.json();
                if (services.length === 0) {
                    list.innerHTML = '<p class="empty-state">Nenhum serviço disponível.</p>';
                    return;
                }
                list.innerHTML = services.map(s => `
                    <label class="checkbox-item">
                        <input type="checkbox" name="services" value="${s.id}">
                        <span>${s.name}</span>
                    </label>
                `).join('');
            }
        } catch (error) {
            list.innerHTML = '<p class="empty-state">Erro ao carregar serviços.</p>';
        }
    },

    handleCreateProfessional: async function(event) {
        event.preventDefault();
        const form = event.target;
        const btn = form.querySelector('button[type="submit"]');
        const selectedServices = Array.from(form.querySelectorAll('input[name="services"]:checked')).map(cb => parseInt(cb.value));

        if (selectedServices.length === 0) {
            Toast.error('Selecione pelo menos um serviço.');
            return;
        }

        const data = {
            fullName: document.getElementById('prof-name').value,
            email: document.getElementById('prof-email').value,
            servicesOfferedByProfessional: selectedServices
        };

        this.setLoading(btn, true);
        try {
            const response = await fetch('/api/v1/admin/professional', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                Toast.success('Profissional cadastrada com sucesso!');
                this.closeProfessionalModal();
                await this.loadProfessionals();
            }
        } finally {
            this.setLoading(btn, false);
        }
    }
};