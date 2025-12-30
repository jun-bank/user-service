package com.jun_bank.user_service.domain.user.presentation.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 응답 DTO
 */
public record UserResponse(
    String userId,
    String email,
    String name,
    String phoneNumber,
    LocalDate birthDate,
    String status,
    LocalDateTime createdAt
) {}