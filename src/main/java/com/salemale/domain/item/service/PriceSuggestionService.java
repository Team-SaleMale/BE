package com.salemale.domain.item.service;

import com.salemale.domain.item.dto.response.PriceSuggestionResponse;
import com.salemale.domain.item.entity.MarketPrice;
import com.salemale.domain.item.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 초기 가격 추천 서비스
 * FastAPI 서버와 통신하여 상품의 적정 시작 가격을 추천받습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceSuggestionService {

    private final WebClient webClient;
    private final MarketPriceRepository marketPriceRepository;

    @Value("${recommendation.api.url:http://localhost:8000}")
    private String recommendationApiUrl;

    /**
     * FastAPI 응답 DTO (내부 클래스)
     */
    private static class FastApiPriceResponse {
        private Integer suggested_start_price;
        private String message;

        public Integer getSuggested_start_price() {
            return suggested_start_price;
        }

        public void setSuggested_start_price(Integer suggested_start_price) {
            this.suggested_start_price = suggested_start_price;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 상품명을 기반으로 초기 가격을 추천받습니다.
     * 
     * @param productName 상품명
     * @return 가격 추천 응답
     */
    public PriceSuggestionResponse suggestPrice(String productName) {
        try {
            log.info("[가격 추천 API 호출] 상품명: {}, URL: {}", productName, recommendationApiUrl);

            // FastAPI 서버 호출
            FastApiPriceResponse apiResponse = webClient
                    .post()
                    .uri(recommendationApiUrl + "/api/price-suggest")
                    .bodyValue(Map.of("product_name", productName))
                    .retrieve()
                    .bodyToMono(FastApiPriceResponse.class)
                    .timeout(Duration.ofSeconds(65))  // 65초 타임아웃 (FastAPI 60초 + 여유 5초)
                    .onErrorResume(e -> {
                        log.error("[가격 추천 API 오류] 상품명: {}, 오류: {}", productName, e.getMessage());
                        
                        // 타임아웃 에러인 경우
                        if (e instanceof TimeoutException || e instanceof WebClientRequestException) {
                            return Mono.empty();  // empty를 반환하여 타임아웃 처리
                        }
                        
                        return Mono.empty();
                    })
                    .block();

            // 응답이 null이거나 타임아웃된 경우
            if (apiResponse == null) {
                log.warn("[가격 추천 API 타임아웃] 상품명: {}", productName);
                return PriceSuggestionResponse.timeout();
            }

            // 시세 데이터가 없는 경우
            if (apiResponse.getSuggested_start_price() == null) {
                log.warn("[가격 추천 API 데이터 없음] 상품명: {}, 메시지: {}", productName, apiResponse.getMessage());
                return PriceSuggestionResponse.noData();
            }

            // 성공 응답
            log.info("[가격 추천 API 성공] 상품명: {}, 추천가: {}", productName, apiResponse.getSuggested_start_price());
            return PriceSuggestionResponse.builder()
                    .recommendedPrice(apiResponse.getSuggested_start_price())
                    .message(apiResponse.getMessage())
                    .dataAvailable(true)
                    .build();

        } catch (Exception e) {
            log.error("[가격 추천 API 예외] 상품명: {}, 예외: {}", productName, e.getMessage(), e);
            return PriceSuggestionResponse.error("시세 조회 중 오류가 발생했습니다");
        }
    }

    /**
     * DB에서 최근 시세 데이터 조회 (캐시된 데이터 활용)
     * 
     * @param keyword 검색 키워드
     * @return 시세 정보 리스트 (최근 7일 이내)
     */
    public List<MarketPrice> getRecentMarketPrices(String keyword) {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return marketPriceRepository.findRecentByKeyword(keyword, weekAgo);
    }

    /**
     * 특정 플랫폼의 최근 시세 데이터 조회
     * 
     * @param keyword 검색 키워드
     * @param platform 플랫폼명 (joongna, daangn 등)
     * @return 시세 정보 리스트 (최근 7일 이내)
     */
    public List<MarketPrice> getRecentMarketPricesByPlatform(String keyword, String platform) {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return marketPriceRepository.findRecentByKeywordAndPlatform(keyword, platform, weekAgo);
    }
}
