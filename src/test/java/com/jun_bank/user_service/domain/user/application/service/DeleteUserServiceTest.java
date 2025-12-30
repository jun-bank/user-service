package com.jun_bank.user_service.domain.user.application.service;

import com.jun_bank.user_service.domain.user.application.port.out.AuthServicePort;
import com.jun_bank.user_service.domain.user.application.port.out.UserEventPublisherPort;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteUserService 테스트")
class DeleteUserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private AuthServicePort authServicePort;

  @Mock
  private UserEventPublisherPort userEventPublisher;

  @InjectMocks
  private DeleteUserService deleteUserService;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  private String userId;
  private String requesterId;
  private User activeUser;

  @BeforeEach
  void setUp() {
    userId = "USR-12345678";
    requesterId = "USR-12345678";  // 본인 탈퇴

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
  }

  // ========================================
  // deleteUser 성공 테스트
  // ========================================

  @Nested
  @DisplayName("deleteUser 성공")
  class DeleteUserSuccessTest {

    @Test
    @DisplayName("회원 탈퇴 성공")
    void deleteUser_Success() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
      given(userRepository.save(any(User.class))).willReturn(activeUser);
      doNothing().when(authServicePort).deleteAuthUser(anyString());
      doNothing().when(userEventPublisher).publishUserDeleted(any(User.class));

      // when
      deleteUserService.deleteUser(userId, requesterId);

      // then
      verify(userRepository).findById(userId);
      verify(userRepository).save(any(User.class));
      verify(authServicePort).deleteAuthUser(userId);
      verify(userEventPublisher).publishUserDeleted(any(User.class));
    }

    @Test
    @DisplayName("탈퇴 시 도메인의 withdraw 메서드가 호출된다")
    void deleteUser_WithdrawMethodCalled() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
      given(userRepository.save(userCaptor.capture())).willReturn(activeUser);
      doNothing().when(authServicePort).deleteAuthUser(anyString());
      doNothing().when(userEventPublisher).publishUserDeleted(any(User.class));

      // when
      deleteUserService.deleteUser(userId, requesterId);

      // then
      User capturedUser = userCaptor.getValue();
      assertThat(capturedUser.isDeleted()).isTrue();
      assertThat(capturedUser.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    @DisplayName("Auth Server에 인증 정보가 삭제된다")
    void deleteUser_AuthUserDeleted() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
      given(userRepository.save(any(User.class))).willReturn(activeUser);
      doNothing().when(authServicePort).deleteAuthUser(anyString());
      doNothing().when(userEventPublisher).publishUserDeleted(any(User.class));

      // when
      deleteUserService.deleteUser(userId, requesterId);

      // then
      verify(authServicePort).deleteAuthUser(userId);
    }

    @Test
    @DisplayName("삭제 이벤트가 발행된다")
    void deleteUser_EventPublished() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
      given(userRepository.save(any(User.class))).willReturn(activeUser);
      doNothing().when(authServicePort).deleteAuthUser(anyString());
      doNothing().when(userEventPublisher).publishUserDeleted(any(User.class));

      // when
      deleteUserService.deleteUser(userId, requesterId);

      // then
      verify(userEventPublisher).publishUserDeleted(any(User.class));
    }

    @Test
    @DisplayName("관리자가 다른 사용자를 탈퇴 처리할 수 있다")
    void deleteUser_AdminDelete() {
      // given
      String adminId = "ADMIN-001";
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
      given(userRepository.save(any(User.class))).willReturn(activeUser);
      doNothing().when(authServicePort).deleteAuthUser(anyString());
      doNothing().when(userEventPublisher).publishUserDeleted(any(User.class));

      // when
      deleteUserService.deleteUser(userId, adminId);

      // then
      verify(userRepository).save(any(User.class));
    }
  }

  // ========================================
  // deleteUser 실패 테스트
  // ========================================

  @Nested
  @DisplayName("deleteUser 실패")
  class DeleteUserFailureTest {

    @Test
    @DisplayName("존재하지 않는 사용자 탈퇴 시 예외 발생")
    void deleteUser_NotFound_ThrowsException() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> deleteUserService.deleteUser(userId, requesterId))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          });

      verify(authServicePort, never()).deleteAuthUser(anyString());
    }

    @Test
    @DisplayName("이미 탈퇴한 사용자 탈퇴 시 예외 발생")
    void deleteUser_AlreadyDeleted_ThrowsException() {
      // given
      User deletedUser = User.restoreBuilder()
          .userId(UserId.of(userId))
          .email(Email.of("test@example.com"))
          .name("탈퇴사용자")
          .phoneNumber(PhoneNumber.of("010-1234-5678"))
          .birthDate(LocalDate.of(1990, 5, 15))
          .status(UserStatus.DELETED)
          .isDeleted(true)
          .deletedAt(LocalDateTime.now().minusDays(1))
          .deletedBy("USR-12345678")
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(deletedUser));

      // when & then
      assertThatThrownBy(() -> deleteUserService.deleteUser(userId, requesterId))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.USER_ALREADY_DELETED);
          });

      verify(authServicePort, never()).deleteAuthUser(anyString());
    }

    @Test
    @DisplayName("Auth Server 호출 실패 시 롤백 후 예외 발생")
    void deleteUser_AuthServerFailed_RollbackAndThrowsException() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
      given(userRepository.save(any(User.class))).willReturn(activeUser);
      doThrow(new UserException(UserErrorCode.AUTH_SERVER_ERROR))
          .when(authServicePort).deleteAuthUser(anyString());

      // when & then
      assertThatThrownBy(() -> deleteUserService.deleteUser(userId, requesterId))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.AUTH_SERVER_ERROR);
          });

      // 롤백을 위해 save가 2번 호출됨 (첫 번째: 탈퇴 저장, 두 번째: 롤백 저장)
      verify(userRepository, times(2)).save(any(User.class));
      verify(userEventPublisher, never()).publishUserDeleted(any());
    }

    @Test
    @DisplayName("이벤트 발행 실패해도 탈퇴는 완료된다")
    void deleteUser_EventFailed_StillSuccess() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
      given(userRepository.save(any(User.class))).willReturn(activeUser);
      doNothing().when(authServicePort).deleteAuthUser(anyString());
      doThrow(new RuntimeException("Kafka unavailable"))
          .when(userEventPublisher).publishUserDeleted(any(User.class));

      // when - 예외 없이 완료
      deleteUserService.deleteUser(userId, requesterId);

      // then
      verify(authServicePort).deleteAuthUser(userId);
      verify(userEventPublisher).publishUserDeleted(any(User.class));
    }
  }

  // ========================================
  // 다양한 상태에서의 탈퇴 테스트
  // ========================================

  @Nested
  @DisplayName("다양한 상태에서의 탈퇴")
  class DifferentStatusDeleteTest {

    @Test
    @DisplayName("휴면 상태 사용자 탈퇴 가능")
    void deleteUser_InactiveUser_Success() {
      // given
      User inactiveUser = User.restoreBuilder()
          .userId(UserId.of(userId))
          .email(Email.of("test@example.com"))
          .name("휴면사용자")
          .phoneNumber(PhoneNumber.of("010-1234-5678"))
          .birthDate(LocalDate.of(1990, 5, 15))
          .status(UserStatus.INACTIVE)
          .isDeleted(false)
          .build();

      given(userRepository.findById(userId)).willReturn(Optional.of(inactiveUser));
      given(userRepository.save(any(User.class))).willReturn(inactiveUser);
      doNothing().when(authServicePort).deleteAuthUser(anyString());
      doNothing().when(userEventPublisher).publishUserDeleted(any(User.class));

      // when
      deleteUserService.deleteUser(userId, requesterId);

      // then
      verify(authServicePort).deleteAuthUser(userId);
    }

    @Test
    @DisplayName("정지 상태 사용자 탈퇴 가능")
    void deleteUser_SuspendedUser_Success() {
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
      given(userRepository.save(any(User.class))).willReturn(suspendedUser);
      doNothing().when(authServicePort).deleteAuthUser(anyString());
      doNothing().when(userEventPublisher).publishUserDeleted(any(User.class));

      // when
      deleteUserService.deleteUser(userId, requesterId);

      // then
      verify(authServicePort).deleteAuthUser(userId);
    }
  }

  // ========================================
  // 처리 순서 검증 테스트
  // ========================================

  @Nested
  @DisplayName("처리 순서 검증")
  class ProcessOrderTest {

    @Test
    @DisplayName("조회 → 탈퇴 저장 → Auth 삭제 → 이벤트 발행 순서로 처리")
    void deleteUser_ProcessOrder() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
      given(userRepository.save(any(User.class))).willReturn(activeUser);
      doNothing().when(authServicePort).deleteAuthUser(anyString());
      doNothing().when(userEventPublisher).publishUserDeleted(any(User.class));

      // when
      deleteUserService.deleteUser(userId, requesterId);

      // then - 순서 검증
      var inOrder = inOrder(userRepository, authServicePort, userEventPublisher);
      inOrder.verify(userRepository).findById(userId);
      inOrder.verify(userRepository).save(any(User.class));
      inOrder.verify(authServicePort).deleteAuthUser(userId);
      inOrder.verify(userEventPublisher).publishUserDeleted(any(User.class));
    }
  }
}