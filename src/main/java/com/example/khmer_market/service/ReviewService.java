package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.OrderItem;
import com.example.khmer_market.domain.entity.Product;
import com.example.khmer_market.domain.entity.Review;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.domain.enums.ReviewStatus;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.OrderItemRepository;
import com.example.khmer_market.repository.ReviewRepository;
import com.example.khmer_market.web.dto.ReviewDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         OrderItemRepository orderItemRepository) {
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public Review addReview(Long buyerId, ReviewDto dto) {
        if (dto.getOrderItemId() == null) {
            throw new IllegalArgumentException("Order item ID is required to leave a review");
        }

        // Verify that the order item belongs to the buyer (IDOR defense)
        OrderItem orderItem = orderItemRepository.findByIdAndBuyerId(dto.getOrderItemId(), buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found or access denied"));

        // Only confirmed (paid) or delivered orders can be reviewed
        if (orderItem.getOrder().getStatus() != OrderStatus.DELIVERED 
                && orderItem.getOrder().getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Reviews can only be submitted for confirmed or delivered orders.");
        }

        // Check unique review per order item
        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new IllegalArgumentException("A review has already been submitted for this item.");
        }

        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars");
        }

        // Product ID is securely derived from order_items -> product_packages -> product
        Product product = orderItem.getProductPackage().getProduct();

        Review review = new Review();
        review.setUser(orderItem.getOrder().getBuyer());
        review.setOrderItem(orderItem);
        review.setProduct(product);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment() != null ? dto.getComment().trim() : "");
        review.setStatus(ReviewStatus.PUBLISHED);

        return reviewRepository.save(review);
    }

    @Transactional
    public void setReviewStatus(Long reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        review.setStatus(status);
        reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Review> getProductReviews(Long productId, org.springframework.data.domain.Pageable pageable) {
        return reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, ReviewStatus.PUBLISHED, pageable);
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getProductRatingSummary(Long productId) {
        Double avg = reviewRepository.getAverageRatingForProduct(productId);
        long count = reviewRepository.countPublishedReviewsForProduct(productId);
        return java.util.Map.of("averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0, "totalReviews", count);
    }
}
