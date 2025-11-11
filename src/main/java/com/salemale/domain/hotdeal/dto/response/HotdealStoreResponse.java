package com.salemale.domain.hotdeal.dto.response;

import com.salemale.domain.hotdeal.entity.HotdealStore;
import com.salemale.domain.hotdeal.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 핫딜 가게 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotdealStoreResponse {

    private Long storeId;
    private String storeName;
    private String address;
    private String detailAddress;
    private Double latitude;
    private Double longitude;
    private ApprovalStatus approvalStatus;

    /**
     * HotdealStore 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static HotdealStoreResponse from(HotdealStore store) {
        return HotdealStoreResponse.builder()
                .storeId(store.getStoreId())
                .storeName(store.getStoreName())
                .address(store.getAddress())
                .detailAddress(store.getDetailAddress())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .approvalStatus(store.getApprovalStatus())
                .build();
    }
}