package com.jun_bank.user_service.domain.user.presentation.internal.dto.response;

/**
 * 사용자 존재 확인 응답 DTO
 */
public record UserExistsResponse(
    boolean exists,
    boolean active,
    String userId
) {}