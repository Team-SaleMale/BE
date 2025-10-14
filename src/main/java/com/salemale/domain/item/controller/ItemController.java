package com.salemale.domain.item.controller;

import com.salemale.common.code.status.SuccessStatus;
import com.salemale.common.response.ApiResponse;
import com.salemale.domain.item.dto.request.BidRequest;
import com.salemale.domain.item.dto.request.ItemRegisterRequest;
import com.salemale.domain.item.dto.response.BidResponse;
import com.salemale.domain.item.dto.response.ItemLikeResponse;
import com.salemale.domain.item.dto.response.ItemRegisterResponse;
import com.salemale.domain.item.dto.response.detail.ItemDetailResponse;
import com.salemale.domain.item.service.ItemService;
import com.salemale.global.security.jwt.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CurrentUserProvider currentUserProvider; // JWT에서 UID 추출

    /**
     * 경매 상품 찜하기
     * POST /auctions/{itemId}/liked
     * 
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
     * 
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
     * 
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
     * 
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
}