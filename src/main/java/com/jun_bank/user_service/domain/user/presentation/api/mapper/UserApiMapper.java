package com.jun_bank.user_service.domain.user.presentation.api.mapper;

import com.jun_bank.user_service.domain.user.application.dto.command.CreateUserCommand;
import com.jun_bank.user_service.domain.user.application.dto.command.UpdateUserCommand;
import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;
import com.jun_bank.user_service.domain.user.presentation.api.dto.request.CreateUserRequest;
import com.jun_bank.user_service.domain.user.presentation.api.dto.request.UpdateUserRequest;
import com.jun_bank.user_service.domain.user.presentation.api.dto.response.UserResponse;
import org.springframework.stereotype.Component;

/**
 * API 계층 Mapper
 * <p>
 * Request ↔ Command, Result ↔ Response 변환을 담당합니다.
 */
@Component
public class UserApiMapper {

  // ========================================
  // Request → Command
  // ========================================

  public CreateUserCommand toCommand(CreateUserRequest request) {
    return new CreateUserCommand(
        request.email(),
        request.password(),
        request.name(),
        request.phoneNumber(),
        request.birthDate()
    );
  }

  public UpdateUserCommand toCommand(UpdateUserRequest request) {
    return new UpdateUserCommand(
        request.name(),
        request.phoneNumber()
    );
  }

  // ========================================
  // Result → Response
  // ========================================

  public UserResponse toResponse(UserResult result) {
    return new UserResponse(
        result.userId(),
        result.email(),
        result.name(),
        result.phoneNumber(),
        result.birthDate(),
        result.status().name(),
        result.createdAt()
    );
  }
}