package com.salemale.domain.item.controller;

import com.salemale.common.code.status.SuccessStatus;
import com.salemale.common.response.ApiResponse;
import com.salemale.domain.item.dto.request.BidRequest;
import com.salemale.domain.item.dto.request.ItemRegisterRequest;
import com.salemale.domain.item.dto.response.AuctionListResponse;
import com.salemale.domain.item.dto.response.BidResponse;
import com.salemale.domain.item.dto.response.ItemLikeResponse;
import com.salemale.domain.item.dto.response.ItemRegisterResponse;
import com.salemale.domain.item.dto.response.detail.ItemDetailResponse;
import com.salemale.domain.item.service.ItemService;
import com.salemale.global.common.enums.AuctionSortType;
import com.salemale.global.common.enums.AuctionStatus;
import com.salemale.global.common.enums.Category;
import com.salemale.global.security.jwt.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CurrentUserProvider currentUserProvider; // JWT에서 UID 추출

    /**
     * 경매 상품 찜하기
     * POST /auctions/{itemId}/liked
     * - JWT 인증 필요 (Authorization: Bearer <token>)
     * - JWT의 subject(UID)를 기반으로 현재 사용자 식별
     */
    @Operation(summary = "경매 상품 찜하기", description = "본인이 등록한 상품은 찜할 수 없습니다.")
    @PostMapping("/{itemId}/liked")
    public ResponseEntity<ApiResponse<ItemLikeResponse>> likeItem(
            @Parameter(hidden = true) HttpServletRequest request,
            @PathVariable Long itemId
    ) {
        // JWT에서 현재 사용자 ID 추출
        Long userId = currentUserProvider.getCurrentUserId(request);
        ItemLikeResponse response = itemService.likeItem(userId, itemId);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 경매 상품 찜 취소
     * DELETE /auctions/{itemId}/liked
     * - JWT 인증 필요 (Authorization: Bearer <token>)
     * - JWT의 subject(UID)를 기반으로 현재 사용자 식별
     */
    @Operation(summary = "경매 상품 찜 취소", description = "경매 상품 찜을 취소합니다.")
    @DeleteMapping("/{itemId}/liked")
    public ResponseEntity<ApiResponse<ItemLikeResponse>> unlikeItem(
            @Parameter(hidden = true) HttpServletRequest request,
            @PathVariable Long itemId
    ) {
        // JWT에서 현재 사용자 ID 추출
        Long userId = currentUserProvider.getCurrentUserId(request);
        ItemLikeResponse response = itemService.unlikeItem(userId, itemId);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 경매 상품 등록 API
     * POST /auctions/registration
     * - JWT 인증 필요 (Authorization: Bearer <token>)
     * - JWT의 subject(UID)를 기반으로 현재 사용자(판매자) 식별
     */
    @Operation(summary = "경매 상품 등록하기")
    @PostMapping("/registration")
    public ResponseEntity<ApiResponse<ItemRegisterResponse>> registerItem(
            @Parameter(hidden = true) HttpServletRequest request,
            @Valid @RequestBody ItemRegisterRequest request_body
    ) {
        // 1. JWT에서 현재 사용자 ID 추출 (판매자)
        Long sellerId = currentUserProvider.getCurrentUserId(request);

        // 2. 서비스 로직 위임 및 결과 반환
        ItemRegisterResponse response = itemService.registerItem(sellerId, request_body);

        // 3. 201 Created 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessStatus._CREATED, response));
    }

    /**
     * 경매 상품 입찰
     * POST /auctions/{itemId}/bid
     * - JWT 인증 필요 (Authorization: Bearer <token>)
     * - JWT의 subject(UID)를 기반으로 현재 사용자(입찰자) 식별
     */
    @Operation(summary = "경매 상품 입찰", description = "경매 중인 상품에 입찰합니다.")
    @PostMapping("/{itemId}/bid")
    public ResponseEntity<ApiResponse<BidResponse>> bidOnItem(
            @Parameter(hidden = true) HttpServletRequest httpRequest,
            @PathVariable Long itemId,
            @Valid @RequestBody BidRequest request
    ) {
        // JWT에서 현재 사용자 ID 추출 (입찰자)
        Long userId = currentUserProvider.getCurrentUserId(httpRequest);
        BidResponse response = itemService.bidOnItem(userId, itemId, request);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 경매 상품 상세 조회
     * GET /auctions/{itemId}
     */
    @Operation(summary = "경매 상품 상세 조회", description = "경매 상품의 상세 정보와 입찰 내역을 조회합니다.")
    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<ItemDetailResponse>> getItemDetail(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, defaultValue = "10") Integer bidHistoryLimit
    ) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        ItemDetailResponse response = itemService.getItemDetail(itemId, email, bidHistoryLimit);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

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
    public ResponseEntity<ApiResponse<com.salemale.domain.item.dto.response.LikedItemListResponse>> getLikedItems(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "페이지당 아이템 개수", example = "20")
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        // JWT에서 현재 사용자 ID 추출
        Long userId = currentUserProvider.getCurrentUserId(request);

        // Pageable 객체 생성 (최신순 고정, sort는 Repository 쿼리에서 처리)
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size);

        // 서비스 호출
        com.salemale.domain.item.dto.response.LikedItemListResponse response =
                itemService.getLikedItems(userId, pageable);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 경매 상품 리스트 조회
     * GET /auctions
     * - 상태별, 카테고리별, 가격별 필터링 지원
     * - 다양한 정렬 옵션 지원
     * - 페이징 지원
     */
    @Operation(summary = "경매 상품 리스트 조회", description = "경매 상품 목록을 조회합니다. 상태, 카테고리, 가격 범위로 필터링하고 다양한 기준으로 정렬할 수 있습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AuctionListResponse>> getAuctions(
            @Parameter(description = "상태 필터 (기본값: BIDDING - 진행중)", example = "BIDDING")
            @RequestParam(required = false, defaultValue = "BIDDING")  // ← 기본값 추가!
            AuctionStatus status,

            @Parameter(description = "카테고리 필터", example = "DIGITAL")
            @RequestParam(required = false) List<Category> categories,

            @Parameter(description = "최소 가격", example = "10000")
            @RequestParam(required = false) Integer minPrice,

            @Parameter(description = "최대 가격", example = "500000")
            @RequestParam(required = false) Integer maxPrice,

            @Parameter(description = "정렬 기준 (CREATED_DESC: 최신순, BID_COUNT_DESC: 입찰많은순, PRICE_ASC: 낮은가격순, PRICE_DESC: 높은가격순, VIEW_COUNT_DESC: 조회수많은순, END_TIME_ASC: 마감임박순)",
                    example = "CREATED_DESC")
            @RequestParam(required = false, defaultValue = "CREATED_DESC")
            AuctionSortType sort,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,

            @Parameter(description = "페이지당 아이템 개수", example = "20")
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        // Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // 서비스 호출
        AuctionListResponse response = itemService.getAuctionList(status, categories, minPrice, maxPrice, sort, pageable);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}