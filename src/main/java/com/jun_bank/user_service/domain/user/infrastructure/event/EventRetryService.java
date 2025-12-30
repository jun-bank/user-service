package com.jun_bank.user_service.domain.user.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jun_bank.common_lib.event.IntegrationEvent;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.FailedEventEntity;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.FailedEventEntity.FailedEventStatus;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.jpa.FailedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 이벤트 재시도 서비스
 * <p>
 * 이벤트 발행 실패 시 재시도를 관리합니다.
 *
 * <h3>재시도 전략:</h3>
 * <ol>
 *   <li>실패 즉시 메모리 큐에 추가</li>
 *   <li>스케줄러가 30초마다 메모리 큐 처리</li>
 *   <li>3회 실패 시 DB에 저장</li>
 *   <li>별도 스케줄러가 5분마다 DB 이벤트 재시도</li>
 * </ol>
 *
 * <h3>메모리 큐 장점:</h3>
 * <ul>
 *   <li>빠른 재시도 (DB I/O 없음)</li>
 *   <li>일시적 장애에 효과적</li>
 * </ul>
 *
 * <h3>DB 저장 장점:</h3>
 * <ul>
 *   <li>서버 재시작 시에도 유지</li>
 *   <li>모니터링 및 수동 처리 가능</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventRetryService {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final FailedEventJpaRepository failedEventRepository;
  private final ObjectMapper objectMapper;

  /**
   * 메모리 기반 재시도 큐 (스레드 세이프)
   */
  private final ConcurrentLinkedQueue<RetryEvent> retryQueue = new ConcurrentLinkedQueue<>();

  /**
   * 최대 재시도 횟수 (메모리 큐)
   */
  private static final int MAX_MEMORY_RETRY = 3;

  /**
   * 최대 재시도 횟수 (DB)
   */
  private static final int MAX_DB_RETRY = 5;

  /**
   * 재시도 대상 래퍼
   */
  private record RetryEvent(
      IntegrationEvent event,
      String topic,
      int retryCount,
      String lastError
  ) {
    RetryEvent incrementRetry(String error) {
      return new RetryEvent(event, topic, retryCount + 1, error);
    }
  }

  // ========================================
  // Public API
  // ========================================

  /**
   * 재시도 큐에 이벤트 추가
   *
   * @param event 실패한 이벤트
   * @param topic Kafka 토픽
   * @param error 에러 메시지
   */
  public void addRetry(IntegrationEvent event, String topic, String error) {
    log.debug("재시도 큐에 이벤트 추가: eventId={}, topic={}", event.getEventId(), topic);
    retryQueue.offer(new RetryEvent(event, topic, 0, error));
  }

  /**
   * 현재 큐 크기 조회 (모니터링용)
   */
  public int getQueueSize() {
    return retryQueue.size();
  }

  // ========================================
  // 스케줄러: 메모리 큐 처리
  // ========================================

  /**
   * 메모리 큐 재시도 처리 (30초마다)
   */
  @Scheduled(fixedDelay = 30000)
  public void processMemoryQueue() {
    if (retryQueue.isEmpty()) {
      return;
    }

    log.info("메모리 큐 재시도 시작: size={}", retryQueue.size());
    int processed = 0;
    int success = 0;
    int failed = 0;

    RetryEvent retryEvent;
    while ((retryEvent = retryQueue.poll()) != null) {
      processed++;
      try {
        // Kafka 발행 시도
        kafkaTemplate.send(retryEvent.topic(), retryEvent.event().getPartitionKey(), retryEvent.event())
            .get();  // 동기 대기

        success++;
        log.info("이벤트 재발행 성공: eventId={}", retryEvent.event().getEventId());

      } catch (Exception e) {
        failed++;
        String errorMsg = e.getMessage();
        RetryEvent incremented = retryEvent.incrementRetry(errorMsg);

        if (incremented.retryCount() < MAX_MEMORY_RETRY) {
          // 다시 큐에 추가
          retryQueue.offer(incremented);
          log.warn("이벤트 재발행 실패, 재시도 예약: eventId={}, retry={}/{}",
              incremented.event().getEventId(), incremented.retryCount(), MAX_MEMORY_RETRY);
        } else {
          // DB에 저장
          saveToDatabase(incremented);
          log.error("이벤트 재발행 최종 실패, DB 저장: eventId={}",
              incremented.event().getEventId());
        }
      }
    }

    log.info("메모리 큐 재시도 완료: processed={}, success={}, failed={}", processed, success, failed);
  }

  // ========================================
  // 스케줄러: DB 이벤트 처리
  // ========================================

  /**
   * DB 저장된 이벤트 재시도 (5분마다)
   */
  @Scheduled(fixedDelay = 300000)
  @Transactional
  public void processDatabaseEvents() {
    List<FailedEventEntity> pendingEvents = failedEventRepository
        .findByStatusWithLimit(FailedEventStatus.PENDING, 100);

    if (pendingEvents.isEmpty()) {
      return;
    }

    log.info("DB 이벤트 재시도 시작: count={}", pendingEvents.size());
    int success = 0;
    int failed = 0;

    for (FailedEventEntity entity : pendingEvents) {
      entity.markProcessing();

      try {
        // JSON → IntegrationEvent 복원
        IntegrationEvent event = objectMapper.readValue(entity.getPayload(), IntegrationEvent.class);
        String topic = resolveTopicFromEventType(entity.getEventType());

        // Kafka 발행
        kafkaTemplate.send(topic, event.getPartitionKey(), event).get();

        entity.markCompleted();
        success++;
        log.info("DB 이벤트 재발행 성공: eventId={}", entity.getEventId());

      } catch (Exception e) {
        failed++;
        String errorMsg = e.getMessage();

        if (entity.getRetryCount() + 1 >= MAX_DB_RETRY) {
          entity.markFailed(errorMsg);
          log.error("DB 이벤트 최종 실패: eventId={}, error={}", entity.getEventId(), errorMsg);
        } else {
          entity.markRetryFailed(errorMsg);
          log.warn("DB 이벤트 재시도 실패: eventId={}, retry={}/{}",
              entity.getEventId(), entity.getRetryCount(), MAX_DB_RETRY);
        }
      }
    }

    log.info("DB 이벤트 재시도 완료: success={}, failed={}", success, failed);
  }

  // ========================================
  // Private 메서드
  // ========================================

  /**
   * DB에 실패 이벤트 저장
   */
  private void saveToDatabase(RetryEvent retryEvent) {
    try {
      IntegrationEvent event = retryEvent.event();
      String payload = objectMapper.writeValueAsString(event);

      // 중복 체크
      if (failedEventRepository.existsByEventId(event.getEventId())) {
        log.warn("이미 DB에 존재하는 이벤트: eventId={}", event.getEventId());
        return;
      }

      FailedEventEntity entity = FailedEventEntity.of(
          event.getEventId(),
          event.getAggregateId(),
          event.getEventType(),
          payload,
          retryEvent.retryCount(),
          retryEvent.lastError(),
          event.getOccurredAt()
      );

      failedEventRepository.save(entity);
      log.info("실패 이벤트 DB 저장: eventId={}", event.getEventId());

    } catch (JsonProcessingException e) {
      log.error("이벤트 직렬화 실패: eventId={}", retryEvent.event().getEventId(), e);
    }
  }

  /**
   * 이벤트 타입으로 토픽 결정
   */
  private String resolveTopicFromEventType(String eventType) {

    return switch (eventType) {
      case "UserCreatedEvent" -> "user.created";
      case "UserUpdatedEvent" -> "user.updated";
      case "UserDeletedEvent" -> "user.deleted";
      default -> "user.events";
    };
  }
}