package com.salemale.domain.item.dto.response.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeInfoDTO {
    private List<String> tradeMethods;
    private String tradeDetails;
}