package com.jun_bank.user_service.domain.user.infrastructure.persistence;

import com.jun_bank.user_service.domain.user.application.port.out.dto.UserSearchCondition;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.domain.model.User;
import com.jun_bank.user_service.domain.user.domain.model.UserStatus;
import com.jun_bank.user_service.domain.user.domain.model.vo.Email;
import com.jun_bank.user_service.domain.user.domain.model.vo.PhoneNumber;
import com.jun_bank.user_service.domain.user.domain.model.vo.UserId;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.UserEntity;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.jpa.UserJpaRepository;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.jpa.UserQueryRepositoryImpl;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.mapper.UserMapper;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepositoryAdapter 테스트")
class UserRepositoryAdapterTest {

  @Mock
  private UserJpaRepository userJpaRepository;

  @Mock
  private UserQueryRepositoryImpl userQueryRepository;

  @Mock
  private UserMapper userMapper;

  @InjectMocks
  private UserRepositoryAdapter userRepositoryAdapter;

  private User newUser;
  private User existingUser;
  private UserEntity userEntity;
  private String userId;

  @BeforeEach
  void setUp() {
    userId = UserId.generateId();

    newUser = User.createBuilder()
        .email(Email.of("new@example.com"))
        .name("신규사용자")
        .phoneNumber(PhoneNumber.of("010-1111-2222"))
        .birthDate(LocalDate.of(1990, 1, 1))
        .build();

    existingUser = User.restoreBuilder()
        .userId(UserId.of(userId))
        .email(Email.of("existing@example.com"))
        .name("기존사용자")
        .phoneNumber(PhoneNumber.of("010-3333-4444"))
        .birthDate(LocalDate.of(1985, 5, 15))
        .status(UserStatus.ACTIVE)
        .isDeleted(false)
        .build();

    userEntity = UserEntity.of(
        userId,
        "existing@example.com",
        "기존사용자",
        "010-3333-4444",
        LocalDate.of(1985, 5, 15),
        UserStatus.ACTIVE
    );
  }

  // ========================================
  // save 테스트
  // ========================================

  @Nested
  @DisplayName("save")
  class SaveTest {

    @Test
    @DisplayName("신규 사용자를 저장한다")
    void save_NewUser_CreatesEntity() {
      // given
      UserEntity newEntity = UserEntity.of(
          UserId.generateId(),
          "new@example.com",
          "신규사용자",
          "010-1111-2222",
          LocalDate.of(1990, 1, 1),
          UserStatus.ACTIVE
      );

      given(userMapper.toEntity(newUser)).willReturn(newEntity);
      given(userJpaRepository.save(newEntity)).willReturn(newEntity);
      given(userMapper.toDomain(newEntity)).willReturn(existingUser);

      // when
      User result = userRepositoryAdapter.save(newUser);

      // then
      assertThat(result).isNotNull();
      verify(userMapper).toEntity(newUser);
      verify(userJpaRepository).save(newEntity);
      verify(userMapper).toDomain(newEntity);
      verify(userJpaRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("기존 사용자를 수정한다 (더티체킹)")
    void save_ExistingUser_UpdatesEntity() {
      // given
      given(userJpaRepository.findById(userId)).willReturn(Optional.of(userEntity));
      given(userMapper.toDomain(userEntity)).willReturn(existingUser);

      // when
      User result = userRepositoryAdapter.save(existingUser);

      // then
      assertThat(result).isNotNull();
      verify(userJpaRepository).findById(userId);
      verify(userMapper).updateEntity(userEntity, existingUser);
      verify(userMapper).toDomain(userEntity);
      verify(userJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 수정 시 예외 발생")
    void save_NonExistingUser_ThrowsException() {
      // given
      given(userJpaRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userRepositoryAdapter.save(existingUser))
          .isInstanceOf(UserException.class);
    }
  }

  // ========================================
  // findById 테스트
  // ========================================

  @Nested
  @DisplayName("findById")
  class FindByIdTest {

    @Test
    @DisplayName("ID로 사용자를 조회한다")
    void findById_ExistingUser_ReturnsUser() {
      // given
      given(userJpaRepository.findByUserIdAndIsDeletedFalse(userId))
          .willReturn(Optional.of(userEntity));
      given(userMapper.toDomain(userEntity)).willReturn(existingUser);

      // when
      Optional<User> result = userRepositoryAdapter.findById(userId);

      // then
      assertThat(result).isPresent();
      assertThat(result.get().getUserId().value()).isEqualTo(userId);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional 반환")
    void findById_NonExistingUser_ReturnsEmpty() {
      // given
      given(userJpaRepository.findByUserIdAndIsDeletedFalse(userId))
          .willReturn(Optional.empty());

      // when
      Optional<User> result = userRepositoryAdapter.findById(userId);

      // then
      assertThat(result).isEmpty();
    }
  }

  // ========================================
  // findByEmail 테스트
  // ========================================

  @Nested
  @DisplayName("findByEmail")
  class FindByEmailTest {

    @Test
    @DisplayName("이메일로 사용자를 조회한다")
    void findByEmail_ExistingEmail_ReturnsUser() {
      // given
      String email = "existing@example.com";
      given(userJpaRepository.findByEmailAndIsDeletedFalse(email))
          .willReturn(Optional.of(userEntity));
      given(userMapper.toDomain(userEntity)).willReturn(existingUser);

      // when
      Optional<User> result = userRepositoryAdapter.findByEmail(email);

      // then
      assertThat(result).isPresent();
      assertThat(result.get().getEmail().value()).isEqualTo(email);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 빈 Optional 반환")
    void findByEmail_NonExistingEmail_ReturnsEmpty() {
      // given
      given(userJpaRepository.findByEmailAndIsDeletedFalse("notfound@example.com"))
          .willReturn(Optional.empty());

      // when
      Optional<User> result = userRepositoryAdapter.findByEmail("notfound@example.com");

      // then
      assertThat(result).isEmpty();
    }
  }

  // ========================================
  // existsByEmail 테스트
  // ========================================

  @Nested
  @DisplayName("existsByEmail")
  class ExistsByEmailTest {

    @Test
    @DisplayName("이메일이 존재하면 true 반환")
    void existsByEmail_Exists_ReturnsTrue() {
      // given
      given(userJpaRepository.existsByEmailAndIsDeletedFalse("existing@example.com"))
          .willReturn(true);

      // when
      boolean result = userRepositoryAdapter.existsByEmail("existing@example.com");

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("이메일이 존재하지 않으면 false 반환")
    void existsByEmail_NotExists_ReturnsFalse() {
      // given
      given(userJpaRepository.existsByEmailAndIsDeletedFalse("notfound@example.com"))
          .willReturn(false);

      // when
      boolean result = userRepositoryAdapter.existsByEmail("notfound@example.com");

      // then
      assertThat(result).isFalse();
    }
  }

  // ========================================
  // existsByPhoneNumber 테스트
  // ========================================

  @Nested
  @DisplayName("existsByPhoneNumber")
  class ExistsByPhoneNumberTest {

    @Test
    @DisplayName("전화번호가 존재하면 true 반환")
    void existsByPhoneNumber_Exists_ReturnsTrue() {
      // given
      given(userJpaRepository.existsByPhoneNumberAndIsDeletedFalse("010-1234-5678"))
          .willReturn(true);

      // when
      boolean result = userRepositoryAdapter.existsByPhoneNumber("010-1234-5678");

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("전화번호가 존재하지 않으면 false 반환")
    void existsByPhoneNumber_NotExists_ReturnsFalse() {
      // given
      given(userJpaRepository.existsByPhoneNumberAndIsDeletedFalse("010-0000-0000"))
          .willReturn(false);

      // when
      boolean result = userRepositoryAdapter.existsByPhoneNumber("010-0000-0000");

      // then
      assertThat(result).isFalse();
    }
  }

  // ========================================
  // search 테스트
  // ========================================

  @Nested
  @DisplayName("search")
  class SearchTest {

    @Test
    @DisplayName("조건으로 사용자를 검색한다 (페이징)")
    void search_WithCondition_ReturnsPage() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder()
          .name("테스트")
          .status(UserStatus.ACTIVE)
          .build();
      Pageable pageable = PageRequest.of(0, 10);
      Page<UserEntity> entityPage = new PageImpl<>(List.of(userEntity), pageable, 1);

      given(userQueryRepository.search(condition, pageable)).willReturn(entityPage);
      given(userMapper.toDomain(userEntity)).willReturn(existingUser);

      // when
      Page<User> result = userRepositoryAdapter.search(condition, pageable);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("빈 조건으로 검색하면 전체 목록 반환")
    void search_EmptyCondition_ReturnsAll() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder().build();
      Pageable pageable = PageRequest.of(0, 10);
      Page<UserEntity> entityPage = new PageImpl<>(List.of(userEntity), pageable, 1);

      given(userQueryRepository.search(condition, pageable)).willReturn(entityPage);
      given(userMapper.toDomain(userEntity)).willReturn(existingUser);

      // when
      Page<User> result = userRepositoryAdapter.search(condition, pageable);

      // then
      assertThat(result).isNotNull();
    }
  }

  // ========================================
  // searchAll 테스트
  // ========================================

  @Nested
  @DisplayName("searchAll")
  class SearchAllTest {

    @Test
    @DisplayName("조건으로 전체 목록을 검색한다")
    void searchAll_WithCondition_ReturnsList() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder()
          .status(UserStatus.ACTIVE)
          .build();

      given(userQueryRepository.searchAll(condition)).willReturn(List.of(userEntity));
      given(userMapper.toDomain(userEntity)).willReturn(existingUser);

      // when
      List<User> result = userRepositoryAdapter.searchAll(condition);

      // then
      assertThat(result).hasSize(1);
    }
  }

  // ========================================
  // count 테스트
  // ========================================

  @Nested
  @DisplayName("count")
  class CountTest {

    @Test
    @DisplayName("조건에 맞는 사용자 수를 반환한다")
    void count_WithCondition_ReturnsCount() {
      // given
      UserSearchCondition condition = UserSearchCondition.builder()
          .status(UserStatus.ACTIVE)
          .build();
      given(userQueryRepository.count(condition)).willReturn(5L);

      // when
      long result = userRepositoryAdapter.count(condition);

      // then
      assertThat(result).isEqualTo(5L);
    }
  }

  // ========================================
  // findByIds 테스트
  // ========================================

  @Nested
  @DisplayName("findByIds")
  class FindByIdsTest {

    @Test
    @DisplayName("여러 ID로 사용자 목록을 조회한다")
    void findByIds_MultipleIds_ReturnsList() {
      // given
      List<String> userIds = List.of(userId, "USR-22222222");
      given(userJpaRepository.findByUserIdInAndIsDeletedFalse(userIds))
          .willReturn(List.of(userEntity));
      given(userMapper.toDomain(userEntity)).willReturn(existingUser);

      // when
      List<User> result = userRepositoryAdapter.findByIds(userIds);

      // then
      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("빈 ID 목록으로 조회하면 빈 리스트 반환")
    void findByIds_EmptyIds_ReturnsEmptyList() {
      // given
      given(userJpaRepository.findByUserIdInAndIsDeletedFalse(List.of()))
          .willReturn(List.of());

      // when
      List<User> result = userRepositoryAdapter.findByIds(List.of());

      // then
      assertThat(result).isEmpty();
    }
  }

  // ========================================
  // delete 테스트
  // ========================================

  @Nested
  @DisplayName("delete")
  class DeleteTest {

    @Test
    @DisplayName("사용자를 soft delete한다")
    void delete_ExistingUser_SoftDeletes() {
      // given
      given(userJpaRepository.findByUserIdAndIsDeletedFalse(userId))
          .willReturn(Optional.of(userEntity));

      // when
      userRepositoryAdapter.delete(userId, "admin");

      // then
      verify(userJpaRepository).findByUserIdAndIsDeletedFalse(userId);
      // userEntity.delete("admin")이 호출됨
    }

    @Test
    @DisplayName("존재하지 않는 사용자 삭제는 무시된다")
    void delete_NonExistingUser_DoesNothing() {
      // given
      given(userJpaRepository.findByUserIdAndIsDeletedFalse(userId))
          .willReturn(Optional.empty());

      // when
      userRepositoryAdapter.delete(userId, "admin");

      // then
      verify(userJpaRepository).findByUserIdAndIsDeletedFalse(userId);
      // 아무것도 하지 않음 (ifPresent)
    }
  }
}