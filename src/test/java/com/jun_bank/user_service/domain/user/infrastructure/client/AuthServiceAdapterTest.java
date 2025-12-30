package com.jun_bank.user_service.domain.user.infrastructure.client;

import com.jun_bank.user_service.domain.user.domain.exception.UserErrorCode;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.infrastructure.client.AuthServiceClient.CreateAuthUserRequest;
import com.jun_bank.user_service.domain.user.infrastructure.client.AuthServiceClient.CreateAuthUserResponse;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
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

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceAdapter 테스트")
class AuthServiceAdapterTest {

  @Mock
  private AuthServiceClient authServiceClient;

  @InjectMocks
  private AuthServiceAdapter authServiceAdapter;

  @Captor
  private ArgumentCaptor<CreateAuthUserRequest> requestCaptor;

  private String userId;
  private String email;
  private String password;

  @BeforeEach
  void setUp() {
    userId = "USR-12345678";
    email = "test@example.com";
    password = "SecurePassword123!";
  }

  // ========================================
  // 헬퍼 메서드
  // ========================================

  private Request createDummyRequest() {
    return Request.create(
        Request.HttpMethod.POST,
        "http://auth-server/internal/auth-users",
        Collections.emptyMap(),
        null,
        new RequestTemplate()
    );
  }

  private FeignException.ServiceUnavailable createServiceUnavailableException() {
    return new FeignException.ServiceUnavailable(
        "Service Unavailable",
        createDummyRequest(),
        null,
        Collections.emptyMap()
    );
  }

  private FeignException.GatewayTimeout createGatewayTimeoutException() {
    return new FeignException.GatewayTimeout(
        "Gateway Timeout",
        createDummyRequest(),
        null,
        Collections.emptyMap()
    );
  }

  private FeignException.NotFound createNotFoundException() {
    return new FeignException.NotFound(
        "Not Found",
        createDummyRequest(),
        null,
        Collections.emptyMap()
    );
  }

  private FeignException.BadRequest createBadRequestException() {
    return new FeignException.BadRequest(
        "Bad Request",
        createDummyRequest(),
        null,
        Collections.emptyMap()
    );
  }

  // ========================================
  // createAuthUser 테스트
  // ========================================

  @Nested
  @DisplayName("createAuthUser")
  class CreateAuthUserTest {

    @Test
    @DisplayName("인증 사용자 생성 성공")
    void createAuthUser_Success() {
      // given
      CreateAuthUserResponse response = new CreateAuthUserResponse(
          userId, email, true, "Created successfully"
      );
      given(authServiceClient.createAuthUser(any(CreateAuthUserRequest.class)))
          .willReturn(response);

      // when
      authServiceAdapter.createAuthUser(userId, email, password);

      // then
      verify(authServiceClient).createAuthUser(requestCaptor.capture());

      CreateAuthUserRequest capturedRequest = requestCaptor.getValue();
      assertThat(capturedRequest.userId()).isEqualTo(userId);
      assertThat(capturedRequest.email()).isEqualTo(email);
      assertThat(capturedRequest.password()).isEqualTo(password);
    }

    @Test
    @DisplayName("Auth Server 응답이 실패면 예외 발생")
    void createAuthUser_ResponseFailed_ThrowsException() {
      // given
      CreateAuthUserResponse response = new CreateAuthUserResponse(
          userId, email, false, "Email already exists"
      );
      given(authServiceClient.createAuthUser(any(CreateAuthUserRequest.class)))
          .willReturn(response);

      // when & then
      assertThatThrownBy(() -> authServiceAdapter.createAuthUser(userId, email, password))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.AUTH_USER_CREATE_FAILED);
          });
    }

    @Test
    @DisplayName("ServiceUnavailable 예외 시 authServerError 발생")
    void createAuthUser_ServiceUnavailable_ThrowsAuthServerError() {
      // given
      given(authServiceClient.createAuthUser(any(CreateAuthUserRequest.class)))
          .willThrow(createServiceUnavailableException());

      // when & then
      assertThatThrownBy(() -> authServiceAdapter.createAuthUser(userId, email, password))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.AUTH_SERVER_ERROR);
          });
    }

    @Test
    @DisplayName("GatewayTimeout 예외 시 authServerTimeout 발생")
    void createAuthUser_GatewayTimeout_ThrowsAuthServerTimeout() {
      // given
      given(authServiceClient.createAuthUser(any(CreateAuthUserRequest.class)))
          .willThrow(createGatewayTimeoutException());

      // when & then
      assertThatThrownBy(() -> authServiceAdapter.createAuthUser(userId, email, password))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.AUTH_SERVER_TIMEOUT);
          });
    }

    @Test
    @DisplayName("기타 FeignException 시 authUserCreateFailed 발생")
    void createAuthUser_OtherFeignException_ThrowsCreateFailed() {
      // given
      given(authServiceClient.createAuthUser(any(CreateAuthUserRequest.class)))
          .willThrow(createBadRequestException());

      // when & then
      assertThatThrownBy(() -> authServiceAdapter.createAuthUser(userId, email, password))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.AUTH_USER_CREATE_FAILED);
          });
    }

    @Test
    @DisplayName("예기치 않은 예외 시 authServerError 발생")
    void createAuthUser_UnexpectedException_ThrowsAuthServerError() {
      // given
      given(authServiceClient.createAuthUser(any(CreateAuthUserRequest.class)))
          .willThrow(new RuntimeException("Unexpected error"));

      // when & then
      assertThatThrownBy(() -> authServiceAdapter.createAuthUser(userId, email, password))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.AUTH_SERVER_ERROR);
          });
    }
  }

  // ========================================
  // deleteAuthUser 테스트
  // ========================================

  @Nested
  @DisplayName("deleteAuthUser")
  class DeleteAuthUserTest {

    @Test
    @DisplayName("인증 사용자 삭제 성공")
    void deleteAuthUser_Success() {
      // given
      doNothing().when(authServiceClient).deleteAuthUser(anyString());

      // when
      authServiceAdapter.deleteAuthUser(userId);

      // then
      verify(authServiceClient).deleteAuthUser(userId);
    }

    @Test
    @DisplayName("NotFound 예외는 정상 처리 (이미 삭제됨)")
    void deleteAuthUser_NotFound_TreatedAsSuccess() {
      // given
      doThrow(createNotFoundException()).when(authServiceClient).deleteAuthUser(anyString());

      // when - 예외 없이 정상 완료
      authServiceAdapter.deleteAuthUser(userId);

      // then
      verify(authServiceClient).deleteAuthUser(userId);
    }

    @Test
    @DisplayName("ServiceUnavailable 예외 시 authServerError 발생")
    void deleteAuthUser_ServiceUnavailable_ThrowsAuthServerError() {
      // given
      doThrow(createServiceUnavailableException()).when(authServiceClient).deleteAuthUser(anyString());

      // when & then
      assertThatThrownBy(() -> authServiceAdapter.deleteAuthUser(userId))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.AUTH_SERVER_ERROR);
          });
    }

    @Test
    @DisplayName("GatewayTimeout 예외 시 authServerTimeout 발생")
    void deleteAuthUser_GatewayTimeout_ThrowsAuthServerTimeout() {
      // given
      doThrow(createGatewayTimeoutException()).when(authServiceClient).deleteAuthUser(anyString());

      // when & then
      assertThatThrownBy(() -> authServiceAdapter.deleteAuthUser(userId))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.AUTH_SERVER_TIMEOUT);
          });
    }

    @Test
    @DisplayName("기타 FeignException 시 authUserDeleteFailed 발생")
    void deleteAuthUser_OtherFeignException_ThrowsDeleteFailed() {
      // given
      doThrow(createBadRequestException()).when(authServiceClient).deleteAuthUser(anyString());

      // when & then
      assertThatThrownBy(() -> authServiceAdapter.deleteAuthUser(userId))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.AUTH_USER_DELETE_FAILED);
          });
    }

    @Test
    @DisplayName("예기치 않은 예외 시 authServerError 발생")
    void deleteAuthUser_UnexpectedException_ThrowsAuthServerError() {
      // given
      doThrow(new RuntimeException("Connection refused")).when(authServiceClient).deleteAuthUser(anyString());

      // when & then
      assertThatThrownBy(() -> authServiceAdapter.deleteAuthUser(userId))
          .isInstanceOf(UserException.class)
          .satisfies(ex -> {
            UserException userException = (UserException) ex;
            assertThat(userException.getErrorCode()).isEqualTo(UserErrorCode.AUTH_SERVER_ERROR);
          });
    }
  }

  // ========================================
  // CreateAuthUserRequest DTO 테스트
  // ========================================

  @Nested
  @DisplayName("CreateAuthUserRequest")
  class CreateAuthUserRequestTest {

    @Test
    @DisplayName("of 팩토리 메서드로 요청 생성")
    void of_CreatesRequest() {
      // when
      CreateAuthUserRequest request = CreateAuthUserRequest.of(userId, email, password);

      // then
      assertThat(request.userId()).isEqualTo(userId);
      assertThat(request.email()).isEqualTo(email);
      assertThat(request.password()).isEqualTo(password);
    }
  }
}