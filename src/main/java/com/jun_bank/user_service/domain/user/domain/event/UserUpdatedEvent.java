package com.jun_bank.user_service.domain.user.domain.event;

import com.jun_bank.common_lib.event.DomainEvent;
import lombok.Getter;

/**
 * 사용자 수정 이벤트
 * <p>
 * 프로필 수정 완료 시 발행되는 도메인 이벤트입니다.
 *
 * <h3>토픽:</h3>
 * <p>user.updated</p>
 *
 * <h3>수신 서비스:</h3>
 * <p>현재 수신자 없음 (향후 확장용)</p>
 */
@Getter
public class UserUpdatedEvent extends DomainEvent {

  private final String userId;
  private final String name;
  private final String phoneNumber;

  public UserUpdatedEvent(String userId, String name, String phoneNumber) {
    super();
    this.userId = userId;
    this.name = name;
    this.phoneNumber = phoneNumber;
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
  public static UserUpdatedEvent of(String userId, String name, String phoneNumber) {
    return new UserUpdatedEvent(userId, name, phoneNumber);
  }
}