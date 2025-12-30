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
import com.jun_bank.user_service.global.security.SecurityContextUtil;
import com.jun_bank.user_service.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
 *   <li>POST /api/v1/users - 회원가입 (비인증)</li>
 *   <li>GET /api/v1/users/me - 본인 정보 조회</li>
 *   <li>GET /api/v1/users/{userId} - 사용자 조회</li>
 *   <li>GET /api/v1/users/by-email - 이메일로 조회</li>
 *   <li>GET /api/v1/users/check-email - 이메일 중복 확인 (비인증)</li>
 *   <li>PATCH /api/v1/users/{userId} - 프로필 수정 (본인/관리자)</li>
 *   <li>DELETE /api/v1/users/{userId} - 회원 탈퇴 (본인/관리자)</li>
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
  private final SecurityContextUtil securityContextUtil;

  // ========================================
  // 회원가입 (비인증)
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
      @AuthenticationPrincipal UserPrincipal principal
  ) {
    log.debug("본인 정보 조회: userId={}", principal.getUserId());

    UserResult result = getUserUseCase.getUserByIdForOwner(principal.getUserId());
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
   * 이메일 중복 확인 (비인증)
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
   * <p>
   * 본인 또는 관리자만 수정 가능합니다.
   */
  @PatchMapping("/{userId}")
  public ResponseEntity<ApiResponse<UserResponse>> updateUser(
      @PathVariable String userId,
      @Valid @RequestBody UpdateUserRequest request,
      @AuthenticationPrincipal UserPrincipal principal
  ) {
    log.info("프로필 수정 요청: userId={}, requesterId={}", userId, principal.getUserId());

    // 본인 또는 관리자 확인
    validateOwnerOrAdmin(userId, principal);

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
   * <p>
   * 본인 또는 관리자만 탈퇴 가능합니다.
   */
  @DeleteMapping("/{userId}")
  public ResponseEntity<ApiResponse<Void>> deleteUser(
      @PathVariable String userId,
      @AuthenticationPrincipal UserPrincipal principal
  ) {
    log.info("회원 탈퇴 요청: userId={}, requesterId={}", userId, principal.getUserId());

    // 본인 또는 관리자 확인
    validateOwnerOrAdmin(userId, principal);

    deleteUserUseCase.deleteUser(userId, principal.getUserId());

    log.info("회원 탈퇴 완료: userId={}", userId);
    return ResponseEntity.ok(ApiResponse.successWithMessage("회원 탈퇴가 완료되었습니다."));
  }

  // ========================================
  // 검색 (관리자용)
  // ========================================

  /**
   * 사용자 검색
   * <p>
   * 관리자 권한이 필요합니다.
   */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String phoneNumber,
      @RequestParam(required = false) UserStatus status,
      @RequestParam(defaultValue = "false") boolean includeDeleted,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    log.debug("사용자 검색: name={}, email={}, status={}", name, email, status);

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

  // ========================================
  // Private 메서드
  // ========================================

  /**
   * 본인 또는 관리자 권한 확인
   *
   * @param targetUserId 대상 사용자 ID
   * @param principal    현재 인증된 사용자
   * @throws AccessDeniedException 본인도 아니고 관리자도 아닌 경우
   */
  private void validateOwnerOrAdmin(String targetUserId, UserPrincipal principal) {
    boolean isOwner = targetUserId.equals(principal.getUserId());
    boolean isAdmin = "ADMIN".equalsIgnoreCase(principal.getRole());

    if (!isOwner && !isAdmin) {
      throw new AccessDeniedException("본인 또는 관리자만 접근할 수 있습니다.");
    }
  }
}