package com.jun_bank.user_service.domain.user.infrastructure.persistence.entity;

import com.jun_bank.user_service.domain.user.domain.model.UserStatus;
import com.jun_bank.user_service.domain.user.domain.model.vo.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserEntity 테스트")
class UserEntityTest {

  // ========================================
  // 헬퍼 메서드
  // ========================================

  private UserEntity createEntity() {
    return UserEntity.of(
        UserId.generateId(),
        "test@example.com",
        "테스트사용자",
        "010-1234-5678",
        LocalDate.of(1990, 5, 15),
        UserStatus.ACTIVE
    );
  }

  // ========================================
  // 생성 테스트
  // ========================================

  @Nested
  @DisplayName("of 팩토리 메서드")
  class OfTest {

    @Test
    @DisplayName("정적 팩토리 메서드로 엔티티를 생성한다")
    void of_CreatesEntity() {
      // given
      String userId = UserId.generateId();
      String email = "factory@example.com";
      String name = "팩토리테스트";
      String phoneNumber = "010-9999-8888";
      LocalDate birthDate = LocalDate.of(1985, 3, 20);
      UserStatus status = UserStatus.INACTIVE;

      // when
      UserEntity entity = UserEntity.of(userId, email, name, phoneNumber, birthDate, status);

      // then
      assertThat(entity.getUserId()).isEqualTo(userId);
      assertThat(entity.getEmail()).isEqualTo(email);
      assertThat(entity.getName()).isEqualTo(name);
      assertThat(entity.getPhoneNumber()).isEqualTo(phoneNumber);
      assertThat(entity.getBirthDate()).isEqualTo(birthDate);
      assertThat(entity.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("모든 UserStatus 값으로 엔티티를 생성할 수 있다")
    void of_WithAllStatuses() {
      for (UserStatus status : UserStatus.values()) {
        // when
        UserEntity entity = UserEntity.of(
            UserId.generateId(),
            "status-" + status.name().toLowerCase() + "@example.com",
            "테스트",
            "010-1111-2222",
            LocalDate.of(2000, 1, 1),
            status
        );

        // then
        assertThat(entity.getStatus()).isEqualTo(status);
      }
    }

    @Test
    @DisplayName("생성 시 BaseEntity 감사 필드는 null이다 (JPA Auditing 전)")
    void of_AuditFieldsAreNull() {
      // when
      UserEntity entity = createEntity();

      // then - JPA Auditing 적용 전이므로 null
      assertThat(entity.getCreatedAt()).isNull();
      assertThat(entity.getUpdatedAt()).isNull();
      assertThat(entity.getCreatedBy()).isNull();
      assertThat(entity.getUpdatedBy()).isNull();
    }

    @Test
    @DisplayName("생성 시 Soft Delete 필드는 기본값이다")
    void of_SoftDeleteFieldsAreDefault() {
      // when
      UserEntity entity = createEntity();

      // then
      assertThat(entity.getIsDeleted()).isFalse();
      assertThat(entity.getDeletedAt()).isNull();
      assertThat(entity.getDeletedBy()).isNull();
    }
  }

  // ========================================
  // update 메서드 테스트
  // ========================================

  @Nested
  @DisplayName("update")
  class UpdateTest {

    @Test
    @DisplayName("이름, 전화번호, 상태를 업데이트한다")
    void update_ChangesFields() {
      // given
      UserEntity entity = createEntity();
      String originalId = entity.getUserId();
      String originalEmail = entity.getEmail();
      LocalDate originalBirthDate = entity.getBirthDate();

      // when
      entity.update("변경된이름", "010-9999-0000", UserStatus.SUSPENDED);

      // then - 변경된 필드
      assertThat(entity.getName()).isEqualTo("변경된이름");
      assertThat(entity.getPhoneNumber()).isEqualTo("010-9999-0000");
      assertThat(entity.getStatus()).isEqualTo(UserStatus.SUSPENDED);

      // then - 불변 필드는 그대로
      assertThat(entity.getUserId()).isEqualTo(originalId);
      assertThat(entity.getEmail()).isEqualTo(originalEmail);
      assertThat(entity.getBirthDate()).isEqualTo(originalBirthDate);
    }

    @Test
    @DisplayName("같은 값으로 업데이트해도 문제없다")
    void update_SameValues_NoError() {
      // given
      UserEntity entity = createEntity();
      String originalName = entity.getName();
      String originalPhone = entity.getPhoneNumber();
      UserStatus originalStatus = entity.getStatus();

      // when
      entity.update(originalName, originalPhone, originalStatus);

      // then
      assertThat(entity.getName()).isEqualTo(originalName);
      assertThat(entity.getPhoneNumber()).isEqualTo(originalPhone);
      assertThat(entity.getStatus()).isEqualTo(originalStatus);
    }

    @Test
    @DisplayName("DELETED 상태로 업데이트해도 isDeleted는 변경되지 않는다")
    void update_ToDeletedStatus_DoesNotChangeIsDeleted() {
      // given
      UserEntity entity = createEntity();

      // when
      entity.update(entity.getName(), entity.getPhoneNumber(), UserStatus.DELETED);

      // then
      assertThat(entity.getStatus()).isEqualTo(UserStatus.DELETED);
      assertThat(entity.getIsDeleted()).isFalse(); // BaseEntity 필드는 변경 안 됨
    }
  }

  // ========================================
  // updateStatus 메서드 테스트
  // ========================================

  @Nested
  @DisplayName("updateStatus")
  class UpdateStatusTest {

    @Test
    @DisplayName("상태만 업데이트한다")
    void updateStatus_ChangesOnlyStatus() {
      // given
      UserEntity entity = createEntity();
      String originalName = entity.getName();
      String originalPhone = entity.getPhoneNumber();

      // when
      entity.updateStatus(UserStatus.INACTIVE);

      // then
      assertThat(entity.getStatus()).isEqualTo(UserStatus.INACTIVE);
      assertThat(entity.getName()).isEqualTo(originalName);
      assertThat(entity.getPhoneNumber()).isEqualTo(originalPhone);
    }

    @Test
    @DisplayName("모든 상태로 변경 가능하다")
    void updateStatus_ToAllStatuses() {
      for (UserStatus targetStatus : UserStatus.values()) {
        // given
        UserEntity entity = createEntity();

        // when
        entity.updateStatus(targetStatus);

        // then
        assertThat(entity.getStatus()).isEqualTo(targetStatus);
      }
    }

    @Test
    @DisplayName("ACTIVE에서 SUSPENDED로 변경")
    void updateStatus_ActiveToSuspended() {
      // given
      UserEntity entity = createEntity();
      assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);

      // when
      entity.updateStatus(UserStatus.SUSPENDED);

      // then
      assertThat(entity.getStatus()).isEqualTo(UserStatus.SUSPENDED);
      assertThat(entity.getStatus().canLogin()).isFalse();
      assertThat(entity.getStatus().canModifyProfile()).isFalse();
    }

    @Test
    @DisplayName("ACTIVE에서 INACTIVE로 변경 (휴면)")
    void updateStatus_ActiveToInactive() {
      // given
      UserEntity entity = createEntity();

      // when
      entity.updateStatus(UserStatus.INACTIVE);

      // then
      assertThat(entity.getStatus()).isEqualTo(UserStatus.INACTIVE);
      assertThat(entity.getStatus().canLogin()).isFalse();
      assertThat(entity.getStatus().canModifyProfile()).isTrue();
    }
  }

  // ========================================
  // BaseEntity - Soft Delete 테스트
  // ========================================

  @Nested
  @DisplayName("BaseEntity - Soft Delete")
  class SoftDeleteTest {

    @Test
    @DisplayName("delete() 메서드로 soft delete를 수행한다")
    void delete_SetsDeleteFields() {
      // given
      UserEntity entity = createEntity();
      assertThat(entity.getIsDeleted()).isFalse();

      // when
      entity.delete("admin");

      // then
      assertThat(entity.getIsDeleted()).isTrue();
      assertThat(entity.getDeletedBy()).isEqualTo("admin");
      assertThat(entity.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("restore() 메서드로 삭제를 복원한다")
    void restore_ClearsDeleteFields() {
      // given
      UserEntity entity = createEntity();
      entity.delete("admin");
      assertThat(entity.getIsDeleted()).isTrue();

      // when
      entity.restore();

      // then
      assertThat(entity.getIsDeleted()).isFalse();
      assertThat(entity.getDeletedBy()).isNull();
      assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("delete() 시 deletedAt은 현재 시간으로 설정된다")
    void delete_SetsCurrentTime() {
      // given
      UserEntity entity = createEntity();
      LocalDateTime before = LocalDateTime.now();

      // when
      entity.delete("admin");

      // then
      assertThat(entity.getDeletedAt()).isAfterOrEqualTo(before);
      assertThat(entity.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("여러 번 delete() 호출하면 마지막 값이 적용된다")
    void delete_MultipleCalls_LastValueApplied() {
      // given
      UserEntity entity = createEntity();

      // when
      entity.delete("user1");
      LocalDateTime firstDeletedAt = entity.getDeletedAt();

      // 시간 차이를 위해 잠시 대기
      try { Thread.sleep(10); } catch (InterruptedException ignored) {}

      entity.delete("user2");

      // then
      assertThat(entity.getDeletedBy()).isEqualTo("user2");
      assertThat(entity.getDeletedAt()).isAfterOrEqualTo(firstDeletedAt);
    }

    @Test
    @DisplayName("restore() 후 다시 delete() 가능하다")
    void restore_ThenDelete_Works() {
      // given
      UserEntity entity = createEntity();
      entity.delete("admin");
      entity.restore();
      assertThat(entity.getIsDeleted()).isFalse();

      // when
      entity.delete("newAdmin");

      // then
      assertThat(entity.getIsDeleted()).isTrue();
      assertThat(entity.getDeletedBy()).isEqualTo("newAdmin");
    }

    @Test
    @DisplayName("delete()는 status를 변경하지 않는다")
    void delete_DoesNotChangeStatus() {
      // given
      UserEntity entity = createEntity();
      assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);

      // when
      entity.delete("admin");

      // then
      assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE); // 변경 안 됨
      assertThat(entity.getIsDeleted()).isTrue();
    }
  }

  // ========================================
  // 필드 검증 테스트
  // ========================================

  @Nested
  @DisplayName("필드 검증")
  class FieldValidationTest {

    @Test
    @DisplayName("userId 형식이 올바르다 (USR-XXXXXXXX)")
    void userId_HasCorrectFormat() {
      // given & when
      UserEntity entity = createEntity();

      // then
      assertThat(entity.getUserId()).startsWith("USR-");
      assertThat(entity.getUserId()).hasSize(12);
    }

    @Test
    @DisplayName("모든 필수 필드가 설정된다")
    void allRequiredFields_AreSet() {
      // given & when
      UserEntity entity = createEntity();

      // then
      assertThat(entity.getUserId()).isNotNull().isNotEmpty();
      assertThat(entity.getEmail()).isNotNull().isNotEmpty();
      assertThat(entity.getName()).isNotNull().isNotEmpty();
      assertThat(entity.getPhoneNumber()).isNotNull().isNotEmpty();
      assertThat(entity.getBirthDate()).isNotNull();
      assertThat(entity.getStatus()).isNotNull();
      assertThat(entity.getIsDeleted()).isNotNull();
    }
  }

  // ========================================
  // 상태와 Soft Delete 조합 테스트
  // ========================================

  @Nested
  @DisplayName("상태와 Soft Delete 조합")
  class StatusAndSoftDeleteTest {

    @Test
    @DisplayName("DELETED 상태와 isDeleted=true는 별개의 필드다")
    void deletedStatus_And_IsDeleted_AreSeparate() {
      // given
      UserEntity entity = createEntity();

      // when - 상태만 DELETED로 변경
      entity.updateStatus(UserStatus.DELETED);

      // then
      assertThat(entity.getStatus()).isEqualTo(UserStatus.DELETED);
      assertThat(entity.getIsDeleted()).isFalse(); // BaseEntity 필드는 변경 안 됨
      assertThat(entity.getDeletedAt()).isNull();
      assertThat(entity.getDeletedBy()).isNull();
    }

    @Test
    @DisplayName("완전한 삭제 처리는 status와 isDeleted 모두 변경해야 한다")
    void fullDeleteProcess() {
      // given
      UserEntity entity = createEntity();

      // when - 도메인 레벨 삭제 처리
      entity.updateStatus(UserStatus.DELETED);
      entity.delete("admin");

      // then
      assertThat(entity.getStatus()).isEqualTo(UserStatus.DELETED);
      assertThat(entity.getStatus().isDeleted()).isTrue();
      assertThat(entity.getIsDeleted()).isTrue();
      assertThat(entity.getDeletedBy()).isEqualTo("admin");
      assertThat(entity.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("isDeleted=true여도 status는 ACTIVE일 수 있다 (비정상 케이스)")
    void isDeletedTrue_StatusActive_Possible() {
      // given
      UserEntity entity = createEntity();
      assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);

      // when - BaseEntity.delete()만 호출
      entity.delete("admin");

      // then - status와 isDeleted가 불일치 (이런 상태는 피해야 함)
      assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(entity.getIsDeleted()).isTrue();
    }
  }

  // ========================================
  // UserStatus 메서드 테스트 (통합)
  // ========================================

  @Nested
  @DisplayName("UserStatus 메서드 통합")
  class UserStatusMethodsTest {

    @Test
    @DisplayName("ACTIVE 상태는 로그인과 프로필 수정이 가능하다")
    void activeStatus_AllActionsAllowed() {
      // given
      UserEntity entity = createEntity();

      // then
      assertThat(entity.getStatus().isActive()).isTrue();
      assertThat(entity.getStatus().canLogin()).isTrue();
      assertThat(entity.getStatus().canModifyProfile()).isTrue();
    }

    @Test
    @DisplayName("INACTIVE 상태는 로그인 불가, 프로필 수정 가능")
    void inactiveStatus_PartialActionsAllowed() {
      // given
      UserEntity entity = createEntity();
      entity.updateStatus(UserStatus.INACTIVE);

      // then
      assertThat(entity.getStatus().isInactive()).isTrue();
      assertThat(entity.getStatus().canLogin()).isFalse();
      assertThat(entity.getStatus().canModifyProfile()).isTrue();
    }

    @Test
    @DisplayName("SUSPENDED 상태는 모든 액션 불가")
    void suspendedStatus_NoActionsAllowed() {
      // given
      UserEntity entity = createEntity();
      entity.updateStatus(UserStatus.SUSPENDED);

      // then
      assertThat(entity.getStatus().isSuspended()).isTrue();
      assertThat(entity.getStatus().canLogin()).isFalse();
      assertThat(entity.getStatus().canModifyProfile()).isFalse();
    }

    @Test
    @DisplayName("DELETED 상태는 최종 상태로 전환 불가")
    void deletedStatus_NoTransitionsAllowed() {
      // given
      UserEntity entity = createEntity();
      entity.updateStatus(UserStatus.DELETED);

      // then
      assertThat(entity.getStatus().isDeleted()).isTrue();
      assertThat(entity.getStatus().getAllowedTransitions()).isEmpty();
      assertThat(entity.getStatus().canTransitionTo(UserStatus.ACTIVE)).isFalse();
    }

    @Test
    @DisplayName("ACTIVE에서 허용된 상태 전이 확인")
    void activeStatus_AllowedTransitions() {
      // given
      UserEntity entity = createEntity();
      UserStatus status = entity.getStatus();

      // then
      assertThat(status.canTransitionTo(UserStatus.INACTIVE)).isTrue();
      assertThat(status.canTransitionTo(UserStatus.SUSPENDED)).isTrue();
      assertThat(status.canTransitionTo(UserStatus.DELETED)).isTrue();
      assertThat(status.canTransitionTo(UserStatus.ACTIVE)).isFalse(); // 자기 자신
    }
  }
}