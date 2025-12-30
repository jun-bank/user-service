package com.jun_bank.user_service.domain.user.infrastructure.persistence.jpa;

import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.FailedEventEntity;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.FailedEventEntity.FailedEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 실패 이벤트 JPA Repository
 * <p>
 * 재시도 실패한 이벤트의 영속성을 관리합니다.
 */
public interface FailedEventJpaRepository extends JpaRepository<FailedEventEntity, String> {

  // ========================================
  // 재시도 대상 조회
  // ========================================

  /**
   * 재시도 대기 상태인 이벤트 조회
   *
   * @param status   상태
   * @param pageable 페이징
   * @return Page<FailedEventEntity>
   */
  Page<FailedEventEntity> findByStatus(FailedEventStatus status, Pageable pageable);

  /**
   * 재시도 대기 상태인 이벤트 목록 조회 (제한)
   *
   * @param status 상태
   * @param limit  최대 개수
   * @return List<FailedEventEntity>
   */
  @Query("SELECT e FROM FailedEventEntity e WHERE e.status = :status ORDER BY e.createdAt ASC LIMIT :limit")
  List<FailedEventEntity> findByStatusWithLimit(@Param("status") FailedEventStatus status,
      @Param("limit") int limit);

  /**
   * 특정 대상의 실패 이벤트 조회
   *
   * @param targetId 대상 ID
   * @return List<FailedEventEntity>
   */
  List<FailedEventEntity> findByTargetIdOrderByCreatedAtDesc(String targetId);

  /**
   * 특정 이벤트 타입의 실패 이벤트 조회
   *
   * @param eventType 이벤트 타입
   * @param status    상태
   * @return List<FailedEventEntity>
   */
  List<FailedEventEntity> findByEventTypeAndStatus(String eventType, FailedEventStatus status);

  // ========================================
  // 통계
  // ========================================

  /**
   * 상태별 이벤트 수 조회
   *
   * @param status 상태
   * @return 개수
   */
  long countByStatus(FailedEventStatus status);

  /**
   * 이벤트 타입별 실패 수 조회
   *
   * @param eventType 이벤트 타입
   * @param status    상태
   * @return 개수
   */
  long countByEventTypeAndStatus(String eventType, FailedEventStatus status);

  // ========================================
  // 정리
  // ========================================

  /**
   * 완료된 오래된 이벤트 삭제
   *
   * @param status    상태
   * @param threshold 기준 시간
   * @return 삭제 건수
   */
  @Modifying
  @Query("DELETE FROM FailedEventEntity e WHERE e.status = :status AND e.completedAt < :threshold")
  int deleteByStatusAndCompletedAtBefore(@Param("status") FailedEventStatus status,
      @Param("threshold") LocalDateTime threshold);

  /**
   * 이벤트 ID 존재 여부 확인
   *
   * @param eventId 이벤트 ID
   * @return 존재하면 true
   */
  boolean existsByEventId(String eventId);
}