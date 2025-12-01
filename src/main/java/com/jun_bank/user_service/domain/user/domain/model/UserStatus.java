package com.jun_bank.user_service.domain.user.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 사용자 상태
 * <p>
 * 사용자의 라이프사이클 상태를 정의합니다.
 * 각 상태별로 허용되는 전환(transition)과 정책을 메서드로 제공합니다.
 *
 * <h3>상태 전이 규칙:</h3>
 * <pre>
 * ┌────────┐     ┌──────────┐     ┌───────────┐
 * │ ACTIVE │────▶│ INACTIVE │────▶│  DELETED  │
 * └────────┘     └──────────┘     └───────────┘
 *     │              │                  ▲
 *     │              └──────────────────┤
 *     ▼                                 │
 * ┌───────────┐                         │
 * │ SUSPENDED │─────────────────────────┘
 * └───────────┘
 *
 * ACTIVE → INACTIVE, SUSPENDED, DELETED
 * INACTIVE → ACTIVE, DELETED
 * SUSPENDED → ACTIVE, DELETED
 * DELETED → (전이 불가, 최종 상태)
 * </pre>
 *
 * <h3>상태별 권한:</h3>
 * <table border="1">
 *   <tr><th>상태</th><th>로그인</th><th>프로필 수정</th></tr>
 *   <tr><td>ACTIVE</td><td>✓</td><td>✓</td></tr>
 *   <tr><td>INACTIVE</td><td>✗</td><td>✓</td></tr>
 *   <tr><td>SUSPENDED</td><td>✗</td><td>✗</td></tr>
 *   <tr><td>DELETED</td><td>✗</td><td>✗</td></tr>
 * </table>
 */
public enum UserStatus {

    /**
     * 정상 상태
     * <p>
     * 모든 기능을 정상적으로 사용할 수 있는 상태입니다.
     * 로그인, 프로필 수정 등 모든 작업이 가능합니다.
     * </p>
     */
    ACTIVE("정상", true, true),

    /**
     * 휴면 상태
     * <p>
     * 장기간 미접속으로 인해 휴면 처리된 상태입니다.
     * 로그인은 불가능하지만, 휴면 해제 후 프로필 수정이 가능합니다.
     * Auth Server에서 휴면 해제 프로세스를 통해 ACTIVE로 전환됩니다.
     * </p>
     */
    INACTIVE("휴면", false, true),

    /**
     * 정지 상태
     * <p>
     * 관리자에 의해 계정이 정지된 상태입니다.
     * 이용약관 위반 등의 사유로 정지될 수 있습니다.
     * 모든 기능이 차단되며, 관리자만 상태를 변경할 수 있습니다.
     * </p>
     */
    SUSPENDED("정지", false, false),

    /**
     * 탈퇴 상태 (Soft Delete)
     * <p>
     * 사용자가 탈퇴한 상태입니다.
     * 실제 데이터는 삭제되지 않고 상태만 변경됩니다(Soft Delete).
     * BaseEntity의 isDeleted=true, deletedAt, deletedBy가 설정됩니다.
     * 최종 상태로 다른 상태로의 전환이 불가능합니다.
     * </p>
     */
    DELETED("탈퇴", false, false);

    private final String description;
    private final boolean canLogin;
    private final boolean canModifyProfile;

    /**
     * UserStatus 생성자
     *
     * @param description 한글 상태 설명
     * @param canLogin 로그인 가능 여부
     * @param canModifyProfile 프로필 수정 가능 여부
     */
    UserStatus(String description, boolean canLogin, boolean canModifyProfile) {
        this.description = description;
        this.canLogin = canLogin;
        this.canModifyProfile = canModifyProfile;
    }

    /**
     * 상태 설명 반환
     *
     * @return 한글 상태 설명 (예: "정상", "휴면")
     */
    public String getDescription() {
        return description;
    }

    /**
     * 로그인 가능 여부 확인
     * <p>
     * ACTIVE 상태에서만 로그인이 가능합니다.
     * Auth Server에서 로그인 시 이 메서드로 검증합니다.
     * </p>
     *
     * @return 로그인 가능하면 true
     */
    public boolean canLogin() {
        return canLogin;
    }

    /**
     * 프로필 수정 가능 여부 확인
     * <p>
     * ACTIVE, INACTIVE 상태에서 프로필 수정이 가능합니다.
     * SUSPENDED, DELETED 상태에서는 수정이 불가능합니다.
     * </p>
     *
     * @return 프로필 수정 가능하면 true
     */
    public boolean canModifyProfile() {
        return canModifyProfile;
    }

    /**
     * 특정 상태로 전환 가능 여부 확인
     * <p>
     * 상태 전이 규칙에 따라 허용된 전환인지 검증합니다.
     * 같은 상태로의 전환은 항상 false를 반환합니다.
     * </p>
     *
     * @param target 전환하려는 상태
     * @return 전환 가능하면 true
     */
    public boolean canTransitionTo(UserStatus target) {
        if (this == target) {
            return false; // 같은 상태로의 전환 불가
        }
        return getAllowedTransitions().contains(target);
    }

    /**
     * 현재 상태에서 전환 가능한 상태 목록 반환
     * <p>
     * 상태 전이 규칙에 따라 허용된 상태들의 Set을 반환합니다.
     * </p>
     *
     * @return 전환 가능한 상태 Set (비어있을 수 있음)
     */
    public Set<UserStatus> getAllowedTransitions() {
        return switch (this) {
            case ACTIVE -> EnumSet.of(INACTIVE, SUSPENDED, DELETED);
            case INACTIVE -> EnumSet.of(ACTIVE, DELETED);
            case SUSPENDED -> EnumSet.of(ACTIVE, DELETED);
            case DELETED -> EnumSet.noneOf(UserStatus.class); // 최종 상태 - 전환 불가
        };
    }

    /**
     * 활성 상태 여부 확인
     *
     * @return ACTIVE 상태이면 true
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 삭제(탈퇴) 상태 여부 확인
     *
     * @return DELETED 상태이면 true
     */
    public boolean isDeleted() {
        return this == DELETED;
    }

    /**
     * 정지 상태 여부 확인
     *
     * @return SUSPENDED 상태이면 true
     */
    public boolean isSuspended() {
        return this == SUSPENDED;
    }

    /**
     * 휴면 상태 여부 확인
     *
     * @return INACTIVE 상태이면 true
     */
    public boolean isInactive() {
        return this == INACTIVE;
    }
}