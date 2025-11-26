package com.salemale.domain.experimental.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.experimental.dto.BrandAnalysisRequest;
import com.salemale.domain.experimental.dto.BrandAnalysisResponse;
import com.salemale.domain.experimental.dto.ProductAnalysisRequest;
import com.salemale.domain.experimental.dto.ProductAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExperimentalBrandAnalysisService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.model}")
    private String model;

    public ExperimentalBrandAnalysisService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.api-url}") String apiUrl
    ) {
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public BrandAnalysisResponse analyzeBrand(BrandAnalysisRequest request) {
        Map<String, Object> body = createRequestBody(buildBrandPrompt(request));

        try {
            String response = webClient.post()
                    .uri("/" + model + ":generateContent")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));

            return BrandAnalysisResponse.builder()
                    .report(extractText(response))
                    .build();
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini 브랜드 분석 실패", e);
            throw new GeneralException(ErrorStatus.GEMINI_API_ERROR);
        }
    }

    public ProductAnalysisResponse analyzeProduct(ProductAnalysisRequest request) {
        Map<String, Object> body = createRequestBody(buildProductPrompt(request));

        try {
            String response = webClient.post()
                    .uri("/" + model + ":generateContent")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));

            return ProductAnalysisResponse.builder()
                    .report(extractText(response))
                    .build();
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini 상품 분석 실패: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.GEMINI_API_ERROR);
        }
    }

    private Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );
    }

    private String buildBrandPrompt(BrandAnalysisRequest request) {
        return """
                당신은 중고 명품 경매 플랫폼을 위한 컨설턴트입니다.
                아래 정보를 기반으로 브랜드 분석을 수행하고, 긍정적이고 거래를 촉진하는 어조로 답변하세요.
                - 브랜드: %s

                출력 형식 지침:
                1) 항상 JSON 형식으로만 응답합니다.
                2) 필수 키:
                   {
                     "brand": {
                       "description": "...",
                       "value": "...",
                       "marketTrend": "..."
                     }
                   }
                3) 각 필드당 2~3문장, 숫자나 구체적 지표가 있다면 포함합니다.
                4) 시장 동향(marketTrend)은 최근 거래 분위기나 관심도를 긍정적으로 설명합니다.
                5) 부정적인 표현은 피하고, 신뢰와 기대감을 주는 문장을 사용합니다.
                """.formatted(request.getBrandName());
    }

    private String buildProductPrompt(ProductAnalysisRequest request) {
        return """
                당신은 중고 명품 경매 플랫폼을 위한 상품 컨설턴트입니다.
                아래 정보를 기반으로 상품 분석을 수행하고, 긍정적이고 거래를 촉진하는 어조로 답변하세요.
                - 브랜드: %s
                - 상품: %s

                출력 형식 지침:
                1) 항상 JSON 형식으로만 응답합니다.
                2) 필수 키:
                   {
                     "product": {
                       "description": "...",
                       "price": "...",
                       "value": "...",
                       "marketTrend": "..."
                     }
                   }
                3) 각 필드당 2~3문장, 가격(price)에는 권장 시작가/목표 낙찰가 범위를 숫자로 포함합니다.
                4) marketTrend는 최근 수요나 거래 열기를 긍정적으로 요약합니다.
                5) 부정적인 표현은 피하고, 입찰을 유도할 수 있는 메시지를 사용합니다.
                """.formatted(request.getBrandName(), request.getProductName());
    }

    private String extractText(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                throw new GeneralException(ErrorStatus.GEMINI_API_ERROR);
            }
            JsonNode candidate = candidates.get(0);
            JsonNode parts = candidate.path("content").path("parts");
            if (parts.isEmpty() || !parts.isArray()) {
                throw new GeneralException(ErrorStatus.GEMINI_API_ERROR);
            }
            return parts.get(0)
                    .path("text")
                    .asText("")
                    .trim();
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini 텍스트 응답 파싱 실패", e);
            throw new GeneralException(ErrorStatus.GEMINI_API_ERROR);
        }
    }
}

