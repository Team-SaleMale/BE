package com.salemale.domain.user.service; // 사용자 프로필 관리 서비스 구현체

import com.salemale.common.code.status.ErrorStatus; // 에러 코드 집합
import com.salemale.common.exception.GeneralException; // 커스텀 예외
import com.salemale.domain.item.service.ImageService; // 이미지 검증 서비스
import com.salemale.domain.region.dto.response.RegionInfoDTO; // 지역 정보 DTO
import com.salemale.domain.s3.service.S3Service; // S3 업로드 서비스
import com.salemale.domain.user.dto.request.NicknameUpdateRequest; // 닉네임 변경 요청 DTO
import com.salemale.domain.user.dto.request.PasswordUpdateRequest; // 비밀번호 변경 요청 DTO
import com.salemale.domain.user.dto.request.RangeSettingUpdateRequest; // 활동 반경 변경 요청 DTO
import com.salemale.domain.user.dto.response.UserProfileResponse; // 사용자 프로필 응답 DTO
import com.salemale.domain.user.entity.User; // 사용자 엔티티
import com.salemale.domain.user.entity.UserAuth; // 사용자 인증 엔티티
import com.salemale.domain.user.repository.UserAuthRepository; // 사용자 인증 저장소
import com.salemale.domain.user.repository.UserRegionRepository; // 사용자-지역 연결 저장소
import com.salemale.domain.user.repository.UserRepository; // 사용자 저장소
import com.salemale.global.common.enums.LoginType; // 인증 제공자 타입
import lombok.RequiredArgsConstructor; // Lombok: 생성자 자동 생성
import lombok.extern.slf4j.Slf4j; // Lombok: 로깅 지원
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 해시/검증
import org.springframework.stereotype.Service; // 서비스 빈 선언
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 처리
import org.springframework.web.multipart.MultipartFile; // 파일 업로드용 MultipartFile

import java.util.List; // 리스트 타입

/**
 * UserServiceImpl: 사용자 프로필 관리 로직을 실제로 구현하는 서비스 클래스입니다.
 *
 * - UserService 인터페이스를 구현합니다.
 * - 프로필 조회, 닉네임/비밀번호/설정 변경 등의 실제 로직을 처리합니다.
 * - 스프링 빈으로 등록되어 컨트롤러에서 주입받아 사용할 수 있습니다.
 *
 * 주요 책임:
 * 1. 사용자 프로필 조회 및 DTO 변환
 * 2. 닉네임, 활동 반경 등 프로필 정보 수정
 * 3. 비밀번호 변경 시 현재 비밀번호 검증 및 새 비밀번호 해시 저장
 */
@Service // 스프링이 이 클래스를 서비스 빈으로 등록하여 다른 곳에서 주입받을 수 있게 합니다.
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자를 자동 생성합니다.
@Slf4j // Lombok: log 객체를 자동으로 생성하여 로깅을 할 수 있게 합니다.
public class UserServiceImpl implements UserService { // UserService 인터페이스 구현

    // 의존성 선언: RequiredArgsConstructor로 자동 주입됩니다.
    private final UserRepository userRepository; // 사용자 프로필 조회/저장 저장소
    private final UserAuthRepository userAuthRepository; // 사용자 인증 정보 조회/저장 저장소
    private final UserRegionRepository userRegionRepository; // 사용자-지역 연결 저장소
    private final PasswordEncoder passwordEncoder; // 비밀번호 해시/검증 도구
    private final S3Service s3Service; // S3 파일 업로드/삭제 서비스
    private final ImageService imageService; // 이미지 파일 검증 서비스

    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회합니다.
     *
     * - JWT 토큰에서 추출한 사용자 ID로 프로필을 조회합니다.
     * - 민감한 정보(비밀번호 등)는 제외하고 반환합니다.
     *
     * @param userId 조회할 사용자의 ID (JWT에서 추출)
     * @return 사용자 프로필 정보 (UserProfileResponse)
     * @throws GeneralException 사용자를 찾을 수 없을 때 발생 (ErrorStatus.USER_NOT_FOUND)
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션: 데이터 변경 없이 조회만 수행합니다.
    public UserProfileResponse getMyProfile(Long userId) {
        // 1) 사용자 조회: 주어진 ID로 사용자를 찾습니다.
        //    - findById: Optional<User>를 반환합니다.
        //    - orElseThrow: 사용자가 없으면 예외를 던집니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        log.debug("프로필 조회 - 사용자 ID: {}, 닉네임: {}", user.getId(), user.getNickname());

        // 2) 사용자의 대표 지역 정보 조회: isPrimary=true인 대표 지역만 조회합니다.
        List<RegionInfoDTO> regions = userRegionRepository.findByPrimaryUser(user)
                .map(userRegion -> {
                    RegionInfoDTO regionInfo = RegionInfoDTO.builder()
                            .sido(userRegion.getRegion().getSido())
                            .sigungu(userRegion.getRegion().getSigungu())
                            .eupmyeondong(userRegion.getRegion().getEupmyeondong())
                            .build();
                    return List.of(regionInfo); // 단일 요소를 포함한 리스트로 변환
                })
                .orElse(List.of()); // 대표 지역이 없으면 빈 리스트

        // 3) 엔티티 → DTO 변환: UserProfileResponse.from() 정적 메서드를 사용하여 지역 정보를 포함합니다.
        return UserProfileResponse.from(user, regions);
    }

    /**
     * 사용자의 닉네임을 변경합니다.
     *
     * - 1자 이상 15자 이하의 닉네임으로 변경할 수 있습니다.
     * - 앞뒤 공백은 자동으로 제거됩니다.
     *
     * @param userId 닉네임을 변경할 사용자의 ID (JWT에서 추출)
     * @param request 닉네임 변경 요청 정보 (새 닉네임)
     * @return 변경된 프로필 정보 (UserProfileResponse)
     * @throws GeneralException 사용자를 찾을 수 없을 때 발생 (ErrorStatus.USER_NOT_FOUND)
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional // 데이터베이스 쓰기 작업을 트랜잭션으로 묶어 일관성을 보장합니다.
    public UserProfileResponse updateNickname(Long userId, NicknameUpdateRequest request) {
        // 1) 사용자 조회: 주어진 ID로 사용자를 찾습니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2) 닉네임 중복 체크: 회원가입과 동일한 검증 로직을 적용합니다.
        //    - 다른 사용자가 이미 사용 중인 닉네임인지 확인합니다.
        //    - 자신의 현재 닉네임과 같은 경우는 변경할 필요가 없으므로 중복 체크를 건너뜁니다.
        String newNickname = request.getNickname().trim();
        if (!user.getNickname().equals(newNickname) && userRepository.existsByNickname(newNickname)) {
            log.warn("닉네임 변경 실패 - 사용자 ID: {}, 원인: 닉네임 중복 ({})", userId, newNickname);
            throw new GeneralException(ErrorStatus.NICKNAME_ALREADY_EXISTS);
        }

        // 3) 닉네임 변경: User 엔티티의 updateNickname 메서드를 호출합니다.
        //    - 엔티티 메서드가 앞뒤 공백 제거 및 null 검증을 수행합니다.
        String oldNickname = user.getNickname();
        user.updateNickname(newNickname);

        log.info("닉네임 변경 - 사용자 ID: {}, 이전: {}, 변경: {}", userId, oldNickname, user.getNickname());

        // 4) 변경 감지: JPA의 Dirty Checking으로 자동으로 UPDATE 쿼리가 실행됩니다.
        //    - @Transactional 메서드가 종료될 때 변경사항이 감지되어 저장됩니다.
        //    - 명시적으로 save()를 호출하지 않아도 됩니다.

        // 5) 엔티티 → DTO 변환: 변경된 정보를 반환합니다.
        return UserProfileResponse.from(user);
    }

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
     * @throws GeneralException 사용자를 찾을 수 없을 때 발생 (ErrorStatus.USER_NOT_FOUND)
     * @throws GeneralException 현재 비밀번호가 일치하지 않거나 LOCAL 인증이 아닐 때 발생
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional // 데이터베이스 쓰기 작업을 트랜잭션으로 묶어 일관성을 보장합니다.
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        // 1) 사용자 조회: 주어진 ID로 사용자를 찾습니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2) LOCAL 인증 정보 조회: 이메일/비밀번호 로그인 계정만 비밀번호 변경이 가능합니다.
        //    - findByProviderAndUser: 특정 제공자(LOCAL)와 사용자로 인증 정보를 찾습니다.
        //    - orElseThrow: LOCAL 인증이 없으면 예외를 던집니다(소셜 로그인 계정).
        UserAuth userAuth = userAuthRepository.findByProviderAndUser(LoginType.LOCAL, user)
                .orElseThrow(() -> new GeneralException(ErrorStatus.AUTH_NOT_LOCAL_ACCOUNT));

        // 3) 현재 비밀번호 검증: 보안을 위해 현재 비밀번호를 먼저 확인합니다.
        //    - passwordEncoder.matches: BCrypt로 해시된 비밀번호와 평문을 비교합니다.
        //    - 일치하지 않으면 예외를 던져 변경을 거부합니다.
        if (!passwordEncoder.matches(request.getCurrentPassword(), userAuth.getPasswordHash())) {
            log.warn("비밀번호 변경 실패 - 사용자 ID: {}, 원인: 현재 비밀번호 불일치", userId);
            throw new GeneralException(ErrorStatus.AUTH_INVALID_CREDENTIALS);
        }

        // 4) 비밀번호 재사용 방지: 새 비밀번호가 현재 비밀번호와 같은지 확인합니다.
        //    - passwordEncoder.matches로 평문과 해시를 비교합니다.
        //    - 보안 정책: 비밀번호 재사용을 차단하여 보안을 강화합니다.
        //    - 재사용 시도 시 명확한 에러 메시지로 사용자에게 안내합니다.
        if (passwordEncoder.matches(request.getNewPassword(), userAuth.getPasswordHash())) {
            log.warn("비밀번호 변경 실패 - 사용자 ID: {}, 원인: 새 비밀번호가 현재 비밀번호와 동일", userId);
            throw new GeneralException(ErrorStatus.PASSWORD_REUSE_NOT_ALLOWED);
        }

        // 5) 새 비밀번호 해시 생성: BCrypt 등으로 새 비밀번호를 해시합니다.
        //    - passwordEncoder.encode: 평문 비밀번호를 안전한 해시값으로 변환합니다.
        String newHash = passwordEncoder.encode(request.getNewPassword());

        // 6) 비밀번호 업데이트: UserAuth 엔티티의 updatePasswordHash 메서드를 호출합니다.
        userAuth.updatePasswordHash(newHash);

        log.info("비밀번호 변경 완료 - 사용자 ID: {}", userId);

        // 7) 변경 감지: JPA의 Dirty Checking으로 자동으로 UPDATE 쿼리가 실행됩니다.
        //    - @Transactional 메서드가 종료될 때 변경사항이 감지되어 저장됩니다.
    }

    /**
     * 사용자의 활동 반경 설정을 변경합니다.
     *
     * - VERY_NEAR, NEAR, MEDIUM, FAR, ALL 중 하나로 변경할 수 있습니다.
     * - 변경된 반경은 지역 검색 시 즉시 반영됩니다.
     *
     * @param userId 활동 반경을 변경할 사용자의 ID (JWT에서 추출)
     * @param request 활동 반경 변경 요청 정보 (새 RangeSetting)
     * @return 변경된 프로필 정보 (UserProfileResponse)
     * @throws GeneralException 사용자를 찾을 수 없을 때 발생 (ErrorStatus.USER_NOT_FOUND)
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional // 데이터베이스 쓰기 작업을 트랜잭션으로 묶어 일관성을 보장합니다.
    public UserProfileResponse updateRangeSetting(Long userId, RangeSettingUpdateRequest request) {
        // 1) 사용자 조회: 주어진 ID로 사용자를 찾습니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2) 활동 반경 변경: User 엔티티의 changeRangeSetting 메서드를 호출합니다.
        //    - 엔티티 메서드가 null 검증 및 기본값 처리를 수행합니다.
        User.RangeSetting oldSetting = user.getRangeSetting();
        user.changeRangeSetting(request.getRangeSetting());

        log.info("활동 반경 변경 - 사용자 ID: {}, 이전: {}, 변경: {}", 
                userId, oldSetting, user.getRangeSetting());

        // 3) 변경 감지: JPA의 Dirty Checking으로 자동으로 UPDATE 쿼리가 실행됩니다.

        // 4) 엔티티 → DTO 변환: 변경된 정보를 반환합니다.
        return UserProfileResponse.from(user);
    }

    /**
     * 사용자의 프로필 이미지를 변경합니다.
     *
     * @param userId      프로필 이미지를 변경할 사용자의 ID
     * @param profileImage 업로드할 프로필 이미지 파일
     * @return 변경된 프로필 정보
     */
    @Override
    @Transactional
    public UserProfileResponse updateProfileImage(Long userId, MultipartFile profileImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        final long profileImageMaxSize = 50L * 1024 * 1024; // 50MB
        imageService.validateFile(profileImage, profileImageMaxSize, ErrorStatus.PROFILE_IMAGE_SIZE_EXCEEDED);

        String existingProfileImage = user.getProfileImage();
        String uploadedProfileImageUrl = s3Service.uploadUserProfileImage(userId, profileImage);

        user.updateProfileImage(uploadedProfileImageUrl);

        if (existingProfileImage != null && !existingProfileImage.equals(uploadedProfileImageUrl)) {
            s3Service.deleteFileByUrl(existingProfileImage);
        }

        log.info("프로필 이미지 변경 - 사용자 ID: {}, 신규 URL: {}", userId, uploadedProfileImageUrl);
        return UserProfileResponse.from(user);
    }
}

