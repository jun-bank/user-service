package com.jun_bank.user_service.domain.user.application.service;

import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;
import com.jun_bank.user_service.domain.user.application.port.in.GetUserUseCase;
import com.jun_bank.user_service.domain.user.application.port.out.UserRepository;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 조회 서비스
 * <p>
 * {@link GetUserUseCase}를 구현하여 사용자 조회 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserService implements GetUserUseCase {

  private final UserRepository userRepository;

  @Override
  public UserResult getUserById(String userId) {
    log.debug("사용자 조회 요청: userId={}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserException.userNotFound(userId));

    return UserResult.from(user);
  }

  @Override
  public UserResult getUserByIdForOwner(String userId) {
    log.debug("내 프로필 조회 요청: userId={}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserException.userNotFound(userId));

    // 본인 조회 시 전화번호 원본 제공
    return UserResult.fromWithFullPhone(user);
  }

  @Override
  public UserResult getUserByEmail(String email) {
    log.debug("이메일로 사용자 조회 요청: email={}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> UserException.userNotFound("email=" + email));

    return UserResult.from(user);
  }

  @Override
  public UserResult getUserByEmailForOwner(String email) {
    log.debug("이메일로 사용자 조회 요청 (본인용): email={}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> UserException.userNotFound("email=" + email));

    // 본인 조회 시 전화번호 원본 제공
    return UserResult.fromWithFullPhone(user);
  }

  @Override
  public boolean existsByEmail(String email) {
    log.debug("이메일 존재 여부 확인: email={}", email);

    return userRepository.existsByEmail(email);
  }
}