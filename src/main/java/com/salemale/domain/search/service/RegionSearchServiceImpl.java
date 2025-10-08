package com.salemale.domain.search.service; // 지역 검색 서비스 구현체(텍스트 기반 부분일치)

import com.salemale.domain.region.entity.Region; // 지역 엔티티
import com.salemale.domain.region.repository.RegionRepository; // 지역 저장소
import com.salemale.domain.search.dto.RegionSearchResponse; // 지역 검색 결과 DTO
import com.salemale.domain.search.converter.RegionSearchConverter; // 엔티티 → DTO 변환 담당
import lombok.RequiredArgsConstructor; // Lombok: 생성자 자동 생성
import lombok.extern.slf4j.Slf4j; // Lombok: 로깅 지원
import org.springframework.stereotype.Service; // 스프링 서비스 빈 선언
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 처리

import java.util.List; // 리스트 컬렉션
import java.util.stream.Collectors; // 스트림 API의 collect 유틸리티

/**
 * RegionSearchServiceImpl: 지역 검색 로직을 실제로 구현하는 서비스 클래스입니다.
 *
 * - RegionSearchService 인터페이스를 구현합니다.
 * - ILIKE 연산자를 사용한 부분 일치 검색을 수행합니다.
 * - 검색 결과를 페이징 처리하여 성능을 최적화합니다.
 *
 * 검색 전략:
 * - 사용자 입력을 양쪽에 `%`를 붙여 부분 일치 패턴으로 변환합니다.
 * - 예: "강남" → "%강남%" (강남구, 강남동, 신강남역 등 모두 검색)
 * - 대소문자를 구분하지 않습니다(ILIKE 사용).
 *
 * 성능 최적화:
 * - 페이지 크기를 최대 5000으로 제한하여 메모리 과부하를 방지합니다.
 * - 읽기 전용 트랜잭션으로 선언하여 DB 성능을 향상시킵니다.
 *
 * 향후 개선 계획:
 * - 거리 기반 검색 추가(위경도 계산)
 * - 가중치 기반 정렬(시/군/구보다 읍/면/동 우선 등)
 * - Full-text search 엔진 연동(Elasticsearch 등)
 */
@Service // 스프링이 이 클래스를 서비스 빈으로 등록하여 컨트롤러에서 주입받을 수 있게 합니다.
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자를 자동 생성합니다.
@Slf4j // Lombok: log 객체를 자동으로 생성하여 검색 로그를 기록할 수 있게 합니다.
public class RegionSearchServiceImpl implements RegionSearchService { // RegionSearchService 인터페이스 구현

    // 의존성 선언: RequiredArgsConstructor로 자동 주입됩니다.
    private final RegionRepository regionRepository; // 지역 정보를 조회하는 저장소

    /**
     * 이름 기반 지역 검색(페이징 지원).
     *
     * - 사용자가 입력한 검색어(q)로 지역을 검색합니다.
     * - 시/군/구 또는 읍/면/동에 검색어가 포함된 모든 지역을 반환합니다.
     * - 페이지 번호와 크기를 지정하여 결과를 나누어 받을 수 있습니다.
     *
     * @param q 검색 키워드(예: "강남", "서울", "중구" 등)
     * @param page 페이지 번호(0부터 시작, 0=첫 페이지)
     * @param size 한 페이지의 크기(최대 5000으로 제한됨)
     * @return 검색된 지역 목록(RegionSearchResponse의 리스트)
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션: 데이터 변경 없이 조회만 수행하여 성능을 향상시킵니다.
    public List<RegionSearchResponse> searchPagedByNameOnly(String q, int page, int size) {
        // 1) 입력 검증 및 정규화: null 또는 공백 검색어는 빈 결과를 반환합니다.
        //    - trim: 앞뒤 공백 제거
        //    - isBlank: null, 빈 문자열, 공백만 있는 문자열 검증
        if (q == null || q.trim().isBlank()) {
            log.debug("검색어가 비어있어 빈 결과를 반환합니다.");
            return java.util.Collections.emptyList();
        }
        
        String trimmedQ = q.trim();
        
        // 2) 검색 패턴 생성: 정규화된 검색어 양쪽에 `%`를 붙여 부분 일치 패턴을 만듭니다.
        //    - 예: "강남" → "%강남%"
        //    - SQL ILIKE 연산자와 함께 사용하여 대소문자를 구분하지 않고 검색합니다.
        String pattern = "%" + trimmedQ + "%";

        // 3) 페이지 크기 검증: 너무 큰 값이 들어오면 메모리 부족이나 성능 저하를 일으킬 수 있습니다.
        //    - Math.max(size, 1): 최소 1개 이상
        //    - Math.min(..., 5000): 최대 5000개로 제한
        int limit = Math.min(Math.max(size, 1), 5000);

        // 4) 오프셋 계산: 페이지 번호에 따라 건너뛸 레코드 수를 계산합니다.
        //    - Math.max(page, 0): 음수 페이지 방지(0 이상만 허용)
        //    - offset = page * limit: 예) page=2, limit=10이면 offset=20 (21번째부터 조회)
        int offset = Math.max(page, 0) * limit;

        // 5) 데이터베이스 조회: 리포지토리를 통해 검색 패턴, 제한, 오프셋을 전달하여 지역을 조회합니다.
        //    - findAllByKeywordPaged: 네이티브 쿼리 또는 JPQL로 ILIKE 검색을 수행합니다.
        //    - 시/군/구, 읍/면/동 모든 필드를 검색 대상으로 합니다.
        List<Region> rows = regionRepository.findAllByKeywordPaged(pattern, limit, offset);

        // 6) DTO 변환: Region 엔티티를 RegionSearchResponse DTO로 변환합니다.
        //    - stream(): 리스트를 스트림으로 변환하여 함수형 처리를 시작합니다.
        //    - map(RegionSearchConverter::toResponse): 각 Region을 DTO로 변환합니다.
        //    - collect(Collectors.toList()): 스트림을 다시 리스트로 모읍니다.
        //    - 변환 로직은 RegionSearchConverter에 위임하여 책임을 분리합니다.
        return rows.stream()
                .map(RegionSearchConverter::toResponse) // 엔티티 → DTO 변환
                .collect(Collectors.toList()); // 결과를 리스트로 수집
    }
}

