package com.salemale.domain.region.dto.request; // 지역 수정 요청 DTO

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class RegionUpdateRequest {

    // 선택적으로 일부 필드만 수정할 수 있도록 nullable 허용
    @Size(max = 50)
    private String sido;

    @Size(max = 50)
    private String sigungu;

    @Size(max = 50)
    private String eupmyeondong;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private java.math.BigDecimal latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private java.math.BigDecimal longitude;
}


