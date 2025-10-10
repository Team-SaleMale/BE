package com.salemale.domain.user.service; // 사용자-지역 할당 서비스 인터페이스

/**
 * UserRegionService: 사용자의 활동 동네를 관리하는 서비스 인터페이스입니다.
 *
 * - 사용자가 활동할 동네를 등록/변경하는 기능을 제공합니다.
 * - 사용자의 현재 주 활동 동네를 조회하는 기능을 제공합니다.
 * - 구현체(UserRegionServiceImpl)에서 실제 비즈니스 로직을 처리합니다.
 *
 * 주요 기능:
 * 1. 동네 설정: 선택된 지역 ID로 사용자의 동네를 직접 설정합니다.
 * 2. 현재 동네 조회: 사용자의 주 활동 동네 ID를 반환합니다.
 *
 * 비즈니스 규칙:
 * - 현재는 사용자당 1개의 동네만 등록 가능합니다(1:1 관계).
 * - 새로운 동네를 설정하면 기존 동네는 자동으로 해제됩니다.
 * - isPrimary는 항상 true로 설정됩니다(단일 관계이므로).
 */
public interface UserRegionService {

    /**
     * 선택된 지역 ID로 사용자의 동네를 직접 설정합니다.
     *
     * - 2단계 프로세스(검색 → 선택)에서 사용됩니다.
     * - 사용자가 여러 검색 결과 중 하나를 선택한 경우 이 메서드를 호출합니다.
     * - 사용자의 기존 동네를 해제하고 새 동네를 설정합니다.
     *
     * @param userId 동네를 설정할 사용자 ID
     * @param regionId 설정할 지역 ID
     * @param primary 주 활동 동네 여부(현재는 항상 true)
     * @return 설정된 지역(Region)의 ID
     * @throws com.salemale.common.exception.GeneralException 사용자를 찾을 수 없을 때 (ErrorStatus.USER_NOT_FOUND) 또는 지역을 찾을 수 없을 때 (ErrorStatus.REGION_NOT_FOUND)
     */
    Long setRegionForUser(Long userId, Long regionId, boolean primary);

    /**
     * 사용자의 현재 주 활동 동네 ID를 조회합니다.
     *
     * - isPrimary=true인 UserRegion을 찾아 해당 지역 ID를 반환합니다.
     * - 등록된 동네가 없으면 null을 반환합니다.
     *
     * @param userId 동네를 조회할 사용자 ID
     * @return 주 활동 동네의 지역 ID, 없으면 null
     * @throws com.salemale.common.exception.GeneralException 사용자를 찾을 수 없을 때 (ErrorStatus.USER_NOT_FOUND)
     */
    Long getPrimaryRegionId(Long userId);
}
