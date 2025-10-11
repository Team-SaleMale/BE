package com.salemale.domain.search.converter; // 검색 응답 변환 전담

import com.salemale.domain.region.entity.Region;
import com.salemale.domain.search.dto.RegionSearchResponse;

/**
 * 검색 결과(Region 엔티티)를 프런트가 사용하기 쉬운 최소 필드(id, displayName)로 변환합니다.
 * 주: user 도메인 스타일의 주석을 유지합니다.
 */
public class RegionSearchConverter {

    public static RegionSearchResponse toResponse(Region region) {
        String dn = String.join(" ", region.getSido(), region.getSigungu(), region.getEupmyeondong());
        return RegionSearchResponse.builder()
                .regionId(region.getRegionId())
                .displayName(dn)
                .build();
    }
}


