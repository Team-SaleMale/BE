package com.salemale.domain.item.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 시장 가격 정보 엔티티
 * FastAPI 크롤링 결과를 저장하는 테이블
 */
@Entity
@Table(name = "market_price")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MarketPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 검색 키워드 (예: "아이폰 14 Pro")
     */
    @Column(name = "keyword", nullable = false, length = 200)
    private String keyword;

    /**
     * 플랫폼 (joongna, daangn 등)
     */
    @Column(name = "platform", nullable = false, length = 50)
    private String platform;

    /**
     * 평균 시세
     */
    @Column(name = "avg_price")
    private Integer avgPrice;

    /**
     * 최저가
     */
    @Column(name = "min_price")
    private Integer minPrice;

    /**
     * 최고가
     */
    @Column(name = "max_price")
    private Integer maxPrice;

    /**
     * 분석한 상품 수
     */
    @Column(name = "sample_count")
    private Integer sampleCount;

    /**
     * 크롤링 시각
     */
    @Column(name = "crawled_at", nullable = false)
    @Builder.Default
    private LocalDateTime crawledAt = LocalDateTime.now();
}
