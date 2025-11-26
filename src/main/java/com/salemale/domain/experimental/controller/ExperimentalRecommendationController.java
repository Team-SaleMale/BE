package com.salemale.domain.experimental.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.experimental.dto.RecommendationRequest;
import com.salemale.domain.experimental.dto.RecommendationResponse;
import com.salemale.domain.experimental.service.ExperimentalRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/experimental")
public class ExperimentalRecommendationController {

    private final ExperimentalRecommendationService experimentalRecommendationService;

    @PostMapping("/recommend-auctions")
    public ResponseEntity<ApiResponse<RecommendationResponse>> recommendAuctions(
            @Valid @RequestBody RecommendationRequest request
    ) {
        RecommendationResponse response = experimentalRecommendationService.requestRecommendations(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}

