package com.jun_bank.user_service.domain.user.domain.model.vo;

import com.jun_bank.common_lib.util.UuidUtils;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;

/**
 * 사용자 식별자 VO (Value Object)
 * <p>
 * 사용자를 고유하게 식별하는 불변 객체입니다.
 * {@link com.jun_bank.common_lib.util.UuidUtils}를 사용하여 ID를 생성하고 검증합니다.
 *
 * <h3>ID 형식:</h3>
 * <pre>USR-xxxxxxxx (예: USR-a1b2c3d4)</pre>
 * <ul>
 *   <li>USR: 사용자 도메인 프리픽스 (고정)</li>
 *   <li>-: 구분자</li>
 *   <li>xxxxxxxx: 8자리 랜덤 영숫자 (UUID 기반)</li>
 * </ul>
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // 새 ID 생성 (Entity의 fromDomain()에서 사용)
 * String newId = UserId.generateId();  // "USR-a1b2c3d4"
 *
 * // 기존 ID로 VO 생성 (DB 복원 시)
 * UserId userId = UserId.of("USR-a1b2c3d4");
 *
 * // 유효하지 않은 형식은 예외 발생
 * UserId invalid = UserId.of("INVALID");  // throws UserException
 * }</pre>
 *
 * <h3>불변성:</h3>
 * <p>Java record로 구현되어 불변성이 보장됩니다.</p>
 *
 * @param value 사용자 ID 문자열 (USR-xxxxxxxx 형식)
 * @see com.jun_bank.common_lib.util.UuidUtils
 * @see UserException
 */
public record UserId(String value) {

    /**
     * ID 프리픽스
     * <p>모든 사용자 ID는 "USR-"로 시작합니다.</p>
     */
    public static final String PREFIX = "USR";

    /**
     * UserId 생성자 (Compact Constructor)
     * <p>
     * ID 형식을 검증하고, 유효하지 않으면 예외를 발생시킵니다.
     * </p>
     *
     * @param value 사용자 ID 문자열
     * @throws UserException ID 형식이 유효하지 않은 경우 (USER_005)
     */
    public UserId {
        if (!UuidUtils.isValidDomainId(value, PREFIX)) {
            throw UserException.invalidUserIdFormat(value);
        }
    }

    /**
     * 문자열로부터 UserId 객체 생성
     * <p>
     * DB에서 복원하거나 외부에서 전달받은 ID를 VO로 변환할 때 사용합니다.
     * </p>
     *
     * @param value 사용자 ID 문자열
     * @return UserId 객체
     * @throws UserException ID 형식이 유효하지 않은 경우
     */
    public static UserId of(String value) {
        return new UserId(value);
    }

    /**
     * 새로운 사용자 ID 생성
     * <p>
     * Entity 레이어에서 새 사용자를 저장할 때 호출합니다.
     * {@code UserEntity.fromDomain()} 메서드에서 사용됩니다.
     * </p>
     *
     * @return 생성된 ID 문자열 (USR-xxxxxxxx 형식)
     */
    public static String generateId() {
        return UuidUtils.generateDomainId(PREFIX);
    }
}