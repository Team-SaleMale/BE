package com.salemale.domain.region.dto.response; // 지역 응답 DTO

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class RegionResponse {
    private Long regionId;
    private String sido;
    private String sigungu;
    private String eupmyeondong;
    private BigDecimal latitude;
    private BigDecimal longitude;
}


