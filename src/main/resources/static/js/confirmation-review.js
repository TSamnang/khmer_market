/**
 * Khmer Marketplace - Confirmation Review Popup
 * Strict CSP Compliant (No inline scripts)
 */
document.addEventListener('DOMContentLoaded', function () {
    // 1. Auto-show the review popup modal if unreviewed items exist
    const reviewModalEl = document.getElementById('reviewPromptModal');
    if (reviewModalEl) {
        // Only auto-show if not already submitted in this session
        const autoShown = sessionStorage.getItem('km_review_prompt_shown');
        if (!autoShown) {
            setTimeout(function () {
                const modal = new bootstrap.Modal(reviewModalEl);
                modal.show();
                sessionStorage.setItem('km_review_prompt_shown', 'true');
            }, 600);
        }
    }

    // 2. Auto-show review modal on order detail page if delivery was just confirmed
    const autoOpenOrderReview = document.getElementById('autoOpenReviewTrigger');
    if (autoOpenOrderReview) {
        const firstItemModal = document.querySelector('.modal[id^="reviewModal"]');
        if (firstItemModal) {
            setTimeout(function () {
                const modal = new bootstrap.Modal(firstItemModal);
                modal.show();
            }, 500);
        }
    }

    // 3. Interactive Star Rating Label Updates
    const ratingInputs = document.querySelectorAll('.km-star-rating input[type="radio"]');
    const descriptions = {
        '5': '⭐⭐⭐⭐⭐ Outstanding Quality! / គុណភាពល្អឥតខ្ចោះ',
        '4': '⭐⭐⭐⭐ Very Good Quality! / គុណភាពល្អណាស់',
        '3': '⭐⭐⭐ Average Quality / គុណភាពមធ្យម',
        '2': '⭐⭐ Below Expectations / ក្រោមការរំពឹងទុក',
        '1': '⭐ Poor Quality / គុណភាពខ្សោយ'
    };

    ratingInputs.forEach(function (radio) {
        radio.addEventListener('change', function () {
            const modalBody = radio.closest('.modal-body') || document;
            const desc = modalBody.querySelector('.km-star-desc') || document.getElementById('cf_starRatingDesc');
            if (desc && descriptions[this.value]) {
                desc.textContent = descriptions[this.value];
            }
        });
    });
});
