package com.jun_bank.user_service.domain.user.application.service;

import com.jun_bank.user_service.domain.user.application.dto.command.UpdateUserCommand;
import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;
import com.jun_bank.user_service.domain.user.application.port.in.UpdateUserUseCase;
import com.jun_bank.user_service.domain.user.application.port.out.UserRepository;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.domain.model.User;
import com.jun_bank.user_service.domain.user.domain.model.vo.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 수정 서비스
 * <p>
 * {@link UpdateUserUseCase}를 구현하여 프로필 수정 기능을 제공합니다.
 *
 * <h3>수정 가능 필드:</h3>
 * <ul>
 *   <li>name: 이름</li>
 *   <li>phoneNumber: 전화번호</li>
 * </ul>
 *
 * <h3>검증:</h3>
 * <ul>
 *   <li>수정 가능 상태 검증은 도메인(User.updateProfile)에서 처리</li>
 *   <li>필드 유효성 검증은 Command에서 처리</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserService implements UpdateUserUseCase {

  private final UserRepository userRepository;

  @Override
  public UserResult updateUser(String userId, UpdateUserCommand command) {
    log.info("사용자 수정 요청: userId={}", userId);

    // 1. 사용자 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserException.userNotFound(userId));

    // 2. 프로필 수정 (도메인에서 상태 검증)
    user.updateProfile(
        command.name(),
        PhoneNumber.of(command.phoneNumber())
    );

    // 3. 저장 (더티체킹)
    User savedUser = userRepository.save(user);

    log.info("사용자 수정 완료: userId={}", userId);

    return UserResult.fromWithFullPhone(savedUser);
  }
}