package com.jun_bank.user_service.domain.user.application.port.in;

/**
 * 사용자 삭제 UseCase (Input Port)
 * <p>
 * 회원 탈퇴 비즈니스 로직을 정의하는 인터페이스입니다.
 *
 * <h3>Soft Delete 처리:</h3>
 * <ul>
 *   <li>status → DELETED</li>
 *   <li>isDeleted → true</li>
 *   <li>deletedAt → 현재 시간</li>
 *   <li>deletedBy → 요청자 ID</li>
 * </ul>
 *
 * <h3>처리 흐름:</h3>
 * <ol>
 *   <li>사용자 조회</li>
 *   <li>탈퇴 처리 (도메인 메서드 호출)</li>
 *   <li>저장</li>
 *   <li>Auth Server에 인증 정보 삭제 요청 (Feign)</li>
 *   <li>user.deleted 이벤트 발행 (Kafka)</li>
 * </ol>
 *
 * <h3>이벤트 수신 서비스:</h3>
 * <ul>
 *   <li>Account Service: 계좌 비활성화</li>
 *   <li>Card Service: 카드 비활성화</li>
 *   <li>Transfer Service: 진행 중 이체 처리</li>
 * </ul>
 */
public interface DeleteUserUseCase {

  /**
   * 사용자 탈퇴 (Soft Delete)
   * <p>
   * 본인 탈퇴 시 userId와 requesterId가 동일합니다.
   * 관리자 삭제 시 requesterId는 관리자 ID입니다.
   * </p>
   *
   * @param userId      탈퇴할 사용자 ID
   * @param requesterId 요청자 ID (deletedBy에 저장)
   * @throws com.jun_bank.user_service.domain.user.domain.exception.UserException 사용자를 찾을 수 없거나 이미 탈퇴한 경우
   */
  void deleteUser(String userId, String requesterId);

  /**
   * 본인 탈퇴
   * <p>
   * userId와 requesterId가 동일한 경우의 편의 메서드입니다.
   * </p>
   *
   * @param userId 탈퇴할 사용자 ID (본인)
   * @throws com.jun_bank.user_service.domain.user.domain.exception.UserException 사용자를 찾을 수 없거나 이미 탈퇴한 경우
   */
  default void withdraw(String userId) {
    deleteUser(userId, userId);
  }
}