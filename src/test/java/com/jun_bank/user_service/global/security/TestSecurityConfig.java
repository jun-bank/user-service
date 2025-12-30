package com.jun_bank.user_service.global.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 테스트용 Security 설정
 * <p>
 * {@code @WebMvcTest}에서 사용하는 간소화된 Security 설정입니다.
 * CSRF 비활성화, 세션 Stateless 등 테스트에 필요한 최소한의 설정만 포함합니다.
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * @WebMvcTest(UserController.class)
 * @Import(TestSecurityConfig.class)
 * class UserControllerTest {
 *     // 테스트 코드
 * }
 * }</pre>
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
public class TestSecurityConfig {

  @Bean
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // 비인증 허용 엔드포인트
            .requestMatchers("/api/v1/users", "/api/v1/users/check-email").permitAll()
            // 내부 API는 허용 (실제 환경에서는 IP 제한 등 적용)
            .requestMatchers("/internal/**").permitAll()
            // 나머지는 인증 필요
            .anyRequest().authenticated()
        );

    return http.build();
  }
}
