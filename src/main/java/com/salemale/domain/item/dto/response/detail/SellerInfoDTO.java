package com.salemale.domain.item.dto.response.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerInfoDTO {
    private Long sellerId;
    private String nickname;
    private Integer mannerScore;
    private String profileImage;
}