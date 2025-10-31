package com.salemale.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI SalemaleAPI() {
        Info info = new Info()
                .title("우리 동네 실시간 경매 API")
                .description("""
                        우리 동네 실시간 경매 플랫폼 API 명세서
                        
                        ## OAuth2 로그인 안내
                        - 카카오 로그인: `GET /oauth2/authorization/kakao`
                        - 네이버 로그인: `GET /oauth2/authorization/naver`
                        - 로그인 성공 후: `{FRONTEND_URL}/auth/callback#token={JWT_TOKEN}`
                        - **토큰은 URL fragment로 전달됩니다** (프론트엔드에서 `window.location.hash`로 추출)
                        - 자세한 내용은 `/auth/oauth2/login` 엔드포인트 참조
                        """)
                .version("1.0.0");

        String jwtSchemeName = "JWT TOKEN";

        // 헤더에 인증정보 포함
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        // SecuritySchemes 등록
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}