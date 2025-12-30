package com.jun_bank.user_service.domain.user.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Auth Server Feign Client
 * <p>
 * Auth Server와의 통신을 담당하는 Feign Client입니다.
 *
 * <h3>연동 API:</h3>
 * <ul>
 *   <li>POST /internal/auth-users - 인증 사용자 생성</li>
 *   <li>DELETE /internal/auth-users/{userId} - 인증 사용자 삭제</li>
 * </ul>
 *
 * <h3>설정:</h3>
 * <ul>
 *   <li>name: auth-server (Eureka 서비스명)</li>
 *   <li>configuration: FeignConfig (인터셉터, 에러 디코더)</li>
 * </ul>
 */
@FeignClient(
    name = "auth-server",
    configuration = com.jun_bank.user_service.global.config.FeignConfig.class
)
public interface AuthServiceClient {

  /**
   * 인증 사용자 생성
   * <p>
   * 회원가입 시 Auth Server에 인증 정보를 생성합니다.
   * </p>
   *
   * @param request 인증 사용자 생성 요청
   * @return 생성 결과
   */
  @PostMapping("/internal/auth-users")
  CreateAuthUserResponse createAuthUser(@RequestBody CreateAuthUserRequest request);

  /**
   * 인증 사용자 삭제
   * <p>
   * 회원 탈퇴 시 Auth Server의 인증 정보를 삭제(비활성화)합니다.
   * </p>
   *
   * @param userId 사용자 ID
   */
  @DeleteMapping("/internal/auth-users/{userId}")
  void deleteAuthUser(@PathVariable("userId") String userId);

  // ========================================
  // Request/Response DTO
  // ========================================

  /**
   * 인증 사용자 생성 요청
   */
  record CreateAuthUserRequest(
      String userId,
      String email,
      String password
  ) {
    public static CreateAuthUserRequest of(String userId, String email, String password) {
      return new CreateAuthUserRequest(userId, email, password);
    }
  }

  /**
   * 인증 사용자 생성 응답
   */
  record CreateAuthUserResponse(
      String userId,
      String email,
      boolean success,
      String message
  ) {}
}