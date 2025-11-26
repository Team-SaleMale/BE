package com.salemale.domain.experimental.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.experimental.dto.VirtualTryOnResponse;
import com.salemale.domain.experimental.service.ExperimentalTryOnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
                    
                    - background: 사람(모델) 전신 이미지 (필수)
                    - garment: 착용시킬 의류 이미지 (필수)
                    
                    기타 파라미터(설명, crop, denoise_steps, seed)는 서버에서 공통 기본값으로 설정하여 전송합니다.
                    
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
            @RequestPart("garment") MultipartFile garment
    ) {
        VirtualTryOnResponse response = experimentalTryOnService.requestVirtualTryOn(
                background,
                garment
        );
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}

