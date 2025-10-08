package com.salemale.domain.region.entity; // 도메인: 행정구역(시/군구/읍면동) + 좌표

import com.salemale.global.common.BaseEntity; // 생성/수정/삭제(소프트) 시각 필드 포함
import jakarta.persistence.*; // JPA 매핑 전반(@Entity, @Table, @Column 등)
import lombok.AccessLevel; // 생성자 접근 제어(PROTECTED)
import lombok.AllArgsConstructor; // 모든 필드 생성자 자동 생성
import lombok.Builder; // 빌더 패턴 자동 생성
import lombok.Getter; // getter 자동 생성
import lombok.NoArgsConstructor; // 파라미터 없는 생성자

import java.math.BigDecimal; // 위경도(정밀도 유지 목적)

@Entity // 테이블과 1:1 매핑되는 엔티티
@Table(
        name = "region",
        indexes = {
                // 복합 조회 빈도가 높은 행정 3단계 인덱스(검색 최적화)
                @Index(name = "idx_region_sido_sigungu_eupmyeondong", columnList = "sido, sigungu, eupmyeondong"),
                // 시군구 단위 검색 최적화(리스트/통계 등)
                @Index(name = "idx_region_sigungu", columnList = "sigungu")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Region extends BaseEntity {

    @Id // 기본 키
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB의 IDENTITY 전략 사용
    @Column(name = "region_id")
    private Long regionId;

    // 행정구역: 시/도 (예: 서울특별시, 경기도)
    @Column(name = "sido", nullable = false, length = 50)
    private String sido;

    // 행정구역: 시/군/구 (예: 강남구, 수원시 권선구)
    @Column(name = "sigungu", nullable = false, length = 50)
    private String sigungu;

    // 행정구역: 읍/면/동 (예: 역삼동, 매탄1동)
    @Column(name = "eupmyeondong", nullable = false, length = 50)
    private String eupmyeondong;

    // 위도(WGS84) — 숫자 연산/정렬을 고려해 BigDecimal 사용(소수 정밀도 보존)
    @Column(name = "latitude", precision = 18, scale = 10, nullable = false)
    private BigDecimal latitude;

    // 경도(WGS84)
    @Column(name = "longitude", precision = 18, scale = 10, nullable = false)
    private BigDecimal longitude;
}