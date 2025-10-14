package com.salemale.domain.region.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionInfoDTO {
    private String sido;
    private String sigungu;
    private String eupmyeondong;
}