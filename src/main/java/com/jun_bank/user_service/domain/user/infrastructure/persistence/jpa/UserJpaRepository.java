package com.jun_bank.user_service.domain.user.infrastructure.persistence.jpa;

import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * User JPA Repository
 * <p>
 * Spring Data JPA 기본 CRUD 및 핵심 조회 메서드를 제공합니다.
 * 동적 검색은 {@link UserQueryRepositoryImpl}에서 처리합니다.
 *
 * <h3>설계 원칙:</h3>
 * <ul>
 *   <li>PK/Unique Key 조회만 제공</li>
 *   <li>존재 여부 확인 메서드 제공</li>
 *   <li>동적 조건 검색은 QueryDSL로 위임</li>
 * </ul>
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, String> {

  // ========================================
  // 단건 조회 (PK, Unique Key)
  // ========================================

  /**
   * 삭제되지 않은 사용자 조회 (ID)
   *
   * @param userId 사용자 ID
   * @return Optional<UserEntity>
   */
  Optional<UserEntity> findByUserIdAndIsDeletedFalse(String userId);

  /**
   * 이메일로 사용자 조회 (삭제되지 않은)
   *
   * @param email 이메일
   * @return Optional<UserEntity>
   */
  Optional<UserEntity> findByEmailAndIsDeletedFalse(String email);

  // ========================================
  // 존재 여부 확인
  // ========================================

  /**
   * 이메일 존재 여부 확인 (삭제되지 않은)
   *
   * @param email 이메일
   * @return 존재하면 true
   */
  boolean existsByEmailAndIsDeletedFalse(String email);

  /**
   * 전화번호 존재 여부 확인 (삭제되지 않은)
   *
   * @param phoneNumber 전화번호
   * @return 존재하면 true
   */
  boolean existsByPhoneNumberAndIsDeletedFalse(String phoneNumber);

  // ========================================
  // 배치 조회
  // ========================================

  /**
   * 여러 ID로 사용자 목록 조회 (삭제되지 않은)
   *
   * @param userIds 사용자 ID 목록
   * @return List<UserEntity>
   */
  List<UserEntity> findByUserIdInAndIsDeletedFalse(List<String> userIds);
}