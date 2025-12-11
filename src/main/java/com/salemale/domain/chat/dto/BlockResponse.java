package com.salemale.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockResponse {

    private Long blockedUserId;   // 내가 차단하거나 해제한 상대의 ID
    private boolean blocked;      // true = 차단됨, false = 차단 해제됨
}
