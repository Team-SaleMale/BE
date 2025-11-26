package com.salemale.domain.experimental.service;

import com.salemale.common.exception.GeneralException;
import com.salemale.common.code.status.ErrorStatus;
import com.salemale.domain.experimental.dto.VirtualTryOnResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.MultipartBodyBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperimentalTryOnService {

    private final WebClient webClient;

    @Value("${experimental.tryon.base-url:http://ai-server:8000}")
    private String tryOnBaseUrl;

    @Value("${experimental.tryon.endpoint:/virtual-tryon}")
    private String tryOnEndpoint;

    @Value("${experimental.tryon.timeout-seconds:120}")
    private long timeoutSeconds;

    public VirtualTryOnResponse requestVirtualTryOn(
            MultipartFile background,
            MultipartFile garment,
            String garmentDesc,
            Boolean crop,
            Integer denoiseSteps,
            Integer seed
    ) {
        validateFile(background, "background");
        validateFile(garment, "garment");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        addFilePart(builder, "background", background);
        addFilePart(builder, "garment", garment);

        builder.part("garment_desc", garmentDesc != null ? garmentDesc : "")
                .contentType(MediaType.TEXT_PLAIN);
        builder.part("crop", String.valueOf(crop != null && crop))
                .contentType(MediaType.TEXT_PLAIN);
        builder.part("denoise_steps", String.valueOf(denoiseSteps != null ? denoiseSteps : 30))
                .contentType(MediaType.TEXT_PLAIN);
        builder.part("seed", String.valueOf(seed != null ? seed : 42))
                .contentType(MediaType.TEXT_PLAIN);

        MultiValueMap<String, org.springframework.http.HttpEntity<?>> body = builder.build();

        TryOnApiResponse apiResponse;
        try {
            apiResponse = webClient.mutate()
                    .baseUrl(tryOnBaseUrl)
                    .build()
                    .post()
                    .uri(tryOnEndpoint)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(bodyStr -> {
                                        log.error("[VirtualTryOn] 외부 API 오류 status={} body={}", response.statusCode(), bodyStr);
                                        return Mono.error(new GeneralException(ErrorStatus.TRYON_API_CALL_FAILED));
                                    })
                    )
                    .bodyToMono(TryOnApiResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
        } catch (Exception e) {
            log.error("[VirtualTryOn] 외부 API 호출 실패", e);
            throw new GeneralException(ErrorStatus.TRYON_API_CALL_FAILED);
        }

        if (apiResponse == null || !apiResponse.isValid()) {
            log.error("[VirtualTryOn] 외부 API 응답이 유효하지 않습니다: {}", apiResponse);
            throw new GeneralException(ErrorStatus.TRYON_API_INVALID_RESPONSE);
        }

        return VirtualTryOnResponse.builder()
                .resultUrl(apiResponse.getResultUrl())
                .maskedUrl(apiResponse.getMaskedUrl())
                .build();
    }

    private void validateFile(MultipartFile file, String name) {
        if (file == null || file.isEmpty()) {
            log.warn("[VirtualTryOn] {} 파일이 비어있습니다.", name);
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
    }

    private void addFilePart(MultipartBodyBuilder builder, String partName, MultipartFile file) {
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), partName);
        MediaType mediaType = resolveMediaType(file);
        builder.part(partName, file.getResource())
                .filename(filename)
                .contentType(mediaType);
    }

    private MediaType resolveMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TryOnApiResponse {
        @JsonProperty("result_url")
        private String resultUrl;
        @JsonProperty("masked_url")
        private String maskedUrl;

        public String getResultUrl() {
            return resultUrl;
        }

        public String getMaskedUrl() {
            return maskedUrl;
        }

        public boolean isValid() {
            return resultUrl != null && maskedUrl != null;
        }
    }
}

