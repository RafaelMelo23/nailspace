export const CoreModule = {
    init: async function() {
        const fragments = ['work-schedule', 'blocks', 'modals'];
        const contentArea = document.getElementById('schedule-content-area');

        await Promise.all(fragments.map(async (tabId) => {
            try {
                const response = await fetch(`/pages/professional/schedule-fragments/${tabId}.html`);
                if (response.ok) {
                    const html = await response.text();
                    contentArea.insertAdjacentHTML('beforeend', html);
                }
            } catch (e) {
                console.error(`Error loading fragment ${tabId}:`, e);
            }
        }));

        const hash = window.location.hash.substring(1) || 'work-schedule';
        this.switchTabUI(hash);
        this.loadTabData(hash);
    },

    switchTab: async function(tabId) {
        window.location.hash = tabId;
        this.switchTabUI(tabId);
        this.loadTabData(tabId);
    },

    switchTabUI: function(tabId) {
        document.querySelectorAll('.tab-btn').forEach(btn => {
            const onClick = btn.getAttribute('onclick') || '';
            btn.classList.toggle('active', onClick.includes(tabId));
        });

        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.toggle('active', content.id === `tab-${tabId}`);
        });
    },

    loadTabData: function(tabId) {
        if (tabId === 'work-schedule') {
            professionalScheduleApp.loadWorkSchedule();
        } else if (tabId === 'blocks') {
            professionalScheduleApp.loadBlocks();
        }
    },

    setLoading: function(btn, loading, text) {
        UI.setLoading(btn, loading, text);
    },

    formatDateTime: function(dateTimeStr) {
        const date = new Date(dateTimeStr);
        return date.toLocaleString('pt-BR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
};
