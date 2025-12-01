package com.jun_bank.user_service.domain.user.domain.model.vo;

import com.jun_bank.user_service.domain.user.domain.exception.UserException;

import java.util.regex.Pattern;

/**
 * 전화번호 VO (Value Object)
 * <p>
 * 한국 휴대폰 번호를 표현하는 불변 객체입니다.
 * 생성 시 형식을 검증하고 정규화합니다.
 *
 * <h3>지원 입력 형식:</h3>
 * <pre>
 * - 01012345678     (하이픈 없음)
 * - 010-1234-5678   (하이픈 포함)
 * - 010 1234 5678   (공백 포함)
 * </pre>
 *
 * <h3>저장 형식:</h3>
 * <pre>010-1234-5678 (하이픈 포함, 정규화)</pre>
 *
 * <h3>지원 통신사 번호:</h3>
 * <ul>
 *   <li>010: SKT, KT, LGU+</li>
 *   <li>011: SKT (구)</li>
 *   <li>016: KT (구)</li>
 *   <li>017: SKT (구)</li>
 *   <li>018: LGU+ (구)</li>
 *   <li>019: LGU+ (구)</li>
 * </ul>
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // 다양한 형식 지원 (자동 정규화)
 * PhoneNumber phone1 = PhoneNumber.of("01012345678");
 * PhoneNumber phone2 = PhoneNumber.of("010-1234-5678");
 * phone1.value();         // "010-1234-5678"
 *
 * // 마스킹 (개인정보 보호)
 * phone1.masked();        // "010-****-5678"
 *
 * // 하이픈 없는 형식
 * phone1.withoutHyphen(); // "01012345678"
 * }</pre>
 *
 * @param value 정규화된 전화번호 (010-1234-5678 형식)
 */
public record PhoneNumber(String value) {

    /**
     * 정규화된 전화번호 패턴 (010-1234-5678 또는 010-123-4567)
     */
    private static final Pattern NORMALIZED_PATTERN =
            Pattern.compile("^01[016789]-\\d{3,4}-\\d{4}$");

    /**
     * 입력 전화번호 패턴 (공백, 하이픈 허용)
     */
    private static final Pattern INPUT_PATTERN =
            Pattern.compile("^01[016789][\\s-]?\\d{3,4}[\\s-]?\\d{4}$");

    /**
     * PhoneNumber 생성자 (Compact Constructor)
     * <p>
     * 전화번호 형식을 검증하고 정규화합니다.
     * 이미 정규화된 형식이면 그대로 사용하고,
     * 입력 형식이면 정규화를 수행합니다.
     * </p>
     *
     * @param value 전화번호 문자열
     * @throws UserException 전화번호 형식이 유효하지 않은 경우 (USER_002)
     */
    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw UserException.invalidPhoneFormat(value);
        }

        // 이미 정규화된 형식인지 확인
        if (NORMALIZED_PATTERN.matcher(value).matches()) {
            // 그대로 사용
        } else if (INPUT_PATTERN.matcher(value).matches()) {
            // 정규화 수행
            value = normalize(value);
        } else {
            throw UserException.invalidPhoneFormat(value);
        }
    }

    /**
     * 문자열로부터 PhoneNumber 객체 생성
     *
     * @param value 전화번호 문자열 (다양한 형식 허용)
     * @return PhoneNumber 객체 (정규화된 형식)
     * @throws UserException 전화번호 형식이 유효하지 않은 경우
     */
    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    /**
     * 마스킹된 전화번호 반환
     * <p>
     * 개인정보 보호를 위해 중간 번호를 마스킹합니다.
     * 로그 출력이나 화면 표시 시 사용합니다.
     * </p>
     *
     * @return 마스킹된 전화번호 (예: "010-****-5678")
     */
    public String masked() {
        String[] parts = value.split("-");
        return parts[0] + "-****-" + parts[2];
    }

    /**
     * 하이픈 없는 전화번호 반환
     * <p>
     * SMS 발송 등 하이픈 없는 형식이 필요한 경우 사용합니다.
     * </p>
     *
     * @return 하이픈 없는 전화번호 (예: "01012345678")
     */
    public String withoutHyphen() {
        return value.replaceAll("-", "");
    }

    /**
     * 전화번호 정규화
     * <p>
     * 공백과 하이픈을 제거하고 표준 형식으로 변환합니다.
     * </p>
     *
     * @param raw 원시 전화번호 문자열
     * @return 정규화된 전화번호 (010-1234-5678 또는 010-123-4567 형식)
     */
    private static String normalize(String raw) {
        // 숫자만 추출
        String digits = raw.replaceAll("[\\s-]", "");

        if (digits.length() == 10) {
            // 010-123-4567 형식 (구 번호 체계)
            return String.format("%s-%s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 6),
                    digits.substring(6));
        } else {
            // 010-1234-5678 형식 (현재 번호 체계)
            return String.format("%s-%s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 7),
                    digits.substring(7));
        }
    }
}