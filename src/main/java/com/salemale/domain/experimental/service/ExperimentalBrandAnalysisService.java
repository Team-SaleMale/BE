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
                아래 브랜드 정보를 기반으로,
                1) 브랜드 분석
                2) 예상 가치 (비슷한 브랜드/시장 대조군을 포함해 설명)
                3) 향후 1~3년 예상 추세
                를 간결하게 한국어로 설명하세요.

                - 브랜드: %s

                출력 형식 지침:
                1) 마크다운, 불릿, 번호, JSON 형식을 사용하지 말고 순수한 문장만 사용합니다.
                2) "브랜드 분석:", "예상 가치:", "예상 추세:" 세 문단으로만 구성합니다.
                3) 각 문단은 2~4문장 이내로, 가능한 한 구체적인 숫자/사례를 포함합니다.
                4) 부정적인 표현은 피하고, 중립적이되 거래를 촉진할 수 있는 어조를 사용합니다.
                """.formatted(request.getBrandName());
    }

    private String buildProductPrompt(ProductAnalysisRequest request) {
        return """
                당신은 중고 명품 경매 플랫폼을 위한 상품 컨설턴트입니다.
                아래 상품 정보를 기반으로,
                1) 상품 분석
                2) 예상 가치 (유사 상품/브랜드 대조군을 포함해 설명)
                3) 향후 1~3년 예상 추세
                를 간결하게 한국어로 설명하세요.

                - 상품: %s

                출력 형식 지침:
                1) 마크다운, 불릿, 번호, JSON 형식을 사용하지 말고 순수한 문장만 사용합니다.
                2) "상품 분석:", "예상 가치:", "예상 추세:" 세 문단으로만 구성합니다.
                3) 각 문단은 2~4문장 이내로, 가격과 관련해서는 권장 시작가/목표 낙찰가 범위를 숫자로 포함합니다.
                4) 부정적인 표현은 피하고, 입찰을 유도할 수 있는 긍정적인 어조를 사용합니다.
                """.formatted(request.getProductName());
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

