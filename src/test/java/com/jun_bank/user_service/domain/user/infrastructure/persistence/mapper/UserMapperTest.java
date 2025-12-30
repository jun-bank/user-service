package com.jun_bank.user_service.domain.user.infrastructure.persistence.mapper;

import com.jun_bank.user_service.domain.user.domain.model.User;
import com.jun_bank.user_service.domain.user.domain.model.UserStatus;
import com.jun_bank.user_service.domain.user.domain.model.vo.Email;
import com.jun_bank.user_service.domain.user.domain.model.vo.PhoneNumber;
import com.jun_bank.user_service.domain.user.domain.model.vo.UserId;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper 테스트")
class UserMapperTest {

  private UserMapper userMapper;

  @BeforeEach
  void setUp() {
    userMapper = new UserMapper();
  }

  // ========================================
  // 테스트 헬퍼 메서드
  // ========================================

  private User createNewUser() {
    return User.createBuilder()
        .email(Email.of("test@example.com"))
        .name("테스트")
        .phoneNumber(PhoneNumber.of("010-1234-5678"))
        .birthDate(LocalDate.of(1990, 1, 15))
        .build();
  }

  private User createExistingUser() {
    return User.restoreBuilder()
        .userId(UserId.of(UserId.generateId()))
        .email(Email.of("existing@example.com"))
        .name("기존사용자")
        .phoneNumber(PhoneNumber.of("010-9876-5432"))
        .birthDate(LocalDate.of(1985, 5, 20))
        .status(UserStatus.ACTIVE)
        .createdAt(LocalDateTime.now().minusDays(30))
        .updatedAt(LocalDateTime.now().minusDays(1))
        .createdBy("system")
        .updatedBy("system")
        .isDeleted(false)
        .build();
  }

  private User createDeletedUser() {
    return User.restoreBuilder()
        .userId(UserId.of(UserId.generateId()))
        .email(Email.of("deleted@example.com"))
        .name("탈퇴사용자")
        .phoneNumber(PhoneNumber.of("010-0000-0000"))
        .birthDate(LocalDate.of(1980, 12, 25))
        .status(UserStatus.DELETED)
        .createdAt(LocalDateTime.now().minusDays(100))
        .updatedAt(LocalDateTime.now().minusDays(1))
        .isDeleted(true)
        .deletedAt(LocalDateTime.now().minusDays(1))
        .deletedBy("admin")
        .build();
  }

  private UserEntity createUserEntity() {
    return UserEntity.of(
        UserId.generateId(),
        "entity@example.com",
        "엔티티사용자",
        "010-1111-2222",
        LocalDate.of(1995, 3, 10),
        UserStatus.ACTIVE
    );
  }

  // ========================================
  // toEntity 테스트
  // ========================================

  @Nested
  @DisplayName("toEntity")
  class ToEntityTest {

    @Test
    @DisplayName("신규 사용자를 엔티티로 변환하면 ID가 생성된다")
    void toEntity_NewUser_GeneratesId() {
      // given
      User newUser = createNewUser();
      assertThat(newUser.isNew()).isTrue();

      // when
      UserEntity entity = userMapper.toEntity(newUser);

      // then
      assertThat(entity.getUserId()).isNotNull();
      assertThat(entity.getUserId()).startsWith("USR-");
      assertThat(entity.getUserId()).hasSize(12);
      assertThat(entity.getEmail()).isEqualTo("test@example.com");
      assertThat(entity.getName()).isEqualTo("테스트");
      assertThat(entity.getPhoneNumber()).isEqualTo("010-1234-5678");
      assertThat(entity.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 15));
    }

    @Test
    @DisplayName("신규 사용자의 상태는 도메인에서 설정한 값을 따른다")
    void toEntity_NewUser_UsesStatusFromDomain() {
      // given
      User newUser = createNewUser();

      // when
      UserEntity entity = userMapper.toEntity(newUser);

      // then
      assertThat(entity.getStatus()).isEqualTo(newUser.getStatus());
    }

    @Test
    @DisplayName("기존 사용자를 엔티티로 변환하면 기존 ID가 유지된다")
    void toEntity_ExistingUser_KeepsId() {
      // given
      User existingUser = createExistingUser();
      String originalId = existingUser.getUserId().value();

      // when
      UserEntity entity = userMapper.toEntity(existingUser);

      // then
      assertThat(entity.getUserId()).isEqualTo(originalId);
      assertThat(entity.getEmail()).isEqualTo("existing@example.com");
      assertThat(entity.getName()).isEqualTo("기존사용자");
      assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("도메인의 모든 비즈니스 필드가 엔티티로 변환된다")
    void toEntity_AllBusinessFieldsMapped() {
      // given
      User user = createExistingUser();

      // when
      UserEntity entity = userMapper.toEntity(user);

      // then
      assertThat(entity.getUserId()).isEqualTo(user.getUserId().value());
      assertThat(entity.getEmail()).isEqualTo(user.getEmail().value());
      assertThat(entity.getName()).isEqualTo(user.getName());
      assertThat(entity.getPhoneNumber()).isEqualTo(user.getPhoneNumber().value());
      assertThat(entity.getBirthDate()).isEqualTo(user.getBirthDate());
      assertThat(entity.getStatus()).isEqualTo(user.getStatus());
    }

    @Test
    @DisplayName("엔티티 생성 시 BaseEntity 감사 필드는 null이다")
    void toEntity_AuditFieldsAreNull() {
      // given
      User user = createExistingUser();

      // when
      UserEntity entity = userMapper.toEntity(user);

      // then - JPA Auditing 전이므로 null
      assertThat(entity.getCreatedAt()).isNull();
      assertThat(entity.getUpdatedAt()).isNull();
      assertThat(entity.getCreatedBy()).isNull();
      assertThat(entity.getUpdatedBy()).isNull();
    }

    @Test
    @DisplayName("엔티티 생성 시 Soft Delete 필드는 기본값이다")
    void toEntity_SoftDeleteFieldsAreDefault() {
      // given
      User user = createExistingUser();

      // when
      UserEntity entity = userMapper.toEntity(user);

      // then
      assertThat(entity.getIsDeleted()).isFalse();
      assertThat(entity.getDeletedAt()).isNull();
      assertThat(entity.getDeletedBy()).isNull();
    }
  }

  // ========================================
  // toDomain 테스트
  // ========================================

  @Nested
  @DisplayName("toDomain")
  class ToDomainTest {

    @Test
    @DisplayName("엔티티를 도메인으로 변환한다")
    void toDomain_ConvertsAllFields() {
      // given
      UserEntity entity = createUserEntity();

      // when
      User domain = userMapper.toDomain(entity);

      // then
      assertThat(domain.getUserId().value()).isEqualTo(entity.getUserId());
      assertThat(domain.getEmail().value()).isEqualTo(entity.getEmail());
      assertThat(domain.getName()).isEqualTo(entity.getName());
      assertThat(domain.getPhoneNumber().value()).isEqualTo(entity.getPhoneNumber());
      assertThat(domain.getBirthDate()).isEqualTo(entity.getBirthDate());
      assertThat(domain.getStatus()).isEqualTo(entity.getStatus());
    }

    @Test
    @DisplayName("변환된 도메인은 신규 사용자가 아니다")
    void toDomain_NotNewUser() {
      // given
      UserEntity entity = createUserEntity();

      // when
      User domain = userMapper.toDomain(entity);

      // then
      assertThat(domain.isNew()).isFalse();
      assertThat(domain.getUserId()).isNotNull();
    }

    @Test
    @DisplayName("엔티티의 BaseEntity 감사 필드가 도메인으로 복원된다")
    void toDomain_RestoresAuditFields() {
      // given
      UserEntity entity = createUserEntity();
      // Note: 단위 테스트에서 BaseEntity 감사 필드는 JPA Auditing이 없어 null

      // when
      User domain = userMapper.toDomain(entity);

      // then
      // createdAt, updatedAt 등은 JPA Auditing에 의해 설정됨
      // 단위 테스트에서는 null일 수 있음
      assertThat(domain.getCreatedAt()).isEqualTo(entity.getCreatedAt());
      assertThat(domain.getUpdatedAt()).isEqualTo(entity.getUpdatedAt());
      assertThat(domain.getCreatedBy()).isEqualTo(entity.getCreatedBy());
      assertThat(domain.getUpdatedBy()).isEqualTo(entity.getUpdatedBy());
    }

    @Test
    @DisplayName("엔티티의 Soft Delete 필드가 도메인으로 복원된다")
    void toDomain_RestoresSoftDeleteFields() {
      // given
      UserEntity entity = createUserEntity();
      entity.delete("admin");

      // when
      User domain = userMapper.toDomain(entity);

      // then
      assertThat(domain.getIsDeleted()).isTrue();
      assertThat(domain.getDeletedAt()).isEqualTo(entity.getDeletedAt());
      assertThat(domain.getDeletedBy()).isEqualTo(entity.getDeletedBy());
    }

    @Test
    @DisplayName("삭제되지 않은 엔티티의 isDeleted는 false다")
    void toDomain_NotDeleted_IsDeletedFalse() {
      // given
      UserEntity entity = createUserEntity();

      // when
      User domain = userMapper.toDomain(entity);

      // then
      assertThat(domain.getIsDeleted()).isFalse();
      assertThat(domain.getDeletedAt()).isNull();
      assertThat(domain.getDeletedBy()).isNull();
    }
  }

  // ========================================
  // updateEntity 테스트
  // ========================================

  @Nested
  @DisplayName("updateEntity")
  class UpdateEntityTest {

    @Test
    @DisplayName("도메인의 변경사항이 엔티티에 반영된다")
    void updateEntity_UpdatesChangedFields() {
      // given
      UserEntity entity = createUserEntity();
      String originalId = entity.getUserId();
      String originalEmail = entity.getEmail();
      LocalDate originalBirthDate = entity.getBirthDate();

      User updatedDomain = User.restoreBuilder()
          .userId(UserId.of(originalId))
          .email(Email.of(originalEmail))
          .name("변경된이름")
          .phoneNumber(PhoneNumber.of("010-9999-8888"))
          .birthDate(originalBirthDate)
          .status(UserStatus.SUSPENDED)
          .isDeleted(false)
          .build();

      // when
      userMapper.updateEntity(entity, updatedDomain);

      // then - 변경된 필드
      assertThat(entity.getName()).isEqualTo("변경된이름");
      assertThat(entity.getPhoneNumber()).isEqualTo("010-9999-8888");
      assertThat(entity.getStatus()).isEqualTo(UserStatus.SUSPENDED);

      // then - 불변 필드는 변경되지 않음
      assertThat(entity.getUserId()).isEqualTo(originalId);
      assertThat(entity.getEmail()).isEqualTo(originalEmail);
      assertThat(entity.getBirthDate()).isEqualTo(originalBirthDate);
    }

    @Test
    @DisplayName("삭제된 도메인으로 업데이트하면 엔티티가 soft delete된다")
    void updateEntity_SoftDeletesWhenDomainDeleted() {
      // given
      UserEntity entity = createUserEntity();
      String userId = entity.getUserId();

      User deletedDomain = User.restoreBuilder()
          .userId(UserId.of(userId))
          .email(Email.of(entity.getEmail()))
          .name(entity.getName())
          .phoneNumber(PhoneNumber.of(entity.getPhoneNumber()))
          .birthDate(entity.getBirthDate())
          .status(UserStatus.DELETED)
          .isDeleted(true)
          .deletedBy("admin")
          .deletedAt(LocalDateTime.now())
          .build();

      // when
      userMapper.updateEntity(entity, deletedDomain);

      // then
      assertThat(entity.getStatus()).isEqualTo(UserStatus.DELETED);
      assertThat(entity.getIsDeleted()).isTrue();
      assertThat(entity.getDeletedBy()).isEqualTo("admin");
      assertThat(entity.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 삭제된 엔티티는 다시 삭제되지 않는다")
    void updateEntity_AlreadyDeletedEntity_NotDeletedAgain() {
      // given
      UserEntity entity = createUserEntity();
      entity.delete("firstDeleter");
      LocalDateTime firstDeletedAt = entity.getDeletedAt();
      String firstDeletedBy = entity.getDeletedBy();

      User deletedDomain = User.restoreBuilder()
          .userId(UserId.of(entity.getUserId()))
          .email(Email.of(entity.getEmail()))
          .name(entity.getName())
          .phoneNumber(PhoneNumber.of(entity.getPhoneNumber()))
          .birthDate(entity.getBirthDate())
          .status(UserStatus.DELETED)
          .isDeleted(true)
          .deletedBy("secondDeleter")
          .build();

      // when
      userMapper.updateEntity(entity, deletedDomain);

      // then - 이미 삭제되었으므로 첫 번째 삭제 정보 유지
      assertThat(entity.getDeletedBy()).isEqualTo(firstDeletedBy);
      assertThat(entity.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    @DisplayName("deletedBy가 없으면 userId로 대체된다")
    void updateEntity_UsesUserIdAsDeletedBy_WhenNull() {
      // given
      UserEntity entity = createUserEntity();
      String userId = entity.getUserId();

      User deletedDomain = User.restoreBuilder()
          .userId(UserId.of(userId))
          .email(Email.of(entity.getEmail()))
          .name(entity.getName())
          .phoneNumber(PhoneNumber.of(entity.getPhoneNumber()))
          .birthDate(entity.getBirthDate())
          .status(UserStatus.DELETED)
          .isDeleted(true)
          .deletedBy(null)  // null
          .build();

      // when
      userMapper.updateEntity(entity, deletedDomain);

      // then
      assertThat(entity.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("isDeleted가 false인 도메인은 엔티티를 삭제하지 않는다")
    void updateEntity_NotDeletedDomain_DoesNotDeleteEntity() {
      // given
      UserEntity entity = createUserEntity();

      User activeDomain = User.restoreBuilder()
          .userId(UserId.of(entity.getUserId()))
          .email(Email.of(entity.getEmail()))
          .name("변경된이름")
          .phoneNumber(PhoneNumber.of(entity.getPhoneNumber()))
          .birthDate(entity.getBirthDate())
          .status(UserStatus.ACTIVE)
          .isDeleted(false)
          .build();

      // when
      userMapper.updateEntity(entity, activeDomain);

      // then
      assertThat(entity.getIsDeleted()).isFalse();
      assertThat(entity.getDeletedAt()).isNull();
      assertThat(entity.getDeletedBy()).isNull();
    }

    @Test
    @DisplayName("상태가 INACTIVE로 변경되면 반영된다")
    void updateEntity_ToInactiveStatus() {
      // given
      UserEntity entity = createUserEntity();
      assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);

      User inactiveDomain = User.restoreBuilder()
          .userId(UserId.of(entity.getUserId()))
          .email(Email.of(entity.getEmail()))
          .name(entity.getName())
          .phoneNumber(PhoneNumber.of(entity.getPhoneNumber()))
          .birthDate(entity.getBirthDate())
          .status(UserStatus.INACTIVE)
          .isDeleted(false)
          .build();

      // when
      userMapper.updateEntity(entity, inactiveDomain);

      // then
      assertThat(entity.getStatus()).isEqualTo(UserStatus.INACTIVE);
      assertThat(entity.getStatus().isInactive()).isTrue();
    }
  }

  // ========================================
  // 양방향 변환 테스트
  // ========================================

  @Nested
  @DisplayName("양방향 변환")
  class BidirectionalConversionTest {

    @Test
    @DisplayName("Domain → Entity → Domain 변환 시 비즈니스 데이터가 유지된다")
    void domainToEntityToDomain_PreservesBusinessData() {
      // given
      User originalUser = createExistingUser();

      // when
      UserEntity entity = userMapper.toEntity(originalUser);
      User convertedUser = userMapper.toDomain(entity);

      // then - 비즈니스 필드 유지
      assertThat(convertedUser.getUserId().value()).isEqualTo(originalUser.getUserId().value());
      assertThat(convertedUser.getEmail().value()).isEqualTo(originalUser.getEmail().value());
      assertThat(convertedUser.getName()).isEqualTo(originalUser.getName());
      assertThat(convertedUser.getPhoneNumber().value()).isEqualTo(originalUser.getPhoneNumber().value());
      assertThat(convertedUser.getBirthDate()).isEqualTo(originalUser.getBirthDate());
      assertThat(convertedUser.getStatus()).isEqualTo(originalUser.getStatus());
    }

    @Test
    @DisplayName("신규 사용자 변환 후에도 ID가 일관된다")
    void newUserConversion_ConsistentId() {
      // given
      User newUser = createNewUser();

      // when
      UserEntity entity = userMapper.toEntity(newUser);
      User convertedUser = userMapper.toDomain(entity);

      // then
      assertThat(convertedUser.getUserId().value()).isEqualTo(entity.getUserId());
      assertThat(convertedUser.isNew()).isFalse();
    }

    @Test
    @DisplayName("삭제된 사용자 변환 시 Soft Delete 필드가 유지된다")
    void deletedUserConversion_PreservesSoftDeleteFields() {
      // given
      UserEntity entity = createUserEntity();
      entity.delete("admin");

      // when
      User domain = userMapper.toDomain(entity);

      // then
      assertThat(domain.getIsDeleted()).isTrue();
      assertThat(domain.getDeletedBy()).isEqualTo("admin");
      assertThat(domain.getDeletedAt()).isNotNull();
    }
  }

  // ========================================
  // 상태 전이 관련 테스트
  // ========================================

  @Nested
  @DisplayName("상태 전이 관련")
  class StatusTransitionTest {

    @Test
    @DisplayName("ACTIVE에서 SUSPENDED로 상태 변경")
    void updateEntity_ActiveToSuspended() {
      // given
      UserEntity entity = createUserEntity();
      assertThat(entity.getStatus().canTransitionTo(UserStatus.SUSPENDED)).isTrue();

      User suspendedDomain = User.restoreBuilder()
          .userId(UserId.of(entity.getUserId()))
          .email(Email.of(entity.getEmail()))
          .name(entity.getName())
          .phoneNumber(PhoneNumber.of(entity.getPhoneNumber()))
          .birthDate(entity.getBirthDate())
          .status(UserStatus.SUSPENDED)
          .isDeleted(false)
          .build();

      // when
      userMapper.updateEntity(entity, suspendedDomain);

      // then
      assertThat(entity.getStatus()).isEqualTo(UserStatus.SUSPENDED);
      assertThat(entity.getStatus().isSuspended()).isTrue();
      assertThat(entity.getStatus().canLogin()).isFalse();
    }

    @Test
    @DisplayName("모든 상태로 엔티티 생성 가능")
    void toEntity_AllStatusesSupported() {
      for (UserStatus status : UserStatus.values()) {
        // given
        User user = User.restoreBuilder()
            .userId(UserId.of(UserId.generateId()))
            .email(Email.of("test-" + status.name() + "@example.com"))
            .name("테스트")
            .phoneNumber(PhoneNumber.of("010-1234-5678"))
            .birthDate(LocalDate.of(1990, 1, 1))
            .status(status)
            .isDeleted(false)
            .build();

        // when
        UserEntity entity = userMapper.toEntity(user);

        // then
        assertThat(entity.getStatus()).isEqualTo(status);
      }
    }
  }
}