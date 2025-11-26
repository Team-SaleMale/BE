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
            log.error("Gemini 브랜드 분석 실패: {}", e.getMessage());
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
                아래 정보를 기반으로 브랜드 심층 분석과 중고 경매 전략을 각각 3~4문장 이내로 정리하세요.
                - 브랜드: %s

                출력 형식 지침:
                1) 마크다운, 불릿, 별표, 번호표를 사용하지 말고 문단 사이에 빈 줄만 넣습니다.
                2) 전문적인 어조를 유지하고 중복 표현은 제외합니다.
                3) 섹션 헤더는 'A. 브랜드 분석', 'B. 경매 전략'으로 명확히 표기합니다.
                4) 경매 전략에는 권장 시작가와 목표 낙찰가 범위를 숫자로 포함합니다.
                """.formatted(request.getBrandName());
    }

    private String buildProductPrompt(ProductAnalysisRequest request) {
        return """
                당신은 중고 명품 경매 플랫폼을 위한 상품 컨설턴트입니다.
                아래 정보를 기반으로 상품 특화 포인트와 중고 경매 전략을 각각 3~4문장 이내로 정리하세요.
                - 브랜드: %s
                - 상품: %s

                출력 형식 지침:
                1) 마크다운, 불릿, 별표, 번호표를 사용하지 말고 문단 사이에 빈 줄만 넣습니다.
                2) 전문적인 어조를 유지하고 중복 표현은 제외합니다.
                3) 섹션 헤더는 'A. 상품 포인트', 'B. 경매 전략'으로 명확히 표기합니다.
                4) 경매 전략에는 권장 시작가와 목표 낙찰가 범위를 숫자로 포함합니다.
                """.formatted(request.getBrandName(), request.getProductName());
    }

    private String extractText(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                throw new GeneralException(ErrorStatus.GEMINI_API_ERROR);
            }
            return candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText("")
                    .trim();
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini 텍스트 응답 파싱 실패: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.GEMINI_API_ERROR);
        }
    }
}

