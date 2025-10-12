package com.salemale.domain.user.service; // 사용자 프로필 관리 서비스 인터페이스

import com.salemale.domain.user.dto.request.NicknameUpdateRequest; // 닉네임 변경 요청 DTO
import com.salemale.domain.user.dto.request.PasswordUpdateRequest; // 비밀번호 변경 요청 DTO
import com.salemale.domain.user.dto.request.RangeSettingUpdateRequest; // 활동 반경 변경 요청 DTO
import com.salemale.domain.user.dto.response.UserProfileResponse; // 사용자 프로필 응답 DTO

/**
 * UserService: 사용자 프로필 관리를 담당하는 서비스 인터페이스입니다.
 *
 * - 로그인한 사용자의 프로필 조회 및 수정 기능을 제공합니다.
 * - 닉네임, 비밀번호, 활동 반경 등의 설정을 변경할 수 있습니다.
 * - 구현체(UserServiceImpl)에서 실제 비즈니스 로직을 처리합니다.
 *
 * 주요 기능:
 * 1. 프로필 조회: 현재 사용자의 프로필 정보를 반환합니다.
 * 2. 닉네임 변경: 사용자의 닉네임을 수정합니다.
 * 3. 비밀번호 변경: 현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.
 * 4. 활동 반경 변경: 사용자의 활동 반경 설정을 변경합니다.
 *
 * 비즈니스 규칙:
 * - 모든 메서드는 현재 로그인한 사용자만 조회/변경 가능합니다.
 * - 비밀번호 변경 시 현재 비밀번호를 먼저 검증합니다(보안).
 * - 닉네임은 1자 이상 15자 이하여야 합니다.
 */
public interface UserService {

    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회합니다.
     *
     * - JWT 토큰에서 추출한 사용자 ID로 프로필을 조회합니다.
     * - 민감한 정보(비밀번호 등)는 제외하고 반환합니다.
     *
     * @param userId 조회할 사용자의 ID (JWT에서 추출)
     * @return 사용자 프로필 정보 (UserProfileResponse)
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때 발생
     */
    UserProfileResponse getMyProfile(Long userId);

    /**
     * 사용자의 닉네임을 변경합니다.
     *
     * - 1자 이상 15자 이하의 닉네임으로 변경할 수 있습니다.
     * - 닉네임 중복 검사는 선택적으로 추가할 수 있습니다(현재는 미적용).
     * - 앞뒤 공백은 자동으로 제거됩니다.
     *
     * @param userId 닉네임을 변경할 사용자의 ID (JWT에서 추출)
     * @param request 닉네임 변경 요청 정보 (새 닉네임)
     * @return 변경된 프로필 정보 (UserProfileResponse)
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때 발생
     */
    UserProfileResponse updateNickname(Long userId, NicknameUpdateRequest request);

    /**
     * 사용자의 비밀번호를 변경합니다.
     *
     * - 보안을 위해 현재 비밀번호를 먼저 검증합니다.
     * - 현재 비밀번호가 일치하지 않으면 예외가 발생합니다.
     * - 새 비밀번호는 BCrypt 등으로 해시되어 저장됩니다.
     * - LOCAL 인증 제공자(이메일/비밀번호 로그인)에서만 사용 가능합니다.
     *
     * @param userId 비밀번호를 변경할 사용자의 ID (JWT에서 추출)
     * @param request 비밀번호 변경 요청 정보 (현재 비밀번호, 새 비밀번호)
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때 발생
     * @throws com.salemale.common.exception.GeneralException 현재 비밀번호가 일치하지 않거나 LOCAL 인증이 아닐 때 발생
     */
    void updatePassword(Long userId, PasswordUpdateRequest request);

    /**
     * 사용자의 활동 반경 설정을 변경합니다.
     *
     * - VERY_NEAR, NEAR, MEDIUM, FAR, ALL 중 하나로 변경할 수 있습니다.
     * - 변경된 반경은 지역 검색 시 즉시 반영됩니다.
     *
     * 거리 매핑:
     * - VERY_NEAR: 2km
     * - NEAR: 5km
     * - MEDIUM: 20km
     * - FAR: 50km
     * - ALL: 20000km
     *
     * @param userId 활동 반경을 변경할 사용자의 ID (JWT에서 추출)
     * @param request 활동 반경 변경 요청 정보 (새 RangeSetting)
     * @return 변경된 프로필 정보 (UserProfileResponse)
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때 발생
     */
    UserProfileResponse updateRangeSetting(Long userId, RangeSettingUpdateRequest request);
}

