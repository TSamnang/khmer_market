/**
 * Khmer Marketplace - Checkout Flow Handler
 * Address Selection & Payment Method Toggles
 * Zero inline scripts - Strict CSP Compliant
 */
document.addEventListener('DOMContentLoaded', function () {
    const addressCards = document.querySelectorAll('.km-address-select-card');
    const selectedAddressInput = document.getElementById('selectedAddressId');
    const addressRadios = document.querySelectorAll('input[name="addressRadio"]');
    const newAddressCollapse = document.getElementById('newAddressCollapse');

    function selectSavedAddress(addressId) {
        if (selectedAddressInput) {
            selectedAddressInput.value = addressId;
        }
        addressCards.forEach(card => {
            const radio = card.querySelector('input[type="radio"]');
            if (radio && radio.value === addressId) {
                card.classList.add('selected', 'border-primary');
                radio.checked = true;
            } else {
                card.classList.remove('selected', 'border-primary');
                if (radio) radio.checked = false;
            }
        });
    }

    addressCards.forEach(card => {
        card.addEventListener('click', function () {
            const radio = this.querySelector('input[type="radio"]');
            if (radio) {
                selectSavedAddress(radio.value);
            }
        });
    });

    addressRadios.forEach(radio => {
        radio.addEventListener('change', function () {
            selectSavedAddress(this.value);
        });
    });

    if (newAddressCollapse) {
        const newAddressInputs = newAddressCollapse.querySelectorAll('input, select');
        newAddressInputs.forEach(input => {
            input.addEventListener('focus', function () {
                // If buyer focuses on new address form, clear saved address selection
                if (selectedAddressInput) {
                    selectedAddressInput.value = '';
                }
                addressCards.forEach(card => {
                    card.classList.remove('selected', 'border-primary');
                    const radio = card.querySelector('input[type="radio"]');
                    if (radio) radio.checked = false;
                });
            });
        });
    }

    // Payment Method Option Selection
    const paymentCards = document.querySelectorAll('.km-payment-card');
    const paymentRadios = document.querySelectorAll('input[name="paymentMethod"]');
    const cardDetailsBox = document.getElementById('cardDetailsBox');

    function updatePaymentCards(method) {
        paymentCards.forEach(card => {
            const radio = card.querySelector('input[name="paymentMethod"]');
            if (radio && radio.value === method) {
                card.classList.add('selected', 'active');
            } else {
                card.classList.remove('selected', 'active');
            }
        });
    }

    function updatePaymentInfo(method) {
        updatePaymentCards(method);
        if (cardDetailsBox) cardDetailsBox.classList.toggle('d-none', method !== 'card');
    }

    paymentCards.forEach(card => {
        card.addEventListener('click', function (e) {
            // Avoid triggering when clicking directly on inputs inside card details
            if (e.target.tagName === 'INPUT' && e.target.name !== 'paymentMethod') {
                return;
            }
            const radio = this.querySelector('input[name="paymentMethod"]');
            if (radio && e.target !== radio) {
                radio.checked = true;
                updatePaymentInfo(radio.value);
            }
        });
    });

    paymentRadios.forEach(radio => {
        radio.addEventListener('change', function () {
            updatePaymentInfo(this.value);
        });
        if (radio.checked) {
            updatePaymentInfo(radio.value);
        }
    });

    // Formatting for Credit/Debit Card Inputs
    const cardNumberInput = document.getElementById('inputCardNumber');
    if (cardNumberInput) {
        cardNumberInput.addEventListener('input', function (e) {
            let val = e.target.value.replace(/\D/g, '').substring(0, 16);
            let parts = [];
            for (let i = 0; i < val.length; i += 4) {
                parts.push(val.substring(i, i + 4));
            }
            e.target.value = parts.join(' ');
        });
    }

    const cardExpiryInput = document.getElementById('inputCardExpiry');
    if (cardExpiryInput) {
        cardExpiryInput.addEventListener('input', function (e) {
            let val = e.target.value.replace(/\D/g, '').substring(0, 4);
            if (val.length >= 3) {
                e.target.value = val.substring(0, 2) + '/' + val.substring(2);
            } else {
                e.target.value = val;
            }
        });
    }

    const cardCvvInput = document.getElementById('inputCardCvv');
    if (cardCvvInput) {
        cardCvvInput.addEventListener('input', function (e) {
            e.target.value = e.target.value.replace(/\D/g, '').substring(0, 4);
        });
    }

    // Prevent double form submission on checkout
    const checkoutForm = document.getElementById('checkoutForm');
    const submitBtn = document.getElementById('btnSubmitOrder');

    if (checkoutForm && submitBtn) {
        checkoutForm.addEventListener('submit', function () {
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Processing Order...';
            setTimeout(function () {
                submitBtn.disabled = true;
            }, 50);
        });
    }
});
