package com.salemale.domain.gemini.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.item.dto.response.ProductAnalysisResponse;
import com.salemale.global.common.enums.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final WebClient.Builder webClientBuilder;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.api-url}")
    private String apiUrl;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * 이미지 URL을 받아 상품 정보를 분석합니다.
     * @param imageUrl S3 temp 폴더의 이미지 URL
     * @return AI가 분석한 상품 정보
     */
    public ProductAnalysisResponse analyzeProductImage(String imageUrl) {

        // 1. temp URL 검증
        if (!imageUrl.contains("/temp/")) {
            throw new GeneralException(ErrorStatus.IMAGE_NOT_TEMP_URL);
        }

        // 2. S3에서 이미지 다운로드
        byte[] imageBytes = downloadImageFromS3(imageUrl);

        // 3. Base64 인코딩
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // 4. Gemini API 호출
        String responseText = callGeminiApi(base64Image);

        // 5. 응답 파싱 및 DTO 생성
        return parseGeminiResponse(responseText);
    }

    /**
     * S3에서 이미지를 다운로드합니다.
     */
    private byte[] downloadImageFromS3(String imageUrl) {
        try {
            // URL에서 S3 키 추출
            String s3Key = extractS3KeyFromUrl(imageUrl);

            // S3에서 이미지 가져오기
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            log.info("S3 이미지 다운로드 성공 - 크기: {} bytes", objectBytes.asByteArray().length);

            return objectBytes.asByteArray();

        } catch (S3Exception e) {
            log.error("S3 이미지 다운로드 실패: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.IMAGE_DOWNLOAD_FAILED);
        }
    }

    /**
     * Gemini API를 호출하여 이미지 분석 결과를 받습니다.
     */
    private String callGeminiApi(String base64Image) {
        try {
            // 요청 본문 생성
            Map<String, Object> requestBody = createGeminiRequestBody(base64Image);

            // WebClient로 API 호출
            WebClient webClient = webClientBuilder.build();

            String response = webClient.post()
                    .uri(apiUrl + "/" + model + ":generateContent")  // key 제거
                    .header("x-goog-api-key", apiKey)  // 헤더로 전달
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return response;

        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.GEMINI_API_ERROR);
        }
    }

    /**
     * Gemini API 요청 본문을 생성합니다.
     */
    private Map<String, Object> createGeminiRequestBody(String base64Image) {
        // 프롬프트 설정
        String prompt = """
            이 상품 이미지를 분석해서 다음 정보를 JSON 형식으로 추출해:
            {
              "productName": "상품의 전체 이름 (브랜드 + 모델명 포함)",
              "category": "카테고리",
              "confidence": 신뢰도
            }
            
            중요: productName은 반드시 구체적으로 작성해
            예시: "메리다 스컬트라 100", "삼성 갤럭시 S24", "나이키 에어맥스 270", "아이폰 17PRO"
            
            카테고리 목록:
            HOME_APPLIANCE, HEALTH_FOOD, BEAUTY, FOOD_PROCESSED, PET, 
            DIGITAL, LIVING_KITCHEN, WOMEN_ACC, SPORTS, PLANT, 
            GAME_HOBBY, TICKET, FURNITURE, BOOK, KIDS, CLOTHES, ETC
            
            JSON 형식만 출력하면돼.
            """;

        // 이미지 파트
        Map<String, Object> imagePart = new HashMap<>();
        Map<String, String> inlineData = new HashMap<>();
        inlineData.put("mime_type", "image/jpeg");
        inlineData.put("data", base64Image);
        imagePart.put("inline_data", inlineData);

        // 텍스트 파트
        Map<String, String> textPart = new HashMap<>();
        textPart.put("text", prompt);

        // 컨텐츠 구성
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(textPart, imagePart));

        // 최종 요청 본문
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        return requestBody;
    }

    /**
     * Gemini 응답을 파싱하여 DTO로 변환합니다.
     */
    private ProductAnalysisResponse parseGeminiResponse(String responseText) {
        try {
            // JSON 파싱
            JsonNode rootNode = objectMapper.readTree(responseText);
            JsonNode candidatesNode = rootNode.path("candidates");

            if (candidatesNode.isEmpty()) {
                throw new GeneralException(ErrorStatus.IMAGE_ANALYSIS_FAILED);
            }

            // 텍스트 추출
            String text = candidatesNode.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // JSON 부분만 추출 (마크다운 코드 블록 제거)
            String jsonText = extractJsonFromMarkdown(text);

            // 상품 정보 파싱
            JsonNode productNode = objectMapper.readTree(jsonText);

            return ProductAnalysisResponse.builder()
                    .productName(productNode.path("productName").asText(""))
                    .category(parseCategory(productNode.path("category").asText("ETC")))
                    .confidence(productNode.path("confidence").asDouble(0.0))
                    .build();

        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.IMAGE_ANALYSIS_FAILED);
        }
    }

    /**
     * 마크다운 코드 블록에서 JSON 추출
     */
    private String extractJsonFromMarkdown(String text) {
        // ```json 또는 ``` 블록 제거
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    /**
     * 문자열을 Category enum으로 변환
     */
    private Category parseCategory(String categoryStr) {
        try {
            return Category.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 카테고리: {}. ETC로 설정합니다.", categoryStr);
            return Category.ETC;
        }
    }

    /**
     * URL에서 S3 키 추출
     */
    private String extractS3KeyFromUrl(String url) {
        try {
            // URL 디코딩 (한글 파일명 처리)
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);

            // Virtual-hosted-style URL: bucket.s3.region.amazonaws.com/key
            String pattern = bucketName + ".s3." + region + ".amazonaws.com/";

            if (decodedUrl.contains(pattern)) {
                String[] parts = decodedUrl.split(pattern);
                if (parts.length >= 2) {
                    return parts[1];
                }
            }

            // 파싱 실패
            log.error("URL 파싱 실패 - URL: {}, Bucket: {}, Region: {}",
                    decodedUrl, bucketName, region);
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_URL);

        } catch (Exception e) {
            log.error("URL 파싱 중 에러: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_URL);
        }
    }
}