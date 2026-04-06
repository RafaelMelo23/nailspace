export const BlocksModule = {
    loadBlocks: async function() {
        const list = document.getElementById('blocks-list');
        const template = document.getElementById('block-item-template');
        
        try {
            const now = this.toZonedDateTimeString(new Date());
            const res = await fetch(`/api/v1/professional/schedule/block?dateAndTime=${encodeURIComponent(now)}`);
            const data = await res.json();
            
            list.innerHTML = '';
            
            if (data.length === 0) {
                list.innerHTML = '<p class="empty-msg">Nenhum bloqueio futuro encontrado.</p>';
                return;
            }

            data.forEach(block => {
                const clone = template.content.cloneNode(true);
                
                clone.querySelector('.block-reason').textContent = block.reason || 'Sem motivo informado';
                
                const startTime = block.startTime || block.dateAndStartTime || block.dateStartTime;
                const endTime = block.endTime || block.dateAndEndTime || block.dateEndTime;

                const startStr = this.formatDateTime(startTime);
                const endStr = this.formatDateTime(endTime);
                clone.querySelector('.block-time').textContent = `${startStr} - ${endStr}`;
                
                clone.querySelector('.btn-delete-block').onclick = () => this.deleteBlock(block.id);

                list.appendChild(clone);
            });

        } catch (err) {
            list.innerHTML = '<p class="error">Erro ao carregar bloqueios.</p>';
        }
    },

    openBlockModal: function() {
        document.getElementById('block-id').value = '';
        document.getElementById('block-reason').value = '';
        document.getElementById('block-whole-day').checked = false;
        
        // Set default dates
        const now = new Date();
        now.setMinutes(0, 0, 0);
        const start = new Date(now);
        start.setHours(start.getHours() + 1);
        const end = new Date(start);
        end.setHours(end.getHours() + 1);

        document.getElementById('block-start').value = this.toZonedDateTimeString(start).substring(0, 16);
        document.getElementById('block-end').value = this.toZonedDateTimeString(end).substring(0, 16);
        
        document.getElementById('block-end-container').style.display = 'block';
        document.getElementById('block-modal').classList.remove('hidden');

        document.getElementById('block-form').onsubmit = (e) => {
            e.preventDefault();
            this.saveBlock();
        };
    },

    closeBlockModal: function() {
        document.getElementById('block-modal').classList.add('hidden');
    },

    toggleWholeDay: function() {
        const isWholeDay = document.getElementById('block-whole-day').checked;
        const endContainer = document.getElementById('block-end-container');
        const startInput = document.getElementById('block-start');
        
        if (isWholeDay) {
            endContainer.style.display = 'none';
            // If whole day, we only need the date part of the start input
            const date = startInput.value.split('T')[0];
            startInput.value = `${date}T00:00`;
        } else {
            endContainer.style.display = 'block';
        }
    },

    saveBlock: async function() {
        const btn = document.getElementById('save-block-btn');
        this.setLoading(btn, true, 'Criando...');

        const isWholeDay = document.getElementById('block-whole-day').checked;
        const startVal = document.getElementById('block-start').value;
        const endVal = isWholeDay ? startVal : document.getElementById('block-end').value;

        // Backend expects ZonedDateTime. We'll send it and let the browser's timezone be used.
        // Actually, the backend might expect a specific format. 
        // Based on DTO: ZonedDateTime startTime.
        
        const payload = {
            startTime: this.toZonedDateTimeString(new Date(startVal)),
            endTime: isWholeDay ? this.toZonedDateTimeString(new Date(new Date(startVal).setHours(23, 59, 59))) : this.toZonedDateTimeString(new Date(endVal)),
            isWholeDayBlocked: isWholeDay,
            reason: document.getElementById('block-reason').value
        };

        try {
            const res = await fetch('/api/v1/professional/schedule/block', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                Toast.success('Bloqueio criado com sucesso!');
                this.closeBlockModal();
                this.loadBlocks();
            } else {
                const err = await res.json();
                Toast.error(err.messages?.[0] || 'Erro ao criar bloqueio.');
            }
        } catch (err) {
            Toast.error('Erro de conexão ao criar bloqueio.');
        } finally {
            this.setLoading(btn, false);
        }
    },

    deleteBlock: async function(id) {
        const confirmed = await UI.confirm('Excluir Bloqueio', 'Tem certeza que deseja excluir este bloqueio?');
        if (!confirmed) return;

        try {
            const res = await fetch(`/api/v1/professional/schedule/block/${id}`, {
                method: 'DELETE'
            });

            if (res.ok) {
                Toast.success('Bloqueio excluído com sucesso!');
                this.loadBlocks();
            } else {
                Toast.error('Erro ao excluir bloqueio.');
            }
        } catch (err) {
            Toast.error('Erro de conexão ao excluir bloqueio.');
        }
    },

    toZonedDateTimeString: function(date) {
        const tzo = -date.getTimezoneOffset();
        const dif = tzo >= 0 ? '+' : '-';
        const pad = (num) => (num < 10 ? '0' : '') + num;
        
        return date.getFullYear() +
            '-' + pad(date.getMonth() + 1) +
            '-' + pad(date.getDate()) +
            'T' + pad(date.getHours()) +
            ':' + pad(date.getMinutes()) +
            ':' + pad(date.getSeconds()) +
            dif + pad(Math.floor(Math.abs(tzo) / 60)) +
            ':' + pad(Math.abs(tzo) % 60);
    }
};

