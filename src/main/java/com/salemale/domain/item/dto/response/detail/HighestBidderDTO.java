package com.salemale.domain.item.dto.response.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighestBidderDTO {
    private Long userId;
    private String nickname;
    private Integer mannerScore;
    private String profileImage;
    private Integer bidPrice;
    private LocalDateTime bidTime;
}