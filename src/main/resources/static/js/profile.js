/**
 * Khmer Marketplace - Profile & Account Settings
 * CSP-compliant, zero inline scripts
 */
document.addEventListener('DOMContentLoaded', function () {
    // 1. Tab Switching Logic
    function activateTab(tabId) {
        if (!tabId) return;

        // Normalize tabId
        if (tabId === 'password') tabId = 'profile';

        // Update Tab Buttons
        var tabButtons = document.querySelectorAll('#settingsTabList [data-tab-id]');
        tabButtons.forEach(function (btn) {
            if (btn.getAttribute('data-tab-id') === tabId) {
                btn.classList.add('active');
                btn.setAttribute('aria-selected', 'true');
            } else {
                btn.classList.remove('active');
                btn.setAttribute('aria-selected', 'false');
            }
        });

        // Update Tab Panes
        var targetPaneId = 'tab-' + (tabId === 'payment-methods' ? 'payment' : tabId);
        var panes = document.querySelectorAll('.tab-content > .tab-pane');
        panes.forEach(function (pane) {
            if (pane.id === targetPaneId) {
                pane.classList.add('show', 'active');
            } else {
                pane.classList.remove('show', 'active');
            }
        });

        // Also trigger Bootstrap Tab instance if available
        var activeBtn = document.querySelector('#settingsTabList [data-tab-id="' + tabId + '"]');
        if (activeBtn && window.bootstrap && window.bootstrap.Tab) {
            try {
                window.bootstrap.Tab.getOrCreateInstance(activeBtn).show();
            } catch (e) {
                // Ignore fallback
            }
        }
    }

    // Attach click listeners to all tab buttons
    document.querySelectorAll('#settingsTabList [data-tab-id]').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            var tabId = this.getAttribute('data-tab-id');
            activateTab(tabId);

            // Update URL without reloading
            if (window.history && window.history.replaceState) {
                var url = new URL(window.location);
                url.searchParams.set('tab', tabId);
                window.history.replaceState(null, '', url);
            }
        });
    });

    // Check URL parameters on page load
    var urlParams = new URLSearchParams(window.location.search);
    var initialTab = urlParams.get('tab');
    if (initialTab) {
        activateTab(initialTab);
    }

    // 2. Add Payment Method Modal Toggle Logic
    var openModalButtons = document.querySelectorAll('[data-bs-target="#addPaymentMethodModal"]');
    var modalEl = document.getElementById('addPaymentMethodModal');

    openModalButtons.forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            if (modalEl) {
                if (window.bootstrap && window.bootstrap.Modal) {
                    window.bootstrap.Modal.getOrCreateInstance(modalEl).show();
                } else {
                    modalEl.classList.add('show');
                    modalEl.style.display = 'block';
                    document.body.classList.add('modal-open');
                }
            }
        });
    });

    if (modalEl) {
        modalEl.querySelectorAll('[data-bs-dismiss="modal"]').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                if (window.bootstrap && window.bootstrap.Modal) {
                    window.bootstrap.Modal.getOrCreateInstance(modalEl).hide();
                } else {
                    modalEl.classList.remove('show');
                    modalEl.style.display = 'none';
                    document.body.classList.remove('modal-open');
                }
            });
        });
    }

    // 3. Payment Method Category Change (Toggle Card Expiry & Labels)
    var methodTypeSelect = document.getElementById('methodTypeSelect');
    var expiryDiv = document.getElementById('cardExpiryDiv');
    var accountLabel = document.getElementById('accountNumberLabel');

    function syncMethodFields() {
        if (!methodTypeSelect) return;
        var isCard = methodTypeSelect.value === 'CREDIT_DEBIT_CARD';
        if (expiryDiv) {
            expiryDiv.style.display = isCard ? 'block' : 'none';
        }
        if (accountLabel) {
            accountLabel.innerText = isCard ? 'Card Number (16 Digits)' : 'Bank Account Number';
        }
    }

    if (methodTypeSelect) {
        methodTypeSelect.addEventListener('change', syncMethodFields);
        syncMethodFields();
    }

    // 4. Confirmation dialogs for forms with data-confirm
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            var msg = this.getAttribute('data-confirm');
            if (msg && !confirm(msg)) {
                e.preventDefault();
            }
        });
    });
});
