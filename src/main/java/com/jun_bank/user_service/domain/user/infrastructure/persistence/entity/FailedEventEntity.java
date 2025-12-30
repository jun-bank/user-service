package com.jun_bank.user_service.domain.user.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 실패 이벤트 JPA 엔티티
 * <p>
 * 메모리 큐에서 최대 재시도 후에도 실패한 이벤트를 저장합니다.
 * 수동 처리 또는 배치 재시도에 사용됩니다.
 *
 * <h3>저장 시점:</h3>
 * <ul>
 *   <li>메모리 큐에서 3회 재시도 실패 후</li>
 *   <li>서버 종료 시 큐에 남은 이벤트 (선택적)</li>
 * </ul>
 *
 * <h3>테이블 정보:</h3>
 * <ul>
 *   <li>테이블명: failed_events</li>
 * </ul>
 *
 * <h3>인덱스:</h3>
 * <ul>
 *   <li>idx_failed_event_status: status</li>
 *   <li>idx_failed_event_target: target_id</li>
 *   <li>idx_failed_event_created: created_at</li>
 * </ul>
 */
@Entity
@Table(
    name = "failed_events",
    indexes = {
        @Index(name = "idx_failed_event_status", columnList = "status"),
        @Index(name = "idx_failed_event_target", columnList = "target_id"),
        @Index(name = "idx_failed_event_created", columnList = "created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FailedEventEntity {

  @Id
  @Column(name = "event_id", length = 50, nullable = false)
  private String eventId;

  /**
   * 대상 ID (예: userId)
   */
  @Column(name = "target_id", length = 50, nullable = false)
  private String targetId;

  /**
   * 이벤트 타입 (예: UserCreatedEvent, UserDeletedEvent)
   */
  @Column(name = "event_type", length = 50, nullable = false)
  private String eventType;

  /**
   * 이벤트 페이로드 (JSON)
   */
  @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
  private String payload;

  /**
   * 재시도 횟수
   */
  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  /**
   * 상태
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private FailedEventStatus status;

  /**
   * 마지막 에러 메시지
   */
  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  /**
   * 원본 이벤트 발생 시간
   */
  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  /**
   * DB 저장 시간
   */
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * 마지막 재시도 시간
   */
  @Column(name = "last_retry_at")
  private LocalDateTime lastRetryAt;

  /**
   * 완료 시간 (성공 또는 최종 실패)
   */
  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  /**
   * 실패 이벤트 상태
   */
  public enum FailedEventStatus {
    PENDING("재시도 대기"),
    PROCESSING("처리 중"),
    COMPLETED("발행 성공"),
    FAILED("최종 실패");

    private final String description;

    FailedEventStatus(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // ========================================
  // 생성자 및 팩토리 메서드
  // ========================================

  private FailedEventEntity(String eventId, String targetId, String eventType,
      String payload, int retryCount, FailedEventStatus status,
      String errorMessage, Instant occurredAt) {
    this.eventId = eventId;
    this.targetId = targetId;
    this.eventType = eventType;
    this.payload = payload;
    this.retryCount = retryCount;
    this.status = status;
    this.errorMessage = errorMessage;
    this.occurredAt = occurredAt;
  }

  /**
   * IntegrationEvent에서 Entity 생성
   *
   * @param eventId      이벤트 ID
   * @param targetId     대상 ID (userId 등)
   * @param eventType    이벤트 타입
   * @param payload      JSON 페이로드
   * @param retryCount   재시도 횟수
   * @param errorMessage 에러 메시지
   * @param occurredAt   원본 이벤트 발생 시간
   * @return FailedEventEntity
   */
  public static FailedEventEntity of(String eventId, String targetId, String eventType,
      String payload, int retryCount, String errorMessage,
      Instant occurredAt) {
    return new FailedEventEntity(
        eventId,
        targetId,
        eventType,
        payload,
        retryCount,
        FailedEventStatus.PENDING,
        errorMessage,
        occurredAt
    );
  }

  // ========================================
  // 상태 변경 메서드
  // ========================================

  /**
   * 처리 중 상태로 변경
   */
  public void markProcessing() {
    this.status = FailedEventStatus.PROCESSING;
    this.lastRetryAt = LocalDateTime.now();
  }

  /**
   * 재시도 실패 처리
   */
  public void markRetryFailed(String errorMessage) {
    this.retryCount++;
    this.errorMessage = errorMessage;
    this.lastRetryAt = LocalDateTime.now();
    this.status = FailedEventStatus.PENDING;
  }

  /**
   * 최종 실패 처리
   */
  public void markFailed(String errorMessage) {
    this.errorMessage = errorMessage;
    this.status = FailedEventStatus.FAILED;
    this.completedAt = LocalDateTime.now();
  }

  /**
   * 성공 처리
   */
  public void markCompleted() {
    this.status = FailedEventStatus.COMPLETED;
    this.completedAt = LocalDateTime.now();
    this.errorMessage = null;
  }

  /**
   * 재시도 가능 여부
   */
  public boolean canRetry() {
    return this.status == FailedEventStatus.PENDING;
  }
}