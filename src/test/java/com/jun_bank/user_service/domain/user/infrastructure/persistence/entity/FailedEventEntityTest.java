package com.jun_bank.user_service.domain.user.infrastructure.persistence.entity;

import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.FailedEventEntity.FailedEventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FailedEventEntity 테스트")
class FailedEventEntityTest {

  // ========================================
  // 헬퍼 메서드
  // ========================================

  private FailedEventEntity createEntity() {
    return FailedEventEntity.of(
        "EVT-12345678",
        "USR-12345678",
        "UserCreatedEvent",
        "{\"userId\":\"USR-12345678\",\"email\":\"test@example.com\"}",
        0,
        "Connection refused",
        Instant.now()
    );
  }

  // ========================================
  // 생성 테스트
  // ========================================

  @Nested
  @DisplayName("of 팩토리 메서드")
  class OfTest {

    @Test
    @DisplayName("정적 팩토리 메서드로 엔티티를 생성한다")
    void of_CreatesEntity() {
      // given
      String eventId = "EVT-98765432";
      String targetId = "USR-11112222";
      String eventType = "UserDeletedEvent";
      String payload = "{\"userId\":\"USR-11112222\"}";
      int retryCount = 3;
      String errorMessage = "Kafka unavailable";
      Instant occurredAt = Instant.now().minusSeconds(60);

      // when
      FailedEventEntity entity = FailedEventEntity.of(
          eventId, targetId, eventType, payload, retryCount, errorMessage, occurredAt
      );

      // then
      assertThat(entity.getEventId()).isEqualTo(eventId);
      assertThat(entity.getTargetId()).isEqualTo(targetId);
      assertThat(entity.getEventType()).isEqualTo(eventType);
      assertThat(entity.getPayload()).isEqualTo(payload);
      assertThat(entity.getRetryCount()).isEqualTo(retryCount);
      assertThat(entity.getErrorMessage()).isEqualTo(errorMessage);
      assertThat(entity.getOccurredAt()).isEqualTo(occurredAt);
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.PENDING);
    }

    @Test
    @DisplayName("초기 상태는 PENDING이다")
    void of_InitialStatusIsPending() {
      // when
      FailedEventEntity entity = createEntity();

      // then
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.PENDING);
    }

    @Test
    @DisplayName("completedAt과 lastRetryAt은 초기에 null이다")
    void of_TimestampsAreNull() {
      // when
      FailedEventEntity entity = createEntity();

      // then
      assertThat(entity.getCompletedAt()).isNull();
      assertThat(entity.getLastRetryAt()).isNull();
    }
  }

  // ========================================
  // 상태 변경 테스트
  // ========================================

  @Nested
  @DisplayName("markProcessing")
  class MarkProcessingTest {

    @Test
    @DisplayName("상태를 PROCESSING으로 변경한다")
    void markProcessing_ChangesStatus() {
      // given
      FailedEventEntity entity = createEntity();

      // when
      entity.markProcessing();

      // then
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.PROCESSING);
    }

    @Test
    @DisplayName("lastRetryAt을 현재 시간으로 설정한다")
    void markProcessing_SetsLastRetryAt() {
      // given
      FailedEventEntity entity = createEntity();
      LocalDateTime before = LocalDateTime.now();

      // when
      entity.markProcessing();

      // then
      assertThat(entity.getLastRetryAt()).isNotNull();
      assertThat(entity.getLastRetryAt()).isAfterOrEqualTo(before);
    }
  }

  @Nested
  @DisplayName("markRetryFailed")
  class MarkRetryFailedTest {

    @Test
    @DisplayName("재시도 횟수를 증가시킨다")
    void markRetryFailed_IncrementsRetryCount() {
      // given
      FailedEventEntity entity = createEntity();
      int initialCount = entity.getRetryCount();

      // when
      entity.markRetryFailed("Network timeout");

      // then
      assertThat(entity.getRetryCount()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("에러 메시지를 업데이트한다")
    void markRetryFailed_UpdatesErrorMessage() {
      // given
      FailedEventEntity entity = createEntity();
      String newError = "New error message";

      // when
      entity.markRetryFailed(newError);

      // then
      assertThat(entity.getErrorMessage()).isEqualTo(newError);
    }

    @Test
    @DisplayName("상태를 PENDING으로 유지한다")
    void markRetryFailed_StatusRemainsPending() {
      // given
      FailedEventEntity entity = createEntity();
      entity.markProcessing();

      // when
      entity.markRetryFailed("Retry failed");

      // then
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.PENDING);
    }

    @Test
    @DisplayName("lastRetryAt을 업데이트한다")
    void markRetryFailed_UpdatesLastRetryAt() {
      // given
      FailedEventEntity entity = createEntity();
      LocalDateTime before = LocalDateTime.now();

      // when
      entity.markRetryFailed("Error");

      // then
      assertThat(entity.getLastRetryAt()).isNotNull();
      assertThat(entity.getLastRetryAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("여러 번 실패해도 횟수가 누적된다")
    void markRetryFailed_MultipleTimes_AccumulatesCount() {
      // given
      FailedEventEntity entity = createEntity();

      // when
      entity.markRetryFailed("Error 1");
      entity.markRetryFailed("Error 2");
      entity.markRetryFailed("Error 3");

      // then
      assertThat(entity.getRetryCount()).isEqualTo(3);
      assertThat(entity.getErrorMessage()).isEqualTo("Error 3");
    }
  }

  @Nested
  @DisplayName("markFailed")
  class MarkFailedTest {

    @Test
    @DisplayName("상태를 FAILED로 변경한다")
    void markFailed_ChangesStatusToFailed() {
      // given
      FailedEventEntity entity = createEntity();

      // when
      entity.markFailed("Final failure");

      // then
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.FAILED);
    }

    @Test
    @DisplayName("completedAt을 설정한다")
    void markFailed_SetsCompletedAt() {
      // given
      FailedEventEntity entity = createEntity();
      LocalDateTime before = LocalDateTime.now();

      // when
      entity.markFailed("Final failure");

      // then
      assertThat(entity.getCompletedAt()).isNotNull();
      assertThat(entity.getCompletedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("에러 메시지를 기록한다")
    void markFailed_RecordsErrorMessage() {
      // given
      FailedEventEntity entity = createEntity();
      String finalError = "Maximum retries exceeded";

      // when
      entity.markFailed(finalError);

      // then
      assertThat(entity.getErrorMessage()).isEqualTo(finalError);
    }
  }

  @Nested
  @DisplayName("markCompleted")
  class MarkCompletedTest {

    @Test
    @DisplayName("상태를 COMPLETED로 변경한다")
    void markCompleted_ChangesStatusToCompleted() {
      // given
      FailedEventEntity entity = createEntity();

      // when
      entity.markCompleted();

      // then
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.COMPLETED);
    }

    @Test
    @DisplayName("completedAt을 설정한다")
    void markCompleted_SetsCompletedAt() {
      // given
      FailedEventEntity entity = createEntity();
      LocalDateTime before = LocalDateTime.now();

      // when
      entity.markCompleted();

      // then
      assertThat(entity.getCompletedAt()).isNotNull();
      assertThat(entity.getCompletedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("에러 메시지를 null로 초기화한다")
    void markCompleted_ClearsErrorMessage() {
      // given
      FailedEventEntity entity = createEntity();
      assertThat(entity.getErrorMessage()).isNotNull();

      // when
      entity.markCompleted();

      // then
      assertThat(entity.getErrorMessage()).isNull();
    }
  }

  // ========================================
  // canRetry 테스트
  // ========================================

  @Nested
  @DisplayName("canRetry")
  class CanRetryTest {

    @Test
    @DisplayName("PENDING 상태면 재시도 가능")
    void canRetry_WhenPending_ReturnsTrue() {
      // given
      FailedEventEntity entity = createEntity();
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.PENDING);

      // when & then
      assertThat(entity.canRetry()).isTrue();
    }

    @Test
    @DisplayName("PROCESSING 상태면 재시도 불가")
    void canRetry_WhenProcessing_ReturnsFalse() {
      // given
      FailedEventEntity entity = createEntity();
      entity.markProcessing();

      // when & then
      assertThat(entity.canRetry()).isFalse();
    }

    @Test
    @DisplayName("COMPLETED 상태면 재시도 불가")
    void canRetry_WhenCompleted_ReturnsFalse() {
      // given
      FailedEventEntity entity = createEntity();
      entity.markCompleted();

      // when & then
      assertThat(entity.canRetry()).isFalse();
    }

    @Test
    @DisplayName("FAILED 상태면 재시도 불가")
    void canRetry_WhenFailed_ReturnsFalse() {
      // given
      FailedEventEntity entity = createEntity();
      entity.markFailed("Final failure");

      // when & then
      assertThat(entity.canRetry()).isFalse();
    }
  }

  // ========================================
  // FailedEventStatus Enum 테스트
  // ========================================

  @Nested
  @DisplayName("FailedEventStatus")
  class FailedEventStatusTest {

    @Test
    @DisplayName("모든 상태에 설명이 있다")
    void allStatuses_HaveDescriptions() {
      for (FailedEventStatus status : FailedEventStatus.values()) {
        assertThat(status.getDescription()).isNotNull();
        assertThat(status.getDescription()).isNotEmpty();
      }
    }

    @Test
    @DisplayName("상태 개수가 4개다")
    void statusCount_IsFour() {
      assertThat(FailedEventStatus.values()).hasSize(4);
    }

    @Test
    @DisplayName("상태 설명이 올바르다")
    void statusDescriptions_AreCorrect() {
      assertThat(FailedEventStatus.PENDING.getDescription()).isEqualTo("재시도 대기");
      assertThat(FailedEventStatus.PROCESSING.getDescription()).isEqualTo("처리 중");
      assertThat(FailedEventStatus.COMPLETED.getDescription()).isEqualTo("발행 성공");
      assertThat(FailedEventStatus.FAILED.getDescription()).isEqualTo("최종 실패");
    }
  }

  // ========================================
  // 상태 전이 시나리오 테스트
  // ========================================

  @Nested
  @DisplayName("상태 전이 시나리오")
  class StateTransitionScenarioTest {

    @Test
    @DisplayName("성공 시나리오: PENDING -> PROCESSING -> COMPLETED")
    void successScenario() {
      // given
      FailedEventEntity entity = createEntity();

      // when
      entity.markProcessing();
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.PROCESSING);

      entity.markCompleted();

      // then
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.COMPLETED);
      assertThat(entity.getCompletedAt()).isNotNull();
      assertThat(entity.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("재시도 후 성공 시나리오")
    void retryThenSuccessScenario() {
      // given
      FailedEventEntity entity = createEntity();

      // when - 첫 번째 실패
      entity.markProcessing();
      entity.markRetryFailed("First failure");
      assertThat(entity.getRetryCount()).isEqualTo(1);

      // when - 두 번째 실패
      entity.markProcessing();
      entity.markRetryFailed("Second failure");
      assertThat(entity.getRetryCount()).isEqualTo(2);

      // when - 세 번째 성공
      entity.markProcessing();
      entity.markCompleted();

      // then
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.COMPLETED);
      assertThat(entity.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("최종 실패 시나리오")
    void finalFailureScenario() {
      // given
      FailedEventEntity entity = createEntity();

      // when - 여러 번 재시도 실패
      for (int i = 1; i <= 5; i++) {
        entity.markProcessing();
        entity.markRetryFailed("Attempt " + i + " failed");
      }

      // when - 최종 실패 처리
      entity.markFailed("Max retries exceeded");

      // then
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.FAILED);
      assertThat(entity.getRetryCount()).isEqualTo(5);
      assertThat(entity.getCompletedAt()).isNotNull();
      assertThat(entity.canRetry()).isFalse();
    }
  }
}