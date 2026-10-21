/**
 * Khmer Marketplace - Core UI Utilities
 * Zero inline scripts - Strict CSP Compliant
 */
document.addEventListener('DOMContentLoaded', function () {
    // 1. Initialize and ensure all Bootstrap dropdown toggles respond smoothly
    document.querySelectorAll('[data-bs-toggle="dropdown"]').forEach(function (toggleEl) {
        if (window.bootstrap && window.bootstrap.Dropdown) {
            window.bootstrap.Dropdown.getOrCreateInstance(toggleEl);
        }

        toggleEl.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            if (window.bootstrap && window.bootstrap.Dropdown) {
                var inst = window.bootstrap.Dropdown.getOrCreateInstance(this);
                inst.toggle();
            }
        });

        toggleEl.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                e.stopPropagation();
                if (window.bootstrap && window.bootstrap.Dropdown) {
                    var inst = window.bootstrap.Dropdown.getOrCreateInstance(this);
                    inst.toggle();
                }
            }
        });
    });

    // 4. User Navigation Dropdown Active State & Current Item Highlight
    var path = window.location.pathname;
    var userDropdown = document.getElementById('userNavDropdown');
    if (userDropdown) {
        if (path.startsWith('/buyer') || path.startsWith('/farmer') || path.startsWith('/wallet') || path.startsWith('/admin')) {
            userDropdown.classList.add('active');
        }
    }
    var dropdownItems = document.querySelectorAll('#userNavDropdown + .dropdown-menu a.dropdown-item');
    dropdownItems.forEach(function (item) {
        var href = item.getAttribute('href');
        if (href && (path === href || (href !== '/' && path.startsWith(href)))) {
            item.classList.add('active', 'bg-success-subtle', 'text-success', 'fw-bold');
        }
    });

    var productsLink = document.querySelector('a.km-nav-link[href$="/products"]');
    if (productsLink && path.startsWith('/products')) {
        productsLink.classList.add('active');
    }
});

