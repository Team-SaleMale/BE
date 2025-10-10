package com.salemale.domain.item.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRegisterResponse {

    private Long sellerId;
    private Long itemId;
    private String title;
    private Integer startPrice;
    private LocalDateTime endTime; // 시분초까지 포함된 최종 종료 시간
    private LocalDateTime createdAt; // 등록일자
}