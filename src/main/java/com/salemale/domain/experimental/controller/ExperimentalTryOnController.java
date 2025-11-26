package com.salemale.domain.experimental.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.experimental.dto.VirtualTryOnResponse;
import com.salemale.domain.experimental.service.ExperimentalTryOnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/experimental")
@Tag(name = "Experimental - TryOn & Analysis", description = "실험실 기능: 가상 피팅 및 브랜드/상품 분석 API")
public class ExperimentalTryOnController {

    private final ExperimentalTryOnService experimentalTryOnService;

    @Operation(
            summary = "가상 피팅 시뮬레이션",
            description = """
                    사람 전신 사진과 의류 이미지를 업로드하면, 외부 AI 서버(Hugging Face Space)를 통해 가상 피팅 결과를 생성합니다.
                    
                    - background: 사람(모델) 전신 이미지
                    - garment: 착용시킬 의류 이미지
                    - garment_desc: 의류 특징 설명(색상, 종류 등)
                    - crop: 모델 상반신만 사용할지 여부 (기본 false)
                    - denoise_steps: 이미지 생성 디노이즈 스텝 수 (기본 30)
                    - seed: 재현 가능한 결과를 위한 시드 값 (기본 42)
                    
                    처리 시간은 수십 초~1분 이상 걸릴 수 있습니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "가상 피팅 성공",
                    content = @Content(schema = @Schema(implementation = VirtualTryOnResponse.class))
            )
    })
    @PostMapping(
            value = "/virtual-tryon",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<VirtualTryOnResponse>> virtualTryOn(
            @RequestPart("background") MultipartFile background,
            @RequestPart("garment") MultipartFile garment,
            @RequestParam("garment_desc") @NotBlank String garmentDesc,
            @RequestParam(value = "crop", required = false) Boolean crop,
            @RequestParam(value = "denoise_steps", required = false) Integer denoiseSteps,
            @RequestParam(value = "seed", required = false) Integer seed
    ) {
        VirtualTryOnResponse response = experimentalTryOnService.requestVirtualTryOn(
                background,
                garment,
                garmentDesc,
                crop,
                denoiseSteps,
                seed
        );
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}

