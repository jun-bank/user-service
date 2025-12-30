package com.jun_bank.user_service.domain.user.domain.exception;

import com.jun_bank.common_lib.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 도메인 에러 코드
 * <p>
 * 에러 코드 체계:
 * <ul>
 *   <li>USER_001~009: 유효성 검증 오류</li>
 *   <li>USER_010~019: 조회 오류</li>
 *   <li>USER_020~029: 중복 오류</li>
 *   <li>USER_030~039: 상태 오류</li>
 *   <li>USER_040~049: 외부 서비스 오류</li>
 *   <li>USER_050~059: 이벤트 오류</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

  // ========================================
  // 유효성 검증 오류 (400)
  // ========================================
  INVALID_EMAIL_FORMAT("USER_001", "유효하지 않은 이메일 형식입니다", 400),
  INVALID_PHONE_FORMAT("USER_002", "유효하지 않은 전화번호 형식입니다", 400),
  INVALID_NAME("USER_003", "이름은 2~50자 사이여야 합니다", 400),
  INVALID_BIRTH_DATE("USER_004", "유효하지 않은 생년월일입니다", 400),
  INVALID_USER_ID_FORMAT("USER_005", "유효하지 않은 사용자 ID 형식입니다", 400),

  // ========================================
  // 조회 오류 (404)
  // ========================================
  USER_NOT_FOUND("USER_010", "사용자를 찾을 수 없습니다", 404),

  // ========================================
  // 중복 오류 (409)
  // ========================================
  EMAIL_ALREADY_EXISTS("USER_020", "이미 사용 중인 이메일입니다", 409),
  PHONE_ALREADY_EXISTS("USER_021", "이미 사용 중인 전화번호입니다", 409),

  // ========================================
  // 상태 오류 (422)
  // ========================================
  USER_ALREADY_ACTIVE("USER_030", "이미 활성화된 사용자입니다", 422),
  USER_ALREADY_INACTIVE("USER_031", "이미 휴면 상태인 사용자입니다", 422),
  USER_ALREADY_SUSPENDED("USER_032", "이미 정지된 사용자입니다", 422),
  USER_ALREADY_DELETED("USER_033", "이미 탈퇴한 사용자입니다", 422),
  CANNOT_MODIFY_DELETED_USER("USER_034", "탈퇴한 사용자는 수정할 수 없습니다", 422),
  CANNOT_MODIFY_SUSPENDED_USER("USER_035", "정지된 사용자는 수정할 수 없습니다", 422),
  INVALID_STATUS_TRANSITION("USER_036", "허용되지 않은 상태 변경입니다", 422),

  // ========================================
  // 외부 서비스 오류 (503)
  // ========================================
  AUTH_SERVER_ERROR("USER_040", "인증 서버 연동 중 오류가 발생했습니다", 503),
  AUTH_SERVER_TIMEOUT("USER_041", "인증 서버 응답 시간이 초과되었습니다", 503),
  AUTH_USER_CREATE_FAILED("USER_042", "인증 정보 생성에 실패했습니다", 503),
  AUTH_USER_DELETE_FAILED("USER_043", "인증 정보 삭제에 실패했습니다", 503),

  // ========================================
  // 이벤트 오류 (500)
  // ========================================
  EVENT_PUBLISH_FAILED("USER_050", "이벤트 발행에 실패했습니다", 500),
  EVENT_SERIALIZE_FAILED("USER_051", "이벤트 직렬화에 실패했습니다", 500);

  private final String code;
  private final String message;
  private final int status;
}