const adminServicesApp = {
    services: [],
    professionals: [],
    editingServiceId: null,

    init: async function() {
        const el = document.getElementById('service-list');
        if (!el) return;

        if (!Auth.getToken()) {
            window.location.href = '/entrar';
            return;
        }

        await Promise.all([
            this.loadProfessionals(),
            this.loadServices()
        ]);
    },

    loadProfessionals: async function() {
        try {
            const res = await fetch('/api/v1/admin/professional', {
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });

            if (res.ok) {
                this.professionals = await res.json();
                this.renderProfessionalSelection();
            }
        } catch (error) {
            console.error('Error loading professionals:', error);
            Toast.error('Erro ao carregar profissionais.');
        }
    },

    loadServices: async function() {
        try {
            const res = await fetch('/api/v1/salon/service', {
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });

            if (res.ok) {
                this.services = await res.json();
                this.renderServices();
            }
        } catch (error) {
            console.error('Error loading services:', error);
            Toast.error('Erro ao carregar serviços.');
        }
    },

    renderServices: function() {
        const el = document.getElementById('service-list');
        if (!el) return;

        if (this.services.length === 0) {
            el.innerHTML = '<div class="empty-state">Nenhum serviço cadastrado.</div>';
            return;
        }

        el.innerHTML = this.services.map(s => `
            <div class="service-card ${s.isActive ? '' : 'inactive'}" id="service-${s.id}">
                <div class="card-top">
                    <div class="service-info">
                        <h3>${s.name}</h3>
                        <div class="service-meta">
                            <span>⏱ ${Math.floor(s.durationInSeconds / 60)} min</span>
                            ${s.nailCount ? `<span>• 💅 ${s.nailCount} unhas</span>` : ''}
                            ${s.isAddOn ? '<span>• ➕ Adicional</span>' : ''}
                        </div>
                        <div class="price-tag">R$ ${(s.value / 100).toFixed(2).replace('.', ',')}</div>
                    </div>
                    <div class="actions">
                        <label class="switch">
                            <input type="checkbox" ${s.isActive ? 'checked' : ''} onchange="adminServicesApp.toggleVisibility(${s.id}, this.checked)">
                            <span class="slider"></span>
                        </label>
                        <button class="btn-icon" onclick="adminServicesApp.editService(${s.id})">✏️</button>
                    </div>
                </div>

                <div class="card-footer">
                    <span class="prof-label">Realizado por:</span>
                    <div class="avatar-group">
                        ${s.professionals && s.professionals.length > 0 
                            ? s.professionals.map(p => `
                                <div class="mini-avatar" title="${p.name}">
                                    ${p.professionalPicture 
                                        ? `<img src="${p.professionalPicture}" alt="${p.name}">` 
                                        : p.name.charAt(0).toUpperCase()}
                                </div>
                            `).join('')
                            : '<span style="font-size: 0.75rem; color: #999;">Nenhum profissional</span>'
                        }
                    </div>
                </div>
            </div>
        `).join('');
    },

    renderProfessionalSelection: function() {
        const el = document.getElementById('prof-selection-list');
        if (!el) return;

        if (this.professionals.length === 0) {
            el.innerHTML = '<p style="padding: 10px; font-size: 0.8rem;">Nenhum profissional encontrado.</p>';
            return;
        }

        el.innerHTML = this.professionals.map(p => `
            <label class="prof-option">
                <input type="checkbox" name="professionals" value="${p.id}" id="prof-check-${p.id}">
                <span>${p.name}</span>
            </label>
        `).join('');
    },

    openModal: function() {
        this.editingServiceId = null;
        document.getElementById('modal-title').innerText = 'Novo Serviço';
        document.getElementById('service-form').reset();
        document.getElementById('service-id').value = '';

        document.querySelectorAll('input[name="professionals"]').forEach(cb => cb.checked = false);

        document.getElementById('service-modal').classList.remove('hidden');
    },

    closeModal: function() {
        document.getElementById('service-modal').classList.add('hidden');
    },

    editService: function(id) {
        const s = this.services.find(item => item.id === id);
        if (!s) return;

        this.editingServiceId = id;
        document.getElementById('modal-title').innerText = 'Editar Serviço';
        document.getElementById('service-id').value = s.id;
        document.getElementById('service-name').value = s.name;
        document.getElementById('service-value').value = (s.value / 100).toFixed(2);
        document.getElementById('service-duration').value = Math.floor(s.durationInSeconds / 60);
        document.getElementById('service-description').value = s.description || '';
        document.getElementById('service-is-addon').checked = s.isAddOn || false;

        document.getElementById('service-maintenance').value = '';

        document.querySelectorAll('input[name="professionals"]').forEach(cb => {
            const internalId = parseInt(cb.value);
            const profObj = this.professionals.find(p => p.id === internalId);
            if (profObj && s.professionals) {
                cb.checked = s.professionals.some(sp => sp.externalId === profObj.externalId);
            }
        });

        document.getElementById('service-modal').classList.remove('hidden');
    },

    handleSave: async function(e) {
        e.preventDefault();
        
        const btn = document.getElementById('btn-save-service');
        const id = document.getElementById('service-id').value;

        const professionalsIds = Array.from(document.querySelectorAll('input[name="professionals"]:checked'))
            .map(cb => parseInt(cb.value));

        const payload = {
            name: document.getElementById('service-name').value,
            value: Math.round(parseFloat(document.getElementById('service-value').value) * 100),
            durationInSeconds: parseInt(document.getElementById('service-duration').value) * 60,
            description: document.getElementById('service-description').value,
            maintenanceIntervalDays: parseInt(document.getElementById('service-maintenance').value) || null,
            isAddOn: document.getElementById('service-is-addon').checked,
            professionalsIds: professionalsIds
        };

        UI.setLoading(btn, true, 'Salvando...');

        try {
            let res;
            if (id) {
                res = await fetch(`/api/v1/admin/salon/service/${id}`, {
                    method: 'PATCH',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${Auth.getToken()}`
                    },
                    body: JSON.stringify(payload)
                });
            } else {
                res = await fetch('/api/v1/admin/salon/service', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${Auth.getToken()}`
                    },
                    body: JSON.stringify(payload)
                });
            }

            if (res.ok) {
                Toast.success('Serviço salvo com sucesso!');
                this.closeModal();
                await this.loadServices();
            } else {
                const err = await res.json();
                Toast.error(err.message || 'Erro ao salvar serviço.');
            }
        } catch (error) {
            console.error('Save error:', error);
            Toast.error('Erro de conexão.');
        } finally {
            UI.setLoading(btn, false, 'Salvar Serviço');
        }
    },

    toggleVisibility: async function(id, isActive) {
        try {
            const res = await fetch(`/api/v1/admin/salon/service/active/${id}/${isActive}`, {
                method: 'PATCH',
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });

            if (res.ok) {
                const card = document.getElementById(`service-${id}`);
                if (isActive) card.classList.remove('inactive');
                else card.classList.add('inactive');
                
                Toast.success(`Serviço ${isActive ? 'ativado' : 'desativado'}.`);
            } else {
                Toast.error('Erro ao alterar visibilidade.');
                await this.loadServices();
            }
        } catch (error) {
            Toast.error('Erro de conexão.');
            await this.loadServices();
        }
    }
};