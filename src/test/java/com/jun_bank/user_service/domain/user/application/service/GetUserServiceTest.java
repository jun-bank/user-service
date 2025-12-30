package com.jun_bank.user_service.domain.user.application.service;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserService 테스트")
class GetUserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private GetUserService getUserService;

  private User activeUser;
  private String userId;
  private String email;

  @BeforeEach
  void setUp() {
    userId = "USR-12345678";
    email = "test@example.com";

    activeUser = User.restoreBuilder()
        .userId(UserId.of(userId))
        .email(Email.of(email))
        .name("테스트사용자")
        .phoneNumber(PhoneNumber.of("010-1234-5678"))
        .birthDate(LocalDate.of(1990, 5, 15))
        .status(UserStatus.ACTIVE)
        .isDeleted(false)
        .createdAt(LocalDateTime.now().minusDays(30))
        .updatedAt(LocalDateTime.now())
        .build();
  }

  // ========================================
  // getUserById 테스트
  // ========================================

  @Nested
  @DisplayName("getUserById")
  class GetUserByIdTest {

    @Test
    @DisplayName("사용자 ID로 조회 성공")
    void getUserById_Success() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

      // when
      UserResult result = getUserService.getUserById(userId);

      // then
      assertThat(result.userId()).isEqualTo(userId);
      assertThat(result.email()).isEqualTo(email);
      assertThat(result.name()).isEqualTo("테스트사용자");
      assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
      verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("전화번호가 마스킹 처리된다")
    void getUserById_PhoneNumberMasked() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

      // when
      UserResult result = getUserService.getUserById(userId);

      // then
      assertThat(result.phoneNumber()).isEqualTo("010-****-5678");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 예외 발생")
    void getUserById_NotFound_ThrowsException() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> getUserService.getUserById(userId))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          });
    }
  }

  // ========================================
  // getUserByIdForOwner 테스트
  // ========================================

  @Nested
  @DisplayName("getUserByIdForOwner")
  class GetUserByIdForOwnerTest {

    @Test
    @DisplayName("본인 프로필 조회 성공")
    void getUserByIdForOwner_Success() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

      // when
      UserResult result = getUserService.getUserByIdForOwner(userId);

      // then
      assertThat(result.userId()).isEqualTo(userId);
      assertThat(result.email()).isEqualTo(email);
    }

    @Test
    @DisplayName("본인 조회 시 전화번호가 마스킹되지 않는다")
    void getUserByIdForOwner_PhoneNumberNotMasked() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

      // when
      UserResult result = getUserService.getUserByIdForOwner(userId);

      // then
      assertThat(result.phoneNumber()).isEqualTo("010-1234-5678");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 예외 발생")
    void getUserByIdForOwner_NotFound_ThrowsException() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> getUserService.getUserByIdForOwner(userId))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          });
    }
  }

  // ========================================
  // getUserByEmail 테스트
  // ========================================

  @Nested
  @DisplayName("getUserByEmail")
  class GetUserByEmailTest {

    @Test
    @DisplayName("이메일로 조회 성공")
    void getUserByEmail_Success() {
      // given
      given(userRepository.findByEmail(email)).willReturn(Optional.of(activeUser));

      // when
      UserResult result = getUserService.getUserByEmail(email);

      // then
      assertThat(result.email()).isEqualTo(email);
      assertThat(result.userId()).isEqualTo(userId);
      verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("전화번호가 마스킹 처리된다")
    void getUserByEmail_PhoneNumberMasked() {
      // given
      given(userRepository.findByEmail(email)).willReturn(Optional.of(activeUser));

      // when
      UserResult result = getUserService.getUserByEmail(email);

      // then
      assertThat(result.phoneNumber()).isEqualTo("010-****-5678");
    }

    @Test
    @DisplayName("존재하지 않는 이메일 조회 시 예외 발생")
    void getUserByEmail_NotFound_ThrowsException() {
      // given
      given(userRepository.findByEmail(email)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> getUserService.getUserByEmail(email))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          });
    }
  }

  // ========================================
  // getUserByEmailForOwner 테스트
  // ========================================

  @Nested
  @DisplayName("getUserByEmailForOwner")
  class GetUserByEmailForOwnerTest {

    @Test
    @DisplayName("이메일로 본인 프로필 조회 성공")
    void getUserByEmailForOwner_Success() {
      // given
      given(userRepository.findByEmail(email)).willReturn(Optional.of(activeUser));

      // when
      UserResult result = getUserService.getUserByEmailForOwner(email);

      // then
      assertThat(result.email()).isEqualTo(email);
    }

    @Test
    @DisplayName("본인 조회 시 전화번호가 마스킹되지 않는다")
    void getUserByEmailForOwner_PhoneNumberNotMasked() {
      // given
      given(userRepository.findByEmail(email)).willReturn(Optional.of(activeUser));

      // when
      UserResult result = getUserService.getUserByEmailForOwner(email);

      // then
      assertThat(result.phoneNumber()).isEqualTo("010-1234-5678");
    }

    @Test
    @DisplayName("존재하지 않는 이메일 조회 시 예외 발생")
    void getUserByEmailForOwner_NotFound_ThrowsException() {
      // given
      given(userRepository.findByEmail(email)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> getUserService.getUserByEmailForOwner(email))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          });
    }
  }

  // ========================================
  // existsByEmail 테스트
  // ========================================

  @Nested
  @DisplayName("existsByEmail")
  class ExistsByEmailTest {

    @Test
    @DisplayName("이메일 존재 여부 확인 - 존재함")
    void existsByEmail_Exists() {
      // given
      given(userRepository.existsByEmail(email)).willReturn(true);

      // when
      boolean result = getUserService.existsByEmail(email);

      // then
      assertThat(result).isTrue();
      verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("이메일 존재 여부 확인 - 존재하지 않음")
    void existsByEmail_NotExists() {
      // given
      given(userRepository.existsByEmail(email)).willReturn(false);

      // when
      boolean result = getUserService.existsByEmail(email);

      // then
      assertThat(result).isFalse();
    }
  }

  // ========================================
  // 다양한 상태의 사용자 조회 테스트
  // ========================================

  @Nested
  @DisplayName("다양한 상태의 사용자 조회")
  class DifferentStatusUsersTest {

    @Test
    @DisplayName("휴면 상태 사용자 조회 가능")
    void getUserById_InactiveUser() {
      // given
      User inactiveUser = User.restoreBuilder()
          .userId(UserId.of(userId))
          .email(Email.of(email))
          .name("휴면사용자")
          .phoneNumber(PhoneNumber.of("010-1234-5678"))
          .birthDate(LocalDate.of(1990, 5, 15))
          .status(UserStatus.INACTIVE)
          .isDeleted(false)
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(inactiveUser));

      // when
      UserResult result = getUserService.getUserById(userId);

      // then
      assertThat(result.status()).isEqualTo(UserStatus.INACTIVE);
      assertThat(result.statusDescription()).isEqualTo("휴면");
    }

    @Test
    @DisplayName("정지 상태 사용자 조회 가능")
    void getUserById_SuspendedUser() {
      // given
      User suspendedUser = User.restoreBuilder()
          .userId(UserId.of(userId))
          .email(Email.of(email))
          .name("정지사용자")
          .phoneNumber(PhoneNumber.of("010-1234-5678"))
          .birthDate(LocalDate.of(1990, 5, 15))
          .status(UserStatus.SUSPENDED)
          .isDeleted(false)
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(suspendedUser));

      // when
      UserResult result = getUserService.getUserById(userId);

      // then
      assertThat(result.status()).isEqualTo(UserStatus.SUSPENDED);
      assertThat(result.statusDescription()).isEqualTo("정지");
    }
  }
}