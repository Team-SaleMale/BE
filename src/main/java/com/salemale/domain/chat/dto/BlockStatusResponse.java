package com.salemale.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BlockStatusResponse {

    private boolean iBlockedPartner;   // 내가 상대를 차단했는지
    private boolean partnerBlockedMe;  // 상대가 나를 차단했는지
}
