const adminSettingsApp = {
    clientsPage: 0,
    clientsSearchTimeout: null,

    init: function() {
        const profList = document.getElementById('professionals-list');
        if (profList) {
            adminSettingsApp.loadProfessionals();
            adminSettingsApp.loadClients();
            adminSettingsApp.loadSalonProfile();
            adminSettingsApp.setupColorPicker();
        }
    },

    switchTab: function(tabId) {
        // Update Buttons
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.innerText.toLowerCase().includes(tabId.substring(0, 3)));
        });

        // Update Content
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.toggle('active', content.id === `tab-${tabId}`);
        });
    },

    loadSalonProfile: async function() {
        try {
            const res = await fetch('/api/v1/admin/salon/profile');
            if (res.ok) {
                const salon = await res.json();
                const form = document.getElementById('salon-profile-form');
                if (form) {
                    form.querySelector('[name="tradeName"]').value = salon.tradeName || '';
                    form.querySelector('[name="slogan"]').value = salon.slogan || '';
                    form.querySelector('[name="comercialPhone"]').value = salon.comercialPhone || '';
                    form.querySelector('[name="primaryColor"]').value = salon.primaryColor || '#E91E63';
                    form.querySelector('.color-text').value = (salon.primaryColor || '#E91E63').toUpperCase();
                    form.querySelector('[name="fullAddress"]').value = salon.fullAddress || '';
                    
                    // New fields
                    const status = salon.status || 'OPEN';
                    form.querySelector('[name="status"]').value = status;
                    adminSettingsApp.handleStatusChange(status);
                    
                    form.querySelector('[name="socialMediaLink"]').value = salon.socialMediaLink || '';
                    form.querySelector('[name="zoneId"]').value = salon.zoneId || 'America/Sao_Paulo';
                    form.querySelector('[name="appointmentBufferMinutes"]').value = salon.appointmentBufferMinutes || 0;
                    form.querySelector('[name="standardBookingWindow"]').value = salon.standardBookingWindow || 30;
                    form.querySelector('[name="warningMessage"]').value = salon.warningMessage || '';
                    
                    const loyalCheckbox = form.querySelector('[name="isLoyalClientelePrioritized"]');
                    loyalCheckbox.checked = !!salon.isLoyalClientelePrioritized;
                    adminSettingsApp.toggleLoyalWindow(loyalCheckbox.checked);
                    
                    form.querySelector('[name="loyalClientBookingWindowDays"]').value = salon.loyalClientBookingWindowDays || 60;
                }
            }
        } catch (e) {
            console.error('Error loading profile:', e);
        }
    },

    handleStatusChange: function(value) {
        const group = document.getElementById('warning-message-group');
        if (value === 'CLOSED_TEMPORARY') {
            group.classList.remove('hidden');
        } else {
            group.classList.add('hidden');
        }
    },

    toggleLoyalWindow: function(checked) {
        const group = document.getElementById('loyal-window-group');
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

        // Process numeric and boolean fields
        data.appointmentBufferMinutes = parseInt(data.appointmentBufferMinutes) || 0;
        data.standardBookingWindow = parseInt(data.standardBookingWindow) || 30;
        data.isLoyalClientelePrioritized = form.querySelector('#isLoyalClientelePrioritized').checked;
        data.loyalClientBookingWindowDays = parseInt(data.loyalClientBookingWindowDays) || 60;
        
        adminSettingsApp.setLoading(btn, true);
        try {
            const response = await fetch('/api/v1/admin/salon/profile', {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                Toast.success('Configurações salvas com sucesso!');
                await adminSettingsApp.loadSalonProfile();
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
            adminSettingsApp.setLoading(btn, false);
        }
    },

    // --- Helpers ---
    setLoading: function(btn, loading) {
        if (!btn) return;
        if (loading) {
            btn.classList.add('btn-loading');
            btn.disabled = true;
        } else {
            btn.classList.remove('btn-loading');
            btn.disabled = false;
        }
    },

    showConfirm: function(title, message) {
        return new Promise((resolve) => {
            const modal = document.getElementById('confirm-modal');
            const btnOk = document.getElementById('confirm-ok');
            const btnCancel = document.getElementById('confirm-cancel');
            
            document.getElementById('confirm-title').textContent = title;
            document.getElementById('confirm-message').textContent = message;
            
            modal.classList.remove('hidden');

            const cleanup = (result) => {
                modal.classList.add('hidden');
                btnOk.onclick = null;
                btnCancel.onclick = null;
                resolve(result);
            };

            btnOk.onclick = () => cleanup(true);
            btnCancel.onclick = () => cleanup(false);
        });
    },

    getInitials: function(name) {
        return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
    },

    // --- Professionals ---
    loadProfessionals: async function() {
        const list = document.getElementById('professionals-list');
        try {
            const response = await fetch('/api/v1/admin/professional');
            if (response.ok) {
                const professionals = await response.json();
                adminSettingsApp.renderProfessionals(professionals);
            }
        } catch (error) {
            list.innerHTML = '<tr><td colspan="5" class="empty-state">Erro ao carregar profissionais.</td></tr>';
        }
    },

    renderProfessionals: function(professionals) {
        const list = document.getElementById('professionals-list');
        if (professionals.length === 0) {
            list.innerHTML = '<tr><td colspan="6" class="empty-state">Nenhum profissional cadastrado.</td></tr>';
            return;
        }

        list.innerHTML = professionals.map(prof => `
            <tr>
                <td>
                    <div class="prof-cell">
                        <div class="prof-initials">${adminSettingsApp.getInitials(prof.name)}</div>
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

    // --- Schedules & Blocks Management ---
    openSchedulesModal: function(id, name) {
        document.getElementById('schedules-modal-title').textContent = `Horários: ${name}`;
        document.getElementById('schedules-modal').classList.remove('hidden');
        adminSettingsApp.loadSchedules(id);
    },

    closeSchedulesModal: function() {
        document.getElementById('schedules-modal').classList.add('hidden');
    },

    loadSchedules: async function(id) {
        const container = document.getElementById('schedules-container');
        container.innerHTML = '<p class="empty-state">Carregando horários...</p>';
        try {
            const response = await fetch(`/api/v1/admin/professional/schedule/${id}`);
            if (response.ok) {
                const schedules = await response.json();
                adminSettingsApp.renderSchedules(schedules);
            }
        } catch (error) {
            container.innerHTML = '<p class="empty-state">Erro ao carregar horários.</p>';
        }
    },

    renderSchedules: function(schedules) {
        const container = document.getElementById('schedules-container');
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
        adminSettingsApp.loadBlocks(id);
    },

    closeBlocksModal: function() {
        document.getElementById('blocks-modal').classList.add('hidden');
    },

    loadBlocks: async function(id) {
        const container = document.getElementById('blocks-container');
        container.innerHTML = '<p class="empty-state">Carregando bloqueios...</p>';
        try {
            const response = await fetch(`/api/v1/admin/professional/schedule/block/${id}`);
            if (response.ok) {
                const blocks = await response.json();
                adminSettingsApp.renderBlocks(blocks);
            }
        } catch (error) {
            container.innerHTML = '<p class="empty-state">Erro ao carregar bloqueios.</p>';
        }
    },

    renderBlocks: function(blocks) {
        const container = document.getElementById('blocks-container');
        if (!blocks || blocks.length === 0) {
            container.innerHTML = '<p class="empty-state">Nenhum bloqueio ativo.</p>';
            return;
        }

        const formatDate = (dateStr) => {
            const date = new Date(dateStr);
            return date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
        };

        container.innerHTML = `
            <table class="admin-table">
                <thead><tr><th>Início</th><th>Fim</th><th>Motivo</th></tr></thead>
                <tbody>
                    ${blocks.map(b => `
                        <tr>
                            <td data-label="Início">${formatDate(b.dateStartTime)}</td>
                            <td data-label="Fim">${formatDate(b.dateEndTime)}</td>
                            <td data-label="Motivo">${b.reason || '-'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    },

    handleDeactivateProfessional: async function(id, btn) {
        const confirmed = await adminSettingsApp.showConfirm('Desativar Profissional', 'Tem certeza que deseja desativar esta profissional?');
        if (!confirmed) return;

        adminSettingsApp.setLoading(btn, true);
        try {
            const response = await fetch(`/api/v1/admin/professional/${id}/deactivate`, { method: 'PATCH' });
            if (response.ok) {
                Toast.success('Profissional desativada com sucesso!');
                await adminSettingsApp.loadProfessionals();
            }
        } finally {
            adminSettingsApp.setLoading(btn, false);
        }
    },

    handleActivateProfessional: async function(id, btn) {
        const confirmed = await adminSettingsApp.showConfirm('Ativar Profissional', 'Deseja reativar esta profissional?');
        if (!confirmed) return;

        adminSettingsApp.setLoading(btn, true);
        try {
            const response = await fetch(`/api/v1/admin/professional/${id}/activate`, { method: 'PATCH' });
            if (response.ok) {
                Toast.success('Profissional ativada com sucesso!');
                await adminSettingsApp.loadProfessionals();
            }
        } finally {
            adminSettingsApp.setLoading(btn, false);
        }
    },

    openProfessionalModal: function() {
        document.getElementById('professional-modal').classList.remove('hidden');
        adminSettingsApp.loadServicesForModal();
    },

    closeProfessionalModal: function() {
        document.getElementById('professional-modal').classList.add('hidden');
        document.getElementById('professional-form').reset();
    },

    loadServicesForModal: async function() {
        const list = document.getElementById('services-checkbox-list');
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

        adminSettingsApp.setLoading(btn, true);
        try {
            const response = await fetch('/api/v1/admin/professional', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                Toast.success('Profissional cadastrada com sucesso!');
                adminSettingsApp.closeProfessionalModal();
                await adminSettingsApp.loadProfessionals();
            }
        } finally {
            adminSettingsApp.setLoading(btn, false);
        }
    },

    // --- Clients ---
    debounceSearchClients: function() {
        clearTimeout(adminSettingsApp.clientsSearchTimeout);
        adminSettingsApp.clientsSearchTimeout = setTimeout(() => {
            adminSettingsApp.clientsPage = 0;
            adminSettingsApp.loadClients();
        }, 500);
    },

    loadClients: async function() {
        const searchTerm = document.getElementById('client-search').value;
        try {
            const response = await fetch(`/api/v1/admin/client?name=${encodeURIComponent(searchTerm)}&page=${adminSettingsApp.clientsPage}&size=10`);
            if (response.ok) {
                const data = await response.json();
                adminSettingsApp.renderClients(data.content);
                adminSettingsApp.renderPagination(data);
            }
        } catch (error) {
            console.error('Error loading clients:', error);
        }
    },

    renderClients: function(clients) {
        const list = document.getElementById('clients-list');
        if (clients.length === 0) {
            list.innerHTML = '<tr><td colspan="4" class="empty-state">Nenhum cliente encontrado.</td></tr>';
            return;
        }

        list.innerHTML = clients.map(client => `
            <tr>
                <td>
                    <strong style="${client.userStatus === 'BANNED' ? 'text-decoration: line-through; opacity: 0.6;' : ''}">${client.fullName}</strong>
                </td>
                <td data-label="Telefone">${client.phoneNumber || 'N/A'}</td>
                <td data-label="Histórico">
                    <div class="client-history-cell" style="display: flex; align-items: center; gap: 8px;">
                        ${client.missedAppointments > 0 ? 
                            `<span class="badge badge-danger">⚠ ${client.missedAppointments} Faltas</span>` : 
                            '<span style="color:green; font-weight: 600; font-size: 0.85rem;">★ Regular</span>'}
                        <button class="btn btn-secondary btn-sm" onclick="adminSettingsApp.openHistoryModal(${client.clientId}, '${client.fullName}')" title="Ver Histórico Completo">Agendamentos</button>
                    </div>
                </td>
                <td data-label="Ação">
                    ${client.userStatus === 'ACTIVE' ? 
                        `<button class="btn-outline-danger btn-sm" onclick="adminSettingsApp.handleUpdateClientStatus(${client.clientId}, 'BANNED', this)">Banir</button>` : 
                        `<button class="btn btn-secondary btn-sm" onclick="adminSettingsApp.handleUpdateClientStatus(${client.clientId}, 'ACTIVE', this)">Desbloquear</button>`
                    }
                </td>
            </tr>
        `).join('');
    },

    // --- Client Appointment History ---
    openHistoryModal: function(clientId, name) {
        document.getElementById('client-history-modal-title').textContent = `Histórico: ${name}`;
        document.getElementById('client-history-modal').classList.remove('hidden');
        adminSettingsApp.currentHistoryClientId = clientId;
        adminSettingsApp.loadAppointmentHistory(clientId, 0);
    },

    closeHistoryModal: function() {
        document.getElementById('client-history-modal').classList.add('hidden');
        adminSettingsApp.currentHistoryClientId = null;
    },

    loadAppointmentHistory: async function(clientId, page) {
        const container = document.getElementById('client-history-container');
        container.innerHTML = '<p class="empty-state">Carregando histórico...</p>';
        try {
            const response = await fetch(`/api/v1/admin/client/${clientId}/appointments?page=${page}&size=5&sort=startDate,desc`);
            if (response.ok) {
                const data = await response.json();
                adminSettingsApp.renderAppointmentHistory(data);
                adminSettingsApp.renderHistoryPagination(data);
            }
        } catch (error) {
            container.innerHTML = '<p class="empty-state">Erro ao carregar histórico.</p>';
        }
    },

    renderAppointmentHistory: function(data) {
        const container = document.getElementById('client-history-container');
        const appointments = data.content;
        
        if (!appointments || appointments.length === 0) {
            container.innerHTML = '<p class="empty-state">Este cliente ainda não possui agendamentos.</p>';
            return;
        }

        const formatDate = (dateStr) => {
            const date = new Date(dateStr);
            return date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
        };

        const statusMap = {
            'SCHEDULED': { label: 'Agendado', class: 'badge-success' },
            'CONFIRMED': { label: 'Confirmado', class: 'badge-success' },
            'COMPLETED': { label: 'Finalizado', class: 'badge-success' },
            'CANCELLED': { label: 'Cancelado', class: 'badge-danger' },
            'MISSED': { label: 'Faltou', class: 'badge-danger' }
        };

        container.innerHTML = `
            <table class="admin-table">
                <thead>
                    <tr>
                        <th>Data</th>
                        <th>Serviço</th>
                        <th>Profissional</th>
                        <th>Valor</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    ${appointments.map(a => {
                        const s = statusMap[a.status] || { label: a.status, class: '' };
                        const services = [a.mainServiceName, ...(a.addOnServiceNames || [])].join(', ');
                        return `
                            <tr>
                                <td data-label="Data">${formatDate(a.startDateAndTime)}</td>
                                <td data-label="Serviço">
                                    <span title="${services}">${a.mainServiceName}${a.addOnServiceNames?.length ? ' (+)' : ''}</span>
                                </td>
                                <td data-label="Profissional">${a.professionalName}</td>
                                <td data-label="Valor">R$ ${a.totalValue?.toFixed(2) || '0,00'}</td>
                                <td data-label="Status"><span class="badge ${s.class}">${s.label}</span></td>
                            </tr>
                        `;
                    }).join('')}
                </tbody>
            </table>
        `;
    },

    renderHistoryPagination: function(data) {
        const container = document.getElementById('history-pagination');
        if (data.totalPages <= 1) {
            container.innerHTML = '';
            return;
        }

        container.innerHTML = `
            <button class="page-btn" ${data.first ? 'disabled' : ''} onclick="adminSettingsApp.loadAppointmentHistory(${adminSettingsApp.currentHistoryClientId}, ${data.number - 1})">Anterior</button>
            <span style="font-size: 0.85rem; color: #666;">Página ${data.number + 1} de ${data.totalPages}</span>
            <button class="page-btn" ${data.last ? 'disabled' : ''} onclick="adminSettingsApp.loadAppointmentHistory(${adminSettingsApp.currentHistoryClientId}, ${data.number + 1})">Próxima</button>
        `;
    },

    renderPagination: function(data) {
        const container = document.getElementById('clients-pagination');
        if (data.totalPages <= 1) {
            container.innerHTML = '';
            return;
        }

        container.innerHTML = `
            <button class="page-btn" ${data.first ? 'disabled' : ''} onclick="adminSettingsApp.changeClientsPage(${adminSettingsApp.clientsPage - 1})">Anterior</button>
            <span style="font-size: 0.85rem; color: #666;">Página ${data.number + 1} de ${data.totalPages}</span>
            <button class="page-btn" ${data.last ? 'disabled' : ''} onclick="adminSettingsApp.changeClientsPage(${adminSettingsApp.clientsPage + 1})">Próxima</button>
        `;
    },

    changeClientsPage: function(page) {
        adminSettingsApp.clientsPage = page;
        adminSettingsApp.loadClients();
    },

    handleUpdateClientStatus: async function(clientId, status, btn) {
        const action = status === 'BANNED' ? 'banir' : 'desbloquear';
        const confirmed = await adminSettingsApp.showConfirm(`${status === 'BANNED' ? 'Banir' : 'Desbloquear'} Cliente`, `Tem certeza que deseja ${action} este cliente?`);
        if (!confirmed) return;

        adminSettingsApp.setLoading(btn, true);
        try {
            const response = await fetch(`/api/v1/admin/client/status/${clientId}/${status}`, { method: 'PATCH' });
            if (response.ok) {
                Toast.success(`Cliente ${status === 'BANNED' ? 'banido' : 'desbloqueado'} com sucesso!`);
                await adminSettingsApp.loadClients();
            }
        } finally {
            adminSettingsApp.setLoading(btn, false);
        }
    },

    // --- Salon Profile ---
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

// adminSettingsApp.init() will be called by App.initPage()
