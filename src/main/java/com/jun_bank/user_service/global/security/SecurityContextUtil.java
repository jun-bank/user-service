package com.jun_bank.user_service.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SecurityContext에서 현재 사용자 정보 조회 유틸리티
 */
@Component
public class SecurityContextUtil {

    /**
     * 현재 인증된 사용자 ID 조회
     * @return 사용자 ID (인증 정보 없으면 null)
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }

        return null;
    }

    /**
     * 현재 인증된 사용자 역할 조회
     * @return 역할 (인증 정보 없으면 null)
     */
    public String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getRole();
        }

        return null;
    }

    /**
     * 현재 인증된 사용자 이메일 조회
     * @return 이메일 (인증 정보 없으면 null)
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getEmail();
        }

        return null;
    }

    /**
     * 현재 인증된 사용자 Principal 조회
     * @return UserPrincipal (인증 정보 없으면 null)
     */
    public UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }

        return null;
    }

    /**
     * 현재 사용자가 특정 역할을 가지고 있는지 확인
     * @param role 확인할 역할
     * @return 역할 보유 여부
     */
    public boolean hasRole(String role) {
        String currentRole = getCurrentUserRole();
        return role != null && role.equalsIgnoreCase(currentRole);
    }

    /**
     * 현재 사용자가 관리자인지 확인
     * @return 관리자 여부
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}