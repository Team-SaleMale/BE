package com.salemale.domain.item.controller;

import com.salemale.common.code.status.SuccessStatus;
import com.salemale.common.response.ApiResponse;
import com.salemale.domain.item.dto.request.BidRequest;
import com.salemale.domain.item.dto.request.ItemRegisterRequest;
import com.salemale.domain.item.dto.response.BidResponse;
import com.salemale.domain.item.dto.response.ItemLikeResponse;
import com.salemale.domain.item.dto.response.ItemRegisterResponse;
import com.salemale.domain.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
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

    /**
     * 경매 상품 찜하기
     * POST /auctions/{itemId}/liked
     */
    @Operation(summary = "경매 상품 찜하기", description = "본인이 등록한 상품은 찜할 수 없습니다.")
    @PostMapping("/{itemId}/liked")
    public ResponseEntity<ApiResponse<ItemLikeResponse>> likeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId
    ) {
        String email = userDetails.getUsername();
        ItemLikeResponse response = itemService.likeItem(email, itemId);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 경매 상품 찜 취소
     * DELETE /auctions/{itemId}/liked
     */
    @Operation(summary = "경매 상품 찜 취소", description = "경매 상품 찜을 취소합니다.")
    @DeleteMapping("/{itemId}/liked")
    public ResponseEntity<ApiResponse<ItemLikeResponse>> unlikeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId
    ) {
        String email = userDetails.getUsername();
        ItemLikeResponse response = itemService.unlikeItem(email, itemId);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 경매 상품 등록 API
     * POST /auctions/registration
     */
    @Operation(summary = "경매 상품 등록하기")
    @PostMapping("/registration")
    public ResponseEntity<ApiResponse<ItemRegisterResponse>> registerItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ItemRegisterRequest request
    ) {
        // 1. 인증된 사용자 식별 (이메일 = UserDetails.getUsername())
        String email = userDetails.getUsername();

        // 2. 서비스 로직 위임 및 결과 반환
        ItemRegisterResponse response = itemService.registerItem(email, request);

        // 3. 201 Created 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessStatus._CREATED, response));
    }

    /**
     * 경매 상품 입찰
     * POST /auctions/{itemId}/bid
     */
    @Operation(summary = "경매 상품 입찰", description = "경매 중인 상품에 입찰합니다.")
    @PostMapping("/{itemId}/bid")
    public ResponseEntity<ApiResponse<BidResponse>> bidOnItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @Valid @RequestBody BidRequest request
    ) {
        String email = userDetails.getUsername();
        BidResponse response = itemService.bidOnItem(email, itemId, request);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}