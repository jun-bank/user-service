package com.jun_bank.user_service.domain.user.application.port.out;

import com.jun_bank.user_service.domain.user.application.port.out.dto.UserSearchCondition;
import com.jun_bank.user_service.domain.user.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * User Repository Port (Output Port)
 * <p>
 * Application Layer에서 사용하는 Repository 인터페이스입니다.
 * Infrastructure Layer의 구현체와 분리하여 의존성 역전을 실현합니다.
 *
 * <h3>설계 원칙:</h3>
 * <ul>
 *   <li>도메인 모델(User)만 다룸 - Entity 노출 안함</li>
 *   <li>Infrastructure 의존성 없음</li>
 *   <li>비즈니스 관점의 메서드명 사용</li>
 *   <li>동적 검색은 {@link UserSearchCondition}으로 통합</li>
 * </ul>
 *
 * <h3>메서드 분류:</h3>
 * <ul>
 *   <li>핵심 조회 (PK, Unique Key): 명시적 메서드</li>
 *   <li>존재 여부 확인: 명시적 메서드</li>
 *   <li>동적 검색/목록: {@link #search(UserSearchCondition, Pageable)}</li>
 *   <li>저장/삭제: 명시적 메서드</li>
 * </ul>
 *
 * @see UserSearchCondition
 */
public interface UserRepository {

  // ========================================
  // 저장
  // ========================================

  /**
   * 사용자 저장 (신규 생성 또는 수정)
   * <p>
   * {@link User#isNew()}가 true이면 신규 저장 (ID 자동 생성),
   * false이면 기존 엔티티 수정 (더티체킹)
   * </p>
   *
   * @param user 저장할 사용자
   * @return 저장된 사용자 (ID 포함)
   */
  User save(User user);

  // ========================================
  // 단건 조회 (PK, Unique Key)
  // ========================================

  /**
   * ID로 사용자 조회
   * <p>
   * Soft Delete된 사용자는 조회되지 않습니다.
   * 삭제된 사용자 포함 조회는 {@link #search(UserSearchCondition, Pageable)}를 사용하세요.
   * </p>
   *
   * @param userId 사용자 ID
   * @return Optional<User>
   */
  Optional<User> findById(String userId);

  /**
   * 이메일로 사용자 조회
   * <p>
   * Soft Delete된 사용자는 조회되지 않습니다.
   * </p>
   *
   * @param email 이메일
   * @return Optional<User>
   */
  Optional<User> findByEmail(String email);

  // ========================================
  // 존재 여부 확인
  // ========================================

  /**
   * 이메일 존재 여부 확인
   * <p>
   * 회원가입 시 이메일 중복 체크에 사용합니다.
   * Soft Delete된 사용자는 제외됩니다.
   * </p>
   *
   * @param email 이메일
   * @return 존재하면 true
   */
  boolean existsByEmail(String email);

  /**
   * 전화번호 존재 여부 확인
   * <p>
   * Soft Delete된 사용자는 제외됩니다.
   * </p>
   *
   * @param phoneNumber 전화번호
   * @return 존재하면 true
   */
  boolean existsByPhoneNumber(String phoneNumber);

  // ========================================
  // 동적 검색 (SearchCondition 방식)
  // ========================================

  /**
   * 동적 조건으로 사용자 검색 (페이징)
   * <p>
   * {@link UserSearchCondition}의 조건들이 AND로 조합됩니다.
   * null인 조건은 무시됩니다.
   * </p>
   *
   * <h4>사용 예:</h4>
   * <pre>{@code
   * // 활성 사용자 중 이름 검색
   * UserSearchCondition condition = UserSearchCondition.builder()
   *     .name("홍길동")
   *     .status(UserStatus.ACTIVE)
   *     .build();
   *
   * Page<User> result = userRepository.search(condition, PageRequest.of(0, 10));
   * }</pre>
   *
   * @param condition 검색 조건
   * @param pageable  페이징 정보
   * @return Page<User>
   */
  Page<User> search(UserSearchCondition condition, Pageable pageable);

  /**
   * 동적 조건으로 사용자 검색 (전체 목록)
   * <p>
   * 페이징 없이 전체 결과를 반환합니다.
   * 대량 데이터 주의가 필요합니다.
   * </p>
   *
   * @param condition 검색 조건
   * @return List<User>
   */
  List<User> searchAll(UserSearchCondition condition);

  /**
   * 동적 조건으로 사용자 수 조회
   *
   * @param condition 검색 조건
   * @return 사용자 수
   */
  long count(UserSearchCondition condition);

  // ========================================
  // 배치/벌크 조회
  // ========================================

  /**
   * 여러 ID로 사용자 목록 조회
   * <p>
   * Soft Delete된 사용자는 제외됩니다.
   * 서비스 간 통신 시 다건 조회에 사용합니다.
   * </p>
   *
   * @param userIds 사용자 ID 목록
   * @return List<User>
   */
  List<User> findByIds(List<String> userIds);

  // ========================================
  // 삭제
  // ========================================

  /**
   * 사용자 삭제 (Soft Delete)
   * <p>
   * 실제 삭제가 아닌 상태 변경으로 처리합니다.
   * 권장: {@link User#withdraw()} 호출 후 {@link #save(User)}를 사용하세요.
   * </p>
   *
   * @param userId    삭제할 사용자 ID
   * @param deletedBy 삭제자 ID
   */
  void delete(String userId, String deletedBy);
}