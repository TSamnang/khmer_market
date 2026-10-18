// External password toggle handler
document.addEventListener('DOMContentLoaded', function () {
    const toggleButtons = document.querySelectorAll('.btn-toggle-password');
    toggleButtons.forEach(function (button) {
        button.addEventListener('click', function () {
            const targetId = button.getAttribute('data-target');
            const targetInput = document.getElementById(targetId);
            if (!targetInput) return;

            if (targetInput.type === 'password') {
                targetInput.type = 'text';
                button.textContent = button.getAttribute('data-label-hide') || 'Hide';
            } else {
                targetInput.type = 'password';
                button.textContent = button.getAttribute('data-label-show') || 'Show';
            }
        });
    });
});
