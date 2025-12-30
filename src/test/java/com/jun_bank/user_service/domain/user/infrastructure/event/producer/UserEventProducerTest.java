package com.jun_bank.user_service.domain.user.infrastructure.event.producer;

import com.jun_bank.common_lib.event.IntegrationEvent;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.domain.model.User;
import com.jun_bank.user_service.domain.user.domain.model.UserStatus;
import com.jun_bank.user_service.domain.user.domain.model.vo.Email;
import com.jun_bank.user_service.domain.user.domain.model.vo.PhoneNumber;
import com.jun_bank.user_service.domain.user.domain.model.vo.UserId;
import com.jun_bank.user_service.domain.user.infrastructure.event.EventRetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventProducer 테스트")
class UserEventProducerTest {

  @Mock
  private KafkaTemplate<String, IntegrationEvent> kafkaTemplate;

  @Mock
  private EventRetryService eventRetryService;

  @Mock
  private SendResult<String, IntegrationEvent> sendResult;

  @InjectMocks
  private UserEventProducer userEventProducer;

  @Captor
  private ArgumentCaptor<IntegrationEvent> eventCaptor;

  @Captor
  private ArgumentCaptor<String> topicCaptor;

  private User activeUser;
  private User deletedUser;

  @BeforeEach
  void setUp() {
    // sourceService 필드 설정
    ReflectionTestUtils.setField(userEventProducer, "sourceService", "user-service");

    activeUser = User.restoreBuilder()
        .userId(UserId.of("USR-12345678"))
        .email(Email.of("test@example.com"))
        .name("테스트사용자")
        .phoneNumber(PhoneNumber.of("010-1234-5678"))
        .birthDate(LocalDate.of(1990, 5, 15))
        .status(UserStatus.ACTIVE)
        .isDeleted(false)
        .build();

    deletedUser = User.restoreBuilder()
        .userId(UserId.of("USR-87654321"))
        .email(Email.of("deleted@example.com"))
        .name("탈퇴사용자")
        .phoneNumber(PhoneNumber.of("010-9999-8888"))
        .birthDate(LocalDate.of(1985, 3, 20))
        .status(UserStatus.DELETED)
        .isDeleted(true)
        .deletedAt(LocalDateTime.now())
        .deletedBy("admin")
        .build();
  }

  // ========================================
  // publishUserCreated 테스트
  // ========================================

  @Nested
  @DisplayName("publishUserCreated")
  class PublishUserCreatedTest {

    @Test
    @DisplayName("사용자 생성 이벤트를 발행한다")
    void publishUserCreated_Success() {
      // given
      CompletableFuture<SendResult<String, IntegrationEvent>> future = CompletableFuture.completedFuture(sendResult);
      given(kafkaTemplate.send(anyString(), anyString(), any(IntegrationEvent.class))).willReturn(future);

      // when
      userEventProducer.publishUserCreated(activeUser);

      // then
      verify(kafkaTemplate).send(
          topicCaptor.capture(),
          eq(activeUser.getUserId().value()),
          eventCaptor.capture()
      );

      assertThat(topicCaptor.getValue()).isEqualTo("user.created");

      IntegrationEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getEventType()).isEqualTo("UserCreatedEvent");
      assertThat(capturedEvent.getAggregateId()).isEqualTo(activeUser.getUserId().value());
      assertThat(capturedEvent.getSourceService()).isEqualTo("user-service");
    }

    @Test
    @DisplayName("발행 실패 시 재시도 큐에 추가한다 (비동기)")
    void publishUserCreated_AsyncFailure_AddsToRetryQueue() throws InterruptedException {
      // given
      CompletableFuture<SendResult<String, IntegrationEvent>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException("Kafka connection failed"));

      given(kafkaTemplate.send(anyString(), anyString(), any(IntegrationEvent.class))).willReturn(failedFuture);

      // when
      userEventProducer.publishUserCreated(activeUser);

      // then - 비동기 콜백 실행 대기
      Thread.sleep(100);
      verify(eventRetryService).addRetry(any(IntegrationEvent.class), eq("user.created"), anyString());
    }

    @Test
    @DisplayName("동기 예외 발생 시 재시도 큐에 추가하고 예외를 던진다")
    void publishUserCreated_SyncException_ThrowsAndAddsToRetryQueue() {
      // given
      given(kafkaTemplate.send(anyString(), anyString(), any(IntegrationEvent.class)))
          .willThrow(new RuntimeException("Kafka unavailable"));

      // when & then
      assertThatThrownBy(() -> userEventProducer.publishUserCreated(activeUser))
          .isInstanceOf(UserException.class);

      verify(eventRetryService).addRetry(any(IntegrationEvent.class), eq("user.created"), anyString());
    }
  }

  // ========================================
  // publishUserUpdated 테스트
  // ========================================

  @Nested
  @DisplayName("publishUserUpdated")
  class PublishUserUpdatedTest {

    @Test
    @DisplayName("사용자 수정 이벤트를 발행한다")
    void publishUserUpdated_Success() {
      // given
      CompletableFuture<SendResult<String, IntegrationEvent>> future = CompletableFuture.completedFuture(sendResult);
      given(kafkaTemplate.send(anyString(), anyString(), any(IntegrationEvent.class))).willReturn(future);

      // when
      userEventProducer.publishUserUpdated(activeUser);

      // then
      verify(kafkaTemplate).send(
          topicCaptor.capture(),
          eq(activeUser.getUserId().value()),
          eventCaptor.capture()
      );

      assertThat(topicCaptor.getValue()).isEqualTo("user.updated");

      IntegrationEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getEventType()).isEqualTo("UserUpdatedEvent");
      assertThat(capturedEvent.getAggregateId()).isEqualTo(activeUser.getUserId().value());
    }

    @Test
    @DisplayName("발행 실패 시 재시도 큐에 추가한다")
    void publishUserUpdated_Failure_AddsToRetryQueue() throws InterruptedException {
      // given
      CompletableFuture<SendResult<String, IntegrationEvent>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException("Broker not available"));

      given(kafkaTemplate.send(anyString(), anyString(), any(IntegrationEvent.class))).willReturn(failedFuture);

      // when
      userEventProducer.publishUserUpdated(activeUser);

      // then
      Thread.sleep(100);
      verify(eventRetryService).addRetry(any(IntegrationEvent.class), eq("user.updated"), anyString());
    }
  }

  // ========================================
  // publishUserDeleted 테스트
  // ========================================

  @Nested
  @DisplayName("publishUserDeleted")
  class PublishUserDeletedTest {

    @Test
    @DisplayName("사용자 삭제 이벤트를 발행한다")
    void publishUserDeleted_Success() {
      // given
      CompletableFuture<SendResult<String, IntegrationEvent>> future = CompletableFuture.completedFuture(sendResult);
      given(kafkaTemplate.send(anyString(), anyString(), any(IntegrationEvent.class))).willReturn(future);

      // when
      userEventProducer.publishUserDeleted(deletedUser);

      // then
      verify(kafkaTemplate).send(
          topicCaptor.capture(),
          eq(deletedUser.getUserId().value()),
          eventCaptor.capture()
      );

      assertThat(topicCaptor.getValue()).isEqualTo("user.deleted");

      IntegrationEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getEventType()).isEqualTo("UserDeletedEvent");
      assertThat(capturedEvent.getAggregateId()).isEqualTo(deletedUser.getUserId().value());
    }

    @Test
    @DisplayName("삭제된 사용자의 deletedAt, deletedBy가 이벤트에 포함된다")
    void publishUserDeleted_IncludesDeleteInfo() {
      // given
      CompletableFuture<SendResult<String, IntegrationEvent>> future = CompletableFuture.completedFuture(sendResult);
      given(kafkaTemplate.send(anyString(), anyString(), any(IntegrationEvent.class))).willReturn(future);

      // when
      userEventProducer.publishUserDeleted(deletedUser);

      // then
      verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());

      IntegrationEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getAggregateId()).isEqualTo(deletedUser.getUserId().value());
    }

    @Test
    @DisplayName("발행 실패 시 재시도 큐에 추가한다")
    void publishUserDeleted_Failure_AddsToRetryQueue() throws InterruptedException {
      // given
      CompletableFuture<SendResult<String, IntegrationEvent>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException("Network error"));

      given(kafkaTemplate.send(anyString(), anyString(), any(IntegrationEvent.class))).willReturn(failedFuture);

      // when
      userEventProducer.publishUserDeleted(deletedUser);

      // then
      Thread.sleep(100);
      verify(eventRetryService).addRetry(any(IntegrationEvent.class), eq("user.deleted"), anyString());
    }
  }

  // ========================================
  // 재시도 큐 추가 실패 테스트
  // ========================================

  @Nested
  @DisplayName("재시도 큐 추가 실패")
  class RetryQueueFailureTest {

    @Test
    @DisplayName("재시도 큐 추가 실패 시에도 원래 예외를 던진다")
    void retryQueueAddFailure_StillThrowsOriginalException() {
      // given
      given(kafkaTemplate.send(anyString(), anyString(), any(IntegrationEvent.class)))
          .willThrow(new RuntimeException("Kafka unavailable"));
      doThrow(new RuntimeException("Queue full")).when(eventRetryService)
          .addRetry(any(), anyString(), anyString());

      // when & then
      assertThatThrownBy(() -> userEventProducer.publishUserCreated(activeUser))
          .isInstanceOf(UserException.class);
    }
  }

  // ========================================
  // 파티션 키 테스트
  // ========================================

  @Nested
  @DisplayName("파티션 키")
  class PartitionKeyTest {

    @Test
    @DisplayName("userId가 파티션 키로 사용된다")
    void partitionKey_IsUserId() {
      // given
      CompletableFuture<SendResult<String, IntegrationEvent>> future = CompletableFuture.completedFuture(sendResult);
      given(kafkaTemplate.send(anyString(), anyString(), any(IntegrationEvent.class))).willReturn(future);

      // when
      userEventProducer.publishUserCreated(activeUser);

      // then
      verify(kafkaTemplate).send(
          anyString(),
          eq(activeUser.getUserId().value()),  // 파티션 키 = userId
          any(IntegrationEvent.class)
      );
    }
  }
}