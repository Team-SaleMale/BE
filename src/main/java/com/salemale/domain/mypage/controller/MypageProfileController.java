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
@Tag(name = "마이페이지", description = "프로필 조회/수정, 활동 반경, 지역 설정 API")
public class MypageProfileController {

    private final UserService userService;
    private final UserRegionService userRegionService;
    private final RegionCrudService regionCrudService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ApiResponse<UserProfileResponse> getMyProfile(@Parameter(hidden = true) HttpServletRequest request) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        UserProfileResponse profile = userService.getMyProfile(userId);
        return ApiResponse.onSuccess(profile);
    }

    @Operation(summary = "닉네임 변경", description = "사용자의 닉네임을 새로운 값으로 변경합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "닉네임 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 닉네임"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
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

    @Operation(summary = "비밀번호 재설정", description = "현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다. LOCAL 계정만 가능")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 비밀번호"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
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

    @Operation(summary = "활동 반경 변경", description = "사용자의 활동 반경 설정을 변경합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활동 반경 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 RangeSetting"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
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

    @Operation(summary = "활동 동네 설정", description = "사용자의 활동 동네를 설정합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "동네 설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 지역 ID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/region")
    public ApiResponse<String> setRegion(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(description = "설정할 지역 ID", required = true, example = "123")
            @RequestParam Long regionId,
            @Parameter(description = "주 활동 동네 여부", example = "true")
            @RequestParam(defaultValue = "true") boolean primary
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        Long applied = userRegionService.setRegionForUser(userId, regionId, primary);
        return ApiResponse.onSuccess("assignedRegionId=" + applied);
    }

    @Operation(summary = "근처 지역 ID 조회", description = "현재 사용자의 동네 기준 반경 내 지역 ID 목록을 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "근처 지역 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/regions/nearby")
    public ApiResponse<List<Long>> getNearbyRegions(@Parameter(hidden = true) HttpServletRequest request) {
        List<Long> nearbyRegionIds = regionCrudService.findNearbyRegionIdsForCurrentUser(request);
        return ApiResponse.onSuccess(nearbyRegionIds);
    }
}


