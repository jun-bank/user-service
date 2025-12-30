package com.jun_bank.user_service.domain.user.application.service;

import com.jun_bank.user_service.domain.user.application.dto.command.CreateUserCommand;
import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;
import com.jun_bank.user_service.domain.user.application.port.in.CreateUserUseCase;
import com.jun_bank.user_service.domain.user.application.port.out.AuthServicePort;
import com.jun_bank.user_service.domain.user.application.port.out.UserEventPublisherPort;
import com.jun_bank.user_service.domain.user.application.port.out.UserRepository;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.domain.model.User;
import com.jun_bank.user_service.domain.user.domain.model.vo.Email;
import com.jun_bank.user_service.domain.user.domain.model.vo.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 생성 서비스
 * <p>
 * {@link CreateUserUseCase}를 구현하여 회원가입 기능을 제공합니다.
 *
 * <h3>처리 흐름:</h3>
 * <ol>
 *   <li>이메일 중복 확인</li>
 *   <li>User 도메인 생성</li>
 *   <li>User 저장 (ID 생성)</li>
 *   <li>Auth Server에 인증 정보 생성 요청</li>
 *   <li>user.created 이벤트 발행</li>
 * </ol>
 *
 * <h3>트랜잭션:</h3>
 * <p>
 * User 저장과 Auth Server 호출이 하나의 트랜잭션으로 처리됩니다.
 * Auth Server 호출 실패 시 User 저장도 롤백됩니다.
 * </p>
 *
 * <h3>이벤트 발행:</h3>
 * <p>
 * 이벤트 발행은 트랜잭션 커밋 후 비동기로 처리됩니다.
 * 발행 실패 시에도 회원가입은 완료됩니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateUserService implements CreateUserUseCase {

  private final UserRepository userRepository;
  private final AuthServicePort authServicePort;
  private final UserEventPublisherPort userEventPublisher;

  @Override
  public UserResult createUser(CreateUserCommand command) {
    log.info("회원가입 요청: email={}", command.email());

    // 1. 이메일 중복 확인
    if (userRepository.existsByEmail(command.email())) {
      throw UserException.emailAlreadyExists(command.email());
    }

    // 2. User 도메인 생성
    User user = User.createBuilder()
        .email(Email.of(command.email()))
        .name(command.name())
        .phoneNumber(PhoneNumber.of(command.phoneNumber()))
        .birthDate(command.birthDate())
        .build();

    // 3. User 저장 (ID 생성)
    User savedUser = userRepository.save(user);
    String userId = savedUser.getUserId().value();

    log.info("사용자 저장 완료: userId={}", userId);

    // 4. Auth Server에 인증 정보 생성 요청
    try {
      authServicePort.createAuthUser(userId, command.email(), command.password());
      log.info("Auth Server 인증 정보 생성 완료: userId={}", userId);
    } catch (Exception e) {
      log.error("Auth Server 인증 정보 생성 실패: userId={}", userId, e);
      throw e;  // 트랜잭션 롤백
    }

    // 5. user.created 이벤트 발행 (비동기)
    try {
      userEventPublisher.publishUserCreated(savedUser);
      log.info("user.created 이벤트 발행 완료: userId={}", userId);
    } catch (Exception e) {
      // 이벤트 발행 실패는 로그만 남기고 진행
      log.warn("user.created 이벤트 발행 실패: userId={}", userId, e);
    }

    log.info("회원가입 완료: userId={}, email={}", userId, command.email());

    return UserResult.fromWithFullPhone(savedUser);
  }
}