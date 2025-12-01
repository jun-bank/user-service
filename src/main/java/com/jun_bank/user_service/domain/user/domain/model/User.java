package com.jun_bank.user_service.domain.user.domain.model;

import com.jun_bank.user_service.domain.user.domain.exception.UserErrorCode;
import com.jun_bank.user_service.domain.user.domain.exception.UserException;
import com.jun_bank.user_service.domain.user.domain.model.vo.Email;
import com.jun_bank.user_service.domain.user.domain.model.vo.PhoneNumber;
import com.jun_bank.user_service.domain.user.domain.model.vo.UserId;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 도메인 모델
 * <p>
 * 사용자의 프로필 정보를 관리하는 핵심 도메인 객체입니다.
 * 인증 정보(비밀번호)는 Auth Server에서 별도로 관리합니다.
 *
 * <h3>책임:</h3>
 * <ul>
 *   <li>사용자 프로필 정보 관리 (이름, 전화번호, 생년월일)</li>
 *   <li>사용자 상태 관리 (활성, 휴면, 정지, 탈퇴)</li>
 *   <li>상태 전이 규칙 적용</li>
 *   <li>비즈니스 규칙 검증</li>
 * </ul>
 *
 * <h3>불변 필드 (생성 후 변경 불가):</h3>
 * <ul>
 *   <li>userId: 사용자 고유 식별자</li>
 *   <li>email: 이메일 (로그인 ID로 사용, 변경 시 별도 프로세스 필요)</li>
 *   <li>birthDate: 생년월일</li>
 * </ul>
 *
 * <h3>감사 필드 (BaseEntity 매핑):</h3>
 * <ul>
 *   <li>createdAt: 생성 일시 (JPA @CreatedDate)</li>
 *   <li>updatedAt: 수정 일시 (JPA @LastModifiedDate)</li>
 *   <li>createdBy: 생성자 ID (JPA @CreatedBy)</li>
 *   <li>updatedBy: 수정자 ID (JPA @LastModifiedBy)</li>
 *   <li>deletedAt: 삭제 일시 (Soft Delete)</li>
 *   <li>deletedBy: 삭제자 ID (Soft Delete)</li>
 *   <li>isDeleted: 삭제 여부 플래그 (Soft Delete)</li>
 * </ul>
 *
 * <h3>Soft Delete 처리:</h3>
 * <p>
 * 사용자 탈퇴 시 실제 데이터는 삭제되지 않고 상태만 변경됩니다.
 * </p>
 * <ul>
 *   <li>status → DELETED</li>
 *   <li>isDeleted → true</li>
 *   <li>deletedAt → 현재 시간</li>
 *   <li>deletedBy → 삭제 요청자 ID</li>
 * </ul>
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // 신규 사용자 생성
 * User user = User.createBuilder()
 *     .email(Email.of("user@example.com"))
 *     .name("홍길동")
 *     .phoneNumber(PhoneNumber.of("010-1234-5678"))
 *     .birthDate(LocalDate.of(1990, 1, 1))
 *     .build();
 *
 * // 프로필 수정
 * user.updateProfile("홍길동", PhoneNumber.of("010-9876-5432"));
 *
 * // 탈퇴 처리
 * user.withdraw();
 * }</pre>
 *
 * @see UserStatus
 * @see UserId
 * @see Email
 * @see PhoneNumber
 */
@Getter
public class User {

    // ========================================
    // 핵심 필드
    // ========================================

    /**
     * 사용자 ID
     * <p>
     * 신규 사용자의 경우 null이며, Entity 저장 시 생성됩니다.
     * {@link #isNew()}로 신규 여부를 판단합니다.
     * </p>
     */
    private UserId userId;

    /**
     * 이메일 (불변)
     * <p>
     * 로그인 ID로 사용되며, 시스템 내에서 유일해야 합니다.
     * Auth Server와 동기화됩니다.
     * </p>
     */
    private Email email;

    /**
     * 이름
     * <p>2~50자의 한글/영문 이름</p>
     */
    private String name;

    /**
     * 전화번호
     * <p>한국 휴대폰 번호 형식 (010-1234-5678)</p>
     */
    private PhoneNumber phoneNumber;

    /**
     * 생년월일 (불변)
     */
    private LocalDate birthDate;

    /**
     * 사용자 상태
     * @see UserStatus
     */
    private UserStatus status;

    // ========================================
    // 감사 필드 (BaseEntity 매핑)
    // ========================================

    /**
     * 생성 일시
     * <p>JPA @CreatedDate에 의해 자동 설정됩니다.</p>
     */
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     * <p>JPA @LastModifiedDate에 의해 자동 설정됩니다.</p>
     */
    private LocalDateTime updatedAt;

    /**
     * 생성자 ID
     * <p>JPA @CreatedBy에 의해 자동 설정됩니다.</p>
     */
    private String createdBy;

    /**
     * 수정자 ID
     * <p>JPA @LastModifiedBy에 의해 자동 설정됩니다.</p>
     */
    private String updatedBy;

    /**
     * 삭제 일시 (Soft Delete)
     * <p>탈퇴 시 설정되며, null이면 삭제되지 않은 상태입니다.</p>
     */
    private LocalDateTime deletedAt;

    /**
     * 삭제자 ID (Soft Delete)
     * <p>탈퇴 요청자의 ID입니다. 본인 탈퇴 또는 관리자 삭제를 구분할 수 있습니다.</p>
     */
    private String deletedBy;

    /**
     * 삭제 여부 플래그 (Soft Delete)
     * <p>true이면 탈퇴한 사용자입니다. QueryDSL 조회 시 필터링에 사용됩니다.</p>
     */
    private Boolean isDeleted;

    /**
     * private 생성자
     * <p>Builder 패턴을 통해서만 인스턴스를 생성합니다.</p>
     */
    private User() {}

    // ========================================
    // 생성 메서드 (Builder 패턴)
    // ========================================

    /**
     * 신규 사용자 생성을 위한 빌더
     * <p>
     * 회원가입 시 사용합니다.
     * ID는 null로 설정되며, Entity 저장 시 생성됩니다.
     * 상태는 ACTIVE, isDeleted는 false로 초기화됩니다.
     * </p>
     *
     * <h4>필수 필드:</h4>
     * <ul>
     *   <li>email: 이메일 주소</li>
     *   <li>name: 이름 (2~50자)</li>
     *   <li>phoneNumber: 전화번호</li>
     *   <li>birthDate: 생년월일</li>
     * </ul>
     *
     * @return UserCreateBuilder
     */
    public static UserCreateBuilder createBuilder() {
        return new UserCreateBuilder();
    }

    /**
     * DB에서 복원을 위한 빌더
     * <p>
     * Repository에서 Entity → Domain 변환 시 사용합니다.
     * 모든 필드(감사 필드 포함)를 설정할 수 있습니다.
     * </p>
     *
     * @return UserRestoreBuilder
     */
    public static UserRestoreBuilder restoreBuilder() {
        return new UserRestoreBuilder();
    }

    // ========================================
    // 상태 확인 메서드
    // ========================================

    /**
     * 신규 사용자 여부 확인
     * <p>
     * userId가 null이면 아직 저장되지 않은 신규 사용자입니다.
     * Repository에서 save/update 판단에 사용됩니다.
     * </p>
     *
     * @return 신규이면 true
     */
    public boolean isNew() {
        return this.userId == null;
    }

    /**
     * 활성 상태 여부 확인
     * <p>로그인, 서비스 이용이 가능한 상태입니다.</p>
     *
     * @return ACTIVE 상태이면 true
     */
    public boolean isActive() {
        return this.status.isActive();
    }

    /**
     * 탈퇴 상태 여부 확인
     * <p>Soft Delete된 사용자입니다.</p>
     *
     * @return DELETED 상태이면 true
     */
    public boolean isDeleted() {
        return this.status.isDeleted();
    }

    /**
     * 정지 상태 여부 확인
     * <p>관리자에 의해 이용이 정지된 상태입니다.</p>
     *
     * @return SUSPENDED 상태이면 true
     */
    public boolean isSuspended() {
        return this.status.isSuspended();
    }

    /**
     * 휴면 상태 여부 확인
     * <p>장기간 미접속으로 휴면 처리된 상태입니다.</p>
     *
     * @return INACTIVE 상태이면 true
     */
    public boolean isInactive() {
        return this.status.isInactive();
    }

    // ========================================
    // 비즈니스 메서드
    // ========================================

    /**
     * 프로필 정보 수정
     * <p>
     * 이름과 전화번호를 수정합니다.
     * 이메일, 생년월일은 불변 필드로 이 메서드로 수정할 수 없습니다.
     * 탈퇴 또는 정지 상태에서는 수정할 수 없습니다.
     * </p>
     *
     * @param name 새 이름 (2~50자)
     * @param phoneNumber 새 전화번호
     * @throws UserException 수정 불가능한 상태인 경우 (USER_034, USER_035)
     * @throws UserException 이름이 유효하지 않은 경우 (USER_003)
     */
    public void updateProfile(String name, PhoneNumber phoneNumber) {
        validateModifiable();
        validateName(name);

        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    /**
     * 사용자 탈퇴 처리
     * <p>
     * 상태를 DELETED로 변경합니다.
     * Entity에서 Soft Delete 처리(isDeleted=true, deletedAt, deletedBy 설정)가 필요합니다.
     * 이미 탈퇴한 경우 예외가 발생합니다.
     * </p>
     *
     * @throws UserException 상태 전이가 허용되지 않는 경우 (USER_036)
     */
    public void withdraw() {
        changeStatus(UserStatus.DELETED);
    }

    /**
     * 사용자 정지 처리
     * <p>
     * 관리자에 의한 계정 정지입니다.
     * 이용약관 위반 등의 사유로 정지됩니다.
     * 상태를 SUSPENDED로 변경합니다.
     * </p>
     *
     * @throws UserException 상태 전이가 허용되지 않는 경우 (USER_036)
     */
    public void suspend() {
        changeStatus(UserStatus.SUSPENDED);
    }

    /**
     * 사용자 활성화
     * <p>
     * 휴면 또는 정지 상태에서 활성 상태로 변경합니다.
     * 휴면 해제 또는 관리자에 의한 정지 해제 시 사용합니다.
     * </p>
     *
     * @throws UserException 상태 전이가 허용되지 않는 경우 (USER_036)
     */
    public void activate() {
        changeStatus(UserStatus.ACTIVE);
    }

    /**
     * 사용자 휴면 처리
     * <p>
     * 장기간 미접속으로 인한 휴면 처리입니다.
     * 배치 작업 또는 로그인 시 자동으로 호출될 수 있습니다.
     * 상태를 INACTIVE로 변경합니다.
     * </p>
     *
     * @throws UserException 상태 전이가 허용되지 않는 경우 (USER_036)
     */
    public void deactivate() {
        changeStatus(UserStatus.INACTIVE);
    }

    // ========================================
    // Private 메서드
    // ========================================

    /**
     * 상태 변경
     * <p>
     * 상태 전이 규칙을 검증하고 상태를 변경합니다.
     * {@link UserStatus#canTransitionTo(UserStatus)}를 통해
     * 허용된 전환인지 확인합니다.
     * </p>
     *
     * @param newStatus 변경할 상태
     * @throws UserException 허용되지 않은 상태 전이인 경우 (USER_036)
     */
    private void changeStatus(UserStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw UserException.invalidStatusTransition(
                    this.status.name(), newStatus.name());
        }
        this.status = newStatus;
    }

    /**
     * 수정 가능 여부 검증
     * <p>
     * 현재 상태에서 프로필 수정이 가능한지 확인합니다.
     * DELETED, SUSPENDED 상태에서는 수정이 불가능합니다.
     * </p>
     *
     * @throws UserException 수정 불가능한 상태인 경우 (USER_034, USER_035)
     */
    private void validateModifiable() {
        if (!this.status.canModifyProfile()) {
            if (this.status.isDeleted()) {
                throw UserException.cannotModifyDeletedUser();
            }
            if (this.status.isSuspended()) {
                throw UserException.cannotModifySuspendedUser();
            }
        }
    }

    /**
     * 이름 유효성 검증
     * <p>
     * 이름이 2~50자 범위인지 확인합니다.
     * </p>
     *
     * @param name 검증할 이름
     * @throws UserException 이름이 유효하지 않은 경우 (USER_003)
     */
    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() < 2 || name.length() > 50) {
            throw UserException.invalidName();
        }
    }

    // ========================================
    // Builder 클래스
    // ========================================

    /**
     * 신규 사용자 생성 빌더
     * <p>
     * 회원가입 시 필요한 필드만 설정합니다.
     * 감사 필드는 JPA에 의해 자동 설정됩니다.
     * </p>
     */
    public static class UserCreateBuilder {
        private Email email;
        private String name;
        private PhoneNumber phoneNumber;
        private LocalDate birthDate;

        /**
         * 이메일 설정
         *
         * @param email 이메일 VO
         * @return this
         */
        public UserCreateBuilder email(Email email) {
            this.email = email;
            return this;
        }

        /**
         * 이름 설정
         *
         * @param name 이름 (2~50자)
         * @return this
         */
        public UserCreateBuilder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 전화번호 설정
         *
         * @param phoneNumber 전화번호 VO
         * @return this
         */
        public UserCreateBuilder phoneNumber(PhoneNumber phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        /**
         * 생년월일 설정
         *
         * @param birthDate 생년월일
         * @return this
         */
        public UserCreateBuilder birthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        /**
         * User 객체 생성
         * <p>
         * 필수 필드 검증 후 신규 User 객체를 생성합니다.
         * userId는 null (신규), status는 ACTIVE, isDeleted는 false로 초기화됩니다.
         * </p>
         *
         * @return 신규 User 객체 (userId는 null)
         * @throws UserException 필수 필드 누락 또는 유효성 검증 실패
         */
        public User build() {
            User user = new User();
            user.email = this.email;
            user.name = this.name;
            user.phoneNumber = this.phoneNumber;
            user.birthDate = this.birthDate;
            user.status = UserStatus.ACTIVE;
            user.isDeleted = false;

            // 필수 필드 검증
            if (user.email == null) {
                throw UserException.invalidEmailFormat(null);
            }
            user.validateName(user.name);

            return user;
        }
    }

    /**
     * DB 복원용 빌더
     * <p>
     * Entity → Domain 변환 시 모든 필드를 설정합니다.
     * 감사 필드를 포함한 전체 상태를 복원합니다.
     * </p>
     */
    public static class UserRestoreBuilder {
        private UserId userId;
        private Email email;
        private String name;
        private PhoneNumber phoneNumber;
        private LocalDate birthDate;
        private UserStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;
        private LocalDateTime deletedAt;
        private String deletedBy;
        private Boolean isDeleted;

        /**
         * 사용자 ID 설정
         *
         * @param userId 사용자 ID VO
         * @return this
         */
        public UserRestoreBuilder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 이메일 설정
         *
         * @param email 이메일 VO
         * @return this
         */
        public UserRestoreBuilder email(Email email) {
            this.email = email;
            return this;
        }

        /**
         * 이름 설정
         *
         * @param name 이름
         * @return this
         */
        public UserRestoreBuilder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 전화번호 설정
         *
         * @param phoneNumber 전화번호 VO
         * @return this
         */
        public UserRestoreBuilder phoneNumber(PhoneNumber phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        /**
         * 생년월일 설정
         *
         * @param birthDate 생년월일
         * @return this
         */
        public UserRestoreBuilder birthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        /**
         * 상태 설정
         *
         * @param status 사용자 상태
         * @return this
         */
        public UserRestoreBuilder status(UserStatus status) {
            this.status = status;
            return this;
        }

        /**
         * 생성 일시 설정
         *
         * @param createdAt 생성 일시
         * @return this
         */
        public UserRestoreBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * 수정 일시 설정
         *
         * @param updatedAt 수정 일시
         * @return this
         */
        public UserRestoreBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * 생성자 ID 설정
         *
         * @param createdBy 생성자 ID
         * @return this
         */
        public UserRestoreBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        /**
         * 수정자 ID 설정
         *
         * @param updatedBy 수정자 ID
         * @return this
         */
        public UserRestoreBuilder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        /**
         * 삭제 일시 설정 (Soft Delete)
         *
         * @param deletedAt 삭제 일시
         * @return this
         */
        public UserRestoreBuilder deletedAt(LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        /**
         * 삭제자 ID 설정 (Soft Delete)
         *
         * @param deletedBy 삭제자 ID
         * @return this
         */
        public UserRestoreBuilder deletedBy(String deletedBy) {
            this.deletedBy = deletedBy;
            return this;
        }

        /**
         * 삭제 여부 설정 (Soft Delete)
         *
         * @param isDeleted 삭제 여부 (true: 탈퇴)
         * @return this
         */
        public UserRestoreBuilder isDeleted(Boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        /**
         * User 객체 복원
         * <p>
         * DB에서 조회한 모든 필드로 User 객체를 복원합니다.
         * </p>
         *
         * @return 복원된 User 객체
         */
        public User build() {
            User user = new User();
            user.userId = this.userId;
            user.email = this.email;
            user.name = this.name;
            user.phoneNumber = this.phoneNumber;
            user.birthDate = this.birthDate;
            user.status = this.status;
            user.createdAt = this.createdAt;
            user.updatedAt = this.updatedAt;
            user.createdBy = this.createdBy;
            user.updatedBy = this.updatedBy;
            user.deletedAt = this.deletedAt;
            user.deletedBy = this.deletedBy;
            user.isDeleted = this.isDeleted;
            return user;
        }
    }
}