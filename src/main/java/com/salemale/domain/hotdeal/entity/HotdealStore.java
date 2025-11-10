package com.salemale.domain.hotdeal.entity;

import com.salemale.domain.region.entity.Region;
import com.salemale.domain.user.entity.User;
import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 핫딜 가게 정보를 저장하는 엔티티
 * - 사업자 등록증을 제출하고 승인된 가게만 핫딜 상품을 등록할 수 있습니다
 * - 관리자가 수동으로 등록하고 승인 처리합니다
 */
@Entity
@Table(name = "hotdeal_store")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class HotdealStore extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long storeId;

    // 가게 주인 (핫딜 승인된 사용자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(name = "store_name", nullable = false, length = 50)
    private String storeName;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "detail_address", length = 100)
    private String detailAddress;

    // 승인 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    // 가게 소속 지역 (위도/경도 기반으로 매핑)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    /**
     * 가게 승인 처리
     */
    public void approve() {
        this.approvalStatus = ApprovalStatus.APPROVED;
    }

    /**
     * 가게 승인 거절
     */
    public void reject() {
        this.approvalStatus = ApprovalStatus.REJECTED;
    }

    /**
     * 승인된 가게인지 확인
     */
    public boolean isApproved() {
        return this.approvalStatus == ApprovalStatus.APPROVED;
    }
}
