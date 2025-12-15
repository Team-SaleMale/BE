package com.salemale.domain.item.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.item.dto.response.ProductAnalysisResponse;
import com.salemale.global.common.enums.Category;
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
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ItemImageAiService {

    private final WebClient geminiWebClient;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${gemini.model}")
    private String model;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    // 이미지 크기 제한 (Base64 인코딩 후 4MB)
    private static final int MAX_IMAGE_SIZE = 4 * 1024 * 1024;

    public ItemImageAiService(
            S3Client s3Client,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.api-url}") String apiUrl) {

        this.s3Client = s3Client;
        this.objectMapper = objectMapper;

        // Gemini 전용 WebClient 설정
        this.geminiWebClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 이미지 분석
     */
    public ProductAnalysisResponse analyzeProductImage(String imageUrl) {
        // 1. temp URL 검증
        if (!imageUrl.contains("/temp/")) {
            throw new GeneralException(ErrorStatus.IMAGE_NOT_TEMP_URL);
        }

        // 2. S3에서 이미지 다운로드 (MIME 타입 포함)
        ImageData imageData = downloadImageFromS3(imageUrl);

        // 3. 이미지 크기 검증
        if (imageData.base64Data.length() > MAX_IMAGE_SIZE) {
            throw new GeneralException(ErrorStatus.IMAGE_SIZE_EXCEEDED);
        }

        // 4. Gemini API 호출
        String responseText = callGeminiApi(imageData);

        // 5. 응답 파싱
        return parseGeminiResponse(responseText);
    }

    /**
     * S3에서 이미지와 MIME 타입 다운로드
     */
    private ImageData downloadImageFromS3(String imageUrl) {
        try {
            String s3Key = extractS3KeyFromUrl(imageUrl);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            
            // 실제 MIME 타입 추출
            String contentType = objectBytes.response().contentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = detectMimeType(s3Key);
            }

            byte[] imageBytes = objectBytes.asByteArray();
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);

            return new ImageData(base64Data, contentType);

        } catch (S3Exception e) {
            throw new GeneralException(ErrorStatus.IMAGE_DOWNLOAD_FAILED);
        }
    }

    /**
     * 파일 확장자로 MIME 타입 추정
     */
    private String detectMimeType(String s3Key) {
        String lowerKey = s3Key.toLowerCase();
        if (lowerKey.endsWith(".png")) return "image/png";
        if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg")) return "image/jpeg";
        if (lowerKey.endsWith(".webp")) return "image/webp";
        if (lowerKey.endsWith(".gif")) return "image/gif";
        return "image/jpeg"; // 기본값
    }

    /**
     * Gemini API 호출
     */
    private String callGeminiApi(ImageData imageData) {
        try {
            Map<String, Object> requestBody = createGeminiRequestBody(imageData);

            String response = geminiWebClient.post()
                    .uri("/" + model + ":generateContent")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return response;

        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.GEMINI_API_ERROR);
        }
    }

    /**
     * Gemini API 요청 본문 생성 (최적화)
     */
    private Map<String, Object> createGeminiRequestBody(ImageData imageData) {
        // 단순하고 명확한 프롬프트
        String prompt = "Analyze this product image and extract information in JSON format:\n" +
                "{\n" +
                "  \"productName\": \"full product name with brand and model\",\n" +
                "  \"category\": \"one of: HOME_APPLIANCE, HEALTH_FOOD, BEAUTY, FOOD_PROCESSED, PET, DIGITAL, LIVING_KITCHEN, WOMEN_ACC, SPORTS, PLANT, GAME_HOBBY, TICKET, FURNITURE, BOOK, KIDS, CLOTHES, ETC\",\n" +
                "  \"confidence\": 0.0 to 1.0\n" +
                "}\n" +
                "Examples: \"Samsung Galaxy S24\", \"Nike Air Max 270\", \"iPhone 15 Pro\"\n" +
                "Return ONLY the JSON object, no markdown, no explanation.";

        // 텍스트 파트
        Map<String, String> textPart = new HashMap<>();
        textPart.put("text", prompt);

        // 이미지 파트 (정확한 MIME 타입 사용)
        Map<String, Object> imagePart = new HashMap<>();
        Map<String, String> inlineData = new HashMap<>();
        inlineData.put("mime_type", imageData.mimeType);
        inlineData.put("data", imageData.base64Data);
        imagePart.put("inline_data", inlineData);

        // 컨텐츠 구성
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(textPart, imagePart));

        // generationConfig 추가 (JSON 응답 강제)
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4);  // 일관성 높이기
        generationConfig.put("topP", 0.8);
        generationConfig.put("topK", 40);
        generationConfig.put("maxOutputTokens", 512);
        generationConfig.put("responseMimeType", "application/json");  // JSON 강제

        // 최종 요청 본문
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    /**
     * Gemini 응답 파싱
     */
    private ProductAnalysisResponse parseGeminiResponse(String responseText) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseText);
            JsonNode candidatesNode = rootNode.path("candidates");

            if (candidatesNode.isEmpty()) {
                throw new GeneralException(ErrorStatus.IMAGE_ANALYSIS_FAILED);
            }

            String text = candidatesNode.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // JSON 정제 (마크다운 제거)
            String jsonText = extractJsonFromMarkdown(text);

            // 상품 정보 파싱
            JsonNode productNode = objectMapper.readTree(jsonText);

            return ProductAnalysisResponse.builder()
                    .productName(productNode.path("productName").asText(""))
                    .category(parseCategory(productNode.path("category").asText("ETC")))
                    .confidence(productNode.path("confidence").asDouble(0.0))
                    .build();

        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.IMAGE_ANALYSIS_FAILED);
        }
    }

    /**
     * 마크다운 제거
     */
    private String extractJsonFromMarkdown(String text) {
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
     * 카테고리 파싱
     */
    private Category parseCategory(String categoryStr) {
        try {
            return Category.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Category.ETC;
        }
    }

    /**
     * S3 키 추출
     */
    private String extractS3KeyFromUrl(String url) {
        try {
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);

            // Virtual-hosted-style
            String virtualHostedPattern = "https?://" + Pattern.quote(bucketName) +
                    "\\.s3\\." + Pattern.quote(region) + "\\.amazonaws\\.com/(.+)";

            Pattern pattern = Pattern.compile(virtualHostedPattern);
            Matcher matcher = pattern.matcher(decodedUrl);

            if (matcher.find()) {
                return matcher.group(1);
            }

            // Path-style
            String pathStylePattern = "https?://s3\\." + Pattern.quote(region) +
                    "\\.amazonaws\\.com/" + Pattern.quote(bucketName) + "/(.+)";

            pattern = Pattern.compile(pathStylePattern);
            matcher = pattern.matcher(decodedUrl);

            if (matcher.find()) {
                return matcher.group(1);
            }

            throw new GeneralException(ErrorStatus.INVALID_IMAGE_URL);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_URL);
        }
    }

    /**
     * 이미지 데이터 클래스
     */
    private static class ImageData {
        private final String base64Data;
        private final String mimeType;

        public ImageData(String base64Data, String mimeType) {
            this.base64Data = base64Data;
            this.mimeType = mimeType;
        }
    }
}
