package com.jun_bank.user_service.domain.user.application.service;

import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;
import com.jun_bank.user_service.domain.user.application.port.in.SearchUserUseCase;
import com.jun_bank.user_service.domain.user.application.port.out.UserRepository;
import com.jun_bank.user_service.domain.user.application.port.out.dto.UserSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 검색 서비스
 * <p>
 * {@link SearchUserUseCase}를 구현하여 사용자 검색/목록 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchUserService implements SearchUserUseCase {

  private final UserRepository userRepository;

  @Override
  public Page<UserResult> search(UserSearchCondition condition, Pageable pageable) {
    log.debug("사용자 검색 요청: condition={}, pageable={}", condition, pageable);

    return userRepository.search(condition, pageable)
        .map(UserResult::from);
  }

  @Override
  public List<UserResult> searchAll(UserSearchCondition condition) {
    log.debug("사용자 전체 검색 요청: condition={}", condition);

    return userRepository.searchAll(condition)
        .stream()
        .map(UserResult::from)
        .toList();
  }

  @Override
  public long count(UserSearchCondition condition) {
    log.debug("사용자 수 조회 요청: condition={}", condition);

    return userRepository.count(condition);
  }

  @Override
  public List<UserResult> findByIds(List<String> userIds) {
    log.debug("ID 목록으로 사용자 조회 요청: userIds={}", userIds);

    return userRepository.findByIds(userIds)
        .stream()
        .map(UserResult::from)
        .toList();
  }
}