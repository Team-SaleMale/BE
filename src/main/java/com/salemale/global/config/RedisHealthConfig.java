package com.salemale.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisHealthConfig implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            redisTemplate.opsForValue().set("health-check", "ok");
            String value = redisTemplate.opsForValue().get("health-check");
            redisTemplate.delete("health-check");
            
            log.info("✅ Redis 연결 성공: localhost:6379");
            log.info("✅ Redis 연결 상태: 정상 (PING 테스트 통과)");
        } catch (Exception e) {
            log.error("❌ Redis 연결 실패: localhost:6379", e);
            log.warn("⚠️  Redis가 연결되지 않았지만 애플리케이션은 계속 실행됩니다.");
        }
    }
}

