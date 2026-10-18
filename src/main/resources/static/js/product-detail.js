/**
 * Khmer Marketplace - Product Detail Interactions
 * Gallery, Package Selection & Live Price Calculation
 * Zero inline scripts - Strict CSP Compliant
 */
document.addEventListener('DOMContentLoaded', function () {
    // Image Gallery Switching
    const mainImg = document.getElementById('mainProductImage');
    const thumbnails = document.querySelectorAll('.km-thumb-item');

    thumbnails.forEach(thumb => {
        thumb.addEventListener('click', function () {
            thumbnails.forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            const targetUrl = this.dataset.imgUrl;
            if (mainImg && targetUrl) {
                mainImg.src = targetUrl;
            }
        });
    });

    // Package Selection
    const packageCards = document.querySelectorAll('.km-pkg-card');
    const selectedPackageInput = document.getElementById('selectedPackageId');
    const priceDisplay = document.getElementById('displayedPrice');
    const stockBadge = document.getElementById('displayedStockBadge');
    const qtyInput = document.getElementById('productQuantityInput');
    const addToCartBtn = document.getElementById('btnAddToCart');

    packageCards.forEach(card => {
        card.addEventListener('click', function () {
            packageCards.forEach(c => c.classList.remove('selected', 'active'));
            this.classList.add('selected', 'active');

            const pkgId = this.dataset.packageId;
            const price = this.dataset.packagePrice;
            const available = parseInt(this.dataset.packageAvailable, 10) || 0;

            if (selectedPackageInput) selectedPackageInput.value = pkgId;
            if (priceDisplay && price) priceDisplay.textContent = '$' + price;

            if (stockBadge) {
                if (available > 0) {
                    stockBadge.className = 'km-badge km-badge-stock';
                    stockBadge.textContent = available + ' in stock';
                    if (addToCartBtn) addToCartBtn.disabled = false;
                } else {
                    stockBadge.className = 'km-badge km-badge-out';
                    stockBadge.textContent = 'Out of Stock';
                    if (addToCartBtn) addToCartBtn.disabled = true;
                }
            }

            if (qtyInput) {
                const maxStock = available > 0 ? Math.min(available, 100) : 1;
                qtyInput.max = maxStock;
                qtyInput.setAttribute('max', maxStock);
                if (parseInt(qtyInput.value, 10) > available && available > 0) {
                    qtyInput.value = available;
                }
            }
        });
    });

    // Quantity Increment / Decrement on product detail
    const btnInc = document.getElementById('btnDetailQtyInc');
    const btnDec = document.getElementById('btnDetailQtyDec');

    if (btnInc && qtyInput) {
        btnInc.addEventListener('click', function () {
            const max = parseInt(qtyInput.max, 10) || parseInt(qtyInput.getAttribute('max'), 10) || 100;
            let val = parseInt(qtyInput.value, 10) || 1;
            if (val < max) {
                qtyInput.value = val + 1;
            }
        });
    }

    if (btnDec && qtyInput) {
        btnDec.addEventListener('click', function () {
            const min = parseInt(qtyInput.min, 10) || parseInt(qtyInput.getAttribute('min'), 10) || 1;
            let val = parseInt(qtyInput.value, 10) || 1;
            if (val > min) {
                qtyInput.value = val - 1;
            }
        });
    }

    const addToCartForm = document.getElementById('addToCartForm');
    if (addToCartForm && addToCartBtn) {
        addToCartForm.addEventListener('submit', function () {
            if (!addToCartBtn.disabled) {
                setTimeout(() => {
                    addToCartBtn.disabled = true;
                }, 10);
            }
        });
    }
});
