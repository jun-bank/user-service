package com.jun_bank.user_service.domain.user.domain.exception;

import com.jun_bank.common_lib.exception.BusinessException;

/**
 * 사용자 도메인 예외
 * <p>
 * 사용자 관련 비즈니스 로직에서 발생하는 예외를 처리합니다.
 * {@link UserErrorCode}를 기반으로 예외를 생성하며,
 * {@link com.jun_bank.common_lib.exception.GlobalExceptionHandler}에서
 * 일관된 에러 응답으로 변환됩니다.
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // 에러 코드만으로 예외 생성
 * throw new UserException(UserErrorCode.USER_NOT_FOUND);
 *
 * // 상세 메시지와 함께 예외 생성
 * throw new UserException(UserErrorCode.USER_NOT_FOUND, "userId=" + userId);
 *
 * // 원인 예외와 함께 예외 생성
 * throw new UserException(UserErrorCode.AUTH_SERVER_ERROR, cause);
 *
 * // 상세 메시지 + 원인 예외
 * throw new UserException(UserErrorCode.AUTH_SERVER_ERROR, "message", cause);
 *
 * // 팩토리 메서드 사용 (권장)
 * throw UserException.userNotFound(userId);
 * }</pre>
 *
 * @see UserErrorCode
 * @see BusinessException
 */
public class UserException extends BusinessException {

  /**
   * 에러 코드로 예외 생성
   *
   * @param errorCode 사용자 에러 코드
   */
  public UserException(UserErrorCode errorCode) {
    super(errorCode);
  }

  /**
   * 에러 코드와 상세 메시지로 예외 생성
   *
   * @param errorCode 사용자 에러 코드
   * @param message 상세 메시지
   */
  public UserException(UserErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  /**
   * 에러 코드와 원인 예외로 예외 생성
   *
   * @param errorCode 사용자 에러 코드
   * @param cause 원인 예외
   */
  public UserException(UserErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }

  /**
   * 에러 코드, 상세 메시지, 원인 예외로 예외 생성
   *
   * @param errorCode 사용자 에러 코드
   * @param message 상세 메시지
   * @param cause 원인 예외
   */
  public UserException(UserErrorCode errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }

  // ========================================
  // 조회 관련 팩토리 메서드
  // ========================================

  /**
   * 사용자를 찾을 수 없을 때 예외 생성
   *
   * @param userId 조회 실패한 사용자 ID
   * @return UserException 인스턴스
   */
  public static UserException userNotFound(String userId) {
    return new UserException(UserErrorCode.USER_NOT_FOUND, "userId=" + userId);
  }

  // ========================================
  // 중복 관련 팩토리 메서드
  // ========================================

  /**
   * 이메일이 이미 존재할 때 예외 생성
   *
   * @param email 중복된 이메일 주소
   * @return UserException 인스턴스
   */
  public static UserException emailAlreadyExists(String email) {
    return new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS, "email=" + email);
  }

  /**
   * 전화번호가 이미 존재할 때 예외 생성
   *
   * @param phoneNumber 중복된 전화번호
   * @return UserException 인스턴스
   */
  public static UserException phoneAlreadyExists(String phoneNumber) {
    return new UserException(UserErrorCode.PHONE_ALREADY_EXISTS, "phoneNumber=" + phoneNumber);
  }

  // ========================================
  // 상태 관련 팩토리 메서드
  // ========================================

  /**
   * 탈퇴한 사용자 수정 시도 시 예외 생성
   *
   * @return UserException 인스턴스
   */
  public static UserException cannotModifyDeletedUser() {
    return new UserException(UserErrorCode.CANNOT_MODIFY_DELETED_USER);
  }

  /**
   * 정지된 사용자 수정 시도 시 예외 생성
   *
   * @return UserException 인스턴스
   */
  public static UserException cannotModifySuspendedUser() {
    return new UserException(UserErrorCode.CANNOT_MODIFY_SUSPENDED_USER);
  }

  /**
   * 허용되지 않은 상태 변경 시 예외 생성
   *
   * @param from 현재 상태
   * @param to 변경하려는 상태
   * @return UserException 인스턴스
   */
  public static UserException invalidStatusTransition(String from, String to) {
    return new UserException(UserErrorCode.INVALID_STATUS_TRANSITION,
        String.format("from=%s, to=%s", from, to));
  }

  // ========================================
  // 유효성 검증 관련 팩토리 메서드
  // ========================================

  /**
   * 유효하지 않은 이메일 형식 예외 생성
   *
   * @param email 유효하지 않은 이메일 주소
   * @return UserException 인스턴스
   */
  public static UserException invalidEmailFormat(String email) {
    return new UserException(UserErrorCode.INVALID_EMAIL_FORMAT, "email=" + email);
  }

  /**
   * 유효하지 않은 전화번호 형식 예외 생성
   *
   * @param phoneNumber 유효하지 않은 전화번호
   * @return UserException 인스턴스
   */
  public static UserException invalidPhoneFormat(String phoneNumber) {
    return new UserException(UserErrorCode.INVALID_PHONE_FORMAT, "phoneNumber=" + phoneNumber);
  }

  /**
   * 유효하지 않은 사용자 ID 형식 예외 생성
   *
   * @param userId 유효하지 않은 사용자 ID
   * @return UserException 인스턴스
   */
  public static UserException invalidUserIdFormat(String userId) {
    return new UserException(UserErrorCode.INVALID_USER_ID_FORMAT, "userId=" + userId);
  }

  /**
   * 유효하지 않은 이름 예외 생성
   *
   * @return UserException 인스턴스
   */
  public static UserException invalidName() {
    return new UserException(UserErrorCode.INVALID_NAME);
  }

  // ========================================
  // 외부 서비스 관련 팩토리 메서드
  // ========================================

  /**
   * Auth Server 오류 예외 생성
   *
   * @param message 상세 메시지
   * @param cause 원인 예외
   * @return UserException 인스턴스
   */
  public static UserException authServerError(String message, Throwable cause) {
    return new UserException(UserErrorCode.AUTH_SERVER_ERROR, message, cause);
  }

  /**
   * Auth Server 타임아웃 예외 생성
   *
   * @param cause 원인 예외
   * @return UserException 인스턴스
   */
  public static UserException authServerTimeout(Throwable cause) {
    return new UserException(UserErrorCode.AUTH_SERVER_TIMEOUT, cause);
  }

  /**
   * 인증 정보 생성 실패 예외 생성
   *
   * @param userId 사용자 ID
   * @param cause 원인 예외
   * @return UserException 인스턴스
   */
  public static UserException authUserCreateFailed(String userId, Throwable cause) {
    return new UserException(UserErrorCode.AUTH_USER_CREATE_FAILED, "userId=" + userId, cause);
  }

  /**
   * 인증 정보 삭제 실패 예외 생성
   *
   * @param userId 사용자 ID
   * @param cause 원인 예외
   * @return UserException 인스턴스
   */
  public static UserException authUserDeleteFailed(String userId, Throwable cause) {
    return new UserException(UserErrorCode.AUTH_USER_DELETE_FAILED, "userId=" + userId, cause);
  }

  // ========================================
  // 이벤트 관련 팩토리 메서드
  // ========================================

  /**
   * 이벤트 발행 실패 예외 생성
   *
   * @param eventType 이벤트 타입
   * @param cause 원인 예외
   * @return UserException 인스턴스
   */
  public static UserException eventPublishFailed(String eventType, Throwable cause) {
    return new UserException(UserErrorCode.EVENT_PUBLISH_FAILED, "eventType=" + eventType, cause);
  }

  /**
   * 이벤트 직렬화 실패 예외 생성
   *
   * @param eventType 이벤트 타입
   * @param cause 원인 예외
   * @return UserException 인스턴스
   */
  public static UserException eventSerializeFailed(String eventType, Throwable cause) {
    return new UserException(UserErrorCode.EVENT_SERIALIZE_FAILED, "eventType=" + eventType, cause);
  }
}