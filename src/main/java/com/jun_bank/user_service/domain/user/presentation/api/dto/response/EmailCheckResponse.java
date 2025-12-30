package com.jun_bank.user_service.domain.user.presentation.api.dto.response;

/**
 * 이메일 중복 확인 응답 DTO
 */
public record EmailCheckResponse(
    boolean available,
    String email
) {}