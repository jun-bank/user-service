package com.jun_bank.user_service.domain.user.infrastructure.persistence.mapper;

import com.jun_bank.user_service.domain.user.domain.model.User;
import com.jun_bank.user_service.domain.user.domain.model.vo.Email;
import com.jun_bank.user_service.domain.user.domain.model.vo.PhoneNumber;
import com.jun_bank.user_service.domain.user.domain.model.vo.UserId;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * User Entity ↔ Domain 변환 매퍼
 * <p>
 * JPA Entity와 도메인 모델 간의 변환을 담당합니다.
 * 양방향 변환을 제공하여 영속성 레이어와 도메인 레이어를 분리합니다.
 *
 * <h3>메서드 역할:</h3>
 * <ul>
 *   <li>{@link #toEntity(User)}: 신규 저장용 (ID 생성 포함)</li>
 *   <li>{@link #updateEntity(UserEntity, User)}: 수정용 (더티체킹, ID 유지)</li>
 *   <li>{@link #toDomain(UserEntity)}: 조회 결과 → 도메인 복원</li>
 * </ul>
 *
 * <h3>ID 생성 책임:</h3>
 * <p>
 * 신규 사용자 저장 시 {@link UserId#generateId()}를 호출하여 ID를 생성합니다.
 * 이는 Mapper의 변환 책임에 포함됩니다.
 * </p>
 */
@Component
public class UserMapper {

  /**
   * 도메인 모델을 JPA 엔티티로 변환 (신규 저장용)
   * <p>
   * 신규 사용자(userId가 null)인 경우 새 ID를 생성합니다.
   * 이 메서드는 신규 저장에만 사용하며, 수정 시에는 {@link #updateEntity}를 사용합니다.
   * </p>
   *
   * @param domain User 도메인 모델
   * @return UserEntity (신규 ID 포함)
   */
  public UserEntity toEntity(User domain) {
    // 신규 사용자인 경우 ID 생성
    String userId = domain.isNew()
        ? UserId.generateId()
        : domain.getUserId().value();

    return UserEntity.of(
        userId,
        domain.getEmail().value(),
        domain.getName(),
        domain.getPhoneNumber().value(),
        domain.getBirthDate(),
        domain.getStatus()
    );
  }

  /**
   * JPA 엔티티를 도메인 모델로 변환
   * <p>
   * BaseEntity의 감사 필드를 포함하여 완전한 도메인 객체를 복원합니다.
   * </p>
   *
   * @param entity UserEntity
   * @return User 도메인 모델 (모든 필드 복원)
   */
  public User toDomain(UserEntity entity) {
    return User.restoreBuilder()
        .userId(UserId.of(entity.getUserId()))
        .email(Email.of(entity.getEmail()))
        .name(entity.getName())
        .phoneNumber(PhoneNumber.of(entity.getPhoneNumber()))
        .birthDate(entity.getBirthDate())
        .status(entity.getStatus())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .createdBy(entity.getCreatedBy())
        .updatedBy(entity.getUpdatedBy())
        .deletedAt(entity.getDeletedAt())
        .deletedBy(entity.getDeletedBy())
        .isDeleted(entity.getIsDeleted())
        .build();
  }

  /**
   * 도메인 모델의 변경사항을 기존 엔티티에 반영 (수정용)
   * <p>
   * 기존 엔티티를 업데이트하여 JPA 변경 감지(dirty checking)를 활용합니다.
   * 불변 필드(userId, email, birthDate)는 변경하지 않습니다.
   * </p>
   *
   * <h4>변경 가능 필드:</h4>
   * <ul>
   *   <li>name: 이름</li>
   *   <li>phoneNumber: 전화번호</li>
   *   <li>status: 상태</li>
   * </ul>
   *
   * <h4>Soft Delete 처리:</h4>
   * <p>
   * 도메인 상태가 DELETED이고 엔티티가 아직 삭제되지 않았다면
   * BaseEntity.delete()를 호출합니다.
   * </p>
   *
   * @param entity 업데이트할 엔티티 (영속 상태)
   * @param domain 변경된 도메인 모델
   */
  public void updateEntity(UserEntity entity, User domain) {
    // 변경 가능한 필드 업데이트
    entity.update(
        domain.getName(),
        domain.getPhoneNumber().value(),
        domain.getStatus()
    );

    // Soft Delete 처리
    if (domain.isDeleted() && !entity.getIsDeleted()) {
      String deletedBy = domain.getDeletedBy() != null
          ? domain.getDeletedBy()
          : domain.getUserId().value();  // 본인 탈퇴인 경우
      entity.delete(deletedBy);
    }
  }
}