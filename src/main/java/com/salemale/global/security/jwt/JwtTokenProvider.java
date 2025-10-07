package com.salemale.global.security.jwt; // JWT 토큰 생성/검증을 담당하는 컴포넌트

import io.jsonwebtoken.Claims; // 토큰의 클레임(payload) 추출용 타입
import io.jsonwebtoken.Jwts; // jjwt의 토큰 빌더/파서 진입점
import io.jsonwebtoken.security.Keys; // HMAC 키 생성 유틸리티
import org.springframework.beans.factory.annotation.Value; // application.yml 값 주입
import org.springframework.stereotype.Component; // 스프링 빈 등록을 위한 애노테이션

import java.nio.charset.StandardCharsets; // 문자열을 바이트로 변환 시 UTF-8 지정
import javax.crypto.SecretKey; // HMAC 서명/검증에 사용하는 대칭키 타입
import java.util.Date; // 발급/만료 시각 표현

@Component // 스프링 컨테이너가 관리하는 싱글톤 빈으로 등록
public class JwtTokenProvider {

    // HMAC 서명에 사용할 키. 대칭키 방식(서버에서만 알고 있는 비밀키)
    private final SecretKey signingKey;
    // 액세스/리프레시 토큰 유효기간(ms)
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessValidity,
            @Value("${jwt.refresh-token-validity}") long refreshValidity
    ) {
        // 최소 32바이트(256비트) 이상 확인 - 부족하면 애플리케이션 시작을 막아 보안을 강제
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes (256 bits) for HMAC-SHA");
        }
        // 비밀키 문자열을 HMAC-SHA 키 객체로 변환. 최소 256bit 권장
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessValidity;
        this.refreshTokenValidityMs = refreshValidity;
    }

    // 주체(subject, 보통 이메일 또는 사용자 ID)로 서명된 JWT 생성
    public String generateToken(String subject) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidityMs);
        // jjwt 0.12 API: 빌더에 subject/시간/서명키를 설정 후 compact()
        return Jwts.builder()
                .subject(subject) // 토큰의 주체 설정(인증된 사용자 식별자)
                .issuedAt(now) // 발급 시각(iat)
                .expiration(expiry) // 만료 시각(exp)
                .signWith(signingKey) // HMAC 서명
                .compact();
    }

    // 서명 검증 후 토큰의 subject를 반환. 유효하지 않으면 예외 발생
    public String getSubject(String token) {
        // 1) 서명 검증을 위한 키 등록
        // 2) 파서 빌드
        // 3) 서명 검증과 함께 클레임 파싱
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}


