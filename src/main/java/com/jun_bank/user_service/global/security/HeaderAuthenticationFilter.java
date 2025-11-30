package com.jun_bank.user_service.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Gateway에서 전달받은 헤더로 인증 객체 생성
 *
 * Gateway가 JWT를 검증한 후 다음 헤더를 전달:
 * - X-User-Id: 사용자 ID
 * - X-User-Role: 사용자 역할 (USER, ADMIN 등)
 * - X-User-Email: 사용자 이메일 (선택)
 */
@Slf4j
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_USER_EMAIL = "X-User-Email";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String userId = request.getHeader(HEADER_USER_ID);
            String role = request.getHeader(HEADER_USER_ROLE);
            String email = request.getHeader(HEADER_USER_EMAIL);

            if (StringUtils.hasText(userId) && StringUtils.hasText(role)) {
                UserPrincipal principal = new UserPrincipal(userId, role, email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("인증 정보 설정 완료 - userId: {}, role: {}", userId, role);
            }
        } catch (Exception e) {
            log.error("인증 정보 설정 실패", e);
        }

        filterChain.doFilter(request, response);
    }
}