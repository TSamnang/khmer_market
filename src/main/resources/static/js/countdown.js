// External countdown timer for Rate Limiting (429) & Account Lockout
document.addEventListener('DOMContentLoaded', function () {
    const countdownElement = document.getElementById('rate-limit-countdown');
    if (!countdownElement) return;

    let seconds = parseInt(countdownElement.getAttribute('data-seconds'), 10) || 60;
    const actionBtn = document.getElementById('countdown-action-btn');

    function formatTime(sec) {
        const m = Math.floor(sec / 60);
        const s = sec % 60;
        return (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
    }

    countdownElement.textContent = formatTime(seconds);

    const interval = setInterval(function () {
        seconds--;
        if (seconds <= 0) {
            clearInterval(interval);
            countdownElement.textContent = '00:00';
            if (actionBtn) {
                actionBtn.removeAttribute('disabled');
                actionBtn.classList.remove('disabled');
            }
        } else {
            countdownElement.textContent = formatTime(seconds);
        }
    }, 1000);

    if (actionBtn) {
        actionBtn.addEventListener('click', function () {
            if (seconds <= 0) {
                window.location.reload();
            }
        });
    }
});
