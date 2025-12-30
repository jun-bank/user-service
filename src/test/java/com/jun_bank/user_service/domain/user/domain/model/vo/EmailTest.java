package com.jun_bank.user_service.domain.user.domain.model.vo;

import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Email VO 테스트")
class EmailTest {

  @Nested
  @DisplayName("of - 생성")
  class Of {

    @Nested
    @DisplayName("성공 케이스")
    class Success {

      @ParameterizedTest
      @ValueSource(strings = {
          "user@example.com",
          "user.name@example.co.kr",
          "user+tag@example.com",
          "user_name@sub.example.com",
          "test123@domain.org"
      })
      @DisplayName("유효한 이메일 형식")
      void validEmails(String email) {
        // when
        Email result = Email.of(email);

        // then
        assertThat(result.value()).isEqualTo(email.toLowerCase());
      }

      @Test
      @DisplayName("대문자 입력 시 소문자로 정규화")
      void normalizeToLowerCase() {
        // when
        Email email = Email.of("User@EXAMPLE.COM");

        // then
        assertThat(email.value()).isEqualTo("user@example.com");
      }

      @Test
      @DisplayName("255자 이메일 (최대 길이)")
      void maxLengthEmail() {
        // given
        String localPart = "a".repeat(243);  // 243 + @ + example.com = 255
        String email = localPart + "@example.com";

        // when
        Email result = Email.of(email);

        // then
        assertThat(result.value()).hasSize(255);
      }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

      @ParameterizedTest
      @NullAndEmptySource
      @DisplayName("null 또는 빈 문자열")
      void nullOrEmpty(String email) {
        assertThatThrownBy(() -> Email.of(email))
            .isInstanceOf(UserException.class);
      }

      @Test
      @DisplayName("공백 문자열")
      void blankString() {
        assertThatThrownBy(() -> Email.of("   "))
            .isInstanceOf(UserException.class);
      }

      @ParameterizedTest
      @ValueSource(strings = {
          "invalid",           // @ 없음
          "invalid@",          // 도메인 없음
          "@example.com",      // 로컬 파트 없음
          "user@",             // 도메인 없음
          "user@.com",         // 도메인 시작이 .
          "user@example",      // TLD 없음
          "user@@example.com", // @ 중복
          "user @example.com", // 공백 포함
          "user@exam ple.com"  // 도메인에 공백
      })
      @DisplayName("유효하지 않은 이메일 형식")
      void invalidEmails(String email) {
        assertThatThrownBy(() -> Email.of(email))
            .isInstanceOf(UserException.class);
      }

      @Test
      @DisplayName("255자 초과")
      void tooLong() {
        // given
        String localPart = "a".repeat(244);  // 244 + @ + example.com = 256
        String email = localPart + "@example.com";

        // when & then
        assertThatThrownBy(() -> Email.of(email))
            .isInstanceOf(UserException.class);
      }
    }
  }

  @Nested
  @DisplayName("value - 값 반환")
  class Value {

    @Test
    @DisplayName("정규화된 값 반환")
    void returnNormalizedValue() {
      // given
      Email email = Email.of("Test@Example.COM");

      // when & then
      assertThat(email.value()).isEqualTo("test@example.com");
    }
  }

  @Nested
  @DisplayName("getDomain - 도메인 추출")
  class GetDomain {

    @Test
    @DisplayName("단일 도메인")
    void singleDomain() {
      // given
      Email email = Email.of("user@example.com");

      // when & then
      assertThat(email.getDomain()).isEqualTo("example.com");
    }

    @Test
    @DisplayName("서브 도메인 포함")
    void subDomain() {
      // given
      Email email = Email.of("user@mail.example.co.kr");

      // when & then
      assertThat(email.getDomain()).isEqualTo("mail.example.co.kr");
    }
  }

  @Nested
  @DisplayName("getLocalPart - 로컬 파트 추출")
  class GetLocalPart {

    @Test
    @DisplayName("단순 로컬 파트")
    void simpleLocalPart() {
      // given
      Email email = Email.of("user@example.com");

      // when & then
      assertThat(email.getLocalPart()).isEqualTo("user");
    }

    @Test
    @DisplayName("특수문자 포함 로컬 파트")
    void localPartWithSpecialChars() {
      // given
      Email email = Email.of("user.name+tag@example.com");

      // when & then
      assertThat(email.getLocalPart()).isEqualTo("user.name+tag");
    }
  }

  @Nested
  @DisplayName("masked - 마스킹")
  class Masked {

    @Test
    @DisplayName("로컬 파트 3자 이상: 첫글자 + *** + 마지막글자")
    void maskedLongLocalPart() {
      // given
      Email email = Email.of("user@example.com");

      // when & then
      assertThat(email.masked()).isEqualTo("u***r@example.com");
    }

    @Test
    @DisplayName("로컬 파트 2자: 첫글자 + ***")
    void maskedTwoCharLocalPart() {
      // given
      Email email = Email.of("ab@example.com");

      // when & then
      assertThat(email.masked()).isEqualTo("a***@example.com");
    }

    @Test
    @DisplayName("로컬 파트 1자: 첫글자 + ***")
    void maskedOneCharLocalPart() {
      // given
      Email email = Email.of("a@example.com");

      // when & then
      assertThat(email.masked()).isEqualTo("a***@example.com");
    }

    @Test
    @DisplayName("긴 로컬 파트")
    void maskedLongLocal() {
      // given
      Email email = Email.of("verylongemail@example.com");

      // when & then
      assertThat(email.masked()).isEqualTo("v***l@example.com");
    }
  }

  @Nested
  @DisplayName("equals & hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("같은 값이면 동등")
    void equalWhenSameValue() {
      // given
      Email email1 = Email.of("user@example.com");
      Email email2 = Email.of("USER@EXAMPLE.COM");

      // then
      assertThat(email1).isEqualTo(email2);
      assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
    }

    @Test
    @DisplayName("다른 값이면 다름")
    void notEqualWhenDifferentValue() {
      // given
      Email email1 = Email.of("user1@example.com");
      Email email2 = Email.of("user2@example.com");

      // then
      assertThat(email1).isNotEqualTo(email2);
    }
  }
}