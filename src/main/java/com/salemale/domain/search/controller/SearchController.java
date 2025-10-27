package com.salemale.domain.search.controller; // 검색 전용 컨트롤러

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.search.dto.RegionSearchResponse;
import com.salemale.domain.search.service.RegionSearchService;
import io.swagger.v3.oas.annotations.Operation; // Swagger: API 설명
import io.swagger.v3.oas.annotations.Parameter; // Swagger: 파라미터 설명
import io.swagger.v3.oas.annotations.responses.ApiResponses; // Swagger: 여러 응답 설명
import io.swagger.v3.oas.annotations.tags.Tag; // Swagger: 컨트롤러 그룹 태그
import jakarta.validation.constraints.Min; // Bean Validation: 최소값 제약
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated; // Bean Validation 활성화
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SearchController: 통합 검색 API를 제공하는 컨트롤러입니다.
 *
 * - 현재는 지역 검색만 제공하지만, 향후 상품 검색, 사용자 검색 등으로 확장 가능합니다.
 * - 모든 검색 관련 엔드포인트를 /api/search 아래로 통합합니다.
 *
 * 엔드포인트 목록:
 * - GET /api/search/regions: 지역 검색 (시/군구/읍면동)
 */
@RestController // 검색 요청을 받고 검색 결과를 JSON으로 돌려주는 컨트롤러입니다.
@RequestMapping("/search") // 검색 API의 베이스 경로입니다.
@RequiredArgsConstructor // 필요한 서비스는 생성자 주입으로 받습니다.
@Slf4j // 검색어/응답시간 등을 로그로 남겨 추적합니다.
@Validated // Bean Validation을 활성화하여 메서드 파라미터 검증을 수행합니다.
@Tag(name = "검색", description = "지역 검색 API (향후 상품, 사용자 검색 등 확장 예정)")
public class SearchController {

    private final RegionSearchService regionSearchService;

    /**
     * 지역 검색: 시도/시군구/읍면동 이름으로 지역을 검색합니다.
     *
     * - 부분 일치 검색을 지원합니다(예: "역삼" 입력 시 "역삼1동", "역삼2동" 모두 검색).
     * - 페이지네이션을 지원하여 대량의 검색 결과를 효율적으로 전달합니다.
     * - 사용자가 "내 동네 설정" 시 지역을 찾는 데 사용됩니다.
     *
     * 요청 예시:
     * GET /api/search/regions?q=역삼&page=0&size=10
     *
     * 응답 예시:
     * {
     *   "isSuccess": true,
     *   "code": "200",
     *   "message": "OK",
     *   "result": [
     *     { "id": 1, "sido": "서울특별시", "sigungu": "강남구", "eupmyeondong": "역삼1동" },
     *     { "id": 2, "sido": "서울특별시", "sigungu": "강남구", "eupmyeondong": "역삼2동" }
     *   ]
     * }
     *
     * @param q 검색어 (시/군구/읍면동 이름)
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return 검색된 지역 목록
     */
    @Operation(
            summary = "지역 검색",
            description = """
                    시도/시군구/읍면동 이름으로 지역을 검색합니다.
                    
                    부분 일치 검색을 지원하며, 사용자가 "내 동네 설정" 시 지역을 찾는 데 사용됩니다.
                    
                    **사용 예시:**
                    - "역삼" 검색 → "역삼1동", "역삼2동" 등 모두 검색
                    - "강남" 검색 → "강남구"의 모든 동 검색
                    - "서울" 검색 → "서울특별시"의 모든 구/동 검색
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공")
    })
    @GetMapping("/regions")
    public ApiResponse<List<RegionSearchResponse>> searchRegions(
            @Parameter(description = "검색어 (시/군구/읍면동 이름)", example = "역삼", required = true)
            @RequestParam String q,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
            @Parameter(description = "페이지 크기 (1-5000)", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.") int size
    ) {
        // 1) 검색어 로깅: 검색 패턴 분석 및 디버깅에 활용
        log.debug("지역 검색 - 검색어: {}, 페이지: {}, 크기: {}", q, page, size);

        // 2) 서비스에 위임: 실제 검색 로직은 RegionSearchService에서 처리
        List<RegionSearchResponse> results = regionSearchService.searchPagedByNameOnly(q, page, size);

        // 3) ApiResponse.onSuccess: 성공 응답으로 감싸서 반환
        return ApiResponse.onSuccess(results);
    }
}

