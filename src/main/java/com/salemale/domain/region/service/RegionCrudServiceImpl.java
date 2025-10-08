package com.salemale.domain.region.service; // 지역 CRUD + 주변 조회 비즈니스 로직 구현체

import com.salemale.common.code.status.ErrorStatus; // 에러 코드 집합
import com.salemale.common.exception.GeneralException; // 커스텀 예외
import com.salemale.domain.region.dto.request.RegionCreateRequest; // 지역 생성 요청 DTO
import com.salemale.domain.region.dto.request.RegionUpdateRequest; // 지역 수정 요청 DTO
import com.salemale.domain.region.dto.response.RegionResponse; // 지역 응답 DTO
import com.salemale.domain.region.entity.Region; // 지역 엔티티
import com.salemale.domain.region.repository.RegionRepository; // 지역 저장소
import com.salemale.domain.region.converter.RegionConverter; // 엔티티 ↔ DTO 변환 담당
import com.salemale.domain.user.entity.User; // 사용자 엔티티
import com.salemale.domain.user.entity.UserRegion; // 사용자-지역 연결 엔티티
import com.salemale.domain.user.repository.UserRepository; // 사용자 저장소
import com.salemale.domain.user.repository.UserRegionRepository; // 사용자-지역 연결 저장소
import com.salemale.global.security.jwt.CurrentUserProvider; // JWT에서 현재 사용자 추출
import lombok.RequiredArgsConstructor; // Lombok: 생성자 자동 생성
import lombok.extern.slf4j.Slf4j; // Lombok: 로깅 지원
import org.springframework.stereotype.Service; // 스프링 서비스 빈 선언
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 처리

import jakarta.servlet.http.HttpServletRequest; // HTTP 요청 객체
import java.math.BigDecimal; // 위경도 정밀도 표현
import java.util.List; // 리스트 컬렉션

/**
 * RegionCrudServiceImpl: 지역 데이터의 CRUD 작업 및 사용자 기반 근처 조회를 구현하는 서비스 클래스입니다.
 *
 * - RegionCrudService 인터페이스를 구현합니다.
 * - 지역 생성, 수정, 삭제 및 주변 지역 조회 기능을 제공합니다.
 * - RegionConverter를 사용하여 엔티티와 DTO 간 변환을 담당합니다.
 *
 * CRUD 작업:
 * - Create: 새로운 지역을 저장합니다.
 * - Update: 기존 지역의 정보를 부분 수정합니다.
 * - Delete: 지역을 삭제합니다(멱등성 보장).
 *
 * 인근 지역 조회:
 * - 사용자 기반: JWT에서 사용자를 추출하고, RangeSetting에 따라 주변 지역을 조회합니다.
 * - 바운딩 박스 방식으로 위경도를 킬로미터로 환산하여 사각형 영역을 만듭니다.
 */
@Service // 스프링이 이 클래스를 서비스 빈으로 등록하여 컨트롤러에서 주입받을 수 있게 합니다.
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자를 자동 생성합니다.
@Slf4j // Lombok: log 객체를 자동으로 생성하여 로깅을 할 수 있게 합니다.
public class RegionCrudServiceImpl implements RegionCrudService { // RegionCrudService 인터페이스 구현

    // 의존성 선언: RequiredArgsConstructor로 자동 주입됩니다.
    private final RegionRepository regionRepository; // 지역 정보를 조회/저장/삭제하는 저장소
    private final UserRepository userRepository; // 사용자 정보를 조회하는 저장소
    private final UserRegionRepository userRegionRepository; // 사용자-지역 연결 정보를 조회하는 저장소
    private final CurrentUserProvider currentUserProvider; // JWT에서 현재 사용자 ID를 추출하는 유틸리티

    /**
     * 새로운 지역을 생성합니다.
     *
     * - 시도, 시군구, 읍면동, 위도, 경도 정보를 받아 저장합니다.
     * - 유니크 제약(sido, sigungu, eupmyeondong)에 위배되면 예외가 발생할 수 있습니다.
     *
     * @param req 지역 생성 요청 정보(시도, 시군구, 읍면동, 위경도)
     * @return 생성된 지역 정보(RegionResponse)
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional // 데이터베이스 쓰기 작업을 트랜잭션으로 묶어 일관성을 보장합니다.
    public RegionResponse create(RegionCreateRequest req) {
        // 1) DTO → 엔티티 변환: RegionConverter를 사용하여 요청 DTO를 Region 엔티티로 변환합니다.
        //    - 변환 로직은 컨버터에 위임하여 서비스 코드를 간결하게 유지합니다.
        Region region = RegionConverter.toEntity(req);

        // 2) 데이터베이스 저장: 변환된 엔티티를 저장하고, ID가 부여된 엔티티를 반환받습니다.
        //    - save: JPA가 제공하는 메서드로 INSERT 쿼리를 실행합니다.
        //    - 유니크 제약(sido, sigungu, eupmyeondong)이 있어 중복 시 예외가 발생할 수 있습니다.
        region = regionRepository.save(region);

        // 3) 엔티티 → DTO 변환: 저장된 엔티티를 응답 DTO로 변환하여 반환합니다.
        return RegionConverter.toResponse(region);
    }

    /**
     * 기존 지역 정보를 수정합니다.
     *
     * - null이 아닌 필드만 부분적으로 수정합니다(Partial Update).
     * - 예: 위경도만 변경하고 싶으면 latitude, longitude만 입력하면 됩니다.
     *
     * @param regionId 수정할 지역의 ID
     * @param req 수정할 정보(null이 아닌 필드만 반영)
     * @return 수정된 지역 정보(RegionResponse)
     * @throws GeneralException 지역을 찾을 수 없을 때 발생 (ErrorStatus.REGION_NOT_FOUND)
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional // 데이터베이스 쓰기 작업을 트랜잭션으로 묶어 일관성을 보장합니다.
    public RegionResponse update(Long regionId, RegionUpdateRequest req) {
        // 1) 지역 조회: 주어진 ID로 기존 지역을 찾습니다.
        //    - findById: Optional<Region>을 반환합니다.
        //    - orElseThrow: 지역이 없으면 예외를 던집니다.
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new GeneralException(com.salemale.common.code.status.ErrorStatus.REGION_NOT_FOUND));

        // 2) 부분 수정 적용: RegionConverter를 사용하여 null이 아닌 필드만 기존 엔티티에 반영합니다.
        //    - applyUpdate: 기존 엔티티와 수정 요청을 받아 업데이트된 엔티티를 반환합니다.
        //    - null 필드는 무시되므로 원하는 필드만 선택적으로 수정할 수 있습니다.
        region = RegionConverter.applyUpdate(region, req);

        // 3) 데이터베이스 저장: JPA의 변경 감지(Dirty Checking) 또는 명시적 save로 UPDATE 쿼리를 실행합니다.
        region = regionRepository.save(region);

        // 4) 엔티티 → DTO 변환: 수정된 엔티티를 응답 DTO로 변환하여 반환합니다.
        return RegionConverter.toResponse(region);
    }

    /**
     * 지역을 삭제합니다.
     *
     * - 이미 삭제되었거나 존재하지 않는 지역을 삭제해도 예외가 발생하지 않습니다(멱등성).
     * - 실제 서비스에서 소프트 삭제가 필요하다면 BaseEntity의 deletedAt을 사용할 수 있습니다.
     *
     * @param regionId 삭제할 지역의 ID
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional // 데이터베이스 쓰기 작업을 트랜잭션으로 묶어 일관성을 보장합니다.
    public void delete(Long regionId) {
        // 1) 존재 확인: 지역이 존재하지 않으면 조기 반환하여 멱등성을 보장합니다.
        //    - existsById: 해당 ID의 레코드가 있는지 확인합니다.
        //    - !existsById: 없으면 return으로 메서드를 종료합니다(예외를 던지지 않음).
        if (!regionRepository.existsById(regionId)) return;

        // 2) 삭제 실행: 지역이 존재하면 DELETE 쿼리를 실행합니다.
        //    - deleteById: 주어진 ID의 레코드를 삭제합니다.
        //    - 여러 번 호출해도 안전합니다(멱등성 보장).
        regionRepository.deleteById(regionId);
    }


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
     * @throws GeneralException 사용자를 찾을 수 없거나 UserRegion이 설정되지 않았을 때 발생
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션: 데이터 변경 없이 조회만 수행합니다.
    public List<Long> findNearbyRegionIdsForCurrentUser(HttpServletRequest request) {
        // 1) JWT 토큰에서 현재 사용자 ID 추출
        //    - CurrentUserProvider: Authorization 헤더에서 Bearer 토큰을 추출하여 subject(사용자 식별자)를 가져옵니다.
        //    - subject가 숫자면 userId로 간주하고, 아니면 이메일로 간주하여 User를 조회합니다.
        Long userId = currentUserProvider.getCurrentUserId(request);
        log.debug("Finding nearby regions for user ID: {}", userId);

        // 2) 사용자 조회: User 엔티티를 가져와 RangeSetting을 확인합니다.
        //    - findById: Optional<User>를 반환합니다.
        //    - orElseThrow: 사용자가 없으면 예외를 던집니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 3) 사용자의 UserRegion 조회: 사용자가 설정한 활동 동네를 가져옵니다.
        //    - findAllByUser: 해당 사용자의 모든 UserRegion을 조회합니다(현재는 1:1이므로 1개 이하).
        //    - stream().findFirst(): 첫 번째 UserRegion을 선택합니다.
        //    - orElseThrow: UserRegion이 없으면 예외를 던집니다.
        UserRegion userRegion = userRegionRepository.findAllByUser(user).stream()
                .findFirst()
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_REGION_NOT_SET));

        // 4) Region에서 위도/경도 추출: UserRegion에 연결된 Region의 좌표를 가져옵니다.
        Region baseRegion = userRegion.getRegion();
        BigDecimal centerLat = baseRegion.getLatitude(); // 중심점 위도
        BigDecimal centerLon = baseRegion.getLongitude(); // 중심점 경도

        log.debug("User's region: {} (lat={}, lon={})", 
                baseRegion.getEupmyeondong(), centerLat, centerLon);

        // 5) RangeSetting을 거리(km)로 변환: 사용자가 설정한 활동 반경을 가져옵니다.
        //    - getRangeInKilometers(): User 엔티티에 추가한 메서드로, RangeSetting을 km로 변환합니다.
        //    - 예: NEAR → 5.0km, FAR → 50.0km
        double radiusKm = user.getRangeInKilometers();

        log.debug("User's range setting: {} ({}km)", user.getRangeSetting(), radiusKm);

        // 6) 바운딩 박스 계산: 중심점과 반경을 사용하여 사각형 영역을 만듭니다.
        //    - 위도 1도 ≈ 111km
        //    - 경도 1도 ≈ 111km × cos(위도)
        double lat = centerLat.doubleValue();
        double lon = centerLon.doubleValue();
        double latDegree = radiusKm / 111.0; // 위도 차이
        double lonDegree = radiusKm / (111.0 * Math.cos(Math.toRadians(lat))); // 경도 차이

        BigDecimal minLat = BigDecimal.valueOf(lat - latDegree); // 최소 위도(남쪽 경계)
        BigDecimal maxLat = BigDecimal.valueOf(lat + latDegree); // 최대 위도(북쪽 경계)
        BigDecimal minLon = BigDecimal.valueOf(lon - lonDegree); // 최소 경도(서쪽 경계)
        BigDecimal maxLon = BigDecimal.valueOf(lon + lonDegree); // 최대 경도(동쪽 경계)

        // 7) 데이터베이스 조회: 바운딩 박스 내의 지역 ID 목록을 조회합니다.
        //    - findIdsInBoundingBox: Region의 ID만 반환합니다(전체 정보가 아니라).
        //    - 이 ID 목록은 다른 API(상품 조회 등)에서 필터 조건으로 사용됩니다.
        List<Long> nearbyRegionIds = regionRepository.findIdsInBoundingBox(minLat, maxLat, minLon, maxLon);

        log.debug("Found {} nearby regions within {}km radius", nearbyRegionIds.size(), radiusKm);

        // 8) 지역 ID 목록 반환: 상품 목록 API 등에서 "WHERE region_id IN (...)" 조건으로 활용 가능
        return nearbyRegionIds;
    }
}

