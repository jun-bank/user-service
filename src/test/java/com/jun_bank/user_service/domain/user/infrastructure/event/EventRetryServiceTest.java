package com.jun_bank.user_service.domain.user.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jun_bank.common_lib.event.IntegrationEvent;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.FailedEventEntity;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.FailedEventEntity.FailedEventStatus;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.jpa.FailedEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventRetryService 테스트")
class EventRetryServiceTest {

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Mock
  private FailedEventJpaRepository failedEventRepository;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private SendResult<String, Object> sendResult;

  @InjectMocks
  private EventRetryService eventRetryService;

  private IntegrationEvent testEvent;

  @BeforeEach
  void setUp() {
    testEvent = createTestEvent();
  }

  // ========================================
  // 헬퍼 메서드
  // ========================================

  private IntegrationEvent createTestEvent() {
    return IntegrationEvent.builder()
        .eventId("EVT-12345678")
        .eventType("UserCreatedEvent")
        .aggregateId("USR-12345678")
        .aggregateType("USER")
        .occurredAt(Instant.now())
        .sourceService("user-service")
        .payload("{\"userId\":\"USR-12345678\"}")
        .partitionKey("USR-12345678")
        .build();
  }

  // ========================================
  // addRetry 테스트
  // ========================================

  @Nested
  @DisplayName("addRetry")
  class AddRetryTest {

    @Test
    @DisplayName("재시도 큐에 이벤트를 추가한다")
    void addRetry_AddsEventToQueue() {
      // given
      assertThat(eventRetryService.getQueueSize()).isZero();

      // when
      eventRetryService.addRetry(testEvent, "user.created", "Connection failed");

      // then
      assertThat(eventRetryService.getQueueSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 이벤트를 추가할 수 있다")
    void addRetry_MultipleEvents() {
      // when
      eventRetryService.addRetry(testEvent, "user.created", "Error 1");
      eventRetryService.addRetry(testEvent, "user.updated", "Error 2");
      eventRetryService.addRetry(testEvent, "user.deleted", "Error 3");

      // then
      assertThat(eventRetryService.getQueueSize()).isEqualTo(3);
    }
  }

  // ========================================
  // getQueueSize 테스트
  // ========================================

  @Nested
  @DisplayName("getQueueSize")
  class GetQueueSizeTest {

    @Test
    @DisplayName("초기 큐 사이즈는 0이다")
    void getQueueSize_InitiallyZero() {
      assertThat(eventRetryService.getQueueSize()).isZero();
    }

    @Test
    @DisplayName("이벤트 추가 후 큐 사이즈가 증가한다")
    void getQueueSize_IncreasesAfterAdd() {
      // when
      eventRetryService.addRetry(testEvent, "user.created", "Error");

      // then
      assertThat(eventRetryService.getQueueSize()).isEqualTo(1);
    }
  }

  // ========================================
  // processMemoryQueue 테스트
  // ========================================

  @Nested
  @DisplayName("processMemoryQueue")
  class ProcessMemoryQueueTest {

    @Test
    @DisplayName("큐가 비어있으면 아무것도 하지 않는다")
    void processMemoryQueue_EmptyQueue_DoesNothing() {
      // when
      eventRetryService.processMemoryQueue();

      // then
      verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("이벤트 재발행 성공 시 큐에서 제거된다")
    void processMemoryQueue_Success_RemovesFromQueue() {
      // given
      eventRetryService.addRetry(testEvent, "user.created", "Initial error");

      CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
      given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);

      // when
      eventRetryService.processMemoryQueue();

      // then
      assertThat(eventRetryService.getQueueSize()).isZero();
      verify(kafkaTemplate).send(eq("user.created"), anyString(), any());
    }

    @Test
    @DisplayName("3회 실패 후 DB에 저장된다")
    void processMemoryQueue_MaxRetry_SavesToDatabase() throws Exception {
      // given - 매번 실패하도록 설정
      CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException("Kafka error"));

      given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(failedFuture);
      given(objectMapper.writeValueAsString(any())).willReturn("{}");
      given(failedEventRepository.existsByEventId(anyString())).willReturn(false);

      // 3회 재시도
      eventRetryService.addRetry(testEvent, "user.created", "Error");
      eventRetryService.processMemoryQueue(); // retry 1
      eventRetryService.processMemoryQueue(); // retry 2
      eventRetryService.processMemoryQueue(); // retry 3 -> DB 저장

      // then
      verify(failedEventRepository).save(any(FailedEventEntity.class));
    }

    @Test
    @DisplayName("이미 DB에 존재하는 이벤트는 저장하지 않는다")
    void processMemoryQueue_AlreadyExists_DoesNotSave() throws Exception {
      // given
      CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException("Kafka error"));

      given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(failedFuture);
      given(objectMapper.writeValueAsString(any())).willReturn("{}");
      given(failedEventRepository.existsByEventId(anyString())).willReturn(true);

      eventRetryService.addRetry(testEvent, "user.created", "Error");
      // 3회 실패시켜 DB 저장 시도
      eventRetryService.processMemoryQueue();
      eventRetryService.processMemoryQueue();
      eventRetryService.processMemoryQueue();

      // then - save 호출되지 않음
      verify(failedEventRepository, never()).save(any());
    }
  }

  // ========================================
  // processDatabaseEvents 테스트
  // ========================================

  @Nested
  @DisplayName("processDatabaseEvents")
  class ProcessDatabaseEventsTest {

    @Test
    @DisplayName("대기 중인 이벤트가 없으면 아무것도 하지 않는다")
    void processDatabaseEvents_NoEvents_DoesNothing() {
      // given
      given(failedEventRepository.findByStatusWithLimit(eq(FailedEventStatus.PENDING), anyInt()))
          .willReturn(Collections.emptyList());

      // when
      eventRetryService.processDatabaseEvents();

      // then
      verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("DB 이벤트 재발행 성공 시 COMPLETED로 변경")
    void processDatabaseEvents_Success_MarksCompleted() throws Exception {
      // given
      FailedEventEntity entity = FailedEventEntity.of(
          "EVT-12345678", "USR-12345678", "UserCreatedEvent",
          "{}", 0, "Initial error", Instant.now()
      );

      CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

      given(failedEventRepository.findByStatusWithLimit(eq(FailedEventStatus.PENDING), anyInt()))
          .willReturn(List.of(entity));
      given(objectMapper.readValue(anyString(), eq(IntegrationEvent.class)))
          .willReturn(testEvent);
      given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);

      // when
      eventRetryService.processDatabaseEvents();

      // then
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.COMPLETED);
    }

    @Test
    @DisplayName("DB 이벤트 재발행 실패 시 재시도 횟수 증가")
    void processDatabaseEvents_Failure_IncrementsRetryCount() throws Exception {
      // given
      FailedEventEntity entity = FailedEventEntity.of(
          "EVT-12345678", "USR-12345678", "UserCreatedEvent",
          "{}", 0, "Initial error", Instant.now()
      );

      CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException("Kafka error"));

      given(failedEventRepository.findByStatusWithLimit(eq(FailedEventStatus.PENDING), anyInt()))
          .willReturn(List.of(entity));
      given(objectMapper.readValue(anyString(), eq(IntegrationEvent.class)))
          .willReturn(testEvent);
      given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(failedFuture);

      // when
      eventRetryService.processDatabaseEvents();

      // then
      assertThat(entity.getRetryCount()).isEqualTo(1);
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.PENDING);
    }

    @Test
    @DisplayName("DB 이벤트 5회 실패 후 FAILED로 변경")
    void processDatabaseEvents_MaxRetry_MarksFailed() throws Exception {
      // given - 이미 4회 실패한 상태
      FailedEventEntity entity = FailedEventEntity.of(
          "EVT-12345678", "USR-12345678", "UserCreatedEvent",
          "{}", 4, "Previous error", Instant.now()
      );

      CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException("Kafka error"));

      given(failedEventRepository.findByStatusWithLimit(eq(FailedEventStatus.PENDING), anyInt()))
          .willReturn(List.of(entity));
      given(objectMapper.readValue(anyString(), eq(IntegrationEvent.class)))
          .willReturn(testEvent);
      given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(failedFuture);

      // when
      eventRetryService.processDatabaseEvents();

      // then - 5회째 실패 -> FAILED
      assertThat(entity.getStatus()).isEqualTo(FailedEventStatus.FAILED);
    }

    @Test
    @DisplayName("이벤트 타입에 따라 올바른 토픽으로 발행한다 - UserCreatedEvent")
    void processDatabaseEvents_ResolvesCorrectTopic_Created() throws Exception {
      // given
      FailedEventEntity entity = FailedEventEntity.of(
          "EVT-1", "USR-1", "UserCreatedEvent", "{}", 0, "Error", Instant.now()
      );

      CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

      given(failedEventRepository.findByStatusWithLimit(eq(FailedEventStatus.PENDING), anyInt()))
          .willReturn(List.of(entity));
      given(objectMapper.readValue(anyString(), eq(IntegrationEvent.class)))
          .willReturn(testEvent);
      given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);

      // when
      eventRetryService.processDatabaseEvents();

      // then
      verify(kafkaTemplate).send(eq("user.created"), anyString(), any());
    }

    @Test
    @DisplayName("이벤트 타입에 따라 올바른 토픽으로 발행한다 - UserUpdatedEvent")
    void processDatabaseEvents_ResolvesCorrectTopic_Updated() throws Exception {
      // given
      FailedEventEntity entity = FailedEventEntity.of(
          "EVT-2", "USR-2", "UserUpdatedEvent", "{}", 0, "Error", Instant.now()
      );

      CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

      given(failedEventRepository.findByStatusWithLimit(eq(FailedEventStatus.PENDING), anyInt()))
          .willReturn(List.of(entity));
      given(objectMapper.readValue(anyString(), eq(IntegrationEvent.class)))
          .willReturn(testEvent);
      given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);

      // when
      eventRetryService.processDatabaseEvents();

      // then
      verify(kafkaTemplate).send(eq("user.updated"), anyString(), any());
    }

    @Test
    @DisplayName("이벤트 타입에 따라 올바른 토픽으로 발행한다 - UserDeletedEvent")
    void processDatabaseEvents_ResolvesCorrectTopic_Deleted() throws Exception {
      // given
      FailedEventEntity entity = FailedEventEntity.of(
          "EVT-3", "USR-3", "UserDeletedEvent", "{}", 0, "Error", Instant.now()
      );

      CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

      given(failedEventRepository.findByStatusWithLimit(eq(FailedEventStatus.PENDING), anyInt()))
          .willReturn(List.of(entity));
      given(objectMapper.readValue(anyString(), eq(IntegrationEvent.class)))
          .willReturn(testEvent);
      given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);

      // when
      eventRetryService.processDatabaseEvents();

      // then
      verify(kafkaTemplate).send(eq("user.deleted"), anyString(), any());
    }

    @Test
    @DisplayName("알 수 없는 이벤트 타입은 기본 토픽으로 발행한다")
    void processDatabaseEvents_UnknownEventType_UsesDefaultTopic() throws Exception {
      // given
      FailedEventEntity entity = FailedEventEntity.of(
          "EVT-1", "USR-1", "UnknownEvent", "{}", 0, "Error", Instant.now()
      );

      CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

      given(failedEventRepository.findByStatusWithLimit(eq(FailedEventStatus.PENDING), anyInt()))
          .willReturn(List.of(entity));
      given(objectMapper.readValue(anyString(), eq(IntegrationEvent.class)))
          .willReturn(testEvent);
      given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);

      // when
      eventRetryService.processDatabaseEvents();

      // then
      verify(kafkaTemplate).send(eq("user.events"), anyString(), any());
    }
  }
}