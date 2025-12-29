package com.jun_bank.user_service.domain.user.infrastructure.persistence;

import com.jun_bank.user_service.domain.user.application.port.out.UserRepository;
import com.jun_bank.user_service.domain.user.application.port.out.dto.UserSearchCondition;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.domain.model.User;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.UserEntity;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.jpa.UserJpaRepository;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.jpa.UserQueryRepositoryImpl;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * User Repository Adapter (Output Adapter)
 * <p>
 * Application Layer의 {@link UserRepository} Port를 구현합니다.
 * JPA Repository와 QueryDSL Repository를 조합하여 영속성 로직을 처리합니다.
 *
 * <h3>책임:</h3>
 * <ul>
 *   <li>Domain ↔ Entity 변환 (Mapper 사용)</li>
 *   <li>JPA/QueryDSL Repository 호출</li>
 *   <li>트랜잭션 관리</li>
 * </ul>
 *
 * <h3>저장 전략:</h3>
 * <ul>
 *   <li>신규 (userId == null): Mapper.toEntity() → save()</li>
 *   <li>수정 (userId 있음): findById() → Mapper.updateEntity() → 더티체킹</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRepositoryAdapter implements UserRepository {

  private final UserJpaRepository userJpaRepository;
  private final UserQueryRepositoryImpl userQueryRepository;
  private final UserMapper userMapper;

  // ========================================
  // 저장
  // ========================================

  @Override
  @Transactional
  public User save(User user) {
    UserEntity entity;

    if (user.isNew()) {
      // 신규 저장: Mapper에서 ID 생성 포함
      entity = userMapper.toEntity(user);
      entity = userJpaRepository.save(entity);
    } else {
      // 수정: 기존 엔티티 조회 후 더티체킹
      entity = userJpaRepository.findById(user.getUserId().value())
          .orElseThrow(() -> UserException.userNotFound(user.getUserId().value()));
      userMapper.updateEntity(entity, user);
      // save() 호출 없이 더티체킹으로 자동 업데이트
    }

    return userMapper.toDomain(entity);
  }

  // ========================================
  // 단건 조회
  // ========================================

  @Override
  public Optional<User> findById(String userId) {
    return userJpaRepository.findByUserIdAndIsDeletedFalse(userId)
        .map(userMapper::toDomain);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return userJpaRepository.findByEmailAndIsDeletedFalse(email)
        .map(userMapper::toDomain);
  }

  // ========================================
  // 존재 여부 확인
  // ========================================

  @Override
  public boolean existsByEmail(String email) {
    return userJpaRepository.existsByEmailAndIsDeletedFalse(email);
  }

  @Override
  public boolean existsByPhoneNumber(String phoneNumber) {
    return userJpaRepository.existsByPhoneNumberAndIsDeletedFalse(phoneNumber);
  }

  // ========================================
  // 동적 검색 (SearchCondition)
  // ========================================

  @Override
  public Page<User> search(UserSearchCondition condition, Pageable pageable) {
    return userQueryRepository.search(condition, pageable)
        .map(userMapper::toDomain);
  }

  @Override
  public List<User> searchAll(UserSearchCondition condition) {
    return userQueryRepository.searchAll(condition)
        .stream()
        .map(userMapper::toDomain)
        .toList();
  }

  @Override
  public long count(UserSearchCondition condition) {
    return userQueryRepository.count(condition);
  }

  // ========================================
  // 배치 조회
  // ========================================

  @Override
  public List<User> findByIds(List<String> userIds) {
    return userJpaRepository.findByUserIdInAndIsDeletedFalse(userIds)
        .stream()
        .map(userMapper::toDomain)
        .toList();
  }

  // ========================================
  // 삭제
  // ========================================

  @Override
  @Transactional
  public void delete(String userId, String deletedBy) {
    userJpaRepository.findByUserIdAndIsDeletedFalse(userId)
        .ifPresent(entity -> entity.delete(deletedBy));
  }
}