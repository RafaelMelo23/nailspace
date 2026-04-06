const bookingApp = {
    step: 1,
    data: {
        professionals: [],
        services: [],
        addons: [],
        availability: null
    },
    booking: {
        professional: null,
        mainService: null,
        addOns: [],
        date: null,
        time: null,
        total: 0
    },
    currentMonth: new Date(),

    init: async function() {
        await this.loadInitialData();
        this.renderProf();

        const savedBooking = localStorage.getItem('pending_booking');
        if (savedBooking) {
            this.restoreState(JSON.parse(savedBooking));
        }

        this.updateUI();
    },

    restoreState: async function(saved) {
        this.booking = saved;
        if (this.booking.date) this.booking.date = new Date(this.booking.date);

        if (this.booking.professional) {
            this.selectProf(this.booking.professional.externalId);
        }
        if (this.booking.mainService) {
            this.selectService(this.booking.mainService.id);
        }
        if (this.booking.addOns && this.booking.addOns.length > 0) {
            this.booking.addOns.forEach(a => {
                const el = document.getElementById(`addon-card-${a.id}`);
                if (el) el.classList.add('selected');
                const qtyDisplay = document.getElementById(`qty-${a.id}`);
                if (qtyDisplay) qtyDisplay.innerText = a.qty;
            });
        }

        if (this.booking.professional && this.booking.mainService) {
            await this.loadAvailability();
            this.step = 5;
            for(let i=1; i<=4; i++) {
                document.getElementById(`st-${i}`).classList.add('completed');
                document.getElementById(`step-${i}`).classList.add('hidden');
            }
            document.getElementById(`st-5`).classList.add('active');
            document.getElementById(`step-5`).classList.remove('hidden');
            this.renderReview();
        }

        localStorage.removeItem('pending_booking');
    },

    loadInitialData: async function() {
        try {
            const [profRes, serviceRes] = await Promise.all([
                fetch('/api/v1/professional/simplified'),
                fetch('/api/v1/salon/service')
            ]);

            if (profRes.ok) {
                this.data.professionals = await profRes.json();
            }

            if (serviceRes.ok) {
                const allServices = await serviceRes.json();
                this.data.services = allServices.filter(s => !s.isAddOn);
                this.data.addons = allServices.filter(s => s.isAddOn);
                this.renderServices();
                this.renderAddons();
            }
        } catch (error) {
            console.error('Error loading initial data:', error);
        }
    },

    renderProf: function() {
        const el = document.getElementById('prof-list');
        if (!el) return;

        if (this.data.professionals.length === 0) {
            el.innerHTML = '<p>Nenhum profissional disponível no momento.</p>';
            return;
        }

        el.innerHTML = this.data.professionals.map(p => `
            <div class="card-item" onclick="bookingApp.selectProf('${p.externalId}')" id="prof-${p.externalId}">
                <div class="item-left">
                    <div class="avatar">
                        ${p.professionalPicture ? `<img src="${p.professionalPicture}" alt="${p.name}">` : p.name.charAt(0)}
                    </div>
                    <div class="item-info">
                        <h3>${p.name}</h3>
                        <p>${p.role || 'Profissional'}</p>
                    </div>
                </div>
                <div class="checkbox-circle">✓</div>
            </div>
        `).join('');
    },

    renderServices: function() {
        const el = document.getElementById('service-list');
        if (!el) return;

        if (this.data.services.length === 0) {
            el.innerHTML = '<p>Nenhum serviço disponível.</p>';
            return;
        }

        el.innerHTML = this.data.services.map(s => `
            <div class="card-item" onclick="bookingApp.selectService(${s.id})" id="service-${s.id}">
                <div class="item-left">
                    <div class="item-info">
                        <h3>${s.name}</h3>
                        <p>${Math.floor(s.durationInSeconds / 60)}min</p>
                    </div>
                </div>
                <div class="price-tag">R$ ${(s.value / 100).toFixed(2)}</div>
            </div>
        `).join('');
    },

    renderAddons: function() {
        const el = document.getElementById('addons-list');
        if (!el) return;

        if (this.data.addons.length === 0) {
            el.innerHTML = '<p>Nenhum item adicional disponível.</p>';
            return;
        }

        el.innerHTML = this.data.addons.map(a => {
            const hasQtyClass = a.nailCount > 0 ? 'has-qty' : '';
            const unitLabel = a.nailCount > 0 ? '<span style="font-size:10px; font-weight:400; color:#999">/unid</span>' : '';

            return `
                <div class="card-item ${hasQtyClass}" id="addon-card-${a.id}" onclick="bookingApp.toggleAddon(${a.id})">
                    <div class="item-left">
                        <div class="item-info">
                            <h3>${a.name}</h3>
                            <p class="price-tag" style="margin:0">+R$ ${(a.value / 100).toFixed(2)} ${unitLabel}</p>
                        </div>
                    </div>
                    <div style="display:flex; align-items:center;">
                        <div class="checkbox-circle">✓</div>
                        ${a.nailCount > 0 ? `
                            <div class="qty-control" onclick="event.stopPropagation()">
                                <button class="btn-qty" type="button" onclick="bookingApp.changeQty(${a.id}, -1)">-</button>
                                <span class="qty-val" id="qty-${a.id}">1</span>
                                <button class="btn-qty" type="button" onclick="bookingApp.changeQty(${a.id}, 1)">+</button>
                            </div>
                        ` : ''}
                    </div>
                </div>
            `;
        }).join('');
    },

    selectProf: function(externalId) {
        document.querySelectorAll('#prof-list .card-item').forEach(e => e.classList.remove('selected'));
        document.getElementById(`prof-${externalId}`).classList.add('selected');
        this.booking.professional = this.data.professionals.find(p => p.externalId === externalId);
        this.data.availability = null;
        this.checkValidity();
    },

    selectService: function(id) {
        document.querySelectorAll('#service-list .card-item').forEach(e => e.classList.remove('selected'));
        document.getElementById(`service-${id}`).classList.add('selected');
        this.booking.mainService = this.data.services.find(s => s.id === id);
        this.data.availability = null;
        this.updateTotal();
        this.checkValidity();
    },

    toggleAddon: function(id) {
        const el = document.getElementById(`addon-card-${id}`);
        const addonData = this.data.addons.find(a => a.id === id);
        const isSelected = el.classList.contains('selected');

        if (isSelected) {
            el.classList.remove('selected');
            this.booking.addOns = this.booking.addOns.filter(item => item.id !== id);
        } else {
            el.classList.add('selected');
            this.booking.addOns.push({...addonData, qty: 1});
            const qtyDisplay = document.getElementById(`qty-${id}`);
            if (qtyDisplay) qtyDisplay.innerText = "1";
        }
        this.data.availability = null;
        this.updateTotal();
    },

    changeQty: function(id, delta) {
        const index = this.booking.addOns.findIndex(a => a.id === id);
        if (index === -1) return;

        let newQty = this.booking.addOns[index].qty + delta;
        if (newQty < 1) newQty = 1;
        if (newQty > 10) newQty = 10;

        this.booking.addOns[index].qty = newQty;
        document.getElementById(`qty-${id}`).innerText = newQty;
        this.data.availability = null;
        this.updateTotal();
    },

    renderCalendar: function() {
        const grid = document.getElementById('calendar-grid');
        const display = document.getElementById('month-display');
        if (!grid || !display) return;

        const months = ["Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"];

        display.innerText = `${months[this.currentMonth.getMonth()]} ${this.currentMonth.getFullYear()}`;
        grid.innerHTML = "";

        const daysInMonth = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth() + 1, 0).getDate();
        const startDay = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth(), 1).getDay();

        for (let i = 0; i < startDay; i++) grid.innerHTML += `<div></div>`;

        const today = new Date();
        today.setHours(0,0,0,0);

        const availableDays = this.data.availability ?
            this.data.availability.appointmentTimesDTOList.map(item => item.date) : [];

        for (let i = 1; i <= daysInMonth; i++) {
            const date = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth(), i);
            const dateISO = date.toISOString().split('T')[0];
            const isPast = date < today;
            const isAvailable = availableDays.includes(dateISO);

            const btn = document.createElement("div");
            btn.className = `day-card`;

            if (isPast || !isAvailable) {
                btn.classList.add('disabled');
            } else {
                btn.classList.add('bookable');
            }

            btn.innerHTML = `<span>${i}</span>`;
            btn.onclick = () => (!isPast && isAvailable) && this.selectDate(btn, i);
            grid.appendChild(btn);
        }
    },

    navMonth: function(dir) {
        this.currentMonth.setDate(1);
        this.currentMonth.setMonth(this.currentMonth.getMonth() + dir);
        this.renderCalendar();
    },

    selectDate: function(el, day) {
        document.querySelectorAll('.day-card').forEach(e => e.classList.remove('selected'));
        el.classList.add('selected');

        const selectedDate = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth(), day);
        this.booking.date = selectedDate;

        const dateISO = selectedDate.toISOString().split('T')[0];
        const dayData = this.data.availability.appointmentTimesDTOList.find(item => item.date === dateISO);

        this.renderTimeSlots(dayData ? dayData.availableTimes : [], selectedDate);
    },

    loadAvailability: async function() {
        if (!this.booking.professional || !this.booking.mainService) return;
        if (this.data.availability) return;

        const totalDuration = this.booking.mainService.durationInSeconds +
            this.booking.addOns.reduce((sum, a) => sum + (a.durationInSeconds * a.qty), 0);

        const servicesIds = [this.booking.mainService.id, ...this.booking.addOns.map(a => a.id)].join(',');

        try {
            const url = `/api/v1/booking/${this.booking.professional.externalId}/availability?serviceDurationInSeconds=${totalDuration}&servicesIds=${servicesIds}`;
            const res = await fetch(url);

            if (res.ok) {
                this.data.availability = await res.json();
                this.renderRecommendedDate();
                this.renderCalendar();
            }
        } catch (error) {
            console.error('Availability fetch error:', error);
        }
    },

    renderRecommendedDate: function() {
        const container = document.getElementById('recommended-date-container');
        if (!container) return;

        if (this.data.availability && this.data.availability.earliestRecommendedDate) {
            const date = new Date(this.data.availability.earliestRecommendedDate);
            const today = new Date();

            const isToday = date.getDate() === today.getDate() &&
                            date.getMonth() === today.getMonth() &&
                            date.getFullYear() === today.getFullYear();

            if (isToday) {
                container.innerHTML = '';
                return;
            }

            const dateStr = date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
            container.innerHTML = `
                <div class="recommended-badge">
                    <span>✨</span>
                    <span>Sugestão de manutenção: <strong>${dateStr}</strong></span>
                </div>
            `;
        } else {
            container.innerHTML = '';
        }
    },

    renderTimeSlots: function(times, selectedDate) {
        const slotContainer = document.getElementById('time-slots');
        slotContainer.classList.remove('hidden');

        if (!times || times.length === 0) {
            slotContainer.innerHTML = '<div class="empty-state-msg">Sem horários disponíveis para este dia.</div>';
            return;
        }

        const now = new Date();
        const isToday = selectedDate.toDateString() === now.toDateString();

        const filteredTimes = times.filter(t => {
            if (!isToday) return true;
            const [hours, minutes] = t.split(':');
            const slotDate = new Date(selectedDate);
            slotDate.setHours(parseInt(hours), parseInt(minutes), 0, 0);
            return slotDate > now;
        });

        if (filteredTimes.length === 0) {
            slotContainer.innerHTML = '<div class="empty-state-msg">Sem horários disponíveis para este dia.</div>';
            return;
        }

        slotContainer.innerHTML = filteredTimes.map(t => {
            const [hours, minutes] = t.split(':');
            const dateObj = new Date(selectedDate);
            dateObj.setHours(parseInt(hours), parseInt(minutes), 0, 0);

            const timeISO = dateObj.toISOString();
            const timeStr = `${hours}:${minutes}`;

            return `<div class="time-pill" onclick="bookingApp.selectTime(this, '${timeISO}')">${timeStr}</div>`;
        }).join('');
    },

    selectTime: function(el, t) {
        document.querySelectorAll('.time-pill').forEach(e => e.classList.remove('selected'));
        el.classList.add('selected');
        this.booking.time = t;
        this.checkValidity();
    },

    updateTotal: function() {
        let t = 0;
        if (this.booking.mainService) t += this.booking.mainService.value;
        this.booking.addOns.forEach(a => t += (a.value * a.qty));

        this.booking.total = t;
        const totalStr = `R$ ${(t / 100).toFixed(2)}`;
        document.getElementById('footer-total').innerText = totalStr;

        if (this.step === 5) this.renderReview();
    },

    checkValidity: function() {
        let ok = false;
        if (this.step === 1 && this.booking.professional) ok = true;
        if (this.step === 2 && this.booking.mainService) ok = true;
        if (this.step === 3) ok = true;
        if (this.step === 4 && this.booking.date && this.booking.time) ok = true;
        if (this.step === 5) ok = true;
        document.getElementById('btn-next').disabled = !ok;
    },

    nextStep: async function() {
        if (this.step === 5) {
            await this.confirmBooking();
            return;
        }

        if (this.step === 3) {
            await this.loadAvailability();
        }

        if (this.step === 4) this.renderReview();

        document.getElementById(`st-${this.step}`).classList.remove('active');
        document.getElementById(`st-${this.step}`).classList.add('completed');
        document.getElementById(`step-${this.step}`).classList.add('hidden');

        this.step++;

        document.getElementById(`st-${this.step}`).classList.add('active');
        document.getElementById(`step-${this.step}`).classList.remove('hidden');

        this.updateUI();
        this.checkValidity();
    },

    prevStep: function() {
        document.getElementById(`st-${this.step}`).classList.remove('active');
        document.getElementById(`step-${this.step}`).classList.add('hidden');
        this.step--;
        document.getElementById(`st-${this.step}`).classList.add('active');
        document.getElementById(`st-${this.step}`).classList.remove('completed');
        document.getElementById(`step-${this.step}`).classList.remove('hidden');
        this.updateUI();
        this.checkValidity();
    },

    updateUI: function() {
        document.getElementById('btn-back').style.visibility = this.step === 1 ? 'hidden' : 'visible';
        document.getElementById('btn-next').innerText = this.step === 5 ? 'Confirmar Agendamento' : 'Continuar';
    },

    renderReview: function() {
        document.getElementById('rev-prof').innerText = this.booking.professional.name;
        document.getElementById('rev-serv').innerText = this.booking.mainService.name;

        const dateObj = new Date(this.booking.time);
        document.getElementById('rev-date').innerText = dateObj.toLocaleString('pt-BR', {
            day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
        });

        document.getElementById('rev-total').innerText = `R$ ${(this.booking.total / 100).toFixed(2)}`;

        const addonsHTML = this.booking.addOns.map(a => {
            const qtyStr = a.nailCount > 0 ? ` <strong>(${a.qty}x)</strong>` : '';
            return `<div style="display:flex; justify-content:space-between; margin-bottom:4px">
                <span>+ ${a.name}${qtyStr}</span>
                <span>R$ ${((a.value * a.qty) / 100).toFixed(2)}</span>
              </div>`;
        }).join('');

        document.getElementById('rev-addons').innerHTML = addonsHTML || '<span style="font-style:italic">Sem itens adicionais</span>';
    },

    showSuccess: function() {
        this.step = 6;
        
        const stepper = document.querySelector('.stepper');
        if (stepper) stepper.style.display = 'none';

        const footer = document.getElementById('booking-footer');
        if (footer) footer.style.display = 'none';

        const headerTitle = document.getElementById('booking-header');
        if (headerTitle) headerTitle.style.display = 'none';

        for (let i = 1; i <= 5; i++) {
            const stepEl = document.getElementById(`step-${i}`);
            if (stepEl) stepEl.classList.add('hidden');
        }

        const step6 = document.getElementById('step-6');
        if (step6) step6.classList.remove('hidden');
    },

    confirmBooking: async function() {
        if (!Auth.getToken()) {
            localStorage.setItem('pending_booking', JSON.stringify(this.booking));
            window.location.href = '/entrar?redirect=/agendar';
            return;
        }

        const payload = {
            professionalExternalId: this.booking.professional.externalId,
            mainServiceId: this.booking.mainService.id,
            addOnsIds: this.booking.addOns.map(a => a.id),
            zonedAppointmentDateTime: this.booking.time,
            observation: document.getElementById('obs-input').value
        };

        const btn = document.getElementById('btn-next');
        btn.disabled = true;
        btn.innerText = 'Agendando...';

        try {
            const res = await fetch('/api/v1/booking', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                localStorage.removeItem('pending_booking');
                this.showSuccess();
            } else {
                btn.disabled = false;
                btn.innerText = 'Confirmar Agendamento';
            }
        } catch (error) {
            Toast.error('Erro de conexão ao tentar agendar.');
            btn.disabled = false;
            btn.innerText = 'Confirmar Agendamento';
        }
    }
    };