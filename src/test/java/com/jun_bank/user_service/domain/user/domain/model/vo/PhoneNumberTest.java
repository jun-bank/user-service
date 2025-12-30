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

@DisplayName("PhoneNumber VO 테스트")
class PhoneNumberTest {

  @Nested
  @DisplayName("of - 생성")
  class Of {

    @Nested
    @DisplayName("성공 케이스")
    class Success {

      @Test
      @DisplayName("이미 정규화된 형식 (하이픈 포함)")
      void alreadyNormalized() {
        // when
        PhoneNumber phone = PhoneNumber.of("010-1234-5678");

        // then
        assertThat(phone.value()).isEqualTo("010-1234-5678");
      }

      @Test
      @DisplayName("하이픈 없는 형식 → 정규화")
      void withoutHyphen() {
        // when
        PhoneNumber phone = PhoneNumber.of("01012345678");

        // then
        assertThat(phone.value()).isEqualTo("010-1234-5678");
      }

      @Test
      @DisplayName("공백 포함 형식 → 정규화")
      void withSpace() {
        // when
        PhoneNumber phone = PhoneNumber.of("010 1234 5678");

        // then
        assertThat(phone.value()).isEqualTo("010-1234-5678");
      }

      @Test
      @DisplayName("혼합 형식 (하이픈 + 공백) → 정규화")
      void mixedFormat() {
        // when
        PhoneNumber phone = PhoneNumber.of("010-1234 5678");

        // then
        assertThat(phone.value()).isEqualTo("010-1234-5678");
      }

      @ParameterizedTest
      @ValueSource(strings = {"011", "016", "017", "018", "019"})
      @DisplayName("구 통신사 번호 (10자리)")
      void legacyCarriersTenDigits(String prefix) {
        // when
        PhoneNumber phone = PhoneNumber.of(prefix + "1234567");

        // then
        assertThat(phone.value()).isEqualTo(prefix + "-123-4567");
      }

      @ParameterizedTest
      @ValueSource(strings = {"011", "016", "017", "018", "019"})
      @DisplayName("구 통신사 번호 (11자리)")
      void legacyCarriersElevenDigits(String prefix) {
        // when
        PhoneNumber phone = PhoneNumber.of(prefix + "12345678");

        // then
        assertThat(phone.value()).isEqualTo(prefix + "-1234-5678");
      }

      @Test
      @DisplayName("구 번호 정규화된 형식 (010-123-4567)")
      void legacyNormalizedFormat() {
        // when
        PhoneNumber phone = PhoneNumber.of("010-123-4567");

        // then
        assertThat(phone.value()).isEqualTo("010-123-4567");
      }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

      @ParameterizedTest
      @NullAndEmptySource
      @DisplayName("null 또는 빈 문자열")
      void nullOrEmpty(String phone) {
        assertThatThrownBy(() -> PhoneNumber.of(phone))
            .isInstanceOf(UserException.class);
      }

      @Test
      @DisplayName("공백 문자열")
      void blankString() {
        assertThatThrownBy(() -> PhoneNumber.of("   "))
            .isInstanceOf(UserException.class);
      }

      @ParameterizedTest
      @ValueSource(strings = {
          "02-1234-5678",       // 지역번호 (02)
          "031-123-4567",       // 지역번호 (031)
          "012-1234-5678",      // 잘못된 접두사
          "010-123-567",        // 자릿수 부족 (마지막 3자리)
          "010-12345-6789",     // 자릿수 초과
          "010-abcd-efgh",      // 숫자가 아님
          "01012345",           // 전체 자릿수 부족
          "0101234567890",      // 전체 자릿수 초과
          "010--1234-5678",     // 하이픈 중복
          "+82-10-1234-5678"    // 국가 코드 포함
      })
      @DisplayName("유효하지 않은 전화번호 형식")
      void invalidPhones(String phone) {
        assertThatThrownBy(() -> PhoneNumber.of(phone))
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
      PhoneNumber phone = PhoneNumber.of("01012345678");

      // when & then
      assertThat(phone.value()).isEqualTo("010-1234-5678");
    }
  }

  @Nested
  @DisplayName("masked - 마스킹")
  class Masked {

    @Test
    @DisplayName("중간 번호 마스킹 (11자리)")
    void maskedElevenDigits() {
      // given
      PhoneNumber phone = PhoneNumber.of("010-1234-5678");

      // when & then
      assertThat(phone.masked()).isEqualTo("010-****-5678");
    }

    @Test
    @DisplayName("중간 번호 마스킹 (10자리)")
    void maskedTenDigits() {
      // given
      PhoneNumber phone = PhoneNumber.of("010-123-4567");

      // when & then
      assertThat(phone.masked()).isEqualTo("010-****-4567");
    }

    @Test
    @DisplayName("구 통신사 번호 마스킹")
    void maskedLegacyCarrier() {
      // given
      PhoneNumber phone = PhoneNumber.of("011-1234-5678");

      // when & then
      assertThat(phone.masked()).isEqualTo("011-****-5678");
    }
  }

  @Nested
  @DisplayName("withoutHyphen - 하이픈 제거")
  class WithoutHyphen {

    @Test
    @DisplayName("11자리 번호")
    void elevenDigits() {
      // given
      PhoneNumber phone = PhoneNumber.of("010-1234-5678");

      // when & then
      assertThat(phone.withoutHyphen()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("10자리 번호")
    void tenDigits() {
      // given
      PhoneNumber phone = PhoneNumber.of("010-123-4567");

      // when & then
      assertThat(phone.withoutHyphen()).isEqualTo("0101234567");
    }
  }

  @Nested
  @DisplayName("equals & hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("같은 값이면 동등 (다른 입력 형식)")
    void equalWhenSameValueDifferentFormat() {
      // given
      PhoneNumber phone1 = PhoneNumber.of("010-1234-5678");
      PhoneNumber phone2 = PhoneNumber.of("01012345678");
      PhoneNumber phone3 = PhoneNumber.of("010 1234 5678");

      // then
      assertThat(phone1).isEqualTo(phone2);
      assertThat(phone2).isEqualTo(phone3);
      assertThat(phone1.hashCode()).isEqualTo(phone2.hashCode());
    }

    @Test
    @DisplayName("다른 값이면 다름")
    void notEqualWhenDifferentValue() {
      // given
      PhoneNumber phone1 = PhoneNumber.of("010-1234-5678");
      PhoneNumber phone2 = PhoneNumber.of("010-9876-5432");

      // then
      assertThat(phone1).isNotEqualTo(phone2);
    }
  }
}