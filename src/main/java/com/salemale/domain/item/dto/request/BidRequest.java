package com.salemale.domain.item.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BidRequest {

    @NotNull(message = "입찰 가격은 필수입니다.")
    private Integer bidPrice;
}