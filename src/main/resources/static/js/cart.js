/**
 * Khmer Marketplace - Shopping Cart Interaction
 * Zero inline scripts - Strict CSP Compliant
 */
document.addEventListener('DOMContentLoaded', function () {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

    // Quantity Increment / Decrement
    document.querySelectorAll('.km-qty-btn').forEach(button => {
        button.addEventListener('click', function () {
            const row = this.closest('.km-cart-row');
            if (!row) return;

            const input = row.querySelector('.km-qty-input');
            const cartItemId = row.dataset.cartItemId;
            let currentVal = parseInt(input.value, 10) || 1;
            const maxVal = parseInt(input.getAttribute('max'), 10) || 100;
            const minVal = parseInt(input.getAttribute('min'), 10) || 1;

            if (this.classList.contains('km-qty-inc')) {
                if (currentVal < maxVal) {
                    currentVal++;
                }
            } else if (this.classList.contains('km-qty-dec')) {
                if (currentVal > minVal) {
                    currentVal--;
                }
            }

            input.value = currentVal;
            submitQuantityUpdate(cartItemId, currentVal);
        });
    });

    // Direct input change
    document.querySelectorAll('.km-qty-input').forEach(input => {
        input.addEventListener('change', function () {
            const row = this.closest('.km-cart-row');
            if (!row) return;

            const cartItemId = row.dataset.cartItemId;
            let val = parseInt(this.value, 10);
            const maxVal = parseInt(this.getAttribute('max'), 10) || 100;
            const minVal = parseInt(this.getAttribute('min'), 10) || 1;

            if (isNaN(val) || val < minVal) val = minVal;
            if (val > maxVal) val = maxVal;
            this.value = val;

            submitQuantityUpdate(cartItemId, val);
        });
    });

    function submitQuantityUpdate(cartItemId, quantity) {
        if (!cartItemId || cartItemId === 'null' || cartItemId === 'undefined') {
            console.error('Invalid cartItemId, cannot update quantity');
            return;
        }
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/cart/update';

        const itemInput = document.createElement('input');
        itemInput.type = 'hidden';
        itemInput.name = 'cartItemId';
        itemInput.value = cartItemId;
        form.appendChild(itemInput);

        const qtyInput = document.createElement('input');
        qtyInput.type = 'hidden';
        qtyInput.name = 'quantity';
        qtyInput.value = quantity;
        form.appendChild(qtyInput);

        if (csrfToken) {
            const csrfInput = document.createElement('input');
            csrfInput.type = 'hidden';
            csrfInput.name = '_csrf';
            csrfInput.value = csrfToken;
            form.appendChild(csrfInput);
        }

        document.body.appendChild(form);
        form.submit();
    }
});
