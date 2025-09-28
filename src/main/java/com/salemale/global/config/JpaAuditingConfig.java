package com.salemale.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }

    /**
     * 현재 사용자를 감지하는 AuditorAware 구현체
     * Spring Security가 설정되면 SecurityContext에서 사용자 정보를 가져옴
     */
    public static class SpringSecurityAuditorAware implements AuditorAware<String> {

        @Override
        public Optional<String> getCurrentAuditor() {
            // TODO: Spring Security 설정 후 SecurityContext에서 사용자 ID 반환
            // 현재는 시스템 사용자로 설정
            return Optional.of("system");
        }
    }
}
