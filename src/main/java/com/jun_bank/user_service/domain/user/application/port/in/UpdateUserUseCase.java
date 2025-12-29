package com.jun_bank.user_service.domain.user.application.port.in;

import com.jun_bank.user_service.domain.user.application.dto.command.UpdateUserCommand;
import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;

/**
 * 사용자 수정 UseCase (Input Port)
 * <p>
 * 프로필 수정 비즈니스 로직을 정의하는 인터페이스입니다.
 *
 * <h3>수정 가능 필드:</h3>
 * <ul>
 *   <li>name: 이름</li>
 *   <li>phoneNumber: 전화번호</li>
 * </ul>
 *
 * <h3>수정 불가 상태:</h3>
 * <ul>
 *   <li>DELETED: 탈퇴한 사용자</li>
 *   <li>SUSPENDED: 정지된 사용자</li>
 * </ul>
 *
 * <h3>처리 흐름:</h3>
 * <ol>
 *   <li>사용자 조회</li>
 *   <li>수정 가능 상태 검증 (도메인에서 처리)</li>
 *   <li>프로필 업데이트</li>
 *   <li>저장 (더티체킹)</li>
 * </ol>
 */
public interface UpdateUserUseCase {

  /**
   * 사용자 프로필 수정
   *
   * @param userId  사용자 ID
   * @param command 수정 커맨드
   * @return 수정된 사용자 정보
   * @throws com.jun_bank.user_service.domain.user.domain.exception.UserException 사용자를 찾을 수 없거나 수정 불가 상태인 경우
   */
  UserResult updateUser(String userId, UpdateUserCommand command);
}