package com.salemale.domain.search.dto; // 검색 응답 DTO(최소필드: id + displayName)

import com.salemale.domain.region.entity.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RegionSearchResponse {
    private Long regionId;      // 선택용 PK
    private String displayName; // 예: "서울특별시 강남구 역삼1동"

    public static RegionSearchResponse from(Region r) {
        String dn = String.join(" ", r.getSido(), r.getSigungu(), r.getEupmyeondong());
        return RegionSearchResponse.builder()
                .regionId(r.getRegionId())
                .displayName(dn)
                .build();
    }
}


