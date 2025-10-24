package com.salemale.domain.item.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.item.dto.response.LikedItemListResponse;
import com.salemale.domain.item.dto.response.MyAuctionListResponse;
import com.salemale.domain.item.service.ItemService;
import com.salemale.global.common.enums.MyAuctionSortType;
import com.salemale.global.common.enums.MyAuctionType;
import com.salemale.global.security.jwt.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "마이페이지", description = "마이페이지 관련 API")
@RestController
@RequestMapping("/mypage/auctions")
@RequiredArgsConstructor
public class MypageController {

    private final ItemService itemService;
    private final CurrentUserProvider currentUserProvider; // JWT에서 UID 추출

    /**
     * 찜한 상품 목록 조회
     * GET /auctions/liked
     * - JWT 인증 필요 (Authorization: Bearer <token>)
     * - JWT의 subject(UID)를 기반으로 현재 사용자 식별
     * - 최신 찜한 순으로 고정 정렬
     */
    @Operation(
            summary = "찜한 상품 목록 조회",
            description = "현재 로그인한 사용자가 찜한 경매 상품 목록을 조회합니다. 최근 찜한 순으로 정렬됩니다.")
    @GetMapping("/liked")
    public ResponseEntity<ApiResponse<LikedItemListResponse>> getLikedItems(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "페이지당 아이템 개수", example = "20")
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        // JWT에서 현재 사용자 ID 추출
        Long userId = currentUserProvider.getCurrentUserId(request);

        // Pageable 객체 생성 (최신순 고정, sort는 Repository 쿼리에서 처리)
        Pageable pageable = PageRequest.of(page, size);

        // 서비스 호출
        LikedItemListResponse response = itemService.getLikedItems(userId, pageable);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 내 경매 목록 조회 (마이페이지)
     * GET /mypage/auctions
     * - JWT 인증 필요 (Authorization: Bearer <token>)
     * - JWT의 subject(UID)를 기반으로 현재 사용자 식별
     * @param request HTTP 요청 (JWT 토큰 포함)
     * @param type 경매 타입 (ALL, SELLING, BIDDING, WON, FAILED)
     * @param sort 정렬 타입 (CREATED_DESC, PRICE_DESC, PRICE_ASC)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 내 경매 목록
     */
    @Operation(
            summary = "내 경매 목록 조회",
            description = "마이페이지에서 내가 판매/입찰/낙찰/유찰한 경매 상품 목록을 조회합니다."
    )
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
        // JWT에서 현재 사용자 ID 추출
        Long userId = currentUserProvider.getCurrentUserId(request);

        // Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // 서비스 호출
        MyAuctionListResponse response = itemService.getMyAuctions(
                userId, type, sort, pageable
        );

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
