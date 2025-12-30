package com.jun_bank.user_service.domain.user.application.dto.result;

import com.jun_bank.user_service.domain.user.domain.model.User;
import com.jun_bank.user_service.domain.user.domain.model.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 결과 DTO
 * <p>
 * Service에서 반환하는 사용자 정보 DTO입니다.
 * Domain 모델을 외부에 노출하지 않고 필요한 정보만 전달합니다.
 *
 * <h3>마스킹 처리:</h3>
 * <ul>
 *   <li>email: 원본 그대로 (로그인 ID)</li>
 *   <li>phoneNumber: 마스킹 처리 (010-****-5678)</li>
 * </ul>
 *
 * @param userId 사용자 ID
 * @param email 이메일
 * @param name 이름
 * @param phoneNumber 전화번호 (마스킹)
 * @param birthDate 생년월일
 * @param status 상태
 * @param statusDescription 상태 설명
 * @param createdAt 생성일시
 * @param updatedAt 수정일시
 */
public record UserResult(
    String userId,
    String email,
    String name,
    String phoneNumber,
    LocalDate birthDate,
    UserStatus status,
    String statusDescription,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  /**
   * Domain 모델에서 Result DTO 생성
   * <p>
   * 전화번호는 마스킹 처리됩니다.
   * </p>
   *
   * @param user User 도메인 모델
   * @return UserResult
   */
  public static UserResult from(User user) {
    return new UserResult(
        user.getUserId().value(),
        user.getEmail().value(),
        user.getName(),
        user.getPhoneNumber().masked(),  // 마스킹 처리
        user.getBirthDate(),
        user.getStatus(),
        user.getStatus().getDescription(),
        user.getCreatedAt(),
        user.getUpdatedAt()
    );
  }

  /**
   * Domain 모델에서 Result DTO 생성 (전화번호 원본)
   * <p>
   * 본인 조회 시 전화번호 원본을 제공합니다.
   * </p>
   *
   * @param user User 도메인 모델
   * @return UserResult (전화번호 원본)
   */
  public static UserResult fromWithFullPhone(User user) {
    return new UserResult(
        user.getUserId().value(),
        user.getEmail().value(),
        user.getName(),
        user.getPhoneNumber().value(),
        user.getBirthDate(),
        user.getStatus(),
        user.getStatus().getDescription(),
        user.getCreatedAt(),
        user.getUpdatedAt()
    );
  }

  /**
   * 빌더 시작
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String userId;
    private String email;
    private String name;
    private String phoneNumber;
    private LocalDate birthDate;
    private UserStatus status;
    private String statusDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

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

    public Builder birthDate(LocalDate birthDate) {
      this.birthDate = birthDate;
      return this;
    }

    public Builder status(UserStatus status) {
      this.status = status;
      this.statusDescription = status != null ? status.getDescription() : null;
      return this;
    }

    public Builder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public UserResult build() {
      return new UserResult(
          userId, email, name, phoneNumber, birthDate,
          status, statusDescription, createdAt, updatedAt
      );
    }
  }
}