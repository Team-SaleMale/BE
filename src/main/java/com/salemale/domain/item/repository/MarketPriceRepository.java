package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    /**
     * 키워드와 플랫폼으로 최신 시세 정보 조회
     * @param keyword 검색 키워드
     * @param platform 플랫폼명
     * @param since 이 시각 이후의 데이터만 조회
     * @return 시세 정보 리스트
     */
    @Query("SELECT mp FROM MarketPrice mp WHERE mp.keyword = :keyword AND mp.platform = :platform AND mp.crawledAt >= :since ORDER BY mp.crawledAt DESC")
    List<MarketPrice> findRecentByKeywordAndPlatform(
            @Param("keyword") String keyword,
            @Param("platform") String platform,
            @Param("since") LocalDateTime since
    );

    /**
     * 키워드로 모든 플랫폼의 최신 시세 정보 조회
     * @param keyword 검색 키워드
     * @param since 이 시각 이후의 데이터만 조회
     * @return 시세 정보 리스트
     */
    @Query("SELECT mp FROM MarketPrice mp WHERE mp.keyword = :keyword AND mp.crawledAt >= :since ORDER BY mp.crawledAt DESC")
    List<MarketPrice> findRecentByKeyword(
            @Param("keyword") String keyword,
            @Param("since") LocalDateTime since
    );
}
