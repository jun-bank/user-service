package com.jun_bank.user_service.domain.user.application.port.out;

import com.jun_bank.user_service.domain.user.domain.model.User;

/**
 * User Event Publisher Port (Output Port)
 * <p>
 * 사용자 도메인 이벤트 발행을 추상화한 인터페이스입니다.
 * Infrastructure Layer에서 Kafka Producer로 구현됩니다.
 *
 * <h3>발행 이벤트:</h3>
 * <ul>
 *   <li>user.created: 회원가입 완료</li>
 *   <li>user.updated: 프로필 수정</li>
 *   <li>user.deleted: 회원 탈퇴</li>
 * </ul>
 *
 * <h3>이벤트 수신 서비스:</h3>
 * <ul>
 *   <li>user.created: (현재 수신자 없음)</li>
 *   <li>user.deleted: Account, Card, Transfer 등</li>
 * </ul>
 */
public interface UserEventPublisherPort {

  /**
   * 사용자 생성 이벤트 발행
   * <p>
   * 토픽: user.created
   * </p>
   *
   * @param user 생성된 사용자
   */
  void publishUserCreated(User user);

  /**
   * 사용자 수정 이벤트 발행
   * <p>
   * 토픽: user.updated
   * </p>
   *
   * @param user 수정된 사용자
   */
  void publishUserUpdated(User user);

  /**
   * 사용자 삭제 이벤트 발행
   * <p>
   * 토픽: user.deleted
   * 수신 서비스에서 연관 데이터를 정리합니다.
   * </p>
   *
   * @param user 삭제된 사용자
   */
  void publishUserDeleted(User user);
}