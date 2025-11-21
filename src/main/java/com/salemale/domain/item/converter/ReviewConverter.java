package com.salemale.domain.item.converter;

import com.salemale.domain.item.dto.response.ReviewResponse;
import com.salemale.domain.item.entity.Review;

public class ReviewConverter {

    public static ReviewResponse toReviewResponse(Review review, Integer updatedMannerScore) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .itemId(review.getItem().getItemId())
                .itemTitle(review.getItem().getTitle())
                .targetUserId(review.getTarget().getId())
                .targetNickname(review.getTarget().getNickname())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .updatedMannerScore(updatedMannerScore)
                .build();
    }
}