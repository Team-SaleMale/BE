package com.salemale.domain.search.controller; // 검색 전용 컨트롤러

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.search.dto.RegionSearchResponse;
import com.salemale.domain.search.service.RegionSearchService;
import com.salemale.domain.search.service.NearbyItemSearchService;
import com.salemale.domain.search.service.KeywordItemSearchService;
import com.salemale.domain.item.dto.response.AuctionListItemDTO;
import com.salemale.domain.search.dto.NearbyItemsResponse;
import com.salemale.domain.user.entity.User; // RangeSetting(enum) 사용
import com.salemale.domain.item.enums.AuctionStatus;
import com.salemale.domain.item.enums.AuctionSortType;
import com.salemale.global.common.enums.Category;
import com.salemale.global.security.jwt.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@Tag(name = "검색", description = """
        통합 검색 API
        
        **제공 기능:**
        - 지역 검색: 시/군구/읍면동 이름으로 지역 검색
        - 내 주변 아이템 검색: 사용자 동네 기준 반경 내 경매 상품 조회
        - 키워드 검색: 제목/이름으로 경매 상품 검색 (필터링 및 정렬 지원)
        - 중고 시세 검색: 낙찰 완료된 상품 검색 (시세 조회용)
        
        **인증 요구사항:**
        - 지역 검색, 중고 시세 검색: 인증 불필요 (공개 정보)
        - 내 주변 아이템 검색, 키워드 검색: 인증 필요 (사용자 지역 정보 사용)
        """)
public class SearchController {

    private final RegionSearchService regionSearchService;
    private final NearbyItemSearchService nearbyItemSearchService;
    private final KeywordItemSearchService keywordItemSearchService;
    private final CurrentUserProvider currentUserProvider;

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


    @Operation(
            summary = "내 주변 아이템 검색",
            description = """
                    사용자의 대표 지역과 거리 설정(range)에 기반해 반경 내 진행중인 경매 상품을 조회합니다.
                    
                    **동작 방식:**
                    - 현재 로그인한 사용자의 주 활동 동네를 기준으로 검색
                    - 사용자가 설정한 활동 반경(RangeSetting) 내의 상품만 조회
                    - ItemStatus가 BIDDING인 상품만 반환 (진행중인 경매)
                    
                    **주의사항:**
                    - 사용자가 동네를 설정하지 않은 경우 오류 발생
                    - 활동 반경이 설정되지 않은 경우 기본값 사용
                    
                    **사용 예시:**
                    - 사용자 동네: 강남구 역삼동, 활동 반경: NEAR (1km)
                      → 역삼동 기준 1km 이내의 진행중인 경매 상품 조회
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "사용자 동네 미설정 또는 활동 반경 미설정")
    })
    @GetMapping("/items/nearby")
    public ApiResponse<NearbyItemsResponse> nearbyItems(
            HttpServletRequest request,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값: 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (기본값: 20, 최대 권장: 100)", example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        Page<AuctionListItemDTO> result = nearbyItemSearchService.findNearbyItemsForUser(userId, PageRequest.of(Math.max(page,0), Math.max(size,1)));
        NearbyItemsResponse body = NearbyItemsResponse.builder()
                .items(result.getContent())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .size(result.getSize())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .build();
        return ApiResponse.onSuccess(body);
    }

    @Operation(
            summary = "키워드 검색",
            description = """
                    키워드로 경매 상품을 검색합니다. 제목 또는 상품명에 키워드가 포함된 상품을 찾습니다.
                    
                    **검색 기능:**
                    - 키워드 부분일치 검색 (대소문자 구분 없음)
                    - 제목(title)과 상품명(name) 모두 검색 대상
                    - 반경 필터: 사용자 동네 기준 거리 필터링 (옵션)
                    - 상태 필터: 진행중(BIDDING), 완료(COMPLETED) 등
                    - 카테고리 필터: 다중 선택 가능
                    - 가격 범위 필터: 최소/최대 가격 설정
                    - 정렬 옵션: 최신순, 가격순, 입찰수순 등
                    
                    **반경 설정:**
                    - radius 파라미터 미지정 시: 사용자의 기본 활동 반경 사용
                    - radius=ALL: 전국 검색 (거리 무시)
                    - radius=VERY_NEAR/NEAR/MEDIUM/FAR: 해당 거리 내 검색
                    
                    **사용 예시:**
                    - "아이폰" 검색 + 반경 NEAR → 사용자 동네 기준 1km 이내 아이폰 검색
                    - "노트북" 검색 + 카테고리 DIGITAL + 가격 10만원~50만원
                    - "의자" 검색 + 반경 ALL → 전국 의자 검색
                    
                    **주의사항:**
                    - 인증 필요 (사용자 동네 정보 사용)
                    - 반경 검색 시 사용자 동네가 설정되어 있어야 함
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "검색어 누락 또는 유효하지 않은 파라미터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 동네 미설정")
    })
    @GetMapping("/items")
    public ApiResponse<NearbyItemsResponse> searchItems(
            HttpServletRequest request,
            @Parameter(description = "검색 키워드 (필수, 제목/상품명 부분일치 검색)", example = "아이폰", required = true) @RequestParam String q,
            @Parameter(description = "표시 반경 Enum (VERY_NEAR: 0.5km, NEAR: 1km, MEDIUM: 3km, FAR: 5km, ALL: 전국). 미지정 시 사용자 기본값 사용", example = "NEAR") @RequestParam(required = false) User.RangeSetting radius,
            @Parameter(description = "상태 필터 (BIDDING: 진행중, COMPLETED: 완료, POPULAR: 인기, RECOMMENDED: 추천). 기본값: BIDDING", example = "BIDDING") @RequestParam(required = false, defaultValue = "BIDDING") AuctionStatus status,
            @Parameter(description = "카테고리 필터 (다중 선택 가능). 예: DIGITAL, HOME_APPLIANCE 등", example = "DIGITAL") @RequestParam(required = false) java.util.List<Category> categories,
            @Parameter(description = "최소 가격 (원 단위, 0 이상). 예: 10000", example = "10000") @RequestParam(required = false) Integer minPrice,
            @Parameter(description = "최대 가격 (원 단위, minPrice보다 커야 함). 예: 500000", example = "500000") @RequestParam(required = false) Integer maxPrice,
            @Parameter(description = """
                    정렬 기준
                    - CREATED_DESC: 최신순 (기본값)
                    - BID_COUNT_DESC: 입찰 많은순
                    - PRICE_ASC: 낮은 가격순
                    - PRICE_DESC: 높은 가격순
                    - VIEW_COUNT_DESC: 조회수 많은순
                    - END_TIME_ASC: 마감 임박순
                    """, example = "CREATED_DESC")
            @RequestParam(required = false, defaultValue = "CREATED_DESC") AuctionSortType sort,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값: 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (기본값: 20, 최대 권장: 100)", example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        Page<AuctionListItemDTO> result = keywordItemSearchService.search(
                userId, q, radius, status, categories, minPrice, maxPrice, sort,
                PageRequest.of(Math.max(page,0), Math.max(size,1))
        );
        NearbyItemsResponse body = NearbyItemsResponse.builder()
                .items(result.getContent())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .size(result.getSize())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .build();
        return ApiResponse.onSuccess(body);
    }

    @Operation(
            summary = "중고 시세 검색",
            description = """
                    낙찰된 상품(거래 완료)에 대해 키워드로 검색합니다.
                    
                    **특징:**
                    - ItemStatus가 SUCCESS인 상품만 조회 (낙찰 완료된 상품)
                    - 키워드로 제목/이름 부분일치 검색
                    - 날짜순 정렬 (최신순)
                    - 페이징 지원
                    
                    **사용 예시:**
                    - "아이폰" 검색 → 낙찰된 아이폰 상품 조회 (날짜순)
                    - "노트북" 검색 → 낙찰된 노트북 상품 조회 (날짜순)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "검색어 누락 또는 유효하지 않은 파라미터")
    })
    @GetMapping("/price-history")
    public ApiResponse<NearbyItemsResponse> searchPriceHistory(
            @Parameter(description = "검색 키워드 (필수, 제목/상품명 부분일치 검색)", example = "아이폰", required = true) @RequestParam String q,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값: 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (기본값: 20, 최대 권장: 100)", example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        Page<AuctionListItemDTO> result = keywordItemSearchService.searchCompletedItems(
                q,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1))
        );
        NearbyItemsResponse body = NearbyItemsResponse.builder()
                .items(result.getContent())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(result.getNumber())
                .size(result.getSize())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .build();
        return ApiResponse.onSuccess(body);
    }
}

