package com.jun_bank.user_service.domain.user.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("도메인 이벤트 테스트")
class DomainEventTest {

  // ========================================
  // UserCreatedEvent 테스트
  // ========================================

  @Nested
  @DisplayName("UserCreatedEvent")
  class UserCreatedEventTest {

    @Test
    @DisplayName("of 팩토리 메서드로 이벤트 생성")
    void of_CreatesEvent() {
      // given
      String userId = "USR-12345678";
      String email = "test@example.com";
      String name = "테스트";
      String phoneNumber = "010-1234-5678";
      LocalDate birthDate = LocalDate.of(1990, 5, 15);

      // when
      UserCreatedEvent event = UserCreatedEvent.of(userId, email, name, phoneNumber, birthDate);

      // then
      assertThat(event.getUserId()).isEqualTo(userId);
      assertThat(event.getEmail()).isEqualTo(email);
      assertThat(event.getName()).isEqualTo(name);
      assertThat(event.getPhoneNumber()).isEqualTo(phoneNumber);
      assertThat(event.getBirthDate()).isEqualTo(birthDate);
    }

    @Test
    @DisplayName("aggregateId는 userId다")
    void getAggregateId_ReturnsUserId() {
      // given
      String userId = "USR-12345678";
      UserCreatedEvent event = UserCreatedEvent.of(userId, "test@example.com", "테스트", "010-1234-5678", LocalDate.now());

      // when & then
      assertThat(event.getAggregateId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("aggregateType은 USER다")
    void getAggregateType_ReturnsUser() {
      // given
      UserCreatedEvent event = UserCreatedEvent.of("USR-12345678", "test@example.com", "테스트", "010-1234-5678", LocalDate.now());

      // when & then
      assertThat(event.getAggregateType()).isEqualTo("USER");
    }

    @Test
    @DisplayName("DomainEvent 필드가 자동 생성된다")
    void domainEventFields_AreGenerated() {
      // when
      UserCreatedEvent event = UserCreatedEvent.of("USR-12345678", "test@example.com", "테스트", "010-1234-5678", LocalDate.now());

      // then
      assertThat(event.getEventId()).isNotNull();
      assertThat(event.getOccurredAt()).isNotNull();
      assertThat(event.getEventType()).isEqualTo("UserCreatedEvent");
    }
  }

  // ========================================
  // UserUpdatedEvent 테스트
  // ========================================

  @Nested
  @DisplayName("UserUpdatedEvent")
  class UserUpdatedEventTest {

    @Test
    @DisplayName("of 팩토리 메서드로 이벤트 생성")
    void of_CreatesEvent() {
      // given
      String userId = "USR-12345678";
      String name = "변경된이름";
      String phoneNumber = "010-9999-8888";

      // when
      UserUpdatedEvent event = UserUpdatedEvent.of(userId, name, phoneNumber);

      // then
      assertThat(event.getUserId()).isEqualTo(userId);
      assertThat(event.getName()).isEqualTo(name);
      assertThat(event.getPhoneNumber()).isEqualTo(phoneNumber);
    }

    @Test
    @DisplayName("aggregateId는 userId다")
    void getAggregateId_ReturnsUserId() {
      // given
      String userId = "USR-12345678";
      UserUpdatedEvent event = UserUpdatedEvent.of(userId, "이름", "010-1234-5678");

      // when & then
      assertThat(event.getAggregateId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("aggregateType은 USER다")
    void getAggregateType_ReturnsUser() {
      // given
      UserUpdatedEvent event = UserUpdatedEvent.of("USR-12345678", "이름", "010-1234-5678");

      // when & then
      assertThat(event.getAggregateType()).isEqualTo("USER");
    }

    @Test
    @DisplayName("DomainEvent 필드가 자동 생성된다")
    void domainEventFields_AreGenerated() {
      // when
      UserUpdatedEvent event = UserUpdatedEvent.of("USR-12345678", "이름", "010-1234-5678");

      // then
      assertThat(event.getEventId()).isNotNull();
      assertThat(event.getOccurredAt()).isNotNull();
      assertThat(event.getEventType()).isEqualTo("UserUpdatedEvent");
    }
  }

  // ========================================
  // UserDeletedEvent 테스트
  // ========================================

  @Nested
  @DisplayName("UserDeletedEvent")
  class UserDeletedEventTest {

    @Test
    @DisplayName("of 팩토리 메서드로 이벤트 생성")
    void of_CreatesEvent() {
      // given
      String userId = "USR-12345678";
      String email = "test@example.com";
      LocalDateTime deletedAt = LocalDateTime.now();
      String deletedBy = "admin";

      // when
      UserDeletedEvent event = UserDeletedEvent.of(userId, email, deletedAt, deletedBy);

      // then
      assertThat(event.getUserId()).isEqualTo(userId);
      assertThat(event.getEmail()).isEqualTo(email);
      assertThat(event.getDeletedAt()).isEqualTo(deletedAt);
      assertThat(event.getDeletedBy()).isEqualTo(deletedBy);
    }

    @Test
    @DisplayName("aggregateId는 userId다")
    void getAggregateId_ReturnsUserId() {
      // given
      String userId = "USR-12345678";
      UserDeletedEvent event = UserDeletedEvent.of(userId, "test@example.com", LocalDateTime.now(), "admin");

      // when & then
      assertThat(event.getAggregateId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("aggregateType은 USER다")
    void getAggregateType_ReturnsUser() {
      // given
      UserDeletedEvent event = UserDeletedEvent.of("USR-12345678", "test@example.com", LocalDateTime.now(), "admin");

      // when & then
      assertThat(event.getAggregateType()).isEqualTo("USER");
    }

    @Test
    @DisplayName("DomainEvent 필드가 자동 생성된다")
    void domainEventFields_AreGenerated() {
      // when
      UserDeletedEvent event = UserDeletedEvent.of("USR-12345678", "test@example.com", LocalDateTime.now(), "admin");

      // then
      assertThat(event.getEventId()).isNotNull();
      assertThat(event.getOccurredAt()).isNotNull();
      assertThat(event.getEventType()).isEqualTo("UserDeletedEvent");
    }

    @Test
    @DisplayName("deletedBy가 null이어도 생성 가능")
    void of_NullDeletedBy_Allowed() {
      // when
      UserDeletedEvent event = UserDeletedEvent.of("USR-12345678", "test@example.com", LocalDateTime.now(), null);

      // then
      assertThat(event.getDeletedBy()).isNull();
    }

    @Test
    @DisplayName("deletedAt이 null이어도 생성 가능")
    void of_NullDeletedAt_Allowed() {
      // when
      UserDeletedEvent event = UserDeletedEvent.of("USR-12345678", "test@example.com", null, "admin");

      // then
      assertThat(event.getDeletedAt()).isNull();
    }
  }

  // ========================================
  // 공통 테스트
  // ========================================

  @Nested
  @DisplayName("공통 이벤트 특성")
  class CommonEventTest {

    @Test
    @DisplayName("각 이벤트는 고유한 eventId를 가진다")
    void differentEvents_HaveDifferentEventIds() {
      // given
      UserCreatedEvent created = UserCreatedEvent.of("USR-1", "a@example.com", "A", "010-1111-1111", LocalDate.now());
      UserUpdatedEvent updated = UserUpdatedEvent.of("USR-2", "B", "010-2222-2222");
      UserDeletedEvent deleted = UserDeletedEvent.of("USR-3", "c@example.com", LocalDateTime.now(), "admin");

      // then
      assertThat(created.getEventId()).isNotEqualTo(updated.getEventId());
      assertThat(updated.getEventId()).isNotEqualTo(deleted.getEventId());
      assertThat(deleted.getEventId()).isNotEqualTo(created.getEventId());
    }

    @Test
    @DisplayName("같은 이벤트를 두 번 생성해도 다른 eventId를 가진다")
    void sameEventCreatedTwice_HaveDifferentEventIds() {
      // given
      UserCreatedEvent event1 = UserCreatedEvent.of("USR-1", "test@example.com", "테스트", "010-1234-5678", LocalDate.now());
      UserCreatedEvent event2 = UserCreatedEvent.of("USR-1", "test@example.com", "테스트", "010-1234-5678", LocalDate.now());

      // then
      assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    @Test
    @DisplayName("eventType은 클래스 이름이다")
    void eventType_IsClassName() {
      // given
      UserCreatedEvent created = UserCreatedEvent.of("USR-1", "a@example.com", "A", "010-1111-1111", LocalDate.now());
      UserUpdatedEvent updated = UserUpdatedEvent.of("USR-2", "B", "010-2222-2222");
      UserDeletedEvent deleted = UserDeletedEvent.of("USR-3", "c@example.com", LocalDateTime.now(), "admin");

      // then
      assertThat(created.getEventType()).isEqualTo("UserCreatedEvent");
      assertThat(updated.getEventType()).isEqualTo("UserUpdatedEvent");
      assertThat(deleted.getEventType()).isEqualTo("UserDeletedEvent");
    }
  }
}