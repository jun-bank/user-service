package com.jun_bank.user_service.domain.user.domain.model.vo;

import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserId VO 테스트")
class UserIdTest {

  @Nested
  @DisplayName("of - 생성")
  class Of {

    @Nested
    @DisplayName("성공 케이스")
    class Success {

      @Test
      @DisplayName("generateId()로 생성된 ID는 유효")
      void generatedIdIsValid() {
        // given
        String generatedId = UserId.generateId();

        // when
        UserId userId = UserId.of(generatedId);

        // then
        assertThat(userId.value()).isEqualTo(generatedId);
        assertThat(userId.value()).startsWith("USR-");
      }

      @Test
      @DisplayName("여러 번 생성해도 모두 유효")
      void multipleGeneratedIdsAreValid() {
        for (int i = 0; i < 10; i++) {
          String id = UserId.generateId();
          UserId userId = UserId.of(id);
          assertThat(userId.value()).isEqualTo(id);
        }
      }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

      @ParameterizedTest
      @NullAndEmptySource
      @DisplayName("null 또는 빈 문자열")
      void nullOrEmpty(String id) {
        assertThatThrownBy(() -> UserId.of(id))
            .isInstanceOf(UserException.class);
      }

      @Test
      @DisplayName("공백 문자열")
      void blankString() {
        assertThatThrownBy(() -> UserId.of("   "))
            .isInstanceOf(UserException.class);
      }

      @ParameterizedTest
      @ValueSource(strings = {
          "INVALID",            // 프리픽스 없음
          "USR",                // 하이픈 없음
          "USR-",               // ID 부분 없음
          "ACC-a1b2c3d4",       // 잘못된 프리픽스
          "usr-a1b2c3d4",       // 소문자 프리픽스
          "USR_a1b2c3d4",       // 하이픈 대신 언더스코어
          "USR--a1b2c3d4",      // 하이픈 중복
          " USR-a1b2c3d4",      // 앞 공백
          "USR-a1b2c3d4 "       // 뒤 공백
      })
      @DisplayName("유효하지 않은 ID 형식")
      void invalidFormat(String id) {
        assertThatThrownBy(() -> UserId.of(id))
            .isInstanceOf(UserException.class);
      }
    }
  }

  @Nested
  @DisplayName("generateId - ID 생성")
  class GenerateId {

    @Test
    @DisplayName("USR- 프리픽스로 시작")
    void startsWithPrefix() {
      // when
      String id = UserId.generateId();

      // then
      assertThat(id).startsWith("USR-");
    }

    @Test
    @DisplayName("일정한 길이")
    void consistentLength() {
      // when
      String id1 = UserId.generateId();
      String id2 = UserId.generateId();
      String id3 = UserId.generateId();

      // then - 모두 같은 길이
      assertThat(id1.length()).isEqualTo(id2.length());
      assertThat(id2.length()).isEqualTo(id3.length());
    }

    @RepeatedTest(10)
    @DisplayName("매번 고유한 ID 생성")
    void uniqueIds() {
      // given
      Set<String> ids = new HashSet<>();

      // when
      for (int i = 0; i < 100; i++) {
        ids.add(UserId.generateId());
      }

      // then
      assertThat(ids).hasSize(100);
    }

    @Test
    @DisplayName("생성된 ID는 UserId.of()로 생성 가능")
    void generatedIdCanBeUsedWithOf() {
      // given
      String id = UserId.generateId();

      // when & then (예외 없이 생성 가능)
      UserId userId = UserId.of(id);
      assertThat(userId.value()).isEqualTo(id);
    }
  }

  @Nested
  @DisplayName("value - 값 반환")
  class Value {

    @Test
    @DisplayName("생성된 값 그대로 반환")
    void returnOriginalValue() {
      // given
      String generatedId = UserId.generateId();
      UserId userId = UserId.of(generatedId);

      // when & then
      assertThat(userId.value()).isEqualTo(generatedId);
    }
  }

  @Nested
  @DisplayName("PREFIX 상수")
  class PrefixConstant {

    @Test
    @DisplayName("PREFIX는 'USR'")
    void prefixValue() {
      assertThat(UserId.PREFIX).isEqualTo("USR");
    }

    @Test
    @DisplayName("generateId() 결과는 PREFIX로 시작")
    void generatedIdStartsWithPrefix() {
      String id = UserId.generateId();
      assertThat(id).startsWith(UserId.PREFIX + "-");
    }
  }

  @Nested
  @DisplayName("equals & hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("같은 값이면 동등")
    void equalWhenSameValue() {
      // given
      String generatedId = UserId.generateId();
      UserId userId1 = UserId.of(generatedId);
      UserId userId2 = UserId.of(generatedId);

      // then
      assertThat(userId1).isEqualTo(userId2);
      assertThat(userId1.hashCode()).isEqualTo(userId2.hashCode());
    }

    @Test
    @DisplayName("다른 값이면 다름")
    void notEqualWhenDifferentValue() {
      // given
      UserId userId1 = UserId.of(UserId.generateId());
      UserId userId2 = UserId.of(UserId.generateId());

      // then
      assertThat(userId1).isNotEqualTo(userId2);
    }
  }
}