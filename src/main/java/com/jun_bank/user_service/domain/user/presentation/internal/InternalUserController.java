package com.jun_bank.user_service.domain.user.presentation.internal;

import com.jun_bank.common_lib.api.ApiResponse;
import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;
import com.jun_bank.user_service.domain.user.application.port.in.GetUserUseCase;
import com.jun_bank.user_service.domain.user.application.port.in.SearchUserUseCase;
import com.jun_bank.user_service.domain.user.domain.model.UserStatus;
import com.jun_bank.user_service.domain.user.presentation.internal.dto.response.UserExistsResponse;
import com.jun_bank.user_service.domain.user.presentation.internal.dto.response.UserInternalResponse;
import com.jun_bank.user_service.domain.user.presentation.internal.mapper.UserInternalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 내부 서비스 통신용 Controller
 * <p>
 * 다른 마이크로서비스에서 호출하는 내부 API입니다.
 * Gateway를 통하지 않고 직접 호출됩니다.
 *
 * <h3>API 목록:</h3>
 * <ul>
 *   <li>GET /internal/users/{userId} - 사용자 조회</li>
 *   <li>GET /internal/users/{userId}/exists - 사용자 존재 확인</li>
 *   <li>GET /internal/users/by-email - 이메일로 조회</li>
 *   <li>POST /internal/users/batch - 다건 사용자 조회</li>
 * </ul>
 *
 * <h3>호출 서비스:</h3>
 * <ul>
 *   <li>Auth Server: 로그인 시 사용자 조회</li>
 *   <li>Account Service: 계좌 생성 시 사용자 확인</li>
 *   <li>Card Service: 카드 발급 시 사용자 확인</li>
 *   <li>Transfer Service: 이체 시 사용자 확인</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

  private final GetUserUseCase getUserUseCase;
  private final SearchUserUseCase searchUserUseCase;
  private final UserInternalMapper mapper;

  /**
   * 사용자 조회 (내부용)
   * <p>
   * 전화번호 원본을 포함한 전체 정보를 반환합니다.
   */
  @GetMapping("/{userId}")
  public ResponseEntity<ApiResponse<UserInternalResponse>> getUser(
      @PathVariable String userId
  ) {
    log.debug("[Internal] 사용자 조회: userId={}", userId);

    UserResult result = getUserUseCase.getUserByIdForOwner(userId);
    UserInternalResponse response = mapper.toInternalResponse(result);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 이메일로 사용자 조회 (내부용)
   * <p>
   * Auth Server에서 로그인 시 사용합니다.
   */
  @GetMapping("/by-email")
  public ResponseEntity<ApiResponse<UserInternalResponse>> getUserByEmail(
      @RequestParam String email
  ) {
    log.debug("[Internal] 이메일로 사용자 조회: email={}", email);

    UserResult result = getUserUseCase.getUserByEmailForOwner(email);
    UserInternalResponse response = mapper.toInternalResponse(result);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 사용자 존재 확인
   * <p>
   * 사용자가 존재하고 활성 상태인지 확인합니다.
   */
  @GetMapping("/{userId}/exists")
  public ResponseEntity<ApiResponse<UserExistsResponse>> checkUserExists(
      @PathVariable String userId
  ) {
    log.debug("[Internal] 사용자 존재 확인: userId={}", userId);

    try {
      UserResult result = getUserUseCase.getUserById(userId);
      boolean isActive = UserStatus.ACTIVE.equals(result.status());
      UserExistsResponse response = new UserExistsResponse(true, isActive, userId);

      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (Exception e) {
      log.debug("[Internal] 사용자 없음: userId={}", userId);
      UserExistsResponse response = new UserExistsResponse(false, false, userId);

      return ResponseEntity.ok(ApiResponse.success(response));
    }
  }

  /**
   * 다건 사용자 조회
   * <p>
   * 여러 사용자 ID로 한 번에 조회합니다.
   * 존재하지 않는 ID는 결과에서 제외됩니다.
   */
  @PostMapping("/batch")
  public ResponseEntity<ApiResponse<List<UserInternalResponse>>> getUsersBatch(
      @RequestBody List<String> userIds
  ) {
    log.debug("[Internal] 다건 사용자 조회: count={}", userIds.size());

    List<UserResult> results = searchUserUseCase.findByIds(userIds);
    List<UserInternalResponse> responses = mapper.toInternalResponses(results);

    return ResponseEntity.ok(ApiResponse.success(responses));
  }
}