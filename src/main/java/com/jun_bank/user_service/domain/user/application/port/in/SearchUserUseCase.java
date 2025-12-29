package com.jun_bank.user_service.domain.user.application.port.in;

import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;
import com.jun_bank.user_service.domain.user.application.port.out.dto.UserSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 사용자 검색 UseCase (Input Port)
 * <p>
 * 사용자 목록/검색 비즈니스 로직을 정의하는 인터페이스입니다.
 *
 * <h3>검색 방식:</h3>
 * <ul>
 *   <li>동적 조건 검색: {@link UserSearchCondition} 사용</li>
 *   <li>페이징 지원: Spring Data Pageable</li>
 *   <li>정렬 지원: SortField + SortDirection</li>
 * </ul>
 *
 * <h3>삭제 처리:</h3>
 * <ul>
 *   <li>기본: 삭제된 사용자 제외</li>
 *   <li>관리자: includeDeleted=true로 삭제 포함 조회</li>
 * </ul>
 */
public interface SearchUserUseCase {

  /**
   * 동적 조건으로 사용자 검색 (페이징)
   *
   * @param condition 검색 조건
   * @param pageable  페이징 정보
   * @return 페이징된 사용자 목록
   */
  Page<UserResult> search(UserSearchCondition condition, Pageable pageable);

  /**
   * 동적 조건으로 사용자 검색 (전체 목록)
   * <p>
   * 페이징 없이 전체 결과를 반환합니다.
   * 대량 데이터 주의가 필요합니다.
   * </p>
   *
   * @param condition 검색 조건
   * @return 사용자 목록
   */
  List<UserResult> searchAll(UserSearchCondition condition);

  /**
   * 검색 조건에 맞는 사용자 수 조회
   *
   * @param condition 검색 조건
   * @return 사용자 수
   */
  long count(UserSearchCondition condition);

  /**
   * 여러 ID로 사용자 목록 조회
   * <p>
   * 서비스 간 통신 시 다건 조회에 사용합니다.
   * </p>
   *
   * @param userIds 사용자 ID 목록
   * @return 사용자 목록
   */
  List<UserResult> findByIds(List<String> userIds);
}