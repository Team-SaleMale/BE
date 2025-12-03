package com.salemale.domain.item.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PriceSuggestionRequest {

    @NotBlank(message = "상품명은 필수입니다.")
    private String productName;
}
