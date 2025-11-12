package com.salemale.domain.mypage.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.region.service.RegionCrudService;
import com.salemale.domain.user.dto.request.NicknameUpdateRequest;
import com.salemale.domain.user.dto.request.PasswordUpdateRequest;
import com.salemale.domain.user.dto.request.RangeSettingUpdateRequest;
import com.salemale.domain.user.dto.response.UserProfileResponse;
import com.salemale.domain.user.service.UserRegionService;
import com.salemale.domain.user.service.UserService;
import com.salemale.global.security.jwt.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "마이페이지 - 프로필", description = """
        사용자 프로필 및 설정 관리 API
        
        **제공 기능:**
        - 프로필 조회: 현재 로그인한 사용자의 프로필 정보 조회
        - 닉네임 변경: 사용자 닉네임 수정
        - 비밀번호 재설정: LOCAL 계정 비밀번호 변경
        - 활동 반경 변경: 상품 검색 시 사용되는 거리 범위 설정
        - 활동 동네 설정: 주/부 활동 동네 설정 및 변경
        - 근처 지역 조회: 사용자 동네 기준 반경 내 지역 ID 목록 조회
        
        **인증 요구사항:**
        - 모든 API는 인증 필요 (로그인 필수)
        """)
public class MypageProfileController {

    private final UserService userService;
    private final UserRegionService userRegionService;
    private final RegionCrudService regionCrudService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(
            summary = "내 프로필 조회",
            description = """
                    현재 로그인한 사용자의 프로필 정보를 조회합니다.
                    
                    **반환 정보:**
                    - 사용자 ID, 이메일, 닉네임
                    - 프로필 이미지 URL
                    - 활동 반경 설정 (RangeSetting)
                    - 주 활동 동네 정보
                    - 계정 타입 (LOCAL, KAKAO, NAVER 등)
                    
                    **사용 예시:**
                    - 마이페이지 진입 시 사용자 정보 표시
                    - 프로필 수정 전 현재 정보 확인
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)")
    })
    @GetMapping
    public ApiResponse<UserProfileResponse> getMyProfile(@Parameter(hidden = true) HttpServletRequest request) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        UserProfileResponse profile = userService.getMyProfile(userId);
        return ApiResponse.onSuccess(profile);
    }

    @Operation(
            summary = "닉네임 변경",
            description = """
                    사용자의 닉네임을 새로운 값으로 변경합니다.
                    
                    **유효성 검사:**
                    - 닉네임 길이: 2자 이상 20자 이하
                    - 중복 확인: 다른 사용자와 중복 불가
                    - 특수문자 제한: 허용된 문자만 사용 가능
                    
                    **주의사항:**
                    - 닉네임 변경 후 즉시 반영됨
                    - 변경 이력은 저장되지 않음
                    
                    **요청 예시:**
                    ```json
                    {
                      "nickname": "새로운닉네임"
                    }
                    ```
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "닉네임 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 닉네임 (길이, 중복, 특수문자 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)")
    })
    @PatchMapping("/nickname")
    public ApiResponse<UserProfileResponse> updateNickname(
            @Parameter(hidden = true) HttpServletRequest request,
            @RequestBody @Valid NicknameUpdateRequest updateRequest
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        UserProfileResponse profile = userService.updateNickname(userId, updateRequest);
        return ApiResponse.onSuccess(profile);
    }

    @Operation(
            summary = "비밀번호 재설정",
            description = """
                    현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.
                    
                    **제한사항:**
                    - LOCAL 계정만 가능 (소셜 로그인 계정 불가)
                    - 현재 비밀번호 확인 필수
                    
                    **비밀번호 규칙:**
                    - 최소 8자 이상
                    - 영문, 숫자, 특수문자 조합 권장
                    - 현재 비밀번호와 동일 불가
                    
                    **보안:**
                    - 비밀번호는 BCrypt로 해시되어 저장됨
                    - 변경 후 즉시 적용됨
                    
                    **요청 예시:**
                    ```json
                    {
                      "currentPassword": "현재비밀번호",
                      "newPassword": "새비밀번호"
                    }
                    ```
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 비밀번호 (규칙 위반, 현재 비밀번호 불일치 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소셜 로그인 계정 (LOCAL 계정만 가능)")
    })
    @PatchMapping("/password")
    public ApiResponse<Void> updatePassword(
            @Parameter(hidden = true) HttpServletRequest request,
            @RequestBody @Valid PasswordUpdateRequest updateRequest
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        userService.updatePassword(userId, updateRequest);
        return ApiResponse.onSuccess();
    }

    @Operation(
            summary = "활동 반경 변경",
            description = """
                    사용자의 활동 반경 설정을 변경합니다. 이 설정은 상품 검색 시 기본 거리 범위로 사용됩니다.
                    
                    **RangeSetting 옵션:**
                    - VERY_NEAR: 0.5km
                    - NEAR: 1km
                    - MEDIUM: 3km
                    - FAR: 5km
                    - ALL: 전국 (거리 무시)
                    
                    **영향 범위:**
                    - 키워드 검색 시 기본 반경으로 사용
                    - 내 주변 아이템 검색에 적용
                    - 검색 API에서 radius 파라미터 미지정 시 이 값 사용
                    
                    **요청 예시:**
                    ```json
                    {
                      "rangeSetting": "NEAR"
                    }
                    ```
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활동 반경 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 RangeSetting 값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)")
    })
    @PatchMapping("/range-setting")
    public ApiResponse<UserProfileResponse> updateRangeSetting(
            @Parameter(hidden = true) HttpServletRequest request,
            @RequestBody @Valid RangeSettingUpdateRequest updateRequest
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        UserProfileResponse profile = userService.updateRangeSetting(userId, updateRequest);
        return ApiResponse.onSuccess(profile);
    }

    @Operation(
            summary = "활동 동네 설정",
            description = """
                    사용자의 활동 동네를 설정하거나 변경합니다. 주 활동 동네는 상품 검색의 기준점으로 사용됩니다.
                    
                    **동작 방식:**
                    - regionId로 지역을 지정하여 활동 동네로 설정
                    - primary=true: 주 활동 동네로 설정 (기존 주 동네는 부 동네로 변경)
                    - primary=false: 부 활동 동네로 추가
                    - 최대 1개의 주 동네, 여러 개의 부 동네 가능
                    
                    **사용 예시:**
                    - 회원가입 시 주 동네 설정
                    - 이사 후 동네 변경
                    - 자주 가는 지역을 부 동네로 추가
                    
                    **주의사항:**
                    - 주 동네는 반드시 1개 이상 설정되어야 함
                    - 지역 ID는 /search/regions API로 조회 가능
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "동네 설정 성공 (assignedRegionId 반환)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 지역 ID 또는 중복 설정"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 지역 ID")
    })
    @PostMapping("/region")
    public ApiResponse<String> setRegion(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(description = "설정할 지역 ID (필수, /search/regions API로 조회 가능)", required = true, example = "123")
            @RequestParam Long regionId,
            @Parameter(description = "주 활동 동네 여부 (true: 주 동네, false: 부 동네, 기본값: true)", example = "true")
            @RequestParam(defaultValue = "true") boolean primary
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        Long applied = userRegionService.setRegionForUser(userId, regionId, primary);
        return ApiResponse.onSuccess("assignedRegionId=" + applied);
    }

    @Operation(
            summary = "근처 지역 ID 조회",
            description = """
                    현재 사용자의 주 활동 동네 기준 반경 내에 있는 지역 ID 목록을 반환합니다.
                    
                    **동작 방식:**
                    - 사용자의 주 활동 동네를 기준점으로 사용
                    - 사용자가 설정한 활동 반경(RangeSetting) 내의 모든 지역 조회
                    - 하버사인 공식을 사용한 거리 계산
                    
                    **사용 목적:**
                    - 상품 검색 시 근처 지역 필터링
                    - 지역별 통계 및 분석
                    - 추천 상품의 지역 범위 결정
                    
                    **주의사항:**
                    - 주 활동 동네가 설정되어 있어야 함
                    - 활동 반경이 설정되어 있어야 함
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "근처 지역 조회 성공 (지역 ID 배열 반환)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 동네 미설정 또는 활동 반경 미설정")
    })
    @GetMapping("/regions/nearby")
    public ApiResponse<List<Long>> getNearbyRegions(@Parameter(hidden = true) HttpServletRequest request) {
        List<Long> nearbyRegionIds = regionCrudService.findNearbyRegionIdsForCurrentUser(request);
        return ApiResponse.onSuccess(nearbyRegionIds);
    }
}


