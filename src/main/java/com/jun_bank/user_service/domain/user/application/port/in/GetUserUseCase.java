package com.jun_bank.user_service.domain.user.application.port.in;

import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;

/**
 * 사용자 조회 UseCase (Input Port)
 * <p>
 * 사용자 단건 조회 비즈니스 로직을 정의하는 인터페이스입니다.
 *
 * <h3>조회 방식:</h3>
 * <ul>
 *   <li>ID로 조회: 기본 조회</li>
 *   <li>이메일로 조회: 로그인 시 사용</li>
 * </ul>
 *
 * <h3>마스킹 정책:</h3>
 * <ul>
 *   <li>본인 조회: 전화번호 원본 제공</li>
 *   <li>타인 조회: 전화번호 마스킹</li>
 * </ul>
 */
public interface GetUserUseCase {

  /**
   * ID로 사용자 조회
   * <p>
   * Soft Delete된 사용자는 조회되지 않습니다.
   * </p>
   *
   * @param userId 사용자 ID
   * @return 사용자 정보 (전화번호 마스킹)
   * @throws com.jun_bank.user_service.domain.user.domain.exception.UserException 사용자를 찾을 수 없는 경우
   */
  UserResult getUserById(String userId);

  /**
   * ID로 사용자 조회 (본인용)
   * <p>
   * 전화번호 원본을 포함하여 반환합니다.
   * </p>
   *
   * @param userId 사용자 ID
   * @return 사용자 정보 (전화번호 원본)
   * @throws com.jun_bank.user_service.domain.user.domain.exception.UserException 사용자를 찾을 수 없는 경우
   */
  UserResult getMyProfile(String userId);

  /**
   * 이메일로 사용자 조회
   * <p>
   * 로그인 또는 이메일 확인 시 사용합니다.
   * </p>
   *
   * @param email 이메일
   * @return 사용자 정보 (전화번호 마스킹)
   * @throws com.jun_bank.user_service.domain.user.domain.exception.UserException 사용자를 찾을 수 없는 경우
   */
  UserResult getUserByEmail(String email);

  /**
   * 이메일 존재 여부 확인
   * <p>
   * 회원가입 시 이메일 중복 체크에 사용합니다.
   * </p>
   *
   * @param email 이메일
   * @return 존재하면 true
   */
  boolean existsByEmail(String email);
}