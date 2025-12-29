package com.jun_bank.user_service.domain.user.application.port.in;

import com.jun_bank.user_service.domain.user.application.dto.command.CreateUserCommand;
import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;

/**
 * 사용자 생성 UseCase (Input Port)
 * <p>
 * 회원가입 비즈니스 로직을 정의하는 인터페이스입니다.
 *
 * <h3>처리 흐름:</h3>
 * <ol>
 *   <li>이메일 중복 확인</li>
 *   <li>User 도메인 생성</li>
 *   <li>User 저장</li>
 *   <li>Auth Server에 인증 정보 생성 요청 (Feign)</li>
 *   <li>user.created 이벤트 발행 (Kafka)</li>
 * </ol>
 *
 * <h3>실패 시:</h3>
 * <ul>
 *   <li>이메일 중복: UserException(EMAIL_ALREADY_EXISTS)</li>
 *   <li>Auth Server 실패: 트랜잭션 롤백</li>
 * </ul>
 */
public interface CreateUserUseCase {

  /**
   * 사용자 생성 (회원가입)
   *
   * @param command 생성 커맨드
   * @return 생성된 사용자 정보
   */
  UserResult createUser(CreateUserCommand command);
}