package com.jun_bank.user_service.domain.user.presentation.internal.mapper;

import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;
import com.jun_bank.user_service.domain.user.domain.model.UserStatus;
import com.jun_bank.user_service.domain.user.presentation.internal.dto.response.UserInternalResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Internal API 계층 Mapper
 */
@Component
public class UserInternalMapper {

  public UserInternalResponse toInternalResponse(UserResult result) {
    return new UserInternalResponse(
        result.userId(),
        result.email(),
        result.name(),
        result.phoneNumber(),
        result.birthDate(),
        result.status().name(),
        UserStatus.ACTIVE.equals(result.status()),
        result.createdAt(),
        result.updatedAt()
    );
  }

  public List<UserInternalResponse> toInternalResponses(List<UserResult> results) {
    return results.stream()
        .map(this::toInternalResponse)
        .toList();
  }
}