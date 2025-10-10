package com.salemale.domain.item.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.item.dto.response.ItemLikeResponse;
import com.salemale.domain.item.service.ItemLikeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class ItemLikeController {

    private final ItemLikeService itemLikeService;

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
        ItemLikeResponse response = itemLikeService.likeItem(email, itemId);

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
        ItemLikeResponse response = itemLikeService.unlikeItem(email, itemId);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}