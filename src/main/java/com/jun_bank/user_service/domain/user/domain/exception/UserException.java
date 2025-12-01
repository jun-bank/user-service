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
     * <p>
     * 상세 메시지는 에러 코드의 기본 메시지 뒤에 추가되어
     * 디버깅에 유용한 정보를 제공합니다.
     * </p>
     *
     * @param errorCode 사용자 에러 코드
     * @param detailMessage 상세 메시지 (예: "userId=USR-12345678")
     */
    public UserException(UserErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    // ========================================
    // 조회 관련 팩토리 메서드
    // ========================================

    /**
     * 사용자를 찾을 수 없을 때 예외 생성
     * <p>
     * 사용자 ID로 조회 시 해당 사용자가 존재하지 않거나
     * Soft Delete된 경우 발생합니다.
     * </p>
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
     * <p>
     * 회원가입 또는 이메일 변경 시 이미 등록된 이메일인 경우 발생합니다.
     * </p>
     *
     * @param email 중복된 이메일 주소
     * @return UserException 인스턴스
     */
    public static UserException emailAlreadyExists(String email) {
        return new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS, "email=" + email);
    }

    // ========================================
    // 상태 관련 팩토리 메서드
    // ========================================

    /**
     * 탈퇴한 사용자 수정 시도 시 예외 생성
     * <p>
     * DELETED 상태의 사용자 정보를 수정하려고 할 때 발생합니다.
     * Soft Delete된 사용자는 복구 전까지 수정이 불가능합니다.
     * </p>
     *
     * @return UserException 인스턴스
     */
    public static UserException cannotModifyDeletedUser() {
        return new UserException(UserErrorCode.CANNOT_MODIFY_DELETED_USER);
    }

    /**
     * 정지된 사용자 수정 시도 시 예외 생성
     * <p>
     * SUSPENDED 상태의 사용자가 본인 정보를 수정하려고 할 때 발생합니다.
     * 관리자에 의한 정지 해제 전까지 수정이 불가능합니다.
     * </p>
     *
     * @return UserException 인스턴스
     */
    public static UserException cannotModifySuspendedUser() {
        return new UserException(UserErrorCode.CANNOT_MODIFY_SUSPENDED_USER);
    }

    /**
     * 허용되지 않은 상태 변경 시 예외 생성
     * <p>
     * 현재 상태에서 요청한 상태로 전환이 불가능한 경우 발생합니다.
     * 예: DELETED → ACTIVE (탈퇴 후 직접 활성화 불가)
     * </p>
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
     * <p>
     * 이메일 주소가 표준 형식에 맞지 않는 경우 발생합니다.
     * </p>
     *
     * @param email 유효하지 않은 이메일 주소
     * @return UserException 인스턴스
     */
    public static UserException invalidEmailFormat(String email) {
        return new UserException(UserErrorCode.INVALID_EMAIL_FORMAT, "email=" + email);
    }

    /**
     * 유효하지 않은 전화번호 형식 예외 생성
     * <p>
     * 전화번호가 한국 휴대폰 형식에 맞지 않는 경우 발생합니다.
     * </p>
     *
     * @param phoneNumber 유효하지 않은 전화번호
     * @return UserException 인스턴스
     */
    public static UserException invalidPhoneFormat(String phoneNumber) {
        return new UserException(UserErrorCode.INVALID_PHONE_FORMAT, "phoneNumber=" + phoneNumber);
    }

    /**
     * 유효하지 않은 사용자 ID 형식 예외 생성
     * <p>
     * 사용자 ID가 USR-xxxxxxxx 형식에 맞지 않는 경우 발생합니다.
     * </p>
     *
     * @param userId 유효하지 않은 사용자 ID
     * @return UserException 인스턴스
     */
    public static UserException invalidUserIdFormat(String userId) {
        return new UserException(UserErrorCode.INVALID_USER_ID_FORMAT, "userId=" + userId);
    }

    /**
     * 유효하지 않은 이름 예외 생성
     * <p>
     * 이름이 null이거나 2~50자 범위를 벗어난 경우 발생합니다.
     * </p>
     *
     * @return UserException 인스턴스
     */
    public static UserException invalidName() {
        return new UserException(UserErrorCode.INVALID_NAME);
    }
}