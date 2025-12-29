package com.jun_bank.user_service.domain.user.application.port.out.dto;

import com.jun_bank.user_service.domain.user.domain.model.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 검색 조건 DTO
 * <p>
 * Repository Port에서 동적 검색에 사용되는 조건 객체입니다.
 * 모든 필드는 nullable이며, null인 필드는 검색 조건에서 제외됩니다.
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // 이름으로 검색 (삭제된 사용자 제외, 생성일 내림차순)
 * UserSearchCondition condition = UserSearchCondition.builder()
 *     .name("홍길동")
 *     .build();
 *
 * // 상태 + 기간 조합 검색
 * UserSearchCondition condition = UserSearchCondition.builder()
 *     .status(UserStatus.ACTIVE)
 *     .createdAtFrom(LocalDateTime.now().minusDays(30))
 *     .sortField(SortField.CREATED_AT)
 *     .sortDirection(SortDirection.DESC)
 *     .build();
 *
 * // 삭제된 사용자 포함 검색 (관리자용)
 * UserSearchCondition condition = UserSearchCondition.builder()
 *     .includeDeleted(true)
 *     .build();
 * }</pre>
 *
 * @param email 이메일 (부분 일치 검색)
 * @param name 이름 (부분 일치 검색)
 * @param phoneNumber 전화번호 (정확히 일치)
 * @param status 사용자 상태
 * @param birthDateFrom 생년월일 시작 범위
 * @param birthDateTo 생년월일 종료 범위
 * @param createdAtFrom 생성일시 시작 범위
 * @param createdAtTo 생성일시 종료 범위
 * @param includeDeleted 삭제된 사용자 포함 여부 (기본값: false)
 * @param sortField 정렬 필드 (기본값: CREATED_AT)
 * @param sortDirection 정렬 방향 (기본값: DESC)
 */
public record UserSearchCondition(
    String email,
    String name,
    String phoneNumber,
    UserStatus status,
    LocalDate birthDateFrom,
    LocalDate birthDateTo,
    LocalDateTime createdAtFrom,
    LocalDateTime createdAtTo,
    Boolean includeDeleted,
    SortField sortField,
    SortDirection sortDirection
) {

  /**
   * 정렬 필드
   */
  public enum SortField {
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
    NAME("name"),
    EMAIL("email"),
    BIRTH_DATE("birthDate");

    private final String fieldName;

    SortField(String fieldName) {
      this.fieldName = fieldName;
    }

    public String getFieldName() {
      return fieldName;
    }
  }

  /**
   * 정렬 방향
   */
  public enum SortDirection {
    ASC, DESC
  }

  /**
   * 기본 검색 조건 생성
   * <p>
   * 삭제된 사용자 제외, 생성일 내림차순 정렬
   * </p>
   *
   * @return 빈 검색 조건 (기본값 적용)
   */
  public static UserSearchCondition empty() {
    return new UserSearchCondition(
        null, null, null, null,
        null, null, null, null,
        false,
        SortField.CREATED_AT,
        SortDirection.DESC
    );
  }

  /**
   * 빌더 시작
   *
   * @return Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * 삭제 포함 여부 반환 (null-safe)
   *
   * @return 삭제 포함 여부 (null이면 false)
   */
  public boolean isIncludeDeleted() {
    return Boolean.TRUE.equals(includeDeleted);
  }

  /**
   * 정렬 필드 반환 (null-safe)
   *
   * @return 정렬 필드 (null이면 CREATED_AT)
   */
  public SortField getSortFieldOrDefault() {
    return sortField != null ? sortField : SortField.CREATED_AT;
  }

  /**
   * 정렬 방향 반환 (null-safe)
   *
   * @return 정렬 방향 (null이면 DESC)
   */
  public SortDirection getSortDirectionOrDefault() {
    return sortDirection != null ? sortDirection : SortDirection.DESC;
  }

  /**
   * UserSearchCondition 빌더
   */
  public static class Builder {
    private String email;
    private String name;
    private String phoneNumber;
    private UserStatus status;
    private LocalDate birthDateFrom;
    private LocalDate birthDateTo;
    private LocalDateTime createdAtFrom;
    private LocalDateTime createdAtTo;
    private Boolean includeDeleted = false;
    private SortField sortField = SortField.CREATED_AT;
    private SortDirection sortDirection = SortDirection.DESC;

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder phoneNumber(String phoneNumber) {
      this.phoneNumber = phoneNumber;
      return this;
    }

    public Builder status(UserStatus status) {
      this.status = status;
      return this;
    }

    public Builder birthDateFrom(LocalDate birthDateFrom) {
      this.birthDateFrom = birthDateFrom;
      return this;
    }

    public Builder birthDateTo(LocalDate birthDateTo) {
      this.birthDateTo = birthDateTo;
      return this;
    }

    public Builder birthDateBetween(LocalDate from, LocalDate to) {
      this.birthDateFrom = from;
      this.birthDateTo = to;
      return this;
    }

    public Builder createdAtFrom(LocalDateTime createdAtFrom) {
      this.createdAtFrom = createdAtFrom;
      return this;
    }

    public Builder createdAtTo(LocalDateTime createdAtTo) {
      this.createdAtTo = createdAtTo;
      return this;
    }

    public Builder createdAtBetween(LocalDateTime from, LocalDateTime to) {
      this.createdAtFrom = from;
      this.createdAtTo = to;
      return this;
    }

    public Builder includeDeleted(Boolean includeDeleted) {
      this.includeDeleted = includeDeleted;
      return this;
    }

    public Builder sortField(SortField sortField) {
      this.sortField = sortField;
      return this;
    }

    public Builder sortDirection(SortDirection sortDirection) {
      this.sortDirection = sortDirection;
      return this;
    }

    public Builder sortBy(SortField field, SortDirection direction) {
      this.sortField = field;
      this.sortDirection = direction;
      return this;
    }

    public UserSearchCondition build() {
      return new UserSearchCondition(
          email, name, phoneNumber, status,
          birthDateFrom, birthDateTo,
          createdAtFrom, createdAtTo,
          includeDeleted, sortField, sortDirection
      );
    }
  }
}