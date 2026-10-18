/**
 * Khmer Marketplace - Core UI Utilities
 * Zero inline scripts - Strict CSP Compliant
 */
document.addEventListener('DOMContentLoaded', function () {
    // 1. Initialize and ensure all Bootstrap dropdown toggles respond smoothly
    if (window.bootstrap && window.bootstrap.Dropdown) {
        document.querySelectorAll('[data-bs-toggle="dropdown"]').forEach(function (toggleEl) {
            window.bootstrap.Dropdown.getOrCreateInstance(toggleEl);
        });
    }

    // 2. Prevent '#' anchor jump on dropdown triggers
    document.querySelectorAll('a.dropdown-toggle[href="#"]').forEach(function (anchor) {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
        });
    });

    // 3. Accessibility: Allow Space / Enter keyboard activation for dropdowns
    document.querySelectorAll('.dropdown-toggle').forEach(function (btn) {
        btn.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                if (window.bootstrap && window.bootstrap.Dropdown) {
                    const inst = window.bootstrap.Dropdown.getOrCreateInstance(this);
                    inst.toggle();
                }
            }
        });
    });
});
