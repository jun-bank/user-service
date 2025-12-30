package com.jun_bank.user_service.domain.user.presentation.api;

import com.jun_bank.common_lib.api.ApiResponse;
import com.jun_bank.common_lib.api.PageResponse;
import com.jun_bank.user_service.domain.user.application.dto.result.UserResult;
import com.jun_bank.user_service.domain.user.application.port.in.CreateUserUseCase;
import com.jun_bank.user_service.domain.user.application.port.in.DeleteUserUseCase;
import com.jun_bank.user_service.domain.user.application.port.in.GetUserUseCase;
import com.jun_bank.user_service.domain.user.application.port.in.SearchUserUseCase;
import com.jun_bank.user_service.domain.user.application.port.in.UpdateUserUseCase;
import com.jun_bank.user_service.domain.user.application.port.out.dto.UserSearchCondition;
import com.jun_bank.user_service.domain.user.domain.model.UserStatus;
import com.jun_bank.user_service.domain.user.presentation.api.dto.request.CreateUserRequest;
import com.jun_bank.user_service.domain.user.presentation.api.dto.request.UpdateUserRequest;
import com.jun_bank.user_service.domain.user.presentation.api.dto.response.EmailCheckResponse;
import com.jun_bank.user_service.domain.user.presentation.api.dto.response.UserResponse;
import com.jun_bank.user_service.domain.user.presentation.api.mapper.UserApiMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 REST Controller
 * <p>
 * 사용자 관련 외부 API 엔드포인트를 제공합니다.
 *
 * <h3>API 목록:</h3>
 * <ul>
 *   <li>POST /api/v1/users - 회원가입</li>
 *   <li>GET /api/v1/users/me - 본인 정보 조회</li>
 *   <li>GET /api/v1/users/{userId} - 사용자 조회</li>
 *   <li>GET /api/v1/users/by-email - 이메일로 조회</li>
 *   <li>GET /api/v1/users/check-email - 이메일 중복 확인</li>
 *   <li>PATCH /api/v1/users/{userId} - 프로필 수정</li>
 *   <li>DELETE /api/v1/users/{userId} - 회원 탈퇴</li>
 *   <li>GET /api/v1/users - 사용자 검색 (관리자)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final CreateUserUseCase createUserUseCase;
  private final GetUserUseCase getUserUseCase;
  private final UpdateUserUseCase updateUserUseCase;
  private final DeleteUserUseCase deleteUserUseCase;
  private final SearchUserUseCase searchUserUseCase;
  private final UserApiMapper mapper;

  // ========================================
  // 회원가입
  // ========================================

  /**
   * 회원가입
   */
  @PostMapping
  public ResponseEntity<ApiResponse<UserResponse>> createUser(
      @Valid @RequestBody CreateUserRequest request
  ) {
    log.info("회원가입 요청: email={}", request.email());

    UserResult result = createUserUseCase.createUser(mapper.toCommand(request));
    UserResponse response = mapper.toResponse(result);

    log.info("회원가입 완료: userId={}", result.userId());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "회원가입이 완료되었습니다."));
  }

  // ========================================
  // 조회
  // ========================================

  /**
   * 본인 정보 조회
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
      @RequestHeader("X-User-Id") String userId
  ) {
    log.debug("본인 정보 조회: userId={}", userId);

    UserResult result = getUserUseCase.getUserByIdForOwner(userId);
    UserResponse response = mapper.toResponse(result);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 사용자 조회
   */
  @GetMapping("/{userId}")
  public ResponseEntity<ApiResponse<UserResponse>> getUser(
      @PathVariable String userId
  ) {
    log.debug("사용자 조회: userId={}", userId);

    UserResult result = getUserUseCase.getUserById(userId);
    UserResponse response = mapper.toResponse(result);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 이메일로 사용자 조회
   */
  @GetMapping("/by-email")
  public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(
      @RequestParam String email
  ) {
    log.debug("이메일로 사용자 조회: email={}", email);

    UserResult result = getUserUseCase.getUserByEmail(email);
    UserResponse response = mapper.toResponse(result);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 이메일 중복 확인
   */
  @GetMapping("/check-email")
  public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(
      @RequestParam String email
  ) {
    log.debug("이메일 중복 확인: email={}", email);

    boolean exists = getUserUseCase.existsByEmail(email);
    EmailCheckResponse response = new EmailCheckResponse(!exists, email);

    String message = exists ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.";
    return ResponseEntity.ok(ApiResponse.success(response, message));
  }

  // ========================================
  // 수정
  // ========================================

  /**
   * 프로필 수정
   */
  @PatchMapping("/{userId}")
  public ResponseEntity<ApiResponse<UserResponse>> updateUser(
      @PathVariable String userId,
      @Valid @RequestBody UpdateUserRequest request,
      @RequestHeader("X-User-Id") String requesterId
  ) {
    log.info("프로필 수정 요청: userId={}, requesterId={}", userId, requesterId);

    // TODO: 본인 확인 로직 (userId == requesterId) 또는 관리자 권한 확인

    UserResult result = updateUserUseCase.updateUser(userId, mapper.toCommand(request));
    UserResponse response = mapper.toResponse(result);

    log.info("프로필 수정 완료: userId={}", userId);
    return ResponseEntity.ok(ApiResponse.success(response, "프로필이 수정되었습니다."));
  }

  // ========================================
  // 삭제
  // ========================================

  /**
   * 회원 탈퇴
   */
  @DeleteMapping("/{userId}")
  public ResponseEntity<ApiResponse<Void>> deleteUser(
      @PathVariable String userId,
      @RequestHeader("X-User-Id") String requesterId
  ) {
    log.info("회원 탈퇴 요청: userId={}, requesterId={}", userId, requesterId);

    // TODO: 본인 확인 로직 (userId == requesterId) 또는 관리자 권한 확인

    deleteUserUseCase.deleteUser(userId, requesterId);

    log.info("회원 탈퇴 완료: userId={}", userId);
    return ResponseEntity.ok(ApiResponse.successWithMessage("회원 탈퇴가 완료되었습니다."));
  }

  // ========================================
  // 검색 (관리자용)
  // ========================================

  /**
   * 사용자 검색
   */
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String phoneNumber,
      @RequestParam(required = false) UserStatus status,
      @RequestParam(defaultValue = "false") boolean includeDeleted,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    log.debug("사용자 검색: name={}, email={}, status={}", name, email, status);

    // TODO: 관리자 권한 확인

    UserSearchCondition condition = UserSearchCondition.builder()
        .name(name)
        .email(email)
        .phoneNumber(phoneNumber)
        .status(status)
        .includeDeleted(includeDeleted)
        .build();

    Page<UserResult> results = searchUserUseCase.search(condition, pageable);
    PageResponse<UserResponse> pageResponse = PageResponse.from(results, mapper::toResponse);

    return ResponseEntity.ok(ApiResponse.success(pageResponse));
  }
}