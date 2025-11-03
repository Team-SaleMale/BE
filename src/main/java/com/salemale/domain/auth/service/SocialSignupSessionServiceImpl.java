package com.salemale.domain.auth.service;

import com.salemale.global.common.enums.LoginType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialSignupSessionServiceImpl implements SocialSignupSessionService {

    private final StringRedisTemplate redis;

    private static final String KEY = "social:signup:"; // social:signup:{token}
    private static final Duration TTL = Duration.ofMinutes(30);

    @Override
    public String create(LoginType provider, String providerUserId, String email) {
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().getBytes());
        String payload = provider.name() + "|" + (providerUserId == null ? "" : providerUserId) + "|" + (email == null ? "" : email);
        ValueOperations<String, String> ops = redis.opsForValue();
        ops.set(KEY + token, payload, TTL);
        return token;
    }

    @Override
    public SocialSession get(String token) {
        String payload = redis.opsForValue().get(KEY + token);
        if (payload == null) return null;
        String[] parts = payload.split("\\|", -1);
        LoginType provider = LoginType.valueOf(parts[0]);
        String providerUserId = parts.length > 1 ? parts[1] : null;
        String email = parts.length > 2 ? parts[2] : null;
        return new SocialSession(provider, providerUserId, email);
    }

    @Override
    public void consume(String token) {
        redis.delete(KEY + token);
    }
}


