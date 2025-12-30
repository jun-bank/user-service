package com.jun_bank.user_service.domain.user.domain.model;

import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.domain.model.vo.Email;
import com.jun_bank.user_service.domain.user.domain.model.vo.PhoneNumber;
import com.jun_bank.user_service.domain.user.domain.model.vo.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User 도메인 테스트")
class UserTest {

  private static final String VALID_EMAIL = "test@example.com";
  private static final String VALID_PHONE = "010-1234-5678";
  private static final String VALID_NAME = "홍길동";
  private static final LocalDate VALID_BIRTH_DATE = LocalDate.of(1990, 1, 15);

  @Nested
  @DisplayName("createBuilder - 신규 사용자 생성")
  class CreateBuilder {

    @Test
    @DisplayName("유효한 정보로 생성 성공")
    void createSuccess() {
      // when
      User user = User.createBuilder()
          .email(Email.of(VALID_EMAIL))
          .name(VALID_NAME)
          .phoneNumber(PhoneNumber.of(VALID_PHONE))
          .birthDate(VALID_BIRTH_DATE)
          .build();

      // then
      assertThat(user.isNew()).isTrue();
      assertThat(user.getUserId()).isNull();
      assertThat(user.getEmail().value()).isEqualTo(VALID_EMAIL);
      assertThat(user.getName()).isEqualTo(VALID_NAME);
      assertThat(user.getPhoneNumber().value()).isEqualTo(VALID_PHONE);
      assertThat(user.getBirthDate()).isEqualTo(VALID_BIRTH_DATE);
      assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(user.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("이메일 누락 시 실패")
    void failWithoutEmail() {
      assertThatThrownBy(() -> User.createBuilder()
          .name(VALID_NAME)
          .phoneNumber(PhoneNumber.of(VALID_PHONE))
          .birthDate(VALID_BIRTH_DATE)
          .build())
          .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("이름이 2자 미만이면 실패")
    void failWithShortName() {
      assertThatThrownBy(() -> User.createBuilder()
          .email(Email.of(VALID_EMAIL))
          .name("홍")
          .phoneNumber(PhoneNumber.of(VALID_PHONE))
          .birthDate(VALID_BIRTH_DATE)
          .build())
          .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("이름이 50자 초과하면 실패")
    void failWithLongName() {
      String longName = "가".repeat(51);
      assertThatThrownBy(() -> User.createBuilder()
          .email(Email.of(VALID_EMAIL))
          .name(longName)
          .phoneNumber(PhoneNumber.of(VALID_PHONE))
          .birthDate(VALID_BIRTH_DATE)
          .build())
          .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("이름이 null이면 실패")
    void failWithNullName() {
      assertThatThrownBy(() -> User.createBuilder()
          .email(Email.of(VALID_EMAIL))
          .name(null)
          .phoneNumber(PhoneNumber.of(VALID_PHONE))
          .birthDate(VALID_BIRTH_DATE)
          .build())
          .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("이름이 공백이면 실패")
    void failWithBlankName() {
      assertThatThrownBy(() -> User.createBuilder()
          .email(Email.of(VALID_EMAIL))
          .name("   ")
          .phoneNumber(PhoneNumber.of(VALID_PHONE))
          .birthDate(VALID_BIRTH_DATE)
          .build())
          .isInstanceOf(UserException.class);
    }
  }

  @Nested
  @DisplayName("restoreBuilder - DB에서 복원")
  class RestoreBuilder {

    @Test
    @DisplayName("모든 필드 복원 성공")
    void restoreSuccess() {
      // given
      String generatedId = UserId.generateId();

      // when
      User user = User.restoreBuilder()
          .userId(UserId.of(generatedId))
          .email(Email.of(VALID_EMAIL))
          .name(VALID_NAME)
          .phoneNumber(PhoneNumber.of(VALID_PHONE))
          .birthDate(VALID_BIRTH_DATE)
          .status(UserStatus.ACTIVE)
          .isDeleted(false)
          .build();

      // then
      assertThat(user.isNew()).isFalse();
      assertThat(user.getUserId().value()).isEqualTo(generatedId);
      assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("삭제된 사용자 복원")
    void restoreDeletedUser() {
      // given
      String generatedId = UserId.generateId();

      // when
      User user = User.restoreBuilder()
          .userId(UserId.of(generatedId))
          .email(Email.of(VALID_EMAIL))
          .name(VALID_NAME)
          .phoneNumber(PhoneNumber.of(VALID_PHONE))
          .birthDate(VALID_BIRTH_DATE)
          .status(UserStatus.DELETED)
          .isDeleted(true)
          .deletedBy("admin")
          .build();

      // then
      assertThat(user.isDeleted()).isTrue();
      assertThat(user.getIsDeleted()).isTrue();
      assertThat(user.getDeletedBy()).isEqualTo("admin");
    }
  }

  @Nested
  @DisplayName("updateProfile - 프로필 수정")
  class UpdateProfile {

    @Test
    @DisplayName("ACTIVE 상태에서 수정 성공")
    void updateSuccessWhenActive() {
      // given
      User user = createActiveUserWithId();
      String newName = "김철수";
      PhoneNumber newPhone = PhoneNumber.of("010-9876-5432");

      // when
      user.updateProfile(newName, newPhone);

      // then
      assertThat(user.getName()).isEqualTo(newName);
      assertThat(user.getPhoneNumber().value()).isEqualTo("010-9876-5432");
    }

    @Test
    @DisplayName("INACTIVE 상태에서도 수정 가능")
    void updateSuccessWhenInactive() {
      // given
      User user = createUserWithStatus(UserStatus.INACTIVE);

      // when
      user.updateProfile("새이름", PhoneNumber.of("010-1111-2222"));

      // then
      assertThat(user.getName()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("DELETED 상태에서 수정 실패")
    void updateFailWhenDeleted() {
      // given
      User user = createUserWithStatus(UserStatus.DELETED);

      // when & then
      assertThatThrownBy(() -> user.updateProfile("새이름", PhoneNumber.of(VALID_PHONE)))
          .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("SUSPENDED 상태에서 수정 실패")
    void updateFailWhenSuspended() {
      // given
      User user = createUserWithStatus(UserStatus.SUSPENDED);

      // when & then
      assertThatThrownBy(() -> user.updateProfile("새이름", PhoneNumber.of(VALID_PHONE)))
          .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("이름 검증 실패")
    void updateFailWithInvalidName() {
      // given
      User user = createActiveUserWithId();

      // when & then
      assertThatThrownBy(() -> user.updateProfile("홍", PhoneNumber.of(VALID_PHONE)))
          .isInstanceOf(UserException.class);
    }
  }

  @Nested
  @DisplayName("withdraw - 탈퇴 처리")
  class Withdraw {

    @Test
    @DisplayName("ACTIVE 상태에서 탈퇴 성공")
    void withdrawFromActive() {
      // given
      User user = createActiveUserWithId();

      // when
      user.withdraw();

      // then
      assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
      assertThat(user.isDeleted()).isTrue();
      assertThat(user.getIsDeleted()).isTrue();
      assertThat(user.getDeletedAt()).isNotNull();
      assertThat(user.canRollback()).isTrue();
    }

    @Test
    @DisplayName("삭제자 지정하여 탈퇴")
    void withdrawWithDeletedBy() {
      // given
      User user = createActiveUserWithId();
      String deletedBy = "admin";

      // when
      user.withdraw(deletedBy);

      // then
      assertThat(user.getDeletedBy()).isEqualTo(deletedBy);
    }

    @Test
    @DisplayName("INACTIVE 상태에서 탈퇴 성공")
    void withdrawFromInactive() {
      // given
      User user = createUserWithStatus(UserStatus.INACTIVE);

      // when
      user.withdraw();

      // then
      assertThat(user.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("이미 DELETED 상태에서 탈퇴 시도 실패")
    void withdrawFailWhenAlreadyDeleted() {
      // given
      User user = createUserWithStatus(UserStatus.DELETED);

      // when & then
      assertThatThrownBy(user::withdraw)
          .isInstanceOf(UserException.class);
    }
  }

  @Nested
  @DisplayName("cancelWithdrawal - 탈퇴 취소 (롤백)")
  class CancelWithdrawal {

    @Test
    @DisplayName("탈퇴 후 취소 성공")
    void cancelWithdrawalSuccess() {
      // given
      User user = createActiveUserWithId();
      user.withdraw();

      // when
      user.cancelWithdrawal();

      // then
      assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(user.isDeleted()).isFalse();
      assertThat(user.getIsDeleted()).isFalse();
      assertThat(user.getDeletedAt()).isNull();
      assertThat(user.getDeletedBy()).isNull();
      assertThat(user.canRollback()).isFalse();
    }

    @Test
    @DisplayName("INACTIVE에서 탈퇴 후 취소 시 INACTIVE로 복원")
    void cancelWithdrawalRestoresInactive() {
      // given
      User user = createUserWithStatus(UserStatus.INACTIVE);
      user.withdraw();

      // when
      user.cancelWithdrawal();

      // then
      assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }
  }

  @Nested
  @DisplayName("clearPreviousStatus - 이전 상태 초기화")
  class ClearPreviousStatus {

    @Test
    @DisplayName("롤백 데이터 정리")
    void clearPreviousStatusSuccess() {
      // given
      User user = createActiveUserWithId();
      user.withdraw();
      assertThat(user.canRollback()).isTrue();

      // when
      user.clearPreviousStatus();

      // then
      assertThat(user.canRollback()).isFalse();
    }
  }

  @Nested
  @DisplayName("상태 확인 메서드")
  class StatusCheck {

    @Test
    @DisplayName("isNew - userId가 null이면 true")
    void isNewWhenUserIdNull() {
      User user = User.createBuilder()
          .email(Email.of(VALID_EMAIL))
          .name(VALID_NAME)
          .phoneNumber(PhoneNumber.of(VALID_PHONE))
          .birthDate(VALID_BIRTH_DATE)
          .build();

      assertThat(user.isNew()).isTrue();
    }

    @Test
    @DisplayName("isNew - userId가 있으면 false")
    void isNewWhenUserIdExists() {
      User user = createActiveUserWithId();
      assertThat(user.isNew()).isFalse();
    }

    @Test
    @DisplayName("isActive")
    void isActive() {
      User user = createUserWithStatus(UserStatus.ACTIVE);
      assertThat(user.isActive()).isTrue();
      assertThat(user.isDeleted()).isFalse();
      assertThat(user.isSuspended()).isFalse();
      assertThat(user.isInactive()).isFalse();
    }

    @Test
    @DisplayName("isInactive")
    void isInactive() {
      User user = createUserWithStatus(UserStatus.INACTIVE);
      assertThat(user.isInactive()).isTrue();
    }

    @Test
    @DisplayName("isSuspended")
    void isSuspended() {
      User user = createUserWithStatus(UserStatus.SUSPENDED);
      assertThat(user.isSuspended()).isTrue();
    }

    @Test
    @DisplayName("isDeleted")
    void isDeleted() {
      User user = createUserWithStatus(UserStatus.DELETED);
      assertThat(user.isDeleted()).isTrue();
    }
  }

  // ========================================
  // Helper 메서드 - UserId.generateId() 사용
  // ========================================

  private User createActiveUserWithId() {
    return User.restoreBuilder()
        .userId(UserId.of(UserId.generateId()))
        .email(Email.of(VALID_EMAIL))
        .name(VALID_NAME)
        .phoneNumber(PhoneNumber.of(VALID_PHONE))
        .birthDate(VALID_BIRTH_DATE)
        .status(UserStatus.ACTIVE)
        .isDeleted(false)
        .build();
  }

  private User createUserWithStatus(UserStatus status) {
    return User.restoreBuilder()
        .userId(UserId.of(UserId.generateId()))
        .email(Email.of(VALID_EMAIL))
        .name(VALID_NAME)
        .phoneNumber(PhoneNumber.of(VALID_PHONE))
        .birthDate(VALID_BIRTH_DATE)
        .status(status)
        .isDeleted(status == UserStatus.DELETED)
        .build();
  }
}