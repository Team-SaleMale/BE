package com.salemale.domain.experimental.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAnalysisRequest {
    @NotBlank
    private String brandName;

    @NotBlank
    private String productName;
}

