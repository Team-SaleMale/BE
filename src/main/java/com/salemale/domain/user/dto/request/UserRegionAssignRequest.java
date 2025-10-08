package com.salemale.domain.user.dto.request; // 유저-지역 할당 요청 DTO

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserRegionAssignRequest {
    // 사용자가 입력하는 'XX동' 명칭(정확 일치)
    @NotBlank
    @Size(max = 50)
    private String eupmyeondong;

    // 기본 지역으로 설정할지 여부(옵션)
    private boolean primary;
}
