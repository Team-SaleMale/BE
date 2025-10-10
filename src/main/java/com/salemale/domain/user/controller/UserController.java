package com.salemale.domain.user.controller; // 사용자 프로필 및 설정 관리 API

import com.salemale.common.response.ApiResponse; // 표준 응답 포맷
import com.salemale.domain.region.service.RegionCrudService; // 근처 지역 ID 계산
import com.salemale.domain.user.dto.request.NicknameUpdateRequest; // 닉네임 변경 요청 DTO
import com.salemale.domain.user.dto.request.PasswordUpdateRequest; // 비밀번호 변경 요청 DTO
import com.salemale.domain.user.dto.request.RangeSettingUpdateRequest; // 활동 반경 변경 요청 DTO
import com.salemale.domain.user.dto.response.UserProfileResponse; // 사용자 프로필 응답 DTO
import com.salemale.domain.user.service.UserRegionService; // 사용자-지역 매핑 조회/설정
import com.salemale.domain.user.service.UserService; // 사용자 프로필 관리
import com.salemale.global.security.jwt.CurrentUserProvider; // JWT에서 userId 추출
import io.swagger.v3.oas.annotations.Operation; // Swagger: API 설명
import io.swagger.v3.oas.annotations.Parameter; // Swagger: 파라미터 설명
import io.swagger.v3.oas.annotations.responses.ApiResponses; // Swagger: 여러 응답 설명
import io.swagger.v3.oas.annotations.tags.Tag; // Swagger: 컨트롤러 그룹 태그
import jakarta.servlet.http.HttpServletRequest; // HTTP 요청 객체
import jakarta.validation.Valid; // 요청 바인딩 유효성
import lombok.RequiredArgsConstructor; // Lombok: 생성자 자동 생성
import lombok.extern.slf4j.Slf4j; // Lombok: 로깅 지원
import org.springframework.web.bind.annotation.*; // 스프링 MVC 애노테이션

import java.util.List; // 리스트 컬렉션

/**
 * UserController: 사용자 프로필, 설정, 지역 관련 HTTP API를 제공하는 컨트롤러입니다.
 *
 * - JWT 인증이 필요한 엔드포인트들입니다.
 * - 사용자의 프로필 조회/수정, 활동 동네 설정, 근처 지역 조회 기능을 제공합니다.
 * - 모든 응답은 ApiResponse로 감싸져 통일된 형식으로 반환됩니다.
 *
 * 엔드포인트 목록:
 * - GET /api/users: 내 프로필 조회
 * - PATCH /api/users/nickname: 닉네임 변경
 * - PATCH /api/users/password: 비밀번호 재설정
 * - PATCH /api/users/range-setting: 활동 반경 변경
 * - POST /api/users/region: 활동 동네 설정
 * - GET /api/users/regions/nearby: 근처 지역 ID 조회
 *
 * 참고:
 * - /me 경로는 사용하지 않습니다. JWT 인증에서는 항상 현재 사용자만 다루므로 불필요합니다.
 * - 지역 관련 엔드포인트도 여기에 통합되어 있습니다.
 */
@RestController // HTTP 요청을 받아 JSON 응답으로 돌려주는 컨트롤러입니다.
@RequestMapping("/api/users") // 사용자 관련 모든 API 경로는 /api/users로 시작합니다.
@RequiredArgsConstructor // 필요한 서비스를 생성자로 주입받습니다.
@Slf4j // 문제 상황을 기록해 추적에 도움을 줍니다.
@Tag(name = "사용자 관리", description = "사용자 프로필, 설정, 지역 관리 API")
public class UserController {

    // 의존성 선언: 필요한 서비스들을 주입받습니다.
    private final UserService userService; // 사용자 프로필 관리
    private final UserRegionService userRegionService; // 사용자-지역 매핑 관리
    private final RegionCrudService regionCrudService; // 근처 지역 조회
    private final CurrentUserProvider currentUserProvider; // JWT에서 사용자 ID 추출

    /**
     * 내 프로필 조회: 현재 로그인한 사용자의 프로필 정보를 반환합니다.
     *
     * - JWT 인증이 필요합니다(Authorization: Bearer <token>).
     * - 민감한 정보(비밀번호 등)는 제외하고 반환합니다.
     * - 사용자 ID, 닉네임, 이메일, 매너 점수, 활동 반경 등을 포함합니다.
     *
     * 요청 예시:
     * GET /api/users
     * Headers:
     *   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * 응답 예시:
     * {
     *   "isSuccess": true,
     *   "code": "200",
     *   "message": "OK",
     *   "result": {
     *     "id": 123,
     *     "nickname": "홍길동",
     *     "email": "hong@example.com",
     *     "mannerScore": 50,
     *     "rangeSetting": "NEAR",
     *     "profileImage": null,
     *     "alarmChecked": "NO",
     *     "phoneNumber": null,
     *     "phoneVerified": false
     *   }
     * }
     *
     * @param request HTTP 요청 객체(JWT 토큰 추출용)
     * @return 사용자 프로필 정보
     */
    @Operation(
            summary = "내 프로필 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 반환합니다. JWT 토큰이 필요합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 토큰 없음 또는 유효하지 않음)")
    })
    @GetMapping // GET /api/users
    public ApiResponse<UserProfileResponse> getMyProfile(
            @Parameter(hidden = true) HttpServletRequest request) {
        // 1) JWT 토큰에서 현재 사용자 ID 추출
        //    - CurrentUserProvider: Authorization 헤더에서 Bearer 토큰을 추출하여 subject(사용자 식별자)를 가져옵니다.
        Long userId = currentUserProvider.getCurrentUserId(request);

        // 2) 서비스에 위임: 사용자 프로필을 조회합니다.
        //    - getMyProfile: User 엔티티를 조회하고 UserProfileResponse로 변환하여 반환합니다.
        UserProfileResponse profile = userService.getMyProfile(userId);

        // 3) ApiResponse.onSuccess: 성공 응답으로 감싸서 반환합니다.
        return ApiResponse.onSuccess(profile);
    }

    /**
     * 닉네임 변경: 사용자의 닉네임을 새로운 값으로 변경합니다.
     *
     * - JWT 인증이 필요합니다.
     * - 1자 이상 15자 이하의 닉네임으로 변경할 수 있습니다.
     * - 앞뒤 공백은 자동으로 제거됩니다.
     *
     * 요청 예시:
     * PATCH /api/users/nickname
     * Headers:
     *   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * Body:
     * {
     *   "nickname": "새로운닉네임"
     * }
     *
     * 응답 예시:
     * {
     *   "isSuccess": true,
     *   "code": "200",
     *   "message": "OK",
     *   "result": {
     *     "id": 123,
     *     "nickname": "새로운닉네임",
     *     ...
     *   }
     * }
     *
     * @param request HTTP 요청 객체(JWT 토큰 추출용)
     * @param updateRequest 닉네임 변경 요청 정보
     * @return 변경된 프로필 정보
     */
    @Operation(
            summary = "닉네임 변경",
            description = "사용자의 닉네임을 새로운 값으로 변경합니다. 1자 이상 15자 이하여야 합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "닉네임 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 닉네임 (빈 문자열, 길이 초과 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PatchMapping("/nickname") // PATCH /api/users/nickname
    public ApiResponse<UserProfileResponse> updateNickname(
            @Parameter(hidden = true) HttpServletRequest request,
            @RequestBody @Valid NicknameUpdateRequest updateRequest
    ) {
        // 1) JWT 토큰에서 현재 사용자 ID 추출
        Long userId = currentUserProvider.getCurrentUserId(request);

        // 2) 서비스에 위임: 닉네임을 변경합니다.
        //    - @Valid: 요청 DTO의 검증 규칙(@NotBlank, @Size 등)을 먼저 확인합니다.
        UserProfileResponse profile = userService.updateNickname(userId, updateRequest);

        // 3) ApiResponse.onSuccess: 성공 응답으로 감싸서 반환합니다.
        return ApiResponse.onSuccess(profile);
    }

    /**
     * 비밀번호 재설정: 현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.
     *
     * - JWT 인증이 필요합니다.
     * - 보안을 위해 현재 비밀번호를 먼저 검증합니다.
     * - 현재 비밀번호가 일치하지 않으면 예외가 발생합니다.
     * - LOCAL 인증(이메일/비밀번호 로그인)에서만 사용 가능합니다.
     * - 소셜 로그인 계정은 비밀번호 변경이 불가능합니다.
     *
     * 요청 예시:
     * PATCH /api/users/password
     * Headers:
     *   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * Body:
     * {
     *   "currentPassword": "old123456",
     *   "newPassword": "new123456"
     * }
     *
     * 응답 예시:
     * {
     *   "isSuccess": true,
     *   "code": "200",
     *   "message": "OK"
     * }
     *
     * @param request HTTP 요청 객체(JWT 토큰 추출용)
     * @param updateRequest 비밀번호 변경 요청 정보
     * @return 성공 응답(바디 없음)
     */
    @Operation(
            summary = "비밀번호 재설정",
            description = "현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다. " +
                    "보안을 위해 현재 비밀번호를 먼저 검증하며, LOCAL 인증(이메일/비밀번호 로그인)에서만 사용 가능합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 비밀번호 (길이 부족, 빈 문자열 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 또는 현재 비밀번호 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소셜 로그인 계정 (비밀번호 변경 불가)")
    })
    @PatchMapping("/password") // PATCH /api/users/password
    public ApiResponse<Void> updatePassword(
            @Parameter(hidden = true) HttpServletRequest request,
            @RequestBody @Valid PasswordUpdateRequest updateRequest
    ) {
        // 1) JWT 토큰에서 현재 사용자 ID 추출
        Long userId = currentUserProvider.getCurrentUserId(request);

        // 2) 서비스에 위임: 비밀번호를 변경합니다.
        //    - @Valid: 요청 DTO의 검증 규칙(@NotBlank, @Size 등)을 먼저 확인합니다.
        //    - 현재 비밀번호 검증 실패 시 GeneralException이 발생합니다.
        userService.updatePassword(userId, updateRequest);

        // 3) ApiResponse.onSuccess: 성공 응답(바디 없음)을 반환합니다.
        return ApiResponse.onSuccess();
    }

    /**
     * 활동 반경 변경: 사용자의 활동 반경 설정을 변경합니다.
     *
     * - JWT 인증이 필요합니다.
     * - VERY_NEAR, NEAR, MEDIUM, FAR, ALL 중 하나로 변경할 수 있습니다.
     * - 변경된 반경은 지역 검색 시 즉시 반영됩니다.
     *
     * 거리 매핑:
     * - VERY_NEAR: 2km (매우 가까운 동네만)
     * - NEAR: 5km (기본값, 인근 동네)
     * - MEDIUM: 20km (중간 거리, 여러 동네)
     * - FAR: 50km (먼 거리, 시/군 단위)
     * - ALL: 20000km (전국, 사실상 제한 없음)
     *
     * 요청 예시:
     * PATCH /api/users/range-setting
     * Headers:
     *   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * Body:
     * {
     *   "rangeSetting": "MEDIUM"
     * }
     *
     * 응답 예시:
     * {
     *   "isSuccess": true,
     *   "code": "200",
     *   "message": "OK",
     *   "result": {
     *     "id": 123,
     *     "rangeSetting": "MEDIUM",
     *     ...
     *   }
     * }
     *
     * @param request HTTP 요청 객체(JWT 토큰 추출용)
     * @param updateRequest 활동 반경 변경 요청 정보
     * @return 변경된 프로필 정보
     */
    @Operation(
            summary = "활동 반경 변경",
            description = """
                    사용자의 활동 반경 설정을 변경합니다. 변경된 반경은 지역 검색 시 즉시 반영됩니다.
                    
                    **거리 매핑:**
                    - VERY_NEAR: 2km (매우 가까운 동네만)
                    - NEAR: 5km (기본값, 인근 동네)
                    - MEDIUM: 20km (중간 거리, 여러 동네)
                    - FAR: 50km (먼 거리, 시/군 단위)
                    - ALL: 20000km (전국, 사실상 제한 없음)
                    
                    **사용 예시:**
                    - VERY_NEAR: 우리 동네만 보고 싶을 때
                    - NEAR: 일반적인 생활권 (기본값)
                    - MEDIUM: 출퇴근 거리까지 확장
                    - FAR: 시/군 전체를 보고 싶을 때
                    - ALL: 전국의 모든 상품을 보고 싶을 때
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활동 반경 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 RangeSetting 값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PatchMapping("/range-setting") // PATCH /api/users/range-setting
    public ApiResponse<UserProfileResponse> updateRangeSetting(
            @Parameter(hidden = true) HttpServletRequest request,
            @RequestBody @Valid RangeSettingUpdateRequest updateRequest
    ) {
        // 1) JWT 토큰에서 현재 사용자 ID 추출
        Long userId = currentUserProvider.getCurrentUserId(request);

        // 2) 서비스에 위임: 활동 반경을 변경합니다.
        //    - @Valid: 요청 DTO의 검증 규칙(@NotNull 등)을 먼저 확인합니다.
        UserProfileResponse profile = userService.updateRangeSetting(userId, updateRequest);

        // 3) ApiResponse.onSuccess: 성공 응답으로 감싸서 반환합니다.
        return ApiResponse.onSuccess(profile);
    }

    /**
     * 활동 동네 설정: 사용자의 활동 동네를 설정합니다.
     *
     * - 검색에서 고른 regionId를 현재 로그인 사용자에게 적용합니다.
     * - 기존 동네는 자동으로 해제되고 새 동네가 설정됩니다(1:1 매핑).
     * - JWT 인증이 필요합니다.
     *
     * 사용 예시:
     * - 사용자가 "역삼1동"을 검색하고 선택한 후 "내 동네로 설정" 버튼을 누를 때
     *
     * 요청 예시:
     * POST /api/users/region?regionId=123&primary=true
     * Headers:
     *   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * 응답 예시:
     * {
     *   "isSuccess": true,
     *   "code": "200",
     *   "message": "OK",
     *   "result": "assignedRegionId=123"
     * }
     *
     * @param request HTTP 요청 객체(JWT 토큰 추출용)
     * @param regionId 설정할 지역의 ID
     * @param primary 주 활동 동네 여부(현재는 항상 true, 기본값)
     * @return 설정된 지역 ID
     */
    @Operation(
            summary = "활동 동네 설정",
            description = "사용자의 활동 동네를 설정합니다. 기존 동네는 자동으로 해제되고 새 동네가 설정됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "동네 설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 지역 ID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/region") // POST /api/users/region
    public ApiResponse<String> setRegion(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(description = "설정할 지역 ID", required = true, example = "123")
            @RequestParam Long regionId,
            @Parameter(description = "주 활동 동네 여부 (현재는 항상 true)", example = "true")
            @RequestParam(defaultValue = "true") boolean primary
    ) {
        // 1) JWT 토큰에서 현재 사용자 ID 추출
        Long userId = currentUserProvider.getCurrentUserId(request);

        // 2) 사용자에게 지역 설정 (기존 동네는 자동 해제)
        //    - setRegionForUser: UserRegion을 생성/업데이트하여 사용자의 동네를 설정합니다.
        Long applied = userRegionService.setRegionForUser(userId, regionId, primary);

        // 3) 성공 응답 반환
        return ApiResponse.onSuccess("assignedRegionId=" + applied);
    }

    /**
     * 근처 지역 ID 조회: 현재 로그인한 사용자의 동네를 중심으로 주변 지역 ID 목록을 반환합니다.
     *
     * - JWT 인증이 필요합니다(Authorization: Bearer <token>).
     * - 사용자의 UserRegion에서 위도/경도를 자동으로 가져옵니다.
     * - 사용자의 RangeSetting(VERY_NEAR/NEAR/MEDIUM/FAR/ALL)에 따라 반경이 자동으로 결정됩니다.
     * - 지역 ID 목록만 반환하므로 다른 API(상품 목록 등)에서 필터로 사용하기 적합합니다.
     *
     * RangeSetting 별 반경:
     * - VERY_NEAR: 2km (매우 가까운 동네만)
     * - NEAR: 5km (기본값, 인근 동네)
     * - MEDIUM: 20km (중간 거리, 여러 동네)
     * - FAR: 50km (먼 거리, 시/군 단위)
     * - ALL: 20000km (전국, 사실상 제한 없음)
     *
     * 사용 예시:
     * - 상품 목록 API에서 "내 동네 근처 상품만 보기"
     *   → WHERE item.region_id IN (이 API의 결과)
     * - 채팅 상대 검색에서 "근처 사용자만 보기"
     *   → WHERE user.region_id IN (이 API의 결과)
     *
     * 요청 예시:
     * GET /api/users/regions/nearby
     * Headers:
     *   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * 응답 예시:
     * {
     *   "isSuccess": true,
     *   "code": "200",
     *   "message": "OK",
     *   "result": [1, 2, 3, 5, 8, 13, 21, 34]  // 지역 ID 목록
     * }
     *
     * @param request HTTP 요청 객체(JWT 토큰 추출용)
     * @return 반경 내의 지역 ID 목록
     * @throws GeneralException 사용자를 찾을 수 없거나 UserRegion이 설정되지 않았을 때 발생
     */
    @Operation(
            summary = "근처 지역 ID 조회",
            description = """
                    현재 로그인한 사용자의 동네를 중심으로 주변 지역 ID 목록을 반환합니다.
                    
                    사용자의 RangeSetting에 따라 반경이 자동으로 결정되며, 
                    지역 ID 목록만 반환하므로 다른 API(상품 목록 등)에서 필터로 사용하기 적합합니다.
                    
                    **사용 예시:**
                    - 상품 목록 API: WHERE item.region_id IN (이 API의 결과)
                    - 채팅 상대 검색: WHERE user.region_id IN (이 API의 결과)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "근처 지역 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "동네가 설정되지 않음")
    })
    @GetMapping("/regions/nearby") // GET /api/users/regions/nearby
    public ApiResponse<List<Long>> getNearbyRegions(
            @Parameter(hidden = true) HttpServletRequest request) {
        // 1) RegionCrudService의 통합 메서드를 사용하여 모든 로직을 한 번에 처리합니다.
        //    - JWT 토큰에서 사용자 ID 추출
        //    - 사용자의 UserRegion 조회
        //    - UserRegion의 Region에서 위도/경도 추출
        //    - 사용자의 RangeSetting을 거리(km)로 변환
        //    - 해당 반경 내의 Region ID 목록 조회 및 반환
        List<Long> nearbyRegionIds = regionCrudService.findNearbyRegionIdsForCurrentUser(request);

        // 2) ApiResponse.onSuccess: 성공 응답으로 감싸서 반환합니다.
        //    - 클라이언트는 통일된 형식의 응답을 받게 됩니다.
        //    - 예: {"isSuccess": true, "result": [1, 2, 3, ...]}
        return ApiResponse.onSuccess(nearbyRegionIds);
    }
}

