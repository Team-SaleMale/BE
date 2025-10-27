package com.salemale.domain.mypage.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자의 경매 참여 역할
 */
@Getter
@RequiredArgsConstructor
public enum MyRole {
    SELLER("SELLER", "판매자"),
    WINNER("WINNER", "낙찰자"),
    BIDDER("BIDDER", "입찰자");

    private final String code;
    private final String description;

    /**
     * JSON 응답 시 code 값만 반환 (기존 API 응답 형식 유지)
     */
    @JsonValue
    public String getCode() {
        return code;
    }
}
