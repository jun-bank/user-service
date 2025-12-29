package com.jun_bank.user_service.domain.user.infrastructure.persistence.jpa;

import com.jun_bank.user_service.domain.user.application.port.out.dto.UserSearchCondition;
import com.jun_bank.user_service.domain.user.application.port.out.dto.UserSearchCondition.SortDirection;
import com.jun_bank.user_service.domain.user.application.port.out.dto.UserSearchCondition.SortField;
import com.jun_bank.user_service.domain.user.domain.model.UserStatus;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.QUserEntity;
import com.jun_bank.user_service.domain.user.infrastructure.persistence.entity.UserEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User QueryDSL Repository 구현체
 * <p>
 * {@link UserSearchCondition}을 기반으로 동적 쿼리를 수행합니다.
 * 모든 목록/검색 쿼리는 이 클래스의 search() 메서드를 통해 처리됩니다.
 *
 * <h3>동적 조건 처리:</h3>
 * <ul>
 *   <li>null인 조건은 쿼리에서 제외</li>
 *   <li>문자열 검색은 부분 일치 (contains)</li>
 *   <li>날짜 범위는 between 또는 gte/lte</li>
 *   <li>정렬은 SortField + SortDirection으로 처리</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl {

  private final JPAQueryFactory queryFactory;

  private static final QUserEntity user = QUserEntity.userEntity;

  /**
   * 동적 조건으로 사용자 검색 (페이징)
   *
   * @param condition 검색 조건
   * @param pageable  페이징 정보
   * @return Page<UserEntity>
   */
  public Page<UserEntity> search(UserSearchCondition condition, Pageable pageable) {
    BooleanBuilder builder = buildConditions(condition);

    // 컨텐츠 조회
    List<UserEntity> content = queryFactory
        .selectFrom(user)
        .where(builder)
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .orderBy(getOrderSpecifier(condition))
        .fetch();

    // 카운트 쿼리 (필요할 때만 실행 - 최적화)
    JPAQuery<Long> countQuery = queryFactory
        .select(user.count())
        .from(user)
        .where(builder);

    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
  }

  /**
   * 동적 조건으로 사용자 검색 (전체 목록)
   *
   * @param condition 검색 조건
   * @return List<UserEntity>
   */
  public List<UserEntity> searchAll(UserSearchCondition condition) {
    BooleanBuilder builder = buildConditions(condition);

    return queryFactory
        .selectFrom(user)
        .where(builder)
        .orderBy(getOrderSpecifier(condition))
        .fetch();
  }

  /**
   * 동적 조건으로 사용자 수 조회
   *
   * @param condition 검색 조건
   * @return 사용자 수
   */
  public long count(UserSearchCondition condition) {
    BooleanBuilder builder = buildConditions(condition);

    Long count = queryFactory
        .select(user.count())
        .from(user)
        .where(builder)
        .fetchOne();

    return count != null ? count : 0L;
  }

  // ========================================
  // Private 메서드
  // ========================================

  /**
   * 검색 조건을 BooleanBuilder로 변환
   */
  private BooleanBuilder buildConditions(UserSearchCondition condition) {
    BooleanBuilder builder = new BooleanBuilder();

    // Soft Delete 처리 (기본: 삭제 제외)
    if (!condition.isIncludeDeleted()) {
      builder.and(user.isDeleted.eq(false));
    }

    // 동적 조건 추가
    builder.and(emailContains(condition.email()));
    builder.and(nameContains(condition.name()));
    builder.and(phoneNumberEquals(condition.phoneNumber()));
    builder.and(statusEquals(condition.status()));
    builder.and(birthDateBetween(condition.birthDateFrom(), condition.birthDateTo()));
    builder.and(createdAtBetween(condition.createdAtFrom(), condition.createdAtTo()));

    return builder;
  }

  /**
   * 정렬 조건 생성
   */
  private OrderSpecifier<?> getOrderSpecifier(UserSearchCondition condition) {
    SortField sortField = condition.getSortFieldOrDefault();
    SortDirection sortDirection = condition.getSortDirectionOrDefault();

    boolean isAsc = sortDirection == SortDirection.ASC;

    return switch (sortField) {
      case CREATED_AT -> isAsc ? user.createdAt.asc() : user.createdAt.desc();
      case UPDATED_AT -> isAsc ? user.updatedAt.asc() : user.updatedAt.desc();
      case NAME -> isAsc ? user.name.asc() : user.name.desc();
      case EMAIL -> isAsc ? user.email.asc() : user.email.desc();
      case BIRTH_DATE -> isAsc ? user.birthDate.asc() : user.birthDate.desc();
    };
  }

  // ========================================
  // 조건 메서드 (BooleanExpression 반환)
  // ========================================

  private BooleanExpression emailContains(String email) {
    return StringUtils.hasText(email) ? user.email.containsIgnoreCase(email) : null;
  }

  private BooleanExpression nameContains(String name) {
    return StringUtils.hasText(name) ? user.name.contains(name) : null;
  }

  private BooleanExpression phoneNumberEquals(String phoneNumber) {
    return StringUtils.hasText(phoneNumber) ? user.phoneNumber.eq(phoneNumber) : null;
  }

  private BooleanExpression statusEquals(UserStatus status) {
    return status != null ? user.status.eq(status) : null;
  }

  private BooleanExpression birthDateBetween(LocalDate from, LocalDate to) {
    if (from != null && to != null) {
      return user.birthDate.between(from, to);
    }
    if (from != null) {
      return user.birthDate.goe(from);
    }
    if (to != null) {
      return user.birthDate.loe(to);
    }
    return null;
  }

  private BooleanExpression createdAtBetween(LocalDateTime from, LocalDateTime to) {
    if (from != null && to != null) {
      return user.createdAt.between(from, to);
    }
    if (from != null) {
      return user.createdAt.goe(from);
    }
    if (to != null) {
      return user.createdAt.loe(to);
    }
    return null;
  }
}