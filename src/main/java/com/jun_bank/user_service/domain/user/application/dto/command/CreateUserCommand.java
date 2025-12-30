package com.jun_bank.user_service.domain.user.application.dto.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 사용자 생성 커맨드
 * <p>
 * 회원가입 시 필요한 정보를 담는 DTO입니다.
 * Controller에서 Request를 변환하여 Service로 전달합니다.
 *
 * <h3>검증 규칙:</h3>
 * <ul>
 *   <li>email: 이메일 형식, 필수</li>
 *   <li>password: 8~100자, 필수 (Auth Server로 전달)</li>
 *   <li>name: 2~50자, 필수</li>
 *   <li>phoneNumber: 한국 휴대폰 형식, 필수</li>
 *   <li>birthDate: 과거 날짜, 필수</li>
 * </ul>
 *
 * @param email 이메일 주소
 * @param password 비밀번호 (Auth Server 전달용)
 * @param name 이름
 * @param phoneNumber 전화번호
 * @param birthDate 생년월일
 */
public record CreateUserCommand(

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8~100자 사이여야 합니다")
    String password,

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2~50자 사이여야 합니다")
    String name,

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
        message = "유효한 전화번호 형식이 아닙니다")
    String phoneNumber,

    @NotNull(message = "생년월일은 필수입니다")
    @Past(message = "생년월일은 과거 날짜여야 합니다")
    LocalDate birthDate

) {

  /**
   * 빌더 시작
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String email;
    private String password;
    private String name;
    private String phoneNumber;
    private LocalDate birthDate;

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
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

    public CreateUserCommand build() {
      return new CreateUserCommand(email, password, name, phoneNumber, birthDate);
    }
  }
}