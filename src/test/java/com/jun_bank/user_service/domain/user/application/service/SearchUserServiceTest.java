package com.jun_bank.user_service.domain.user.application.service;

import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;
import com.jun_bank.user_service.domain.user.application.port.out.UserRepository;
import com.jun_bank.user_service.domain.user.application.port.out.dto.UserSearchCondition;
import com.jun_bank.user_service.domain.user.domain.model.User;
import com.jun_bank.user_service.domain.user.domain.model.UserStatus;
import com.jun_bank.user_service.domain.user.domain.model.vo.Email;
import com.jun_bank.user_service.domain.user.domain.model.vo.PhoneNumber;
import com.jun_bank.user_service.domain.user.domain.model.vo.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchUserService 테스트")
class SearchUserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private SearchUserService searchUserService;

  private User user1;
  private User user2;
  private User user3;

  @BeforeEach
  void setUp() {
    user1 = User.restoreBuilder()
        .userId(UserId.of("USR-00000001"))
        .email(Email.of("user1@example.com"))
        .name("사용자1")
        .phoneNumber(PhoneNumber.of("010-1111-1111"))
        .birthDate(LocalDate.of(1990, 1, 1))
        .status(UserStatus.ACTIVE)
        .isDeleted(false)
        .createdAt(LocalDateTime.now().minusDays(30))
        .updatedAt(LocalDateTime.now())
        .build();

    user2 = User.restoreBuilder()
        .userId(UserId.of("USR-00000002"))
        .email(Email.of("user2@example.com"))
        .name("사용자2")
        .phoneNumber(PhoneNumber.of("010-2222-2222"))
        .birthDate(LocalDate.of(1991, 2, 2))
        .status(UserStatus.ACTIVE)
        .isDeleted(false)
        .createdAt(LocalDateTime.now().minusDays(20))
        .updatedAt(LocalDateTime.now())
        .build();

    user3 = User.restoreBuilder()
        .userId(UserId.of("USR-00000003"))
        .email(Email.of("user3@example.com"))
        .name("사용자3")
        .phoneNumber(PhoneNumber.of("010-3333-3333"))
        .birthDate(LocalDate.of(1992, 3, 3))
        .status(UserStatus.INACTIVE)
        .isDeleted(false)
        .createdAt(LocalDateTime.now().minusDays(10))
        .updatedAt(LocalDateTime.now())
        .build();
  }

  // ========================================
  // search (페이징) 테스트
  // ========================================

  @Nested
  @DisplayName("search (페이징)")
  class SearchPagingTest {

    @Test
    @DisplayName("조건 없이 전체 검색")
    void search_NoCondition_ReturnsAll() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder().build();
      Pageable pageable = PageRequest.of(0, 10);
      Page<User> userPage = new PageImpl<>(List.of(user1, user2, user3), pageable, 3);

      given(userRepository.search(eq(condition), eq(pageable))).willReturn(userPage);

      // when
      Page<UserResult> result = searchUserService.search(condition, pageable);

      // then
      assertThat(result.getTotalElements()).isEqualTo(3);
      assertThat(result.getContent()).hasSize(3);
      verify(userRepository).search(condition, pageable);
    }

    @Test
    @DisplayName("이름으로 검색")
    void search_ByName() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder()
          .name("사용자1")
          .build();
      Pageable pageable = PageRequest.of(0, 10);
      Page<User> userPage = new PageImpl<>(List.of(user1), pageable, 1);

      given(userRepository.search(eq(condition), eq(pageable))).willReturn(userPage);

      // when
      Page<UserResult> result = searchUserService.search(condition, pageable);

      // then
      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent().get(0).name()).isEqualTo("사용자1");
    }

    @Test
    @DisplayName("상태로 검색")
    void search_ByStatus() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder()
          .status(UserStatus.INACTIVE)
          .build();
      Pageable pageable = PageRequest.of(0, 10);
      Page<User> userPage = new PageImpl<>(List.of(user3), pageable, 1);

      given(userRepository.search(eq(condition), eq(pageable))).willReturn(userPage);

      // when
      Page<UserResult> result = searchUserService.search(condition, pageable);

      // then
      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent().get(0).status()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    @DisplayName("검색 결과가 없는 경우 빈 페이지 반환")
    void search_NoResults_ReturnsEmptyPage() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder()
          .name("존재하지않는이름")
          .build();
      Pageable pageable = PageRequest.of(0, 10);
      Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

      given(userRepository.search(eq(condition), eq(pageable))).willReturn(emptyPage);

      // when
      Page<UserResult> result = searchUserService.search(condition, pageable);

      // then
      assertThat(result.getTotalElements()).isZero();
      assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("전화번호가 마스킹 처리된다")
    void search_PhoneNumberMasked() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder().build();
      Pageable pageable = PageRequest.of(0, 10);
      Page<User> userPage = new PageImpl<>(List.of(user1), pageable, 1);

      given(userRepository.search(eq(condition), eq(pageable))).willReturn(userPage);

      // when
      Page<UserResult> result = searchUserService.search(condition, pageable);

      // then
      assertThat(result.getContent().get(0).phoneNumber()).isEqualTo("010-****-1111");
    }
  }

  // ========================================
  // searchAll (전체) 테스트
  // ========================================

  @Nested
  @DisplayName("searchAll (전체)")
  class SearchAllTest {

    @Test
    @DisplayName("조건에 맞는 전체 사용자 반환")
    void searchAll_ReturnsAllMatching() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder()
          .status(UserStatus.ACTIVE)
          .build();
      List<User> users = List.of(user1, user2);

      given(userRepository.searchAll(eq(condition))).willReturn(users);

      // when
      List<UserResult> result = searchUserService.searchAll(condition);

      // then
      assertThat(result).hasSize(2);
      assertThat(result).extracting(UserResult::status)
          .containsOnly(UserStatus.ACTIVE);
      verify(userRepository).searchAll(condition);
    }

    @Test
    @DisplayName("조건에 맞는 사용자가 없으면 빈 리스트 반환")
    void searchAll_NoResults_ReturnsEmptyList() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder()
          .status(UserStatus.DELETED)
          .build();

      given(userRepository.searchAll(eq(condition))).willReturn(Collections.emptyList());

      // when
      List<UserResult> result = searchUserService.searchAll(condition);

      // then
      assertThat(result).isEmpty();
    }
  }

  // ========================================
  // count 테스트
  // ========================================

  @Nested
  @DisplayName("count")
  class CountTest {

    @Test
    @DisplayName("조건에 맞는 사용자 수 반환")
    void count_ReturnsMatchingCount() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder()
          .status(UserStatus.ACTIVE)
          .build();

      given(userRepository.count(eq(condition))).willReturn(2L);

      // when
      long result = searchUserService.count(condition);

      // then
      assertThat(result).isEqualTo(2L);
      verify(userRepository).count(condition);
    }

    @Test
    @DisplayName("조건에 맞는 사용자가 없으면 0 반환")
    void count_NoResults_ReturnsZero() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder()
          .status(UserStatus.DELETED)
          .build();

      given(userRepository.count(eq(condition))).willReturn(0L);

      // when
      long result = searchUserService.count(condition);

      // then
      assertThat(result).isZero();
    }
  }

  // ========================================
  // findByIds 테스트
  // ========================================

  @Nested
  @DisplayName("findByIds")
  class FindByIdsTest {

    @Test
    @DisplayName("ID 목록으로 사용자 조회")
    void findByIds_ReturnsMatchingUsers() {
      // given
      List<String> userIds = Arrays.asList("USR-00000001", "USR-00000002");
      List<User> users = List.of(user1, user2);

      given(userRepository.findByIds(eq(userIds))).willReturn(users);

      // when
      List<UserResult> result = searchUserService.findByIds(userIds);

      // then
      assertThat(result).hasSize(2);
      assertThat(result).extracting(UserResult::userId)
          .containsExactlyInAnyOrder("USR-00000001", "USR-00000002");
      verify(userRepository).findByIds(userIds);
    }

    @Test
    @DisplayName("존재하지 않는 ID는 결과에 포함되지 않는다")
    void findByIds_NonExistentIds_NotIncluded() {
      // given
      List<String> userIds = Arrays.asList("USR-00000001", "USR-99999999");
      List<User> users = List.of(user1);

      given(userRepository.findByIds(eq(userIds))).willReturn(users);

      // when
      List<UserResult> result = searchUserService.findByIds(userIds);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).userId()).isEqualTo("USR-00000001");
    }

    @Test
    @DisplayName("빈 ID 목록은 빈 결과 반환")
    void findByIds_EmptyIds_ReturnsEmptyList() {
      // given
      List<String> emptyIds = Collections.emptyList();

      given(userRepository.findByIds(eq(emptyIds))).willReturn(Collections.emptyList());

      // when
      List<UserResult> result = searchUserService.findByIds(emptyIds);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("전화번호가 마스킹 처리된다")
    void findByIds_PhoneNumberMasked() {
      // given
      List<String> userIds = List.of("USR-00000001");
      List<User> users = List.of(user1);

      given(userRepository.findByIds(eq(userIds))).willReturn(users);

      // when
      List<UserResult> result = searchUserService.findByIds(userIds);

      // then
      assertThat(result.get(0).phoneNumber()).isEqualTo("010-****-1111");
    }
  }

  // ========================================
  // UserResult 변환 검증 테스트
  // ========================================

  @Nested
  @DisplayName("UserResult 변환 검증")
  class UserResultMappingTest {

    @Test
    @DisplayName("모든 필드가 올바르게 매핑된다")
    void search_AllFieldsMapped() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder().build();
      Pageable pageable = PageRequest.of(0, 10);
      Page<User> userPage = new PageImpl<>(List.of(user1), pageable, 1);

      given(userRepository.search(eq(condition), eq(pageable))).willReturn(userPage);

      // when
      Page<UserResult> result = searchUserService.search(condition, pageable);

      // then
      UserResult userResult = result.getContent().get(0);
      assertThat(userResult.userId()).isEqualTo("USR-00000001");
      assertThat(userResult.email()).isEqualTo("user1@example.com");
      assertThat(userResult.name()).isEqualTo("사용자1");
      assertThat(userResult.birthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
      assertThat(userResult.status()).isEqualTo(UserStatus.ACTIVE);
      assertThat(userResult.statusDescription()).isEqualTo("정상");
    }
  }
}