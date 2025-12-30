package com.jun_bank.user_service.domain.user.application.service;

import com.jun_bank.user_service.domain.user.application.port.in.DeleteUserUseCase;
import com.jun_bank.user_service.domain.user.application.port.out.AuthServicePort;
import com.jun_bank.user_service.domain.user.application.port.out.UserEventPublisherPort;
import com.jun_bank.user_service.domain.user.application.port.out.UserRepository;
import com.jun_bank.user_service.domain.user.domain.exception.UserErrorCode;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 삭제 서비스
 * <p>
 * {@link DeleteUserUseCase}를 구현하여 회원 탈퇴 기능을 제공합니다.
 *
 * <h3>처리 흐름:</h3>
 * <ol>
 *   <li>사용자 조회</li>
 *   <li>이미 탈퇴 여부 확인</li>
 *   <li>탈퇴 처리 (도메인 메서드 호출, 이전 상태 백업)</li>
 *   <li>User 저장 (Soft Delete)</li>
 *   <li>Auth Server에 인증 정보 삭제 요청</li>
 *   <li>실패 시: User 상태 롤백 → 저장 → 예외</li>
 *   <li>성공 시: user.deleted 이벤트 발행</li>
 * </ol>
 *
 * <h3>Soft Delete:</h3>
 * <ul>
 *   <li>status → DELETED</li>
 *   <li>isDeleted → true</li>
 *   <li>deletedAt → 현재 시간</li>
 *   <li>deletedBy → 요청자 ID</li>
 * </ul>
 *
 * <h3>롤백 처리:</h3>
 * <p>
 * Auth Server 호출 실패 시 User 상태를 이전 상태로 롤백합니다.
 * User.cancelWithdrawal()을 호출하여 상태, isDeleted, deletedAt, deletedBy를 복구합니다.
 * </p>
 *
 * <h3>이벤트 발행:</h3>
 * <p>
 * 이벤트 발행 실패 시 EventRetryService를 통해 재시도됩니다.
 * 이벤트 발행 실패로 탈퇴가 취소되지는 않습니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteUserService implements DeleteUserUseCase {

  private final UserRepository userRepository;
  private final AuthServicePort authServicePort;
  private final UserEventPublisherPort userEventPublisher;

  @Override
  public void deleteUser(String userId, String requesterId) {
    log.info("회원 탈퇴 요청: userId={}, requesterId={}", userId, requesterId);

    // 1. 사용자 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserException.userNotFound(userId));

    // 2. 이미 탈퇴한 경우 예외
    if (user.isDeleted()) {
      throw new UserException(UserErrorCode.USER_ALREADY_DELETED, "userId=" + userId);
    }

    // 3. 탈퇴 처리 (도메인 메서드, 이전 상태 백업됨)
    user.withdraw(requesterId);

    // 4. User 저장 (Soft Delete 반영)
    User deletedUser = userRepository.save(user);
    log.info("사용자 Soft Delete 완료: userId={}", userId);

    // 5. Auth Server에 인증 정보 삭제 요청
    try {
      authServicePort.deleteAuthUser(userId);
      log.info("Auth Server 인증 정보 삭제 완료: userId={}", userId);

      // 성공 시 이전 상태 정리
      deletedUser.clearPreviousStatus();

    } catch (Exception e) {
      log.error("Auth Server 인증 정보 삭제 실패, 롤백 시작: userId={}, error={}",
          userId, e.getMessage());

      // 6. 롤백: User 상태 복구
      rollbackUserDeletion(deletedUser);

      // 예외 다시 던지기
      throw UserException.authServerError(
          "Auth Server 인증 정보 삭제 실패로 탈퇴가 취소되었습니다. userId=" + userId, e);
    }

    // 7. user.deleted 이벤트 발행 (실패해도 탈퇴는 완료)
    publishDeleteEventSafely(deletedUser);

    log.info("회원 탈퇴 완료: userId={}", userId);
  }

  /**
   * User 탈퇴 롤백
   * <p>
   * Auth Server 호출 실패 시 User 상태를 복구합니다.
   * </p>
   */
  private void rollbackUserDeletion(User user) {
    try {
      user.cancelWithdrawal();
      userRepository.save(user);
      log.info("사용자 탈퇴 롤백 완료: userId={}", user.getUserId().value());
    } catch (Exception rollbackEx) {
      // 롤백 실패 시 심각한 상황 - 로그만 남김
      log.error("사용자 탈퇴 롤백 실패! 수동 처리 필요: userId={}, error={}",
          user.getUserId().value(), rollbackEx.getMessage(), rollbackEx);
    }
  }

  /**
   * 삭제 이벤트 발행 (안전하게)
   * <p>
   * 실패 시 UserEventProducer 내부에서 EventRetryService를 통해 재시도 큐에 추가.
   * </p>
   */
  private void publishDeleteEventSafely(User deletedUser) {
    try {
      userEventPublisher.publishUserDeleted(deletedUser);
      log.info("user.deleted 이벤트 발행 완료: userId={}", deletedUser.getUserId().value());
    } catch (Exception e) {
      // 이벤트 발행 실패는 로그만 남김
      // UserEventProducer에서 EventRetryService에 이미 추가됨
      log.warn("user.deleted 이벤트 발행 실패 (재시도 예약됨): userId={}, error={}",
          deletedUser.getUserId().value(), e.getMessage());
    }
  }
}