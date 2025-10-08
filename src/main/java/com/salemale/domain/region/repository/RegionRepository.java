package com.salemale.domain.region.repository; // Region 엔티티에 대한 데이터 접근 레이어

import com.salemale.domain.region.entity.Region; // 행정구역 엔티티
import org.springframework.data.jpa.repository.JpaRepository; // CRUD 기본 제공
import org.springframework.data.jpa.repository.Modifying; // 변경쿼리 표시
import org.springframework.data.jpa.repository.Query; // 네이티브 쿼리 사용
import org.springframework.data.repository.query.Param; // 파라미터 바인딩

import java.util.Optional; // 존재 여부를 명확히 표현
import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {

    // 행정 3단계(시/군구/읍면동)로 고유 레코드 조회 — 비즈니스 유니크키
    Optional<Region> findBySidoAndSigunguAndEupmyeondong(String sido, String sigungu, String eupmyeondong);
    
    // 행정 3단계(시/군구/읍면동)로 고유 레코드 조회 (복수 결과 허용 - 데이터 정합성 검증용)
    List<Region> findAllBySidoAndSigunguAndEupmyeondong(String sido, String sigungu, String eupmyeondong);

    // 동명으로 후보 조회(정확 일치). 동명이 여러 시군구에 존재할 수 있어 다수 반환 가능
    List<Region> findAllByEupmyeondong(String eupmyeondong);

    // 대량 적재 시 충돌(중복) 발생해도 안전하게 병합(UPSERT)하는 네이티브 쿼리
    // 원리: (sido,sigungu,eupmyeondong) 유니크 제약 기반 ON CONFLICT DO UPDATE
    @Modifying
    @Query(value = """
            INSERT INTO region (sido, sigungu, eupmyeondong, latitude, longitude, created_at, updated_at)
            VALUES (:sido, :sigungu, :eupmyeondong, :latitude, :longitude, NOW(), NOW())
            ON CONFLICT (sido, sigungu, eupmyeondong)
            DO UPDATE SET
              latitude = EXCLUDED.latitude,
              longitude = EXCLUDED.longitude,
              updated_at = NOW()
            """
            , nativeQuery = true)
    void upsert(
            @Param("sido") String sido,
            @Param("sigungu") String sigungu,
            @Param("eupmyeondong") String eupmyeondong,
            @Param("latitude") java.math.BigDecimal latitude,
            @Param("longitude") java.math.BigDecimal longitude
    );

    // 위/경도 범위(바운딩 박스) 조회 — 근처 지역 id 조회용으로 활용 가능
    @Query(value = """
            SELECT r.region_id
            FROM region r
            WHERE r.latitude  BETWEEN :minLat AND :maxLat
              AND r.longitude BETWEEN :minLon AND :maxLon
            """ , nativeQuery = true)
    java.util.List<Long> findIdsInBoundingBox(
            @Param("minLat") java.math.BigDecimal minLat,
            @Param("maxLat") java.math.BigDecimal maxLat,
            @Param("minLon") java.math.BigDecimal minLon,
            @Param("maxLon") java.math.BigDecimal maxLon
    );

    // 위/경도 범위(바운딩 박스) 조회 — 근처 지역 엔티티 전체 조회
    // - findIdsInBoundingBox는 ID만 반환하지만, 이 메서드는 Region 엔티티 전체를 반환합니다.
    // - 지역 정보를 클라이언트에 제공할 때 사용합니다.
    @Query(value = """
            SELECT * FROM region r
            WHERE r.latitude  BETWEEN :minLat AND :maxLat
              AND r.longitude BETWEEN :minLon AND :maxLon
            ORDER BY r.sido, r.sigungu, r.eupmyeondong
            """ , nativeQuery = true)
    List<Region> findAllInBoundingBox(
            @Param("minLat") java.math.BigDecimal minLat,
            @Param("maxLat") java.math.BigDecimal maxLat,
            @Param("minLon") java.math.BigDecimal minLon,
            @Param("maxLon") java.math.BigDecimal maxLon
    );

    // 검색어 기반(간단 ILIKE) — limit 적용, 정렬은 기본 이름순
    @Query(value = """
            SELECT * FROM region r
            WHERE r.sido ILIKE :pattern OR r.sigungu ILIKE :pattern OR r.eupmyeondong ILIKE :pattern
            ORDER BY r.sido, r.sigungu, r.eupmyeondong
            LIMIT :limit
            """ , nativeQuery = true)
    List<Region> findAllByKeyword(@Param("pattern") String pattern, @Param("limit") int limit);

    // 페이징 지원(I LIKE + limit/offset)
    @Query(value = """
            SELECT * FROM region r
            WHERE r.sido ILIKE :pattern OR r.sigungu ILIKE :pattern OR r.eupmyeondong ILIKE :pattern
            ORDER BY r.sido, r.sigungu, r.eupmyeondong
            LIMIT :limit OFFSET :offset
            """ , nativeQuery = true)
    List<Region> findAllByKeywordPaged(@Param("pattern") String pattern,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);
}


