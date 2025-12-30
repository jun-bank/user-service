package com.jun_bank.user_service.domain.user.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 사용자 수정 커맨드
 * <p>
 * 프로필 수정 시 필요한 정보를 담는 DTO입니다.
 * 불변 필드(email, birthDate)는 수정할 수 없습니다.
 *
 * <h3>수정 가능 필드:</h3>
 * <ul>
 *   <li>name: 이름 (2~50자)</li>
 *   <li>phoneNumber: 전화번호</li>
 * </ul>
 *
 * @param name 이름
 * @param phoneNumber 전화번호
 */
public record UpdateUserCommand(

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2~50자 사이여야 합니다")
    String name,

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
        message = "유효한 전화번호 형식이 아닙니다")
    String phoneNumber

) {

  /**
   * 빌더 시작
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private String phoneNumber;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder phoneNumber(String phoneNumber) {
      this.phoneNumber = phoneNumber;
      return this;
    }

    public UpdateUserCommand build() {
      return new UpdateUserCommand(name, phoneNumber);
    }
  }
}