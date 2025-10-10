package com.salemale.domain.item.entity;

import com.salemale.domain.user.entity.User;
import com.salemale.global.common.BaseEntity;
import com.salemale.global.common.enums.Category;
import com.salemale.global.common.enums.ItemStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private Integer price;

    @Column(name = "start_price", nullable = false)
    private Integer startPrice;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "photo_url", length = 100)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false, columnDefinition = "VARCHAR(20)")
    private ItemStatus itemStatus;
}
