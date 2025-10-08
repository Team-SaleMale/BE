package com.salemale.domain.region.converter; // Region 엔티티 <-> DTO 변환 전담

import com.salemale.domain.region.dto.request.RegionCreateRequest;
import com.salemale.domain.region.dto.request.RegionUpdateRequest;
import com.salemale.domain.region.dto.response.RegionResponse;
import com.salemale.domain.region.entity.Region;

/**
 * 이 파일은 "지역(Region) 데이터"를 서로 다른 형태로 바꿔주는 변환기(Converter)입니다.
 * 무슨 일을 하나요?
 * 1) 사용자가 보낸 등록/수정 요청(RegionCreateRequest/RegionUpdateRequest)을
 *    DB에 저장할 수 있는 Region 엔티티로 바꿉니다.
 * 2) DB에서 읽어온 Region 엔티티를 화면/응답에 쓰기 좋은 RegionResponse로 바꿉니다.
 *
 * 왜 필요한가요?
 * - 컨트롤러/서비스는 "비즈니스 흐름"에 집중하고, 형태 변환(가공)은 이 클래스로 모아둡니다.
 * - 덕분에 화면 요구사항이 바뀌어도 변환 규칙만 여기서 바꾸면 전체 코드가 따라옵니다.
 */
public class RegionConverter {

    // 생성 요청 -> 엔티티
    public static Region toEntity(RegionCreateRequest req) {
        return Region.builder()
                .sido(req.getSido())
                .sigungu(req.getSigungu())
                .eupmyeondong(req.getEupmyeondong())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .build();
    }

    /**
     * In-place 업데이트: 관리 중인(managed) Region 엔티티를 직접 수정합니다.
     * 
     * - 새 인스턴스를 생성하지 않고 기존 엔티티의 필드를 변경합니다.
     * - BaseEntity 필드(createdAt, updatedAt, deletedAt) 및 연관관계 보존
     * - JPA의 변경 감지(Dirty Checking)가 자동으로 UPDATE 쿼리 실행
     * 
     * @param current 관리 중인 Region 엔티티 (영속성 컨텍스트 내)
     * @param req 부분 수정 요청 (null 필드는 무시)
     */
    public static void applyUpdateInPlace(Region current, RegionUpdateRequest req) {
        current.updateFields(
                req.getSido(),
                req.getSigungu(),
                req.getEupmyeondong(),
                req.getLatitude(),
                req.getLongitude()
        );
    }
    
    /**
     * @deprecated 새 인스턴스 생성 방식은 BaseEntity 필드를 덮어씌울 수 있습니다.
     * 대신 {@link #applyUpdateInPlace(Region, RegionUpdateRequest)}를 사용하세요.
     */
    @Deprecated
    public static Region applyUpdate(Region current, RegionUpdateRequest req) {
        return Region.builder()
                .regionId(current.getRegionId())
                .sido(req.getSido() != null ? req.getSido() : current.getSido())
                .sigungu(req.getSigungu() != null ? req.getSigungu() : current.getSigungu())
                .eupmyeondong(req.getEupmyeondong() != null ? req.getEupmyeondong() : current.getEupmyeondong())
                .latitude(req.getLatitude() != null ? req.getLatitude() : current.getLatitude())
                .longitude(req.getLongitude() != null ? req.getLongitude() : current.getLongitude())
                .build();
    }

    // 엔티티 -> 응답 DTO
    public static RegionResponse toResponse(Region region) {
        return RegionResponse.builder()
                .regionId(region.getRegionId())
                .sido(region.getSido())
                .sigungu(region.getSigungu())
                .eupmyeondong(region.getEupmyeondong())
                .latitude(region.getLatitude())
                .longitude(region.getLongitude())
                .build();
    }
}


