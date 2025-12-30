package com.jun_bank.user_service.domain.user.domain.event;

import com.jun_bank.common_lib.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 사용자 생성 이벤트
 * <p>
 * 회원가입 완료 시 발행되는 도메인 이벤트입니다.
 *
 * <h3>토픽:</h3>
 * <p>user.created</p>
 *
 * <h3>수신 서비스:</h3>
 * <p>현재 수신자 없음 (향후 확장용)</p>
 */
@Getter
public class UserCreatedEvent extends DomainEvent {

  private final String userId;
  private final String email;
  private final String name;
  private final String phoneNumber;
  private final LocalDate birthDate;

  public UserCreatedEvent(String userId, String email, String name,
      String phoneNumber, LocalDate birthDate) {
    super();
    this.userId = userId;
    this.email = email;
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.birthDate = birthDate;
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
  public static UserCreatedEvent of(String userId, String email, String name,
      String phoneNumber, LocalDate birthDate) {
    return new UserCreatedEvent(userId, email, name, phoneNumber, birthDate);
  }
}