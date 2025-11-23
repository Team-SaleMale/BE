package com.salemale.domain.search.controller;

import com.salemale.global.external.naver.NaverShopSearchResponse;
import com.salemale.global.external.naver.NaverShoppingClient;
import com.salemale.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class NaverSearchController {

    private final NaverShoppingClient naverShoppingClient;

    /**
     * 네이버 쇼핑 검색
     */
    @Operation(summary = "네이버쇼핑 가격 검색", description = "10개 표시")
    @GetMapping("/naver")
    public ResponseEntity<ApiResponse<NaverShopSearchResponse>> getNaverShoppingSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        NaverShopSearchResponse response = naverShoppingClient.search(query, limit);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
