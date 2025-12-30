package com.jun_bank.user_service.domain.user.presentation.api.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 프로필 수정 요청 DTO
 */
public record UpdateUserRequest(

    @Size(min = 2, max = 50, message = "이름은 2~50자 사이여야 합니다")
    String name,

    @Pattern(
        regexp = "^01[016789]-\\d{3,4}-\\d{4}$",
        message = "유효한 전화번호 형식이 아닙니다 (예: 010-1234-5678)"
    )
    String phoneNumber

) {
  public boolean hasUpdates() {
    return name != null || phoneNumber != null;
  }
}