package com.jun_bank.user_service.domain.user.application.port.out;

/**
 * Auth Service Port (Output Port)
 * <p>
 * Auth Server와의 통신을 추상화한 인터페이스입니다.
 * Infrastructure Layer에서 Feign Client로 구현됩니다.
 *
 * <h3>책임:</h3>
 * <ul>
 *   <li>인증 사용자(AuthUser) 생성</li>
 *   <li>인증 사용자 삭제</li>
 * </ul>
 */
public interface AuthServicePort {

  /**
   * 인증 사용자 생성
   * <p>
   * 회원가입 시 Auth Server에 인증 정보를 생성합니다.
   * </p>
   *
   * @param userId   사용자 ID (User Service에서 생성)
   * @param email    이메일 (로그인 ID)
   * @param password 비밀번호 (평문, Auth Server에서 암호화)
   * @throws com.jun_bank.common_lib.exception.BusinessException Auth Server 호출 실패 시
   */
  void createAuthUser(String userId, String email, String password);

  /**
   * 인증 사용자 삭제
   * <p>
   * 회원 탈퇴 시 Auth Server의 인증 정보를 삭제(비활성화)합니다.
   * </p>
   *
   * @param userId 사용자 ID
   * @throws com.jun_bank.common_lib.exception.BusinessException Auth Server 호출 실패 시
   */
  void deleteAuthUser(String userId);
}