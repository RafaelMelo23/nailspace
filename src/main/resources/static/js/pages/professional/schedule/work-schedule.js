export const WorkScheduleModule = {
    daysOfWeek: [
        { key: 'MONDAY', label: 'Segunda-feira' },
        { key: 'TUESDAY', label: 'Terça-feira' },
        { key: 'WEDNESDAY', label: 'Quarta-feira' },
        { key: 'THURSDAY', label: 'Quinta-feira' },
        { key: 'FRIDAY', label: 'Sexta-feira' },
        { key: 'SATURDAY', label: 'Sábado' },
        { key: 'SUNDAY', label: 'Domingo' }
    ],

    loadWorkSchedule: async function() {
        const list = document.getElementById('work-schedule-list');
        const template = document.getElementById('schedule-day-template');
        
        try {
            const res = await fetch('/api/v1/professional/schedule');
            const data = await res.json();
            
            list.innerHTML = '';
            
            this.daysOfWeek.forEach(day => {
                const schedule = data.find(s => s.dayOfWeek === day.key);
                const isRegistered = !!schedule;
                
                const dayData = schedule || {
                    dayOfWeek: day.key,
                    startTime: '09:00',
                    endTime: '18:00',
                    lunchBreakStartTime: '12:00',
                    lunchBreakEndTime: '13:00',
                    isActive: false
                };

                const clone = template.content.cloneNode(true);
                const row = clone.querySelector('.schedule-day-row');
                row.dataset.day = day.key;
                row.dataset.id = dayData.id || '';

                row.classList.remove('status-unregistered', 'status-active', 'status-inactive');
                if (!isRegistered) {
                    row.classList.add('status-unregistered');
                } else {
                    row.classList.add(dayData.isActive ? 'status-active' : 'status-inactive');
                    const statusTag = row.querySelector('.day-status-tag');
                    if (statusTag) statusTag.textContent = dayData.isActive ? 'Ativo' : 'Inativo';
                }

                clone.querySelector('.day-name').textContent = day.label;
                
                const toggle = clone.querySelector('.day-active-toggle');
                toggle.checked = dayData.isActive;

                toggle.onchange = (e) => {
                    const isActive = e.target.checked;
                    row.classList.remove('status-active', 'status-inactive', 'status-unregistered');
                    row.classList.add(isActive ? 'status-active' : 'status-inactive');
                    const statusTag = row.querySelector('.day-status-tag');
                    if (statusTag) statusTag.textContent = isActive ? 'Ativo' : 'Inativo';
                };

                clone.querySelector('.start-time').value = dayData.startTime.substring(0, 5);
                clone.querySelector('.end-time').value = dayData.endTime.substring(0, 5);
                clone.querySelector('.lunch-start').value = dayData.lunchBreakStartTime.substring(0, 5);
                clone.querySelector('.lunch-end').value = dayData.lunchBreakEndTime.substring(0, 5);

                const deleteBtn = clone.querySelector('.btn-delete-schedule');
                if (isRegistered && dayData.id) {
                    deleteBtn.style.display = 'flex';
                    deleteBtn.onclick = () => this.confirmDelete(dayData.id, day.label);
                }

                list.appendChild(clone);
            });

            document.getElementById('work-schedule-form').onsubmit = (e) => {
                e.preventDefault();
                this.saveWorkSchedule();
            };

        } catch (err) {
            console.error(err);
            list.innerHTML = '<p class="error">Erro ao carregar horários.</p>';
        }
    },

    confirmDelete: async function(id, dayLabel) {
        const confirmed = await UI.confirm('Excluir Horário', `Deseja realmente excluir o horário de ${dayLabel}?`);
        if (confirmed) {
            try {
                const res = await fetch(`/api/v1/professional/schedule/${id}`, {
                    method: 'DELETE'
                });

                if (res.ok) {
                    Toast.success(`Horário de ${dayLabel} excluído.`);
                    this.loadWorkSchedule();
                } else {
                    Toast.error('Erro ao excluir horário.');
                }
            } catch (err) {
                Toast.error('Erro de conexão ao excluir horário.');
            }
        }
    },

    saveWorkSchedule: async function() {
        const btn = document.getElementById('save-work-schedule');
        this.setLoading(btn, true, 'Salvando...');

        const schedules = [];
        const rows = document.querySelectorAll('.schedule-day-row');
        
        rows.forEach(row => {
            const idVal = row.dataset.id;
            const id = (idVal && idVal.trim() !== '' && idVal !== 'null') ? parseInt(idVal) : null;
            const isActive = row.querySelector('.day-active-toggle').checked;

            if (id !== null || isActive) {
                schedules.push({
                    id: id,
                    dayOfWeek: row.dataset.day,
                    isActive: isActive,
                    startTime: row.querySelector('.start-time').value + ':00',
                    endTime: row.querySelector('.end-time').value + ':00',
                    lunchBreakStartTime: row.querySelector('.lunch-start').value + ':00',
                    lunchBreakEndTime: row.querySelector('.lunch-end').value + ':00'
                });
            }
        });

        if (schedules.length === 0) {
            Toast.error('Ative pelo menos um dia de trabalho para salvar.');
            this.setLoading(btn, false);
            return;
        }

        try {
            const res = await fetch('/api/v1/professional/schedule', {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(schedules)
            });

            if (res.ok) {
                Toast.success('Horários atualizados com sucesso!');
                this.loadWorkSchedule();
            } else {
                const err = await res.json();
                Toast.error(err.messages?.[0] || 'Erro ao salvar horários.');
            }
        } catch (err) {
            Toast.error('Erro de conexão ao salvar horários.');
        } finally {
            this.setLoading(btn, false);
        }
    }
};
