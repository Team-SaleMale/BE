package com.salemale.domain.region.dto.request; // 지역 등록 요청 DTO

import jakarta.validation.constraints.*; // 요청 유효성 검사용 애노테이션
import lombok.Getter; // 컨트롤러 단에서 읽기 전용으로 사용

@Getter
public class RegionCreateRequest {

    // 행정구역: 시/도
    @NotBlank
    @Size(max = 50)
    private String sido;

    // 행정구역: 시/군/구
    @NotBlank
    @Size(max = 50)
    private String sigungu;

    // 행정구역: 읍/면/동
    @NotBlank
    @Size(max = 50)
    private String eupmyeondong;

    // 위도: -90 ~ 90
    @NotNull
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private java.math.BigDecimal latitude;

    // 경도: -180 ~ 180
    @NotNull
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private java.math.BigDecimal longitude;
}


