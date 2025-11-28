package com.salemale.domain.experimental.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.experimental.dto.BrandAnalysisRequest;
import com.salemale.domain.experimental.dto.BrandAnalysisResponse;
import com.salemale.domain.experimental.dto.ProductAnalysisRequest;
import com.salemale.domain.experimental.dto.ProductAnalysisResponse;
import com.salemale.domain.experimental.service.ExperimentalBrandAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Experimental - TryOn & Analysis", description = "실험실 기능: 가상 피팅 및 브랜드/상품 분석 API")
public class ExperimentalBrandAnalysisController {

    private final ExperimentalBrandAnalysisService experimentalBrandAnalysisService;

    @Operation(
            summary = "브랜드 분석",
            description = """
                    특정 명품/브랜드에 대해,
                    1) 브랜드 분석
                    2) 예상 가치 (비슷한 브랜드/시장 대조군 포함)
                    3) 향후 1~3년 예상 추세
                    를 한 번에 텍스트로 제공합니다.
                    
                    프론트에서는 반환된 report 문자열을 그대로 보여주면 됩니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "브랜드 분석 성공",
                    content = @Content(schema = @Schema(implementation = BrandAnalysisResponse.class))
            )
    })
    @PostMapping("/brand")
    public ResponseEntity<ApiResponse<BrandAnalysisResponse>> analyzeBrand(
            @Valid @RequestBody BrandAnalysisRequest request
    ) {
        BrandAnalysisResponse response = experimentalBrandAnalysisService.analyzeBrand(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(
            summary = "상품 분석",
            description = """
                    특정 상품에 대해,
                    1) 상품 분석
                    2) 예상 가치 (유사 상품/브랜드 대조군 포함)
                    3) 향후 1~3년 예상 추세
                    를 한 번에 텍스트로 제공합니다.
                    
                    요청에는 productName만 필요하며,
                    응답 report 문자열을 그대로 렌더링하면 됩니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "상품 분석 성공",
                    content = @Content(schema = @Schema(implementation = ProductAnalysisResponse.class))
            )
    })
    @PostMapping("/product")
    public ResponseEntity<ApiResponse<ProductAnalysisResponse>> analyzeProduct(
            @Valid @RequestBody ProductAnalysisRequest request
    ) {
        ProductAnalysisResponse response = experimentalBrandAnalysisService.analyzeProduct(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}

