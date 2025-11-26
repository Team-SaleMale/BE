package com.salemale.domain.experimental.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.experimental.dto.BrandAnalysisRequest;
import com.salemale.domain.experimental.dto.BrandAnalysisResponse;
import com.salemale.domain.experimental.dto.ProductAnalysisRequest;
import com.salemale.domain.experimental.dto.ProductAnalysisResponse;
import com.salemale.domain.experimental.service.ExperimentalBrandAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/experimental/analysis")
public class ExperimentalBrandAnalysisController {

    private final ExperimentalBrandAnalysisService experimentalBrandAnalysisService;

    @PostMapping("/brand")
    public ResponseEntity<ApiResponse<BrandAnalysisResponse>> analyzeBrand(
            @Valid @RequestBody BrandAnalysisRequest request
    ) {
        BrandAnalysisResponse response = experimentalBrandAnalysisService.analyzeBrand(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PostMapping("/product")
    public ResponseEntity<ApiResponse<ProductAnalysisResponse>> analyzeProduct(
            @Valid @RequestBody ProductAnalysisRequest request
    ) {
        ProductAnalysisResponse response = experimentalBrandAnalysisService.analyzeProduct(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}

