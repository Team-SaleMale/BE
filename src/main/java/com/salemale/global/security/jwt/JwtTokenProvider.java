package com.salemale.global.security.jwt; // JWT 토큰 생성/검증을 담당하는 컴포넌트

import io.jsonwebtoken.Claims; // 토큰의 클레임(payload) 추출용 타입
import io.jsonwebtoken.Jwts; // jjwt의 토큰 빌더/파서 진입점
import io.jsonwebtoken.security.Keys; // HMAC 키 생성 유틸리티
import org.springframework.beans.factory.annotation.Value; // application.yml 값 주입
import org.springframework.stereotype.Component; // 스프링 빈 등록을 위한 애노테이션

import java.nio.charset.StandardCharsets; // 문자열을 바이트로 변환 시 UTF-8 지정
import javax.crypto.SecretKey; // HMAC 서명/검증에 사용하는 대칭키 타입
import java.util.Date; // 발급/만료 시각 표현

/**
 * JwtTokenProvider: JWT 액세스 토큰을 생성하고 검증하는 컴포넌트입니다.
 *
 * - HMAC-SHA 대칭키 방식으로 서명/검증을 수행합니다.
 * - 토큰의 주체(subject)에 사용자 ID(UID)를 담습니다 (소셜/로컬 계정 통합 식별).
 * - 만료 시간을 설정하여 토큰의 유효기간을 제한합니다.
 *
 * 주의사항:
 * - jwt.secret은 최소 32바이트(256비트) 이상이어야 합니다.
 * - 운영 환경에서는 환경변수로 비밀키를 관리하세요(코드에 하드코딩 금지).
 */
@Component // 스프링 컨테이너가 관리하는 싱글톤 빈으로 등록
public class JwtTokenProvider {

    // HMAC 서명에 사용할 대칭키: 서버에서만 알고 있는 비밀키로, 토큰의 무결성을 보장합니다.
    private final SecretKey signingKey;

    // 액세스 토큰 유효기간(밀리초): 짧게 설정하여 보안을 강화합니다(예: 1시간).
    private final long accessTokenValidityMs;

    // 리프레시 토큰 유효기간(밀리초): 향후 리프레시 토큰 기능 구현 시 사용 예정입니다.
    // 현재는 액세스 토큰만 발급하고 있으므로 사용되지 않지만, 설정값을 미리 주입받아 준비해 둡니다.
    @SuppressWarnings("unused") // 경고 억제: 리프레시 토큰 기능 구현 전까지 임시로 유지
    private final long refreshTokenValidityMs;

    /**
     * 생성자 주입: application.yml에서 JWT 설정값을 읽어와 초기화합니다.
     *
     * @param secret JWT 서명에 사용할 비밀키 문자열(최소 32바이트 필요)
     * @param accessValidity 액세스 토큰 유효기간(밀리초)
     * @param refreshValidity 리프레시 토큰 유효기간(밀리초, 현재 미사용)
     * @throws IllegalArgumentException 비밀키가 32바이트 미만일 경우 발생
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret, // application.yml의 jwt.secret 값 주입
            @Value("${jwt.access-token-validity}") long accessValidity, // 액세스 토큰 유효기간 주입
            @Value("${jwt.refresh-token-validity}") long refreshValidity // 리프레시 토큰 유효기간 주입(향후 사용)
    ) {
        // 1) 보안 검증: 비밀키가 최소 32바이트(256비트) 이상인지 확인합니다.
        //    - HMAC-SHA256을 안전하게 사용하려면 키 길이가 충분해야 합니다.
        //    - 부족하면 애플리케이션 시작 자체를 막아 보안 취약점을 사전 차단합니다.
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes (256 bits) for HMAC-SHA");
        }

        // 2) 비밀키 문자열을 SecretKey 객체로 변환: HMAC 서명에 사용할 키를 생성합니다.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // 3) 토큰 유효기간 설정: 주입받은 값을 필드에 저장합니다.
        this.accessTokenValidityMs = accessValidity; // 액세스 토큰 유효기간
        this.refreshTokenValidityMs = refreshValidity; // 리프레시 토큰 유효기간(향후 사용 예정)
    }

    /**
     * JWT 액세스 토큰 생성: 사용자 ID를 담은 서명된 토큰을 발급합니다.
     *
     * @param subject 토큰의 주체(사용자 ID를 문자열로 변환한 값, 예: "123")
     * @return 서명된 JWT 문자열(헤더.페이로드.서명 형태)
     */
    public String generateToken(String subject) {
        // 1) 현재 시각을 기준으로 발급 시각과 만료 시각을 계산합니다.
        Date now = new Date(); // 현재 시각(발급 시각으로 사용)
        Date expiry = new Date(now.getTime() + accessTokenValidityMs); // 현재 시각 + 유효기간 = 만료 시각

        // 2) JWT 빌더를 사용하여 토큰을 생성합니다.
        //    - subject: 토큰의 주체(사용자 ID)를 설정합니다.
        //    - issuedAt: 발급 시각(iat 클레임)을 설정합니다.
        //    - expiration: 만료 시각(exp 클레임)을 설정합니다.
        //    - signWith: HMAC 알고리즘으로 서명하여 무결성을 보장합니다.
        //    - compact: 최종적으로 "헤더.페이로드.서명" 형태의 문자열로 변환합니다.
        return Jwts.builder()
                .subject(subject) // 토큰의 주체 설정(사용자 ID)
                .issuedAt(now) // 발급 시각(iat)
                .expiration(expiry) // 만료 시각(exp)
                .signWith(signingKey) // HMAC 서명
                .compact(); // JWT 문자열 생성
    }

    /**
     * JWT 토큰 검증 및 주체 추출: 서명을 확인하고 토큰에서 사용자 ID를 가져옵니다.
     *
     * @param token 검증할 JWT 문자열
     * @return 토큰의 주체(subject, 사용자 ID를 문자열로 표현, 예: "123")
     * @throws io.jsonwebtoken.JwtException 토큰이 유효하지 않거나(서명 불일치/만료 등) 파싱 실패 시 발생
     */
    public String getSubject(String token) {
        // 1) JWT 파서를 생성하여 토큰을 검증하고 클레임을 추출합니다.
        //    - verifyWith(signingKey): 서명 검증을 위해 비밀키를 등록합니다.
        //    - build(): 파서를 빌드합니다.
        //    - parseSignedClaims(token): 토큰을 파싱하면서 서명을 검증합니다.
        //      · 서명이 올바르지 않으면 SignatureException 발생
        //      · 만료된 토큰이면 ExpiredJwtException 발생
        //      · 형식이 잘못되면 MalformedJwtException 발생
        //    - getPayload(): 검증이 완료된 클레임(payload)을 가져옵니다.
        Claims claims = Jwts.parser()
                .verifyWith(signingKey) // 서명 검증을 위한 키 등록
                .build() // 파서 빌드
                .parseSignedClaims(token) // 서명 검증과 함께 클레임 파싱
                .getPayload(); // 클레임 추출

        // 2) 클레임에서 subject(사용자 ID)를 반환합니다.
        return claims.getSubject();
    }
}


