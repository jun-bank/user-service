package com.jun_bank.user_service.domain.user.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserStatus 테스트")
class UserStatusTest {

  @Nested
  @DisplayName("상태별 속성")
  class StatusProperties {

    @Nested
    @DisplayName("ACTIVE")
    class Active {

      @Test
      @DisplayName("description = '정상'")
      void description() {
        assertThat(UserStatus.ACTIVE.getDescription()).isEqualTo("정상");
      }

      @Test
      @DisplayName("로그인 가능")
      void canLogin() {
        assertThat(UserStatus.ACTIVE.canLogin()).isTrue();
      }

      @Test
      @DisplayName("프로필 수정 가능")
      void canModifyProfile() {
        assertThat(UserStatus.ACTIVE.canModifyProfile()).isTrue();
      }
    }

    @Nested
    @DisplayName("INACTIVE")
    class Inactive {

      @Test
      @DisplayName("description = '휴면'")
      void description() {
        assertThat(UserStatus.INACTIVE.getDescription()).isEqualTo("휴면");
      }

      @Test
      @DisplayName("로그인 불가")
      void cannotLogin() {
        assertThat(UserStatus.INACTIVE.canLogin()).isFalse();
      }

      @Test
      @DisplayName("프로필 수정 가능")
      void canModifyProfile() {
        assertThat(UserStatus.INACTIVE.canModifyProfile()).isTrue();
      }
    }

    @Nested
    @DisplayName("SUSPENDED")
    class Suspended {

      @Test
      @DisplayName("description = '정지'")
      void description() {
        assertThat(UserStatus.SUSPENDED.getDescription()).isEqualTo("정지");
      }

      @Test
      @DisplayName("로그인 불가")
      void cannotLogin() {
        assertThat(UserStatus.SUSPENDED.canLogin()).isFalse();
      }

      @Test
      @DisplayName("프로필 수정 불가")
      void cannotModifyProfile() {
        assertThat(UserStatus.SUSPENDED.canModifyProfile()).isFalse();
      }
    }

    @Nested
    @DisplayName("DELETED")
    class Deleted {

      @Test
      @DisplayName("description = '탈퇴'")
      void description() {
        assertThat(UserStatus.DELETED.getDescription()).isEqualTo("탈퇴");
      }

      @Test
      @DisplayName("로그인 불가")
      void cannotLogin() {
        assertThat(UserStatus.DELETED.canLogin()).isFalse();
      }

      @Test
      @DisplayName("프로필 수정 불가")
      void cannotModifyProfile() {
        assertThat(UserStatus.DELETED.canModifyProfile()).isFalse();
      }
    }
  }

  @Nested
  @DisplayName("canTransitionTo - 상태 전이 가능 여부")
  class CanTransitionTo {

    @Nested
    @DisplayName("ACTIVE에서")
    class FromActive {

      @Test
      @DisplayName("INACTIVE로 전이 가능")
      void toInactive() {
        assertThat(UserStatus.ACTIVE.canTransitionTo(UserStatus.INACTIVE)).isTrue();
      }

      @Test
      @DisplayName("SUSPENDED로 전이 가능")
      void toSuspended() {
        assertThat(UserStatus.ACTIVE.canTransitionTo(UserStatus.SUSPENDED)).isTrue();
      }

      @Test
      @DisplayName("DELETED로 전이 가능")
      void toDeleted() {
        assertThat(UserStatus.ACTIVE.canTransitionTo(UserStatus.DELETED)).isTrue();
      }

      @Test
      @DisplayName("ACTIVE로 전이 불가 (같은 상태)")
      void toActive() {
        assertThat(UserStatus.ACTIVE.canTransitionTo(UserStatus.ACTIVE)).isFalse();
      }
    }

    @Nested
    @DisplayName("INACTIVE에서")
    class FromInactive {

      @Test
      @DisplayName("ACTIVE로 전이 가능 (휴면 해제)")
      void toActive() {
        assertThat(UserStatus.INACTIVE.canTransitionTo(UserStatus.ACTIVE)).isTrue();
      }

      @Test
      @DisplayName("DELETED로 전이 가능")
      void toDeleted() {
        assertThat(UserStatus.INACTIVE.canTransitionTo(UserStatus.DELETED)).isTrue();
      }

      @Test
      @DisplayName("SUSPENDED로 전이 불가")
      void toSuspended() {
        assertThat(UserStatus.INACTIVE.canTransitionTo(UserStatus.SUSPENDED)).isFalse();
      }
    }

    @Nested
    @DisplayName("SUSPENDED에서")
    class FromSuspended {

      @Test
      @DisplayName("ACTIVE로 전이 가능 (정지 해제)")
      void toActive() {
        assertThat(UserStatus.SUSPENDED.canTransitionTo(UserStatus.ACTIVE)).isTrue();
      }

      @Test
      @DisplayName("DELETED로 전이 가능")
      void toDeleted() {
        assertThat(UserStatus.SUSPENDED.canTransitionTo(UserStatus.DELETED)).isTrue();
      }

      @Test
      @DisplayName("INACTIVE로 전이 불가")
      void toInactive() {
        assertThat(UserStatus.SUSPENDED.canTransitionTo(UserStatus.INACTIVE)).isFalse();
      }
    }

    @Nested
    @DisplayName("DELETED에서")
    class FromDeleted {

      @ParameterizedTest
      @EnumSource(UserStatus.class)
      @DisplayName("어떤 상태로도 전이 불가 (최종 상태)")
      void toAnyStatus(UserStatus target) {
        assertThat(UserStatus.DELETED.canTransitionTo(target)).isFalse();
      }
    }

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    @DisplayName("같은 상태로의 전이는 모두 불가")
    void sameStatusTransitionNotAllowed(UserStatus status) {
      assertThat(status.canTransitionTo(status)).isFalse();
    }
  }

  @Nested
  @DisplayName("getAllowedTransitions - 전이 가능한 상태 목록")
  class GetAllowedTransitions {

    @Test
    @DisplayName("ACTIVE: INACTIVE, SUSPENDED, DELETED")
    void fromActive() {
      Set<UserStatus> allowed = UserStatus.ACTIVE.getAllowedTransitions();

      assertThat(allowed).containsExactlyInAnyOrder(
          UserStatus.INACTIVE,
          UserStatus.SUSPENDED,
          UserStatus.DELETED
      );
    }

    @Test
    @DisplayName("INACTIVE: ACTIVE, DELETED")
    void fromInactive() {
      Set<UserStatus> allowed = UserStatus.INACTIVE.getAllowedTransitions();

      assertThat(allowed).containsExactlyInAnyOrder(
          UserStatus.ACTIVE,
          UserStatus.DELETED
      );
    }

    @Test
    @DisplayName("SUSPENDED: ACTIVE, DELETED")
    void fromSuspended() {
      Set<UserStatus> allowed = UserStatus.SUSPENDED.getAllowedTransitions();

      assertThat(allowed).containsExactlyInAnyOrder(
          UserStatus.ACTIVE,
          UserStatus.DELETED
      );
    }

    @Test
    @DisplayName("DELETED: 빈 Set (최종 상태)")
    void fromDeleted() {
      Set<UserStatus> allowed = UserStatus.DELETED.getAllowedTransitions();

      assertThat(allowed).isEmpty();
    }
  }

  @Nested
  @DisplayName("상태 확인 메서드")
  class StatusCheckMethods {

    @Test
    @DisplayName("isActive()")
    void isActive() {
      assertThat(UserStatus.ACTIVE.isActive()).isTrue();
      assertThat(UserStatus.INACTIVE.isActive()).isFalse();
      assertThat(UserStatus.SUSPENDED.isActive()).isFalse();
      assertThat(UserStatus.DELETED.isActive()).isFalse();
    }

    @Test
    @DisplayName("isInactive()")
    void isInactive() {
      assertThat(UserStatus.INACTIVE.isInactive()).isTrue();
      assertThat(UserStatus.ACTIVE.isInactive()).isFalse();
      assertThat(UserStatus.SUSPENDED.isInactive()).isFalse();
      assertThat(UserStatus.DELETED.isInactive()).isFalse();
    }

    @Test
    @DisplayName("isSuspended()")
    void isSuspended() {
      assertThat(UserStatus.SUSPENDED.isSuspended()).isTrue();
      assertThat(UserStatus.ACTIVE.isSuspended()).isFalse();
      assertThat(UserStatus.INACTIVE.isSuspended()).isFalse();
      assertThat(UserStatus.DELETED.isSuspended()).isFalse();
    }

    @Test
    @DisplayName("isDeleted()")
    void isDeleted() {
      assertThat(UserStatus.DELETED.isDeleted()).isTrue();
      assertThat(UserStatus.ACTIVE.isDeleted()).isFalse();
      assertThat(UserStatus.INACTIVE.isDeleted()).isFalse();
      assertThat(UserStatus.SUSPENDED.isDeleted()).isFalse();
    }
  }

  @Nested
  @DisplayName("권한 요약")
  class PermissionSummary {

    @Test
    @DisplayName("로그인 가능한 상태는 ACTIVE만")
    void onlyActiveCanLogin() {
      for (UserStatus status : UserStatus.values()) {
        if (status == UserStatus.ACTIVE) {
          assertThat(status.canLogin()).isTrue();
        } else {
          assertThat(status.canLogin()).isFalse();
        }
      }
    }

    @Test
    @DisplayName("프로필 수정 가능: ACTIVE, INACTIVE")
    void canModifyProfileStatuses() {
      assertThat(UserStatus.ACTIVE.canModifyProfile()).isTrue();
      assertThat(UserStatus.INACTIVE.canModifyProfile()).isTrue();
      assertThat(UserStatus.SUSPENDED.canModifyProfile()).isFalse();
      assertThat(UserStatus.DELETED.canModifyProfile()).isFalse();
    }
  }
}