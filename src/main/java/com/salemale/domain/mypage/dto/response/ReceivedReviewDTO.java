package com.salemale.domain.mypage.dto.response;

import com.salemale.domain.item.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 받은 후기 단일 항목 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class ReceivedReviewDTO {

    private Long reviewId;                    // 후기 ID
    private Long reviewerId;                  // 작성자 ID
    private String reviewerNickname;          // 작성자 닉네임
    private String reviewerProfileImage;      // 작성자 프로필 이미지
    private Long itemId;                      // 거래 상품 ID
    private String itemTitle;                 // 거래 상품 제목
    private String itemImageUrl;              // 거래 상품 대표 이미지
    private Review.Rating rating;             // 별점
    private String content;                   // 후기 내용 (nullable)
    private LocalDateTime createdAt;          // 작성일시

    /**
     * Review 엔티티를 DTO로 변환
     */
    public static ReceivedReviewDTO from(Review review) {
        return ReceivedReviewDTO.builder()
                .reviewId(review.getReviewId())
                .reviewerId(review.getReviewer().getId())
                .reviewerNickname(review.getReviewer().getNickname())
                .reviewerProfileImage(review.getReviewer().getProfileImage())
                .itemId(review.getItem().getItemId())
                .itemTitle(review.getItem().getTitle())
                .itemImageUrl(
                        review.getItem().getImages().isEmpty()
                                ? null
                                : review.getItem().getImages().get(0).getImageUrl()
                )
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}