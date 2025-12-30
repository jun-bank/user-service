package com.jun_bank.user_service.domain.user.infrastructure.client;

import com.jun_bank.user_service.domain.user.application.port.out.AuthServicePort;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.infrastructure.client.AuthServiceClient.CreateAuthUserRequest;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Auth Service Adapter
 * <p>
 * {@link AuthServicePort}를 구현하여 Auth Server와 통신합니다.
 * Feign Client를 래핑하여 예외 처리 및 로깅을 담당합니다.
 *
 * <h3>예외 처리:</h3>
 * <ul>
 *   <li>FeignException.ServiceUnavailable → authServerError</li>
 *   <li>FeignException.GatewayTimeout → authServerTimeout</li>
 *   <li>기타 FeignException → authUserCreateFailed / authUserDeleteFailed</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServiceAdapter implements AuthServicePort {

  private final AuthServiceClient authServiceClient;

  @Override
  public void createAuthUser(String userId, String email, String password) {
    log.debug("Auth Server 인증 사용자 생성 요청: userId={}, email={}", userId, email);

    try {
      CreateAuthUserRequest request = CreateAuthUserRequest.of(userId, email, password);
      var response = authServiceClient.createAuthUser(request);

      if (!response.success()) {
        log.error("Auth Server 인증 사용자 생성 실패: userId={}, message={}",
            userId, response.message());
        throw UserException.authUserCreateFailed(userId,
            new RuntimeException(response.message()));
      }

      log.info("Auth Server 인증 사용자 생성 성공: userId={}", userId);

    } catch (FeignException.ServiceUnavailable e) {
      log.error("Auth Server 서비스 불가: userId={}", userId, e);
      throw UserException.authServerError("userId=" + userId, e);

    } catch (FeignException.GatewayTimeout e) {
      log.error("Auth Server 타임아웃: userId={}", userId, e);
      throw UserException.authServerTimeout(e);

    } catch (FeignException e) {
      log.error("Auth Server 호출 실패: userId={}, status={}", userId, e.status(), e);
      throw UserException.authUserCreateFailed(userId, e);

    } catch (UserException e) {
      throw e;

    } catch (Exception e) {
      log.error("Auth Server 예기치 않은 오류: userId={}", userId, e);
      throw UserException.authServerError("userId=" + userId, e);
    }
  }

  @Override
  public void deleteAuthUser(String userId) {
    log.debug("Auth Server 인증 사용자 삭제 요청: userId={}", userId);

    try {
      authServiceClient.deleteAuthUser(userId);
      log.info("Auth Server 인증 사용자 삭제 성공: userId={}", userId);

    } catch (FeignException.NotFound e) {
      // 이미 삭제된 경우 - 정상 처리
      log.warn("Auth Server에 인증 사용자 없음 (이미 삭제됨): userId={}", userId);

    } catch (FeignException.ServiceUnavailable e) {
      log.error("Auth Server 서비스 불가: userId={}", userId, e);
      throw UserException.authServerError("userId=" + userId, e);

    } catch (FeignException.GatewayTimeout e) {
      log.error("Auth Server 타임아웃: userId={}", userId, e);
      throw UserException.authServerTimeout(e);

    } catch (FeignException e) {
      log.error("Auth Server 호출 실패: userId={}, status={}", userId, e.status(), e);
      throw UserException.authUserDeleteFailed(userId, e);

    } catch (Exception e) {
      log.error("Auth Server 예기치 않은 오류: userId={}", userId, e);
      throw UserException.authServerError("userId=" + userId, e);
    }
  }
}