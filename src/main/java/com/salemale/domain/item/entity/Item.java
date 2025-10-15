package com.salemale.domain.item.entity;

import com.salemale.domain.item.converter.TradeMethodListConverter;
import com.salemale.domain.region.entity.Region;
import com.salemale.domain.user.entity.User;
import com.salemale.global.common.BaseEntity;
import com.salemale.global.common.enums.Category;
import com.salemale.global.common.enums.ItemStatus;
import com.salemale.global.common.enums.TradeMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    // 낙찰자id로 상품이 낙찰되기 전까진 null이다, 유찰되어도 null임
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "title", nullable = false, length = 30)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", columnDefinition = "VARCHAR(20)")
    private Category category;

    @Column(name = "current_price", nullable = false)
    private Integer currentPrice;

    @Column(name = "start_price", nullable = false)
    private Integer startPrice;

    @Column(name = "bid_increment", nullable = false)
    private Integer bidIncrement;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false, columnDefinition = "VARCHAR(20)")
    private ItemStatus itemStatus;

    @Convert(converter = TradeMethodListConverter.class)
    @Column(name = "trade_methods", nullable = false, columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<TradeMethod> tradeMethods = new ArrayList<>();

    @Column(name = "trade_details", length = 500)
    private String tradeDetails;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("imageOrder ASC")
    @Builder.Default
    private List<ItemImage> images = new ArrayList<>();

    // 상품 등록 지역 (판매자의 대표 동네)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    // 조회수컬럼 추가
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    // 입찰이 없을때 낙찰로 경매 상품 상태 변경
    public void completeAuction(User winner) {
        this.winner = winner;
        this.itemStatus = ItemStatus.SUCCESS;
    }

    // 입찰이 없었을 경우 유찰로 경매 상품 상태 변경
    public void failAuction() {
        this.itemStatus = ItemStatus.FAIL;
    }

    // 현재 입찰가 업데이트
    public void updateCurrentPrice(Integer newPrice) {
        this.currentPrice = newPrice;
    }

    // 경매가 종료되었는지 확인 @param newPrice 새로운 입찰가
    public boolean isAuctionEnded() {
        return LocalDateTime.now().isAfter(this.endTime);
    }

    // 경매가 입찰 중인지 확인 @return 입찰 가능 여부
    public boolean isBiddingStatus() {
        return this.itemStatus == ItemStatus.BIDDING;
    }

    // ===== 조회수 증가 메서드 추가 =====
    /**
     * 우선은 임시로 상품 상세보기 api 호출 될때마다 해당 상품 조회수 늘리는 로직으로 간편하게 구현하고
     * 추후에 더 세밀한 로직으로 변경 예정
     */
    public void incrementViewCount() {
        this.viewCount = this.viewCount == null ? 1L : this.viewCount + 1;
    }
}
