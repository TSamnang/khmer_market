/**
 * Khmer Marketplace - Order Review Interaction Handler
 * Strict CSP Compliant (No inline scripts)
 */
document.addEventListener('DOMContentLoaded', function () {
    // 1. Auto-show review modal on order detail page when delivery was confirmed or when clicking "Review" from orders list
    const autoOpenOrderReview = document.getElementById('autoOpenReviewTrigger');
    const urlParams = new URLSearchParams(window.location.search);
    const isReviewAction = urlParams.get('action') === 'review';

    if (autoOpenOrderReview || isReviewAction) {
        const firstItemModal = document.querySelector('.modal[id^="reviewModal"]');
        if (firstItemModal) {
            setTimeout(function () {
                const modal = new bootstrap.Modal(firstItemModal);
                modal.show();
            }, 350);
        }
    }

    // 2. Interactive Star Rating Label Updates across all review modals
    const descriptions = {
        '5': '⭐⭐⭐⭐⭐ Outstanding Quality! / គុណភាពល្អឥតខ្ចោះ',
        '4': '⭐⭐⭐⭐ Very Good Quality! / គុណភាពល្អណាស់',
        '3': '⭐⭐⭐ Average Quality / គុណភាពមធ្យម',
        '2': '⭐⭐ Below Expectations / ក្រោមការរំពឹងទុក',
        '1': '⭐ Poor Quality / គុណភាពខ្សោយ'
    };

    const ratingInputs = document.querySelectorAll('.km-star-rating input[type="radio"]');
    ratingInputs.forEach(function (radio) {
        radio.addEventListener('change', function () {
            const container = radio.closest('.modal-body') || radio.closest('.text-center') || document;
            const desc = container.querySelector('.km-star-desc');
            if (desc && descriptions[this.value]) {
                desc.textContent = descriptions[this.value];
            }
        });
    });
});
