package com.jun_bank.user_service.domain.user.application.service;

import com.jun_bank.user_service.domain.user.application.dto.command.UpdateUserCommand;
import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;
import com.jun_bank.user_service.domain.user.application.port.out.UserRepository;
import com.jun_bank.user_service.domain.user.domain.exception.UserErrorCode;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.domain.model.User;
import com.jun_bank.user_service.domain.user.domain.model.UserStatus;
import com.jun_bank.user_service.domain.user.domain.model.vo.Email;
import com.jun_bank.user_service.domain.user.domain.model.vo.PhoneNumber;
import com.jun_bank.user_service.domain.user.domain.model.vo.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateUserService 테스트")
class UpdateUserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UpdateUserService updateUserService;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  private String userId;
  private User activeUser;
  private UpdateUserCommand validCommand;

  @BeforeEach
  void setUp() {
    userId = "USR-12345678";

    activeUser = User.restoreBuilder()
        .userId(UserId.of(userId))
        .email(Email.of("test@example.com"))
        .name("테스트사용자")
        .phoneNumber(PhoneNumber.of("010-1234-5678"))
        .birthDate(LocalDate.of(1990, 5, 15))
        .status(UserStatus.ACTIVE)
        .isDeleted(false)
        .createdAt(LocalDateTime.now().minusDays(30))
        .updatedAt(LocalDateTime.now())
        .build();

    validCommand = UpdateUserCommand.builder()
        .name("수정된이름")
        .phoneNumber("010-9999-8888")
        .build();
  }

  // ========================================
  // updateUser 성공 테스트
  // ========================================

  @Nested
  @DisplayName("updateUser 성공")
  class UpdateUserSuccessTest {

    @Test
    @DisplayName("프로필 수정 성공")
    void updateUser_Success() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

      // 수정된 사용자 생성
      User updatedUser = User.restoreBuilder()
          .userId(UserId.of(userId))
          .email(Email.of("test@example.com"))
          .name("수정된이름")
          .phoneNumber(PhoneNumber.of("010-9999-8888"))
          .birthDate(LocalDate.of(1990, 5, 15))
          .status(UserStatus.ACTIVE)
          .isDeleted(false)
          .createdAt(LocalDateTime.now().minusDays(30))
          .updatedAt(LocalDateTime.now())
          .build();

      given(userRepository.save(any(User.class))).willReturn(updatedUser);

      // when
      UserResult result = updateUserService.updateUser(userId, validCommand);

      // then
      assertThat(result.userId()).isEqualTo(userId);
      assertThat(result.name()).isEqualTo("수정된이름");
      assertThat(result.phoneNumber()).isEqualTo("010-9999-8888");
    }

    @Test
    @DisplayName("본인 수정이므로 전화번호가 마스킹되지 않는다")
    void updateUser_PhoneNumberNotMasked() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

      User updatedUser = User.restoreBuilder()
          .userId(UserId.of(userId))
          .email(Email.of("test@example.com"))
          .name("수정된이름")
          .phoneNumber(PhoneNumber.of("010-9999-8888"))
          .birthDate(LocalDate.of(1990, 5, 15))
          .status(UserStatus.ACTIVE)
          .isDeleted(false)
          .build();

      given(userRepository.save(any(User.class))).willReturn(updatedUser);

      // when
      UserResult result = updateUserService.updateUser(userId, validCommand);

      // then
      assertThat(result.phoneNumber()).isEqualTo("010-9999-8888");
    }

    @Test
    @DisplayName("도메인 객체의 updateProfile 메서드가 호출된다")
    void updateUser_DomainMethodCalled() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
      given(userRepository.save(userCaptor.capture())).willReturn(activeUser);

      // when
      updateUserService.updateUser(userId, validCommand);

      // then
      User capturedUser = userCaptor.getValue();
      assertThat(capturedUser.getName()).isEqualTo("수정된이름");
      assertThat(capturedUser.getPhoneNumber().value()).isEqualTo("010-9999-8888");
    }

    @Test
    @DisplayName("휴면 상태에서도 프로필 수정 가능")
    void updateUser_InactiveUser_Success() {
      // given
      User inactiveUser = User.restoreBuilder()
          .userId(UserId.of(userId))
          .email(Email.of("test@example.com"))
          .name("테스트사용자")
          .phoneNumber(PhoneNumber.of("010-1234-5678"))
          .birthDate(LocalDate.of(1990, 5, 15))
          .status(UserStatus.INACTIVE)
          .isDeleted(false)
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(inactiveUser));
      given(userRepository.save(any(User.class))).willReturn(inactiveUser);

      // when
      UserResult result = updateUserService.updateUser(userId, validCommand);

      // then
      assertThat(result.status()).isEqualTo(UserStatus.INACTIVE);
      verify(userRepository).save(any(User.class));
    }
  }

  // ========================================
  // updateUser 실패 테스트
  // ========================================

  @Nested
  @DisplayName("updateUser 실패")
  class UpdateUserFailureTest {

    @Test
    @DisplayName("존재하지 않는 사용자 수정 시 예외 발생")
    void updateUser_NotFound_ThrowsException() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> updateUserService.updateUser(userId, validCommand))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          });

      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("탈퇴한 사용자 수정 시 예외 발생")
    void updateUser_DeletedUser_ThrowsException() {
      // given
      User deletedUser = User.restoreBuilder()
          .userId(UserId.of(userId))
          .email(Email.of("test@example.com"))
          .name("탈퇴사용자")
          .phoneNumber(PhoneNumber.of("010-1234-5678"))
          .birthDate(LocalDate.of(1990, 5, 15))
          .status(UserStatus.DELETED)
          .isDeleted(true)
          .deletedAt(LocalDateTime.now())
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(deletedUser));

      // when & then
      assertThatThrownBy(() -> updateUserService.updateUser(userId, validCommand))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.CANNOT_MODIFY_DELETED_USER);
          });

      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("정지된 사용자 수정 시 예외 발생")
    void updateUser_SuspendedUser_ThrowsException() {
      // given
      User suspendedUser = User.restoreBuilder()
          .userId(UserId.of(userId))
          .email(Email.of("test@example.com"))
          .name("정지사용자")
          .phoneNumber(PhoneNumber.of("010-1234-5678"))
          .birthDate(LocalDate.of(1990, 5, 15))
          .status(UserStatus.SUSPENDED)
          .isDeleted(false)
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(suspendedUser));

      // when & then
      assertThatThrownBy(() -> updateUserService.updateUser(userId, validCommand))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.CANNOT_MODIFY_SUSPENDED_USER);
          });

      verify(userRepository, never()).save(any());
    }
  }

  // ========================================
  // 이메일/생년월일 불변성 테스트
  // ========================================

  @Nested
  @DisplayName("불변 필드 검증")
  class ImmutableFieldsTest {

    @Test
    @DisplayName("이메일은 변경되지 않는다")
    void updateUser_EmailNotChanged() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
      given(userRepository.save(userCaptor.capture())).willReturn(activeUser);

      // when
      updateUserService.updateUser(userId, validCommand);

      // then
      User capturedUser = userCaptor.getValue();
      assertThat(capturedUser.getEmail().value()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("생년월일은 변경되지 않는다")
    void updateUser_BirthDateNotChanged() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
      given(userRepository.save(userCaptor.capture())).willReturn(activeUser);

      // when
      updateUserService.updateUser(userId, validCommand);

      // then
      User capturedUser = userCaptor.getValue();
      assertThat(capturedUser.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
    }
  }
}