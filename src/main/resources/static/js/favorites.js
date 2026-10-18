/**
 * Khmer Marketplace - Async Favorites Toggle
 * Zero inline scripts - Strict CSP Compliant
 */
document.addEventListener('DOMContentLoaded', function () {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

    document.querySelectorAll('.km-fav-btn').forEach(button => {
        button.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();

            const productId = this.dataset.productId;
            if (!productId) return;

            const icon = this.querySelector('i');
            const self = this;

            fetch('/buyer/favorites/toggle', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    [csrfHeader]: csrfToken
                },
                body: new URLSearchParams({ productId: productId })
            })
            .then(res => {
                if (res.redirected) {
                    window.location.href = res.url;
                    return null;
                }
                return res.json();
            })
            .then(data => {
                if (!data) return;
                if (data.favorited) {
                    self.classList.add('active');
                    if (icon) {
                        icon.className = 'bi bi-heart-fill';
                    }
                } else {
                    self.classList.remove('active');
                    if (icon) {
                        icon.className = 'bi bi-heart';
                    }
                }
            })
            .catch(err => {
                console.error('Favorite toggle failed:', err);
            });
        });
    });
});
