package com.salemale.domain.item.dto.response;

import com.salemale.domain.item.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ReviewResponse {
    private Long reviewId;
    private Long itemId;
    private String itemTitle;
    private Long targetUserId;
    private String targetNickname;
    private Review.Rating rating;
    private String content;
    private LocalDateTime createdAt;
    private Integer updatedMannerScore;
}