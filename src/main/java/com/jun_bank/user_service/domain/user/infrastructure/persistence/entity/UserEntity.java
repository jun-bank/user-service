package com.jun_bank.user_service.domain.user.infrastructure.persistence.entity;

import com.jun_bank.user_service.domain.user.domain.model.UserStatus;
import com.jun_bank.user_service.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 사용자 JPA 엔티티
 * <p>
 * 도메인 모델 {@link com.jun_bank.user_service.domain.user.domain.model.User}와
 * DB 테이블을 매핑합니다.
 *
 * <h3>테이블 정보:</h3>
 * <ul>
 *   <li>테이블명: users</li>
 *   <li>스키마: user_db</li>
 * </ul>
 *
 * <h3>인덱스:</h3>
 * <ul>
 *   <li>idx_user_email: email (유니크)</li>
 *   <li>idx_user_phone: phone_number</li>
 *   <li>idx_user_status: status</li>
 *   <li>idx_user_is_deleted: is_deleted</li>
 * </ul>
 *
 * @see BaseEntity
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_phone", columnList = "phone_number"),
        @Index(name = "idx_user_status", columnList = "status"),
        @Index(name = "idx_user_is_deleted", columnList = "is_deleted")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {

  /**
   * 사용자 ID (PK)
   * <p>USR-xxxxxxxx 형식</p>
   */
  @Id
  @Column(name = "user_id", length = 12, nullable = false)
  private String userId;

  /**
   * 이메일 (유니크)
   */
  @Column(name = "email", length = 255, nullable = false, unique = true)
  private String email;

  /**
   * 이름
   */
  @Column(name = "name", length = 50, nullable = false)
  private String name;

  /**
   * 전화번호
   * <p>010-1234-5678 형식으로 저장</p>
   */
  @Column(name = "phone_number", length = 20, nullable = false)
  private String phoneNumber;

  /**
   * 생년월일
   */
  @Column(name = "birth_date", nullable = false)
  private LocalDate birthDate;

  /**
   * 사용자 상태
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private UserStatus status;

  // ========================================
  // 생성자 (Builder 대신 정적 팩토리 메서드 사용)
  // ========================================

  /**
   * 전체 필드 생성자 (Mapper에서 사용)
   */
  private UserEntity(String userId, String email, String name,
      String phoneNumber, LocalDate birthDate, UserStatus status) {
    this.userId = userId;
    this.email = email;
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.birthDate = birthDate;
    this.status = status;
  }

  /**
   * 신규 엔티티 생성용 정적 팩토리 메서드
   *
   * @param userId      사용자 ID (생성된 ID)
   * @param email       이메일
   * @param name        이름
   * @param phoneNumber 전화번호
   * @param birthDate   생년월일
   * @param status      상태
   * @return UserEntity
   */
  public static UserEntity of(String userId, String email, String name,
      String phoneNumber, LocalDate birthDate, UserStatus status) {
    return new UserEntity(userId, email, name, phoneNumber, birthDate, status);
  }

  // ========================================
  // 업데이트 메서드
  // ========================================

  /**
   * 프로필 정보 업데이트
   * <p>
   * 변경 가능한 필드만 업데이트합니다.
   * 불변 필드(userId, email, birthDate)는 변경하지 않습니다.
   * </p>
   *
   * @param name        새 이름
   * @param phoneNumber 새 전화번호
   * @param status      새 상태
   */
  public void update(String name, String phoneNumber, UserStatus status) {
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.status = status;
  }

  /**
   * 상태만 업데이트
   *
   * @param status 새 상태
   */
  public void updateStatus(UserStatus status) {
    this.status = status;
  }
}