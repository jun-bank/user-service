package com.jun_bank.user_service.domain.user.infrastructure.event.producer;

import com.jun_bank.common_lib.event.IntegrationEvent;
import com.jun_bank.user_service.domain.user.application.port.out.UserEventPublisherPort;
import com.jun_bank.user_service.domain.user.domain.event.UserCreatedEvent;
import com.jun_bank.user_service.domain.user.domain.event.UserDeletedEvent;
import com.jun_bank.user_service.domain.user.domain.event.UserUpdatedEvent;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.domain.model.User;
import com.jun_bank.user_service.domain.user.infrastructure.event.EventRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 사용자 이벤트 Producer
 * <p>
 * {@link UserEventPublisherPort}를 구현하여 Kafka로 이벤트를 발행합니다.
 *
 * <h3>토픽:</h3>
 * <ul>
 *   <li>user.created - 회원가입</li>
 *   <li>user.updated - 프로필 수정</li>
 *   <li>user.deleted - 회원 탈퇴</li>
 * </ul>
 *
 * <h3>재시도 처리:</h3>
 * <p>
 * 발행 실패 시 {@link EventRetryService}를 통해 재시도 큐에 추가됩니다.
 * 메모리 큐에서 3회 재시도 후에도 실패하면 DB에 저장됩니다.
 * </p>
 *
 * <h3>발행 방식:</h3>
 * <p>
 * 비동기 발행 후 콜백에서 결과를 처리합니다.
 * 동기 예외 발생 시에도 재시도 큐에 추가됩니다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer implements UserEventPublisherPort {

  private final KafkaTemplate<String, IntegrationEvent> kafkaTemplate;
  private final EventRetryService eventRetryService;

  @Value("${spring.application.name:user-service}")
  private String sourceService;

  // ========================================
  // 토픽명
  // ========================================
  private static final String TOPIC_USER_CREATED = "user.created";
  private static final String TOPIC_USER_UPDATED = "user.updated";
  private static final String TOPIC_USER_DELETED = "user.deleted";

  @Override
  public void publishUserCreated(User user) {
    String userId = user.getUserId().value();
    log.debug("user.created 이벤트 발행 시작: userId={}", userId);

    UserCreatedEvent domainEvent = UserCreatedEvent.of(
        userId,
        user.getEmail().value(),
        user.getName(),
        user.getPhoneNumber().value(),
        user.getBirthDate()
    );

    IntegrationEvent integrationEvent = IntegrationEvent.from(domainEvent, sourceService);
    publishWithRetry(TOPIC_USER_CREATED, integrationEvent);
  }

  @Override
  public void publishUserUpdated(User user) {
    String userId = user.getUserId().value();
    log.debug("user.updated 이벤트 발행 시작: userId={}", userId);

    UserUpdatedEvent domainEvent = UserUpdatedEvent.of(
        userId,
        user.getName(),
        user.getPhoneNumber().value()
    );

    IntegrationEvent integrationEvent = IntegrationEvent.from(domainEvent, sourceService);
    publishWithRetry(TOPIC_USER_UPDATED, integrationEvent);
  }

  @Override
  public void publishUserDeleted(User user) {
    String userId = user.getUserId().value();
    log.debug("user.deleted 이벤트 발행 시작: userId={}", userId);

    UserDeletedEvent domainEvent = UserDeletedEvent.of(
        userId,
        user.getEmail().value(),
        user.getDeletedAt(),
        user.getDeletedBy()
    );

    IntegrationEvent integrationEvent = IntegrationEvent.from(domainEvent, sourceService);
    publishWithRetry(TOPIC_USER_DELETED, integrationEvent);
  }

  // ========================================
  // Private 메서드
  // ========================================

  /**
   * Kafka 이벤트 발행 (재시도 포함)
   * <p>
   * 발행 실패 시 EventRetryService에 추가하여 재시도합니다.
   * </p>
   *
   * @param topic 토픽명
   * @param event IntegrationEvent
   */
  private void publishWithRetry(String topic, IntegrationEvent event) {
    String partitionKey = event.getPartitionKey();
    String eventId = event.getEventId();
    String eventType = event.getEventType();

    try {
      CompletableFuture<?> future = kafkaTemplate.send(topic, partitionKey, event);

      future.whenComplete((result, ex) -> {
        if (ex != null) {
          // 비동기 발행 실패
          log.error("이벤트 발행 실패 (비동기): topic={}, eventId={}, eventType={}, error={}",
              topic, eventId, eventType, ex.getMessage());

          // 재시도 큐에 추가
          addToRetryQueue(event, topic, ex.getMessage());
        } else {
          log.info("이벤트 발행 성공: topic={}, eventId={}, eventType={}",
              topic, eventId, eventType);
        }
      });

    } catch (Exception e) {
      // 동기 예외 (KafkaTemplate.send 호출 자체 실패)
      log.error("이벤트 발행 실패 (동기): topic={}, eventId={}, eventType={}, error={}",
          topic, eventId, eventType, e.getMessage());

      // 재시도 큐에 추가
      addToRetryQueue(event, topic, e.getMessage());

      // 예외 던지기 (호출자에게 실패 알림)
      throw UserException.eventPublishFailed(eventType, e);
    }
  }

  /**
   * 재시도 큐에 이벤트 추가
   *
   * @param event IntegrationEvent
   * @param topic 토픽명
   * @param errorMessage 에러 메시지
   */
  private void addToRetryQueue(IntegrationEvent event, String topic, String errorMessage) {
    try {
      eventRetryService.addRetry(event, topic, errorMessage);
      log.info("재시도 큐에 이벤트 추가: eventId={}, topic={}", event.getEventId(), topic);
    } catch (Exception retryEx) {
      // 재시도 큐 추가 실패 시 로그만 남김 (심각한 상황)
      log.error("재시도 큐 추가 실패! eventId={}, topic={}, error={}",
          event.getEventId(), topic, retryEx.getMessage(), retryEx);
    }
  }
}