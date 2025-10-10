package com.salemale.domain.item.dto.request;

import com.salemale.global.common.enums.Category;
import com.salemale.global.common.enums.TradeMethod;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.util.List;

@Getter
public class ItemRegisterRequest {

    // 경매 게시글 제목
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 30, message = "제목은 30자 이내여야 합니다.")
    private String title;

    // 상품 모델명
    @NotBlank(message = "상품 이름은 필수입니다.")
    @Size(max = 30, message = "상품 이름은 30자 이내여야 합니다.")
    private String name;

    // 상품 상세 설명
    @NotBlank(message = "상세 설명은 필수입니다.")
    private String description;

    // 상품 카테고리 (Enum으로 받음)
    @NotNull(message = "카테고리는 필수입니다.")
    private Category category;

    // 경매 시작가
    @NotNull(message = "시작 가격은 필수입니다.")
    @PositiveOrZero(message = "시작 가격은 0 이상이어야 합니다.")
    private Integer startPrice;

    // 경매 종료 날짜 (YYYY-MM-DD 형식의 문자열로 받을 예정)
    @NotBlank(message = "경매 종료 날짜는 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식(YYYY-MM-DD)이 올바르지 않습니다.")
    private String endDate;

    // 거래 방법 (다중 선택 가능)
    @NotEmpty(message = "최소 한 가지 이상의 거래 방식을 선택해야 합니다.")
    private List<TradeMethod> tradeMethods;

    // 거래 방법 세부사항
    @Size(max = 500, message = "거래 상세 정보는 500자 이내여야 합니다.")
    private String tradeDetails;

    // 상품 이미지 URL 목록 (최소 1개 이상)
    @NotEmpty(message = "이미지 URL은 최소 1개 이상 필수입니다.")
    private List<String> imageUrls;
}