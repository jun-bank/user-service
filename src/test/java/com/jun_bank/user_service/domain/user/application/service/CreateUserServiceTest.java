package com.jun_bank.user_service.domain.user.application.service;

import com.jun_bank.user_service.domain.user.application.dto.command.CreateUserCommand;
import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateUserService 테스트")
class CreateUserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private AuthServicePort authServicePort;

  @Mock
  private UserEventPublisherPort userEventPublisher;

  @InjectMocks
  private CreateUserService createUserService;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  private CreateUserCommand validCommand;
  private User savedUser;
  private String userId;

  @BeforeEach
  void setUp() {
    userId = "USR-12345678";

    validCommand = CreateUserCommand.builder()
        .email("test@example.com")
        .password("SecurePassword123!")
        .name("테스트사용자")
        .phoneNumber("010-1234-5678")
        .birthDate(LocalDate.of(1990, 5, 15))
        .build();

    savedUser = User.restoreBuilder()
        .userId(UserId.of(userId))
        .email(Email.of("test@example.com"))
        .name("테스트사용자")
        .phoneNumber(PhoneNumber.of("010-1234-5678"))
        .birthDate(LocalDate.of(1990, 5, 15))
        .status(UserStatus.ACTIVE)
        .isDeleted(false)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  // ========================================
  // createUser 성공 테스트
  // ========================================

  @Nested
  @DisplayName("createUser 성공")
  class CreateUserSuccessTest {

    @Test
    @DisplayName("회원가입 성공")
    void createUser_Success() {
      // given
      given(userRepository.existsByEmail(anyString())).willReturn(false);
      given(userRepository.save(any(User.class))).willReturn(savedUser);
      doNothing().when(authServicePort).createAuthUser(anyString(), anyString(), anyString());
      doNothing().when(userEventPublisher).publishUserCreated(any(User.class));

      // when
      UserResult result = createUserService.createUser(validCommand);

      // then
      assertThat(result.userId()).isEqualTo(userId);
      assertThat(result.email()).isEqualTo("test@example.com");
      assertThat(result.name()).isEqualTo("테스트사용자");
      assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("본인 회원가입이므로 전화번호가 마스킹되지 않는다")
    void createUser_PhoneNumberNotMasked() {
      // given
      given(userRepository.existsByEmail(anyString())).willReturn(false);
      given(userRepository.save(any(User.class))).willReturn(savedUser);
      doNothing().when(authServicePort).createAuthUser(anyString(), anyString(), anyString());
      doNothing().when(userEventPublisher).publishUserCreated(any(User.class));

      // when
      UserResult result = createUserService.createUser(validCommand);

      // then
      assertThat(result.phoneNumber()).isEqualTo("010-1234-5678");
    }

    @Test
    @DisplayName("User 도메인 객체가 올바르게 생성된다")
    void createUser_DomainCreated() {
      // given
      given(userRepository.existsByEmail(anyString())).willReturn(false);
      given(userRepository.save(userCaptor.capture())).willReturn(savedUser);
      doNothing().when(authServicePort).createAuthUser(anyString(), anyString(), anyString());
      doNothing().when(userEventPublisher).publishUserCreated(any(User.class));

      // when
      createUserService.createUser(validCommand);

      // then
      User capturedUser = userCaptor.getValue();
      assertThat(capturedUser.isNew()).isTrue();  // 저장 전이므로 ID 없음
      assertThat(capturedUser.getEmail().value()).isEqualTo("test@example.com");
      assertThat(capturedUser.getName()).isEqualTo("테스트사용자");
      assertThat(capturedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Auth Server에 인증 정보가 생성된다")
    void createUser_AuthUserCreated() {
      // given
      given(userRepository.existsByEmail(anyString())).willReturn(false);
      given(userRepository.save(any(User.class))).willReturn(savedUser);
      doNothing().when(authServicePort).createAuthUser(anyString(), anyString(), anyString());
      doNothing().when(userEventPublisher).publishUserCreated(any(User.class));

      // when
      createUserService.createUser(validCommand);

      // then
      verify(authServicePort).createAuthUser(
          eq(userId),
          eq("test@example.com"),
          eq("SecurePassword123!")
      );
    }

    @Test
    @DisplayName("생성 이벤트가 발행된다")
    void createUser_EventPublished() {
      // given
      given(userRepository.existsByEmail(anyString())).willReturn(false);
      given(userRepository.save(any(User.class))).willReturn(savedUser);
      doNothing().when(authServicePort).createAuthUser(anyString(), anyString(), anyString());
      doNothing().when(userEventPublisher).publishUserCreated(any(User.class));

      // when
      createUserService.createUser(validCommand);

      // then
      verify(userEventPublisher).publishUserCreated(any(User.class));
    }
  }

  // ========================================
  // createUser 실패 테스트
  // ========================================

  @Nested
  @DisplayName("createUser 실패")
  class CreateUserFailureTest {

    @Test
    @DisplayName("이메일 중복 시 예외 발생")
    void createUser_EmailDuplicate_ThrowsException() {
      // given
      given(userRepository.existsByEmail("test@example.com")).willReturn(true);

      // when & then
      assertThatThrownBy(() -> createUserService.createUser(validCommand))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS);
          });

      verify(userRepository, never()).save(any());
      verify(authServicePort, never()).createAuthUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Auth Server 호출 실패 시 예외 발생 (롤백)")
    void createUser_AuthServerFailed_ThrowsException() {
      // given
      given(userRepository.existsByEmail(anyString())).willReturn(false);
      given(userRepository.save(any(User.class))).willReturn(savedUser);
      doThrow(new UserException(UserErrorCode.AUTH_SERVER_ERROR))
          .when(authServicePort).createAuthUser(anyString(), anyString(), anyString());

      // when & then
      assertThatThrownBy(() -> createUserService.createUser(validCommand))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.AUTH_SERVER_ERROR);
          });

      // 이벤트 발행되지 않음
      verify(userEventPublisher, never()).publishUserCreated(any());
    }

    @Test
    @DisplayName("이벤트 발행 실패해도 회원가입은 성공한다")
    void createUser_EventFailed_StillSuccess() {
      // given
      given(userRepository.existsByEmail(anyString())).willReturn(false);
      given(userRepository.save(any(User.class))).willReturn(savedUser);
      doNothing().when(authServicePort).createAuthUser(anyString(), anyString(), anyString());
      doThrow(new RuntimeException("Kafka unavailable"))
          .when(userEventPublisher).publishUserCreated(any(User.class));

      // when
      UserResult result = createUserService.createUser(validCommand);

      // then - 회원가입은 성공
      assertThat(result.userId()).isEqualTo(userId);
      verify(userEventPublisher).publishUserCreated(any(User.class));
    }
  }

  // ========================================
  // 처리 순서 검증 테스트
  // ========================================

  @Nested
  @DisplayName("처리 순서 검증")
  class ProcessOrderTest {

    @Test
    @DisplayName("이메일 중복 확인 → 저장 → Auth 생성 → 이벤트 발행 순서로 처리")
    void createUser_ProcessOrder() {
      // given
      given(userRepository.existsByEmail(anyString())).willReturn(false);
      given(userRepository.save(any(User.class))).willReturn(savedUser);
      doNothing().when(authServicePort).createAuthUser(anyString(), anyString(), anyString());
      doNothing().when(userEventPublisher).publishUserCreated(any(User.class));

      // when
      createUserService.createUser(validCommand);

      // then - 순서 검증
      var inOrder = inOrder(userRepository, authServicePort, userEventPublisher);
      inOrder.verify(userRepository).existsByEmail(anyString());
      inOrder.verify(userRepository).save(any(User.class));
      inOrder.verify(authServicePort).createAuthUser(anyString(), anyString(), anyString());
      inOrder.verify(userEventPublisher).publishUserCreated(any(User.class));
    }
  }
}