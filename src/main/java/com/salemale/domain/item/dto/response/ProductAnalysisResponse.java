package com.salemale.domain.item.dto.response;

import com.salemale.global.common.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAnalysisResponse {

    // AI가 추천한 상품 전체 이름 (브랜드 + 모델명)
    private String productName;

    // 추천 카테고리
    private Category category;

    // AI 분석 신뢰도 (0.0 ~ 1.0)
    private Double confidence;
}