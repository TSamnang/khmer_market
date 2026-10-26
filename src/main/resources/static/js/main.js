/**
 * Khmer Marketplace - Core UI Utilities
 * Strict CSP Compliant - Native Bootstrap Integration
 */
function initMainUI() {
    // Highlight active state for user dropdown when browsing account sections
    var path = window.location.pathname;
    var userDropdown = document.getElementById('userNavDropdown');
    if (userDropdown) {
        if (path.startsWith('/buyer') || path.startsWith('/farmer') || path.startsWith('/wallet') || path.startsWith('/admin')) {
            userDropdown.classList.add('active');
        }
    }

    // Highlight matching item in user dropdown
    var dropdownItems = document.querySelectorAll('#userNavDropdown + .dropdown-menu a.dropdown-item');
    var currentPath = window.location.pathname;
    var currentParams = new URLSearchParams(window.location.search);
    var currentTab = currentParams.get('tab');

    var bestMatch = null;
    var bestScore = 0;

    dropdownItems.forEach(function (item) {
        item.classList.remove('active', 'bg-success-subtle', 'text-success', 'fw-bold');
        var href = item.getAttribute('href');
        if (!href || href === '#' || href === '/') return;

        var itemUrl;
        try {
            itemUrl = new URL(item.href, window.location.origin);
        } catch (e) {
            return;
        }

        var itemPath = itemUrl.pathname;
        var itemTab = itemUrl.searchParams.get('tab');
        var score = 0;

        if (itemPath === currentPath) {
            if (itemTab && itemTab === currentTab) {
                // Exact tab match (e.g. ?tab=payment-methods)
                score = 100;
            } else if (!itemTab && (!currentTab || currentTab === 'profile' || currentTab === 'password')) {
                // Base settings match when on default profile/password tab
                score = 90;
            } else if (!itemTab && !currentTab) {
                // Exact path match without tab
                score = 80;
            }
        } else if (itemPath !== '/' && currentPath.startsWith(itemPath + '/')) {
            // Nested path match (e.g. /buyer/orders/123 -> /buyer/orders)
            score = 50;
        }

        if (score > bestScore) {
            bestScore = score;
            bestMatch = item;
        }
    });

    if (bestMatch) {
        bestMatch.classList.add('active', 'bg-success-subtle', 'text-success', 'fw-bold');
    }

    // Highlight main products nav link
    var productsLink = document.querySelector('a.km-nav-link[href$="/products"]');
    if (productsLink && path.startsWith('/products')) {
        productsLink.classList.add('active');
    }

    // Language switcher: preserve current page URL, query parameters, and state
    var langPills = document.querySelectorAll('.km-lang-pill');
    langPills.forEach(function (pill) {
        pill.addEventListener('click', function (e) {
            e.preventDefault();
            var targetLang = pill.getAttribute('data-lang');
            if (!targetLang) {
                var href = pill.getAttribute('href') || '';
                if (href.indexOf('lang=en') !== -1) {
                    targetLang = 'en';
                } else if (href.indexOf('lang=km') !== -1) {
                    targetLang = 'km';
                }
            }
            if (targetLang) {
                var url = new URL(window.location.href);
                url.searchParams.set('lang', targetLang);
                window.location.href = url.toString();
            } else if (pill.href) {
                window.location.href = pill.href;
            }
        });
    });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initMainUI);
} else {
    initMainUI();
}
