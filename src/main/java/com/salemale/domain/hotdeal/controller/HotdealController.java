package com.salemale.domain.hotdeal.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.hotdeal.dto.request.HotdealRegisterRequest;
import com.salemale.domain.hotdeal.dto.response.HotdealListResponse;
import com.salemale.domain.hotdeal.dto.response.HotdealStoreResponse;
import com.salemale.domain.hotdeal.service.HotdealService;
import com.salemale.domain.hotdeal.service.HotdealStoreService;
import com.salemale.domain.item.dto.response.AuctionListResponse;
import com.salemale.domain.item.dto.response.ItemRegisterResponse;
import com.salemale.domain.item.enums.AuctionSortType;
import com.salemale.global.security.jwt.CurrentUserProvider;  // 🔥 추가
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;  // 🔥 추가
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 핫딜 관련 API 컨트롤러
 */
@Slf4j
@Tag(name = "Hotdeal", description = "핫딜 관련 API")
@RestController
@RequiredArgsConstructor
public class HotdealController {

    private final HotdealStoreService hotdealStoreService;
    private final HotdealService hotdealService;
    private final CurrentUserProvider currentUserProvider;  // 🔥 추가

    /**
     * 내 가게 정보 조회
     * - 핫딜 판매자만 조회 가능
     * - 승인된 가게 정보 반환
     */
    @Operation(summary = "내 가게 정보 조회", description = "핫딜 판매자의 가게 정보를 조회합니다.")
    @GetMapping("/api/hotdeals/my-store")
    public ResponseEntity<ApiResponse<HotdealStoreResponse>> getMyStore(
            @Parameter(hidden = true) HttpServletRequest request  // 🔥 변경
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);  // 🔥 추가
        log.info("[API] 내 가게 정보 조회 - 사용자 ID: {}", userId);

        HotdealStoreResponse response = hotdealStoreService.getMyStore(userId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 핫딜 상품 등록
     * - 핫딜 판매자만 등록 가능
     * - 카테고리, 거래방식, title은 자동 설정
     */
    @Operation(summary = "핫딜 상품 등록", description = "핫딜 상품을 등록합니다. (핫딜 판매자 전용)")
    @PostMapping("/api/hotdeals")
    public ResponseEntity<ApiResponse<ItemRegisterResponse>> registerHotdeal(
            @Parameter(hidden = true) HttpServletRequest request,  // 🔥 변경
            @Valid @RequestBody HotdealRegisterRequest requestBody
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);  // 🔥 추가
        log.info("[API] 핫딜 상품 등록 - 사용자 ID: {}, 상품명: {}",
                userId, requestBody.getName());

        ItemRegisterResponse response = hotdealService.registerHotdeal(userId, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess(response));
    }

    /**
     * 핫딜 상품 리스트 조회
     * - 모든 사용자 조회 가능
     * - 가격 필터링 및 정렬 지원
     */
    @Operation(summary = "핫딜 상품 리스트 조회", description = "핫딜 상품 리스트를 조회합니다.")
    @GetMapping("/api/hotdeals")
    public ResponseEntity<ApiResponse<HotdealListResponse>> getHotdealList(
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false, defaultValue = "CREATED_DESC") AuctionSortType sortType,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("[API] 핫딜 리스트 조회 - minPrice: {}, maxPrice: {}, sortType: {}, page: {}",
                minPrice, maxPrice, sortType, pageable.getPageNumber());

        HotdealListResponse response = hotdealService.getHotdealList(
                minPrice, maxPrice, sortType, pageable
        );
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}