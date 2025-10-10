package com.salemale.domain.region.service; // 지역 CRUD 서비스 인터페이스

import com.salemale.domain.region.dto.request.RegionCreateRequest; // 지역 생성 요청 DTO
import com.salemale.domain.region.dto.request.RegionUpdateRequest; // 지역 수정 요청 DTO
import com.salemale.domain.region.dto.response.RegionResponse; // 지역 응답 DTO

import java.util.List; // 리스트 컬렉션

/**
 * RegionCrudService: 지역(Region) 데이터의 생성/조회/수정/삭제를 관리하는 서비스 인터페이스입니다.
 *
 * - 기본적인 CRUD(Create, Read, Update, Delete) 기능을 제공합니다.
 * - 사용자 기반 인근 지역 조회 기능을 제공합니다.
 * - 구현체(RegionCrudServiceImpl)에서 실제 비즈니스 로직을 처리합니다.
 *
 * 주요 기능:
 * 1. 생성(Create): 새로운 지역을 등록합니다.
 * 2. 수정(Update): 기존 지역 정보를 부분 수정합니다.
 * 3. 삭제(Delete): 지역을 삭제합니다(멱등성 보장).
 * 4. 사용자 기반 인근 지역 조회: 로그인한 사용자 중심으로 주변 지역 ID를 반환합니다.
 *
 * 비즈니스 규칙:
 * - 지역은 시도/시군구/읍면동 조합으로 유니크하게 관리됩니다.
 * - 중복된 지역을 생성하려 하면 예외가 발생할 수 있습니다.
 */
public interface RegionCrudService {

    /**
     * 새로운 지역을 생성합니다.
     *
     * - 시도, 시군구, 읍면동, 위도, 경도 정보를 받아 저장합니다.
     * - 유니크 제약(sido, sigungu, eupmyeondong)에 위배되면 예외가 발생할 수 있습니다.
     *
     * @param req 지역 생성 요청 정보(시도, 시군구, 읍면동, 위경도)
     * @return 생성된 지역 정보(RegionResponse)
     */
    RegionResponse create(RegionCreateRequest req);

    /**
     * 기존 지역 정보를 수정합니다.
     *
     * - null이 아닌 필드만 부분적으로 수정합니다(Partial Update).
     * - 예: 위경도만 변경하고 싶으면 latitude, longitude만 입력하면 됩니다.
     *
     * @param regionId 수정할 지역의 ID
     * @param req 수정할 정보(null이 아닌 필드만 반영)
     * @return 수정된 지역 정보(RegionResponse)
     * @throws IllegalArgumentException 지역을 찾을 수 없을 때 발생
     */
    RegionResponse update(Long regionId, RegionUpdateRequest req);

    /**
     * 지역을 삭제합니다.
     *
     * - 이미 삭제되었거나 존재하지 않는 지역을 삭제해도 예외가 발생하지 않습니다(멱등성).
     * - 실제 서비스에서 소프트 삭제가 필요하다면 BaseEntity의 deletedAt을 사용할 수 있습니다.
     *
     * @param regionId 삭제할 지역의 ID
     */
    void delete(Long regionId);

    /**
     * 현재 로그인한 사용자의 동네를 기준으로 주변 지역 ID 목록을 조회합니다.
     *
     * - JWT에서 현재 사용자를 추출합니다.
     * - 사용자의 UserRegion에서 위도/경도를 가져옵니다.
     * - 사용자의 RangeSetting에 따라 반경을 결정합니다.
     * - 해당 반경 내의 모든 지역 ID를 반환합니다.
     *
     * 동작 흐름:
     * 1. JWT 토큰에서 사용자 ID 추출
     * 2. 사용자의 UserRegion 조회 (1:1 매칭)
     * 3. UserRegion의 Region에서 위도/경도 추출
     * 4. 사용자의 RangeSetting을 거리(km)로 변환
     * 5. 해당 반경 내의 Region ID 목록 조회 및 반환
     *
     * 반환되는 ID 목록은 다른 API(예: 상품 목록)에서 "내 동네 근처만 보기" 필터로 사용할 수 있습니다.
     *
     * @param request HTTP 요청 객체 (JWT 토큰 추출용)
     * @return 반경 내의 지역 ID 목록
     * @throws IllegalStateException 사용자를 찾을 수 없거나 UserRegion이 없을 때 발생
     */
    List<Long> findNearbyRegionIdsForCurrentUser(jakarta.servlet.http.HttpServletRequest request);
}
