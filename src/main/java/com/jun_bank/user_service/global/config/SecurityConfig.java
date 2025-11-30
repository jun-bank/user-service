package com.jun_bank.user_service.global.config;

import com.jun_bank.user_service.global.security.HeaderAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 * - Gateway에서 전달받은 헤더(X-User-Id, X-User-Role)로 인증 객체 생성
 * - JWT 검증은 Gateway에서 처리, 내부 서비스는 헤더만 신뢰
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 비활성화 (Stateless REST API)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 미사용 (Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // Actuator 엔드포인트 허용
                        .requestMatchers("/actuator/**").permitAll()
                        // Swagger UI 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 내부 API (서비스 간 통신) 허용
                        .requestMatchers("/internal/**").permitAll()
                        // 나머지 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 헤더 기반 인증 필터 추가
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}