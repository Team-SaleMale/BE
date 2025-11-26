package com.salemale.domain.experimental.service;

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.experimental.dto.RecommendationRequest;
import com.salemale.domain.experimental.dto.RecommendationResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperimentalRecommendationService {

    private final WebClient webClient;

    @Value("${RECOMMENDATION_API_URL:http://ai-server:8000}")
    private String recommendationBaseUrl;

    @Value("${EXPERIMENTAL_RECOMMENDATION_ENDPOINT:/recommend-auctions}")
    private String recommendationEndpoint;

    @Value("${EXPERIMENTAL_RECOMMENDATION_TIMEOUT_SECONDS:10}")
    private long timeoutSeconds;

    public RecommendationResponse requestRecommendations(RecommendationRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", request.getUserId());
        if (request.getLimit() != null) {
            payload.put("limit", request.getLimit());
        }

        RecommendationApiResponse apiResponse;
        try {
            apiResponse = webClient.mutate()
                    .baseUrl(recommendationBaseUrl)
                    .build()
                    .post()
                    .uri(recommendationEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(bodyStr -> {
                                        log.error("[Recommendation] 외부 API 오류 status={} body={}", response.statusCode(), bodyStr);
                                        return Mono.error(new GeneralException(ErrorStatus.RECOMMENDATION_API_CALL_FAILED));
                                    }))
                    .bodyToMono(RecommendationApiResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
        } catch (Exception e) {
            log.error("[Recommendation] 외부 API 호출 실패 userId={}", request.getUserId(), e);
            throw new GeneralException(ErrorStatus.RECOMMENDATION_API_CALL_FAILED);
        }

        if (apiResponse == null || !apiResponse.isValid()) {
            log.error("[Recommendation] 외부 API 응답이 유효하지 않습니다: {}", apiResponse);
            throw new GeneralException(ErrorStatus.RECOMMENDATION_API_INVALID_RESPONSE);
        }

        return RecommendationResponse.builder()
                .recommendedItemIds(apiResponse.getRecommendedItemIds())
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RecommendationApiResponse {
        @JsonProperty("recommended_items")
        private List<RecommendedItem> recommendedItems = new ArrayList<>();

        public List<Long> getRecommendedItemIds() {
            return recommendedItems.stream()
                    .map(RecommendedItem::getItemId)
                    .toList();
        }

        public boolean isValid() {
            return recommendedItems != null && !recommendedItems.isEmpty();
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class RecommendedItem {
            @JsonProperty("item_id")
            private Long itemId;

            public Long getItemId() {
                return itemId;
            }
        }
    }
}

