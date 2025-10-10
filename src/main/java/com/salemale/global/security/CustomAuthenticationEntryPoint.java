package com.salemale.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salemale.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 사용자가 보호된 API 접근 시 401 응답을 ApiResponse 형식으로 반환
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("인증되지 않은 접근: {}", request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // ApiResponse 형식으로 통일
        ApiResponse<Object> errorResponse = ApiResponse.onFailure(
                "COMMON401",
                "인증이 필요합니다.",
                null
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}