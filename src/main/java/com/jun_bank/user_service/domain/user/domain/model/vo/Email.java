package com.jun_bank.user_service.domain.user.domain.model.vo;

import com.jun_bank.user_service.domain.user.domain.exception.UserException;

import java.util.regex.Pattern;

/**
 * 이메일 VO (Value Object)
 * <p>
 * 이메일 주소를 표현하는 불변 객체입니다.
 * 생성 시 형식을 검증하고 소문자로 정규화합니다.
 *
 * <h3>지원 형식:</h3>
 * <pre>
 * - user@example.com
 * - user.name@example.co.kr
 * - user+tag@example.com
 * - user_name@sub.example.com
 * </pre>
 *
 * <h3>검증 규칙:</h3>
 * <ul>
 *   <li>null이거나 빈 문자열 불가</li>
 *   <li>최대 255자</li>
 *   <li>RFC 5322 기반 형식 검증</li>
 *   <li>소문자로 정규화하여 저장</li>
 * </ul>
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // 생성 (자동으로 소문자 변환)
 * Email email = Email.of("User@Example.COM");
 * email.value();      // "user@example.com"
 *
 * // 도메인 추출
 * email.getDomain();  // "example.com"
 *
 * // 마스킹 (개인정보 보호)
 * email.masked();     // "u***r@example.com"
 * }</pre>
 *
 * @param value 이메일 주소 문자열 (소문자로 정규화됨)
 */
public record Email(String value) {

    /**
     * 이메일 정규식 패턴
     * <p>RFC 5322를 기반으로 한 간소화된 패턴입니다.</p>
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 최대 길이 (RFC 5321 기준)
     */
    private static final int MAX_LENGTH = 255;

    /**
     * Email 생성자 (Compact Constructor)
     * <p>
     * 이메일 형식을 검증하고 소문자로 정규화합니다.
     * </p>
     *
     * @param value 이메일 주소 문자열
     * @throws UserException 이메일 형식이 유효하지 않은 경우 (USER_001)
     */
    public Email {
        if (value == null || value.isBlank()) {
            throw UserException.invalidEmailFormat(value);
        }
        if (value.length() > MAX_LENGTH) {
            throw UserException.invalidEmailFormat(value);
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw UserException.invalidEmailFormat(value);
        }
        // 소문자로 정규화 (대소문자 구분 없이 처리)
        value = value.toLowerCase();
    }

    /**
     * 문자열로부터 Email 객체 생성
     *
     * @param value 이메일 주소 문자열
     * @return Email 객체
     * @throws UserException 이메일 형식이 유효하지 않은 경우
     */
    public static Email of(String value) {
        return new Email(value);
    }

    /**
     * 이메일 도메인 부분 추출
     * <p>
     * @ 기호 이후의 도메인 부분을 반환합니다.
     * </p>
     *
     * @return 도메인 부분 (예: "example.com")
     */
    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }

    /**
     * 이메일 로컬 파트 추출
     * <p>
     * @ 기호 이전의 로컬 파트를 반환합니다.
     * </p>
     *
     * @return 로컬 파트 (예: "user")
     */
    public String getLocalPart() {
        return value.substring(0, value.indexOf('@'));
    }

    /**
     * 마스킹된 이메일 반환
     * <p>
     * 개인정보 보호를 위해 로컬 파트의 일부를 마스킹합니다.
     * 로그 출력이나 화면 표시 시 사용합니다.
     * </p>
     *
     * <h4>마스킹 규칙:</h4>
     * <ul>
     *   <li>로컬 파트가 2자 이하: 첫 글자 + "***" (예: a@... → a***@...)</li>
     *   <li>로컬 파트가 3자 이상: 첫 글자 + "***" + 마지막 글자 (예: user@... → u***r@...)</li>
     * </ul>
     *
     * @return 마스킹된 이메일 (예: "u***r@example.com")
     */
    public String masked() {
        String local = getLocalPart();
        String domain = getDomain();

        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }
}