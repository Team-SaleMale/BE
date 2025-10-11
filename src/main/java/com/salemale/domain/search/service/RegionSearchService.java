package com.salemale.domain.search.service; // 지역 검색 서비스 인터페이스

import com.salemale.domain.search.dto.RegionSearchResponse; // 지역 검색 결과 DTO
import java.util.List; // 리스트 컬렉션

/**
 * RegionSearchService: 지역을 검색하는 서비스 인터페이스입니다.
 *
 * - 사용자가 입력한 검색어로 시/군/구, 읍/면/동을 검색하는 기능을 제공합니다.
 * - 페이징을 지원하여 대량의 검색 결과를 효율적으로 처리합니다.
 * - 구현체(RegionSearchServiceImpl)에서 실제 검색 로직을 처리합니다.
 *
 * 주요 기능:
 * 1. 이름 기반 검색: 사용자가 입력한 키워드로 지역을 검색합니다.
 * 2. 페이징 지원: 검색 결과를 페이지 단위로 나누어 반환합니다.
 *
 * 검색 원리:
 * - ILIKE 연산자를 사용하여 부분 일치 검색을 수행합니다.
 * - 시/군/구, 읍/면/동 모든 필드를 검색 대상으로 합니다.
 * - 예: "강남"을 입력하면 "강남구", "강남동" 등이 모두 검색됩니다.
 */
public interface RegionSearchService {

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
    List<RegionSearchResponse> searchPagedByNameOnly(String q, int page, int size);
}
