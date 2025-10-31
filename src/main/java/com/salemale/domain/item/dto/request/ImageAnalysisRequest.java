package com.salemale.domain.item.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ImageAnalysisRequest {

    @NotBlank(message = "이미지 URL은 필수입니다.")
    private String imageUrl;
}