package com.salemale.domain.region.controller; // 지역 CRUD 엔드포인트

import com.salemale.common.response.ApiResponse; // 표준 응답 래퍼
import com.salemale.domain.region.dto.request.RegionCreateRequest; // 지역 생성 요청 DTO
import com.salemale.domain.region.dto.request.RegionUpdateRequest; // 지역 수정 요청 DTO
import com.salemale.domain.region.dto.response.RegionResponse; // 지역 응답 DTO
import com.salemale.domain.region.service.RegionCrudService; // 지역 CRUD 서비스
import io.swagger.v3.oas.annotations.Operation; // Swagger: API 설명
import io.swagger.v3.oas.annotations.Parameter; // Swagger: 파라미터 설명
import io.swagger.v3.oas.annotations.responses.ApiResponses; // Swagger: 여러 응답 설명
import io.swagger.v3.oas.annotations.tags.Tag; // Swagger: 컨트롤러 그룹 태그
import jakarta.validation.Valid; // 요청 바인딩 유효성
import lombok.RequiredArgsConstructor; // Lombok: 생성자 자동 생성
import lombok.extern.slf4j.Slf4j; // Lombok: 로깅 지원
import org.springframework.web.bind.annotation.*; // 스프링 MVC 애노테이션

/**
 * RegionController: 지역(Region) 관련 HTTP API를 제공하는 컨트롤러입니다.
 *
 * - 지역의 생성/수정/삭제(CRUD) 기능을 제공합니다.
 * - 모든 응답은 ApiResponse로 감싸져 통일된 형식으로 반환됩니다.
 * - 인근 지역 조회는 UserRegionController의 /api/users/me/regions/nearby를 사용하세요.
 *
 * 엔드포인트 목록:
 * - POST /api/regions: 새 지역 등록
 * - PATCH /api/regions/{id}: 지역 정보 수정
 * - DELETE /api/regions/{id}: 지역 삭제
 */
@RestController // HTTP 요청을 받아 JSON 응답으로 돌려주는 컨트롤러입니다.
@RequestMapping("/api/regions") // 지역 관련 모든 API 경로는 /api/regions로 시작합니다.
@RequiredArgsConstructor // 필요한 의존성은 생성자 주입으로 받아옵니다.
@Slf4j // 서버 로그에 정보를 남겨 문제를 추적할 수 있게 합니다.
@Tag(name = "지역 관리", description = "지역(행정동) 생성, 수정, 삭제 API (관리자용)")
public class RegionController {

    // 의존성 선언: RegionCrudService를 주입받아 비즈니스 로직을 처리합니다.
    private final RegionCrudService regionCrudService;

    /**
     * 지역 등록: 새 행정동(시/군구/읍면동 + 좌표)을 추가합니다.
     *
     * - 관리자가 신규 지역 데이터를 수동으로 입력할 때 사용합니다.
     * - 유니크 제약(시도, 시군구, 읍면동)에 위배되면 예외가 발생할 수 있습니다.
     *
     * 요청 예시:
     * POST /api/regions
     * {
     *   "sido": "서울특별시",
     *   "sigungu": "강남구",
     *   "eupmyeondong": "역삼동",
     *   "latitude": 37.4979,
     *   "longitude": 127.0376
     * }
     *
     * @param request 지역 생성 요청 정보
     * @return 생성된 지역 정보(RegionResponse)
     */
    @Operation(
            summary = "지역 등록",
            description = "새로운 행정동(시/군구/읍면동 + 좌표)을 등록합니다. 관리자가 신규 지역 데이터를 추가할 때 사용합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지역 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 입력 또는 중복된 지역")
    })
    @PostMapping // POST /api/regions
    public ApiResponse<RegionResponse> create(@RequestBody @Valid RegionCreateRequest request) {
        // 1) @Valid: request 객체의 필드 검증을 수행합니다(@NotBlank, @NotNull 등).
        // 2) 서비스에 위임: 실제 생성 로직은 RegionCrudService에서 처리합니다.
        // 3) ApiResponse.onSuccess: 성공 응답으로 감싸서 반환합니다.
        return ApiResponse.onSuccess(regionCrudService.create(request));
    }

    /**
     * 지역 수정: 기존 지역의 일부 정보를 수정합니다(부분 수정 지원).
     *
     * - null이 아닌 필드만 업데이트됩니다.
     * - 잘못된 철자나 좌표를 발견한 경우 해당 필드만 수정할 수 있습니다.
     *
     * 요청 예시:
     * PATCH /api/regions/123
     * {
     *   "latitude": 37.4980,
     *   "longitude": 127.0377
     * }
     *
     * @param regionId 수정할 지역의 ID
     * @param request 수정할 정보(null이 아닌 필드만 반영)
     * @return 수정된 지역 정보(RegionResponse)
     */
    @Operation(
            summary = "지역 정보 수정",
            description = "기존 지역의 정보를 부분 수정합니다. null이 아닌 필드만 업데이트됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지역 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "지역을 찾을 수 없음")
    })
    @PatchMapping("/{regionId}") // PATCH /api/regions/{regionId}
    public ApiResponse<RegionResponse> update(
            @Parameter(description = "수정할 지역 ID", example = "123")
            @PathVariable Long regionId, // URL 경로에서 지역 ID 추출
            @RequestBody @Valid RegionUpdateRequest request // 요청 바디에서 수정할 정보 추출
    ) {
        // 1) 서비스에 위임: 실제 수정 로직은 RegionCrudService에서 처리합니다.
        // 2) ApiResponse.onSuccess: 성공 응답으로 감싸서 반환합니다.
        return ApiResponse.onSuccess(regionCrudService.update(regionId, request));
    }

    /**
     * 지역 삭제: 더 이상 사용하지 않는 지역을 제거합니다.
     *
     * - 멱등성을 보장합니다(이미 삭제된 지역을 다시 삭제해도 오류가 발생하지 않음).
     * - 주의: 운영 환경에서는 복구를 위해 보통 하드 삭제 대신 "소프트 삭제"(deleted_at 사용)를 권장합니다.
     *
     * 요청 예시:
     * DELETE /api/regions/123
     *
     * @param regionId 삭제할 지역의 ID
     * @return 성공 응답(바디 없음)
     */
    @Operation(
            summary = "지역 삭제",
            description = "지역을 삭제합니다. 멱등성을 보장하므로 이미 삭제된 지역을 다시 삭제해도 오류가 발생하지 않습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지역 삭제 성공")
    })
    @DeleteMapping("/{regionId}") // DELETE /api/regions/{regionId}
    public ApiResponse<Void> delete(
            @Parameter(description = "삭제할 지역 ID", example = "123")
            @PathVariable Long regionId) {
        // 1) 서비스에 위임: 실제 삭제 로직은 RegionCrudService에서 처리합니다.
        regionCrudService.delete(regionId);
        // 2) ApiResponse.onSuccess: 성공 응답(바디 없음)을 반환합니다.
        return ApiResponse.onSuccess();
    }
}

