package com.jun_bank.user_service.domain.user.presentation.internal.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 내부 서비스용 사용자 응답 DTO
 * <p>
 * 다른 마이크로서비스에서 필요한 사용자 정보를 반환합니다.
 * 전화번호가 마스킹되지 않은 원본으로 제공됩니다.
 */
public record UserInternalResponse(
    String userId,
    String email,
    String name,
    String phoneNumber,
    LocalDate birthDate,
    String status,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}