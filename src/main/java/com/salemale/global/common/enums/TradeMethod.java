package com.salemale.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeMethod {
    SHIPPING("택배"),
    IN_PERSON("직거래"),
    OTHER("기타");

    private final String description;
}