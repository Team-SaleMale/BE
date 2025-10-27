package com.salemale.domain.mypage.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.mypage.dto.response.LikedItemListResponse;
import com.salemale.domain.mypage.dto.response.MyAuctionListResponse;
import com.salemale.domain.mypage.service.MypageService;
import com.salemale.domain.mypage.enums.MyAuctionSortType;
import com.salemale.domain.mypage.enums.MyAuctionType;
import com.salemale.global.security.jwt.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마이페이지", description = "마이페이지 관련 API")
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
            description = "현재 로그인한 사용자가 찜한 경매 상품 목록을 조회합니다. 최근 찜한 순으로 정렬됩니다."
    )
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
        Long userId = currentUserProvider.getCurrentUserId(request);
        Pageable pageable = PageRequest.of(page, size);
        MyAuctionListResponse response = mypageService.getMyAuctions(
                userId, type, sort, pageable
        );

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}