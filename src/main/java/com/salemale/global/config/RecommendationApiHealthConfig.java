package com.salemale.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationApiHealthConfig implements CommandLineRunner {

    private final WebClient webClient;

    @Value("${recommendation.api.url:http://localhost:8000}")
    private String recommendationApiUrl;

    @Override
    public void run(String... args) throws Exception {
        try {
            // AI 서버 헬스체크 (루트 경로 또는 /health 엔드포인트 시도)
            // FastAPI는 보통 /docs, /health, / 등이 있음
            String[] healthUrls = {
                    recommendationApiUrl + "/health",
                    recommendationApiUrl + "/docs",
                    recommendationApiUrl + "/"
            };
            
            boolean connected = false;
            for (String healthUrl : healthUrls) {
                try {
                    String response = webClient
                            .get()
                            .uri(healthUrl)
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(3))
                            .block();
                    
                    if (response != null) {
                        connected = true;
                        log.info("✅ AI 서버 연결 성공: {} (엔드포인트: {})", recommendationApiUrl, healthUrl);
                        log.info("✅ AI 서버 연결 상태: 정상 (헬스체크 통과)");
                        break;
                    }
                } catch (Exception e) {
                    // 다음 URL 시도
                    continue;
                }
            }
            
            if (!connected) {
                log.warn("⚠️  AI 서버 연결 실패: {} (모든 엔드포인트 시도 실패)", recommendationApiUrl);
                log.warn("⚠️  AI 서버가 연결되지 않았지만 애플리케이션은 계속 실행됩니다.");
            }
        } catch (Exception e) {
            log.error("❌ AI 서버 연결 실패: {}", recommendationApiUrl, e);
            log.warn("⚠️  AI 서버가 연결되지 않았지만 애플리케이션은 계속 실행됩니다.");
        }
    }
}

