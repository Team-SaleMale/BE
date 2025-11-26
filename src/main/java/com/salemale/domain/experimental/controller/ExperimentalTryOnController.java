package com.salemale.domain.experimental.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.experimental.dto.VirtualTryOnResponse;
import com.salemale.domain.experimental.service.ExperimentalTryOnService;
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
public class ExperimentalTryOnController {

    private final ExperimentalTryOnService experimentalTryOnService;

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

