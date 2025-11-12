package com.salemale.domain.mypage.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.mypage.dto.request.UpdatePreferredCategoryRequest;
import com.salemale.domain.mypage.dto.response.LikedItemListResponse;
import com.salemale.domain.mypage.dto.response.MyAuctionListResponse;
import com.salemale.domain.mypage.dto.response.PreferredCategoryResponse;
import com.salemale.domain.mypage.service.MypageService;
import com.salemale.domain.mypage.enums.MyAuctionSortType;
import com.salemale.domain.mypage.enums.MyAuctionType;
import com.salemale.global.security.jwt.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마이페이지 - 경매 관리", description = """
        마이페이지 경매 및 선호 카테고리 관리 API
        
        **제공 기능:**
        - 찜한 상품 목록: 사용자가 찜한 경매 상품 조회
        - 내 경매 목록: 판매/입찰/낙찰/유찰한 경매 상품 조회
        - 선호 카테고리 설정: 개인화 추천을 위한 선호 카테고리 설정
        - 선호 카테고리 조회: 설정한 선호 카테고리 목록 조회
        
        **인증 요구사항:**
        - 모든 API는 인증 필요 (로그인 필수)
        """)
@RestController
@RequestMapping("/mypage/auctions")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;  // ⭐ ItemService → MypageService
    private final CurrentUserProvider currentUserProvider;

    /**
     * 찜한 상품 목록 조회
     * GET /mypage/auctions/liked
     */
    @Operation(
            summary = "찜한 상품 목록 조회",
            description = """
                    현재 로그인한 사용자가 찜한 경매 상품 목록을 조회합니다.
                    
                    **정렬:**
                    - 최근 찜한 순으로 정렬 (최신순)
                    
                    **반환 정보:**
                    - 상품 기본 정보 (제목, 가격, 이미지 등)
                    - 찜한 날짜
                    - 경매 상태 (진행중, 완료 등)
                    - 페이징 정보
                    
                    **사용 예시:**
                    - 마이페이지 찜 목록 탭
                    - 찜한 상품 모니터링
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)")
    })
    @GetMapping("/liked")
    public ResponseEntity<ApiResponse<LikedItemListResponse>> getLikedItems(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "페이지당 아이템 개수", example = "20")
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        Pageable pageable = PageRequest.of(page, size);
        LikedItemListResponse response = mypageService.getLikedItems(userId, pageable);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 내 경매 목록 조회
     * GET /mypage/auctions
     */
    @Operation(
            summary = "내 경매 목록 조회",
            description = """
                    마이페이지에서 내가 판매/입찰/낙찰/유찰한 경매 상품 목록을 조회합니다.
                    
                    **경매 타입 (type):**
                    - ALL: 전체 (판매 + 입찰 + 낙찰 + 유찰)
                    - SELLING: 판매 중인 상품 (내가 판매자인 상품)
                    - BIDDING: 입찰 중인 상품 (내가 입찰했지만 판매자가 아닌 상품)
                    - WON: 낙찰받은 상품 (경매 종료 후 내가 낙찰자로 확정된 상품)
                    - FAILED: 유찰된 상품 (내가 판매했지만 입찰이 없어 유찰된 상품)
                    
                    **정렬 옵션 (sort):**
                    - CREATED_DESC: 최신순 (기본값)
                    - PRICE_DESC: 높은 가격순
                    - PRICE_ASC: 낮은 가격순
                    
                    **반환 정보:**
                    - 상품 기본 정보
                    - 경매 상태 및 진행 상황
                    - 입찰 수, 현재 가격
                    - 마감 시간
                    - 페이징 정보
                    
                    **사용 예시:**
                    - 마이페이지 내 경매 탭
                    - 판매/입찰 내역 확인
                    - 낙찰/유찰 결과 확인
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 타입 또는 정렬 옵션"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<MyAuctionListResponse>> getMyAuctions(
            @Parameter(hidden = true) HttpServletRequest request,

            @Parameter(
                    description = "경매 타입 (ALL: 전체, SELLING: 판매, BIDDING: 입찰, WON: 낙찰, FAILED: 유찰)",
                    example = "ALL"
            )
            @RequestParam(required = false, defaultValue = "ALL")
            MyAuctionType type,

            @Parameter(
                    description = "정렬 기준 (CREATED_DESC: 최신순, PRICE_DESC: 높은가격순, PRICE_ASC: 낮은가격순)",
                    example = "CREATED_DESC"
            )
            @RequestParam(required = false, defaultValue = "CREATED_DESC")
            MyAuctionSortType sort,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false, defaultValue = "0")
            int page,

            @Parameter(description = "페이지당 아이템 개수", example = "20")
            @RequestParam(required = false, defaultValue = "20")
            int size
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        Pageable pageable = PageRequest.of(page, size);
        MyAuctionListResponse response = mypageService.getMyAuctions(
                userId, type, sort, pageable
        );

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 선호 카테고리 설정
     * POST /mypage/category
     */
    @Operation(
            summary = "선호 카테고리 설정",
            description = """
                    사용자의 선호 카테고리를 설정합니다. 개인화 추천 및 맞춤 상품 노출에 사용됩니다.
                    
                    **동작 방식:**
                    - 기존 선호 카테고리는 모두 삭제됨
                    - 요청한 카테고리 목록으로 새로 설정됨
                    - 다중 선택 가능 (여러 카테고리 동시 설정)
                    
                    **카테고리 옵션:**
                    - HOME_APPLIANCE, HEALTH_FOOD, BEAUTY, FOOD_PROCESSED
                    - PET, DIGITAL, LIVING_KITCHEN, WOMEN_ACC
                    - SPORTS, PLANT, GAME_HOBBY, TICKET
                    - FURNITURE, BOOK, KIDS, CLOTHES, ETC
                    
                    **사용 목적:**
                    - AI 기반 개인화 추천
                    - 맞춤 상품 우선 노출
                    - 관심 카테고리 필터링
                    
                    **요청 예시:**
                    ```json
                    {
                      "categories": ["DIGITAL", "HOME_APPLIANCE", "SPORTS"]
                    }
                    ```
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 카테고리 값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)")
    })
    @PostMapping("/category")
    public ResponseEntity<ApiResponse<PreferredCategoryResponse>> updatePreferredCategories(
            @Parameter(hidden = true) HttpServletRequest request,
            @Valid @RequestBody UpdatePreferredCategoryRequest requestDto
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        PreferredCategoryResponse response = mypageService.updatePreferredCategories(
                userId,
                requestDto.getCategories()
        );

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 선호 카테고리 조회
     * GET /mypage/category
     */
    @Operation(
            summary = "선호 카테고리 조회",
            description = """
                    사용자가 설정한 선호 카테고리 목록을 조회합니다.
                    
                    **반환 정보:**
                    - 설정된 선호 카테고리 배열
                    - 카테고리가 설정되지 않은 경우 빈 배열 반환
                    
                    **사용 예시:**
                    - 마이페이지 설정 화면에서 현재 선호 카테고리 표시
                    - 선호 카테고리 수정 전 현재 설정 확인
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)")
    })
    @GetMapping("/category")
    public ResponseEntity<ApiResponse<PreferredCategoryResponse>> getPreferredCategories(
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        PreferredCategoryResponse response = mypageService.getPreferredCategories(userId);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}