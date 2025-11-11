package com.salemale.domain.hotdeal.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 핫딜 상품 등록 요청 DTO
 * - 일반 경매와 달리 카테고리, 거래방식, title은 입력받지 않음 (디폴트값 사용)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HotdealRegisterRequest {

    // 상품명
    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 30, message = "상품명은 30자 이내여야 합니다.")
    private String name;

    // 경매 시작가
    @NotNull(message = "시작가는 필수입니다.")
    @PositiveOrZero(message = "시작가는 0 이상이어야 합니다.")
    private Integer startPrice;

    // 상품 설명
    @NotBlank(message = "상품 설명은 필수입니다.")
    private String description;

    // 경매 종료 일시 (YYYY-MM-DDTHH:mm)
    @NotBlank(message = "경매 종료 일시는 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$",
            message = "날짜/시간 형식(YYYY-MM-DDTHH:mm)이 올바르지 않습니다.")
    private String endDateTime;

    // 상품 이미지 URL 목록 (최소 1개 이상)
    @NotEmpty(message = "이미지는 최소 1개 이상 필수입니다.")
    private List<String> imageUrls;
}