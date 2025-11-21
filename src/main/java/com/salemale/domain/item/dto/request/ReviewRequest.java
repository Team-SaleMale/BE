package com.salemale.domain.item.dto.request;
import com.salemale.domain.item.entity.Review;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewRequest {
    @NotNull(message = "별점은 필수입니다")
    private Review.Rating rating;  // ONE, TWO, THREE, FOUR, FIVE

    @Size(max = 500, message = "후기는 500자 이하로 작성해주세요")
    private String content;  // nullable, 별점만 선택 가능
}