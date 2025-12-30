package com.jun_bank.user_service.domain.user.domain.event;

import com.jun_bank.common_lib.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 삭제 이벤트
 * <p>
 * 회원 탈퇴 완료 시 발행되는 도메인 이벤트입니다.
 *
 * <h3>토픽:</h3>
 * <p>user.deleted</p>
 *
 * <h3>수신 서비스:</h3>
 * <ul>
 *   <li>Account Service: 계좌 비활성화</li>
 *   <li>Card Service: 카드 비활성화</li>
 *   <li>Transfer Service: 진행 중 이체 처리</li>
 * </ul>
 */
@Getter
public class UserDeletedEvent extends DomainEvent {

  private final String userId;
  private final String email;
  private final LocalDateTime deletedAt;
  private final String deletedBy;

  public UserDeletedEvent(String userId, String email,
      LocalDateTime deletedAt, String deletedBy) {
    super();
    this.userId = userId;
    this.email = email;
    this.deletedAt = deletedAt;
    this.deletedBy = deletedBy;
  }

  @Override
  public String getAggregateId() {
    return this.userId;
  }

  @Override
  public String getAggregateType() {
    return "USER";
  }

  /**
   * 팩토리 메서드
   */
  public static UserDeletedEvent of(String userId, String email,
      LocalDateTime deletedAt, String deletedBy) {
    return new UserDeletedEvent(userId, email, deletedAt, deletedBy);
  }
}