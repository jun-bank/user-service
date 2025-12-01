# User Service

> 사용자 관리 서비스 - 회원가입, 프로필 관리, 회원 탈퇴

## 📋 개요

| 항목 | 내용 |
|------|------|
| 포트 | 8087 |
| 데이터베이스 | user_db (PostgreSQL) |
| 주요 역할 | 사용자 프로필 관리 (인증 정보는 Auth Server에서 관리) |

## 🏗️ 아키텍처 결정사항

### Auth Server와의 역할 분리
```
┌─────────────────────────────────────────────────────────────────┐
│                        회원가입 흐름                              │
├─────────────────────────────────────────────────────────────────┤
│  Client                                                          │
│    │                                                             │
│    ▼                                                             │
│  User Service ──────Feign (동기)─────▶ Auth Server              │
│    │                                        │                    │
│    │ User 저장                              │ AuthUser 저장       │
│    │ (프로필 정보)                          │ (인증 정보)         │
│    ▼                                        ▼                    │
│  user_db                                 auth_db                 │
│  - name                                  - email                 │
│  - phoneNumber                           - password (암호화)     │
│  - birthDate                             - role                  │
│  - status                                - status                │
└─────────────────────────────────────────────────────────────────┘
```

**분리 이유:**
1. **인증 독립성**: Auth Server 장애 시에도 로그인 가능
2. **빠른 응답**: 인증 시 User Service 조회 불필요
3. **확장성**: 인증 방식 변경이 User Service에 영향 없음

---

## 🎯 학습 포인트

### 1. 헥사고날 아키텍처 + 도메인 중심 설계
```
domain/
├── domain/        # 순수 도메인 (Infrastructure 의존성 없음)
│   ├── exception/ # 도메인 예외 (ErrorCode, Exception)
│   └── model/     # 도메인 모델, Enum, VO
├── application/   # 유스케이스, Port (인터페이스)
├── infrastructure/# Adapter (Out) - Repository, Feign, Kafka
└── presentation/  # Adapter (In) - Controller
```

### 2. 이벤트 기반 통신 (Kafka)
```
회원가입 완료 → user.created 이벤트 발행
회원 탈퇴 → user.deleted 이벤트 발행 → 모든 서비스 수신 (연관 데이터 정리)
```

### 3. 동기 호출 (Feign)
```
회원가입 → User Service → Feign → Auth Server (인증 정보 생성)
```

---

## 🗄️ 도메인 모델

### 도메인 구조
```
domain/user/domain/
├── exception/
│   ├── UserErrorCode.java      # 에러 코드 정의
│   └── UserException.java      # 도메인 예외
└── model/
    ├── User.java               # 사용자 Aggregate Root
    ├── UserStatus.java         # 상태 Enum (정책 메서드 포함)
    └── vo/
        ├── UserId.java         # 사용자 ID (USR-xxxxxxxx)
        ├── Email.java          # 이메일 (검증, 마스킹)
        └── PhoneNumber.java    # 전화번호 (검증, 정규화)
```

### User 도메인 모델
```
┌─────────────────────────────────────────────────────────────┐
│                          User                                │
├─────────────────────────────────────────────────────────────┤
│ 【핵심 필드】                                                 │
│ userId: UserId (PK, USR-xxxxxxxx)                           │
│ email: Email (Unique, 불변)                                  │
│ name: String (2~50자)                                       │
│ phoneNumber: PhoneNumber (010-xxxx-xxxx)                    │
│ birthDate: LocalDate (불변)                                  │
│ status: UserStatus (ACTIVE/INACTIVE/SUSPENDED/DELETED)      │
├─────────────────────────────────────────────────────────────┤
│ 【감사 필드 - BaseEntity】                                    │
│ createdAt: LocalDateTime (자동)                              │
│ updatedAt: LocalDateTime (자동)                              │
│ createdBy: String (자동)                                     │
│ updatedBy: String (자동)                                     │
│ deletedAt: LocalDateTime (Soft Delete)                      │
│ deletedBy: String (Soft Delete)                             │
│ isDeleted: Boolean (Soft Delete)                            │
├─────────────────────────────────────────────────────────────┤
│ 【비즈니스 메서드】                                           │
│ + updateProfile(name, phoneNumber): void                    │
│ + withdraw(): void        // 탈퇴 (→ DELETED)               │
│ + suspend(): void         // 정지 (→ SUSPENDED)             │
│ + activate(): void        // 활성화 (→ ACTIVE)              │
│ + deactivate(): void      // 휴면 (→ INACTIVE)              │
├─────────────────────────────────────────────────────────────┤
│ 【상태 확인 메서드】                                          │
│ + isNew(): boolean        // userId == null                 │
│ + isActive(): boolean                                       │
│ + isDeleted(): boolean                                      │
│ + isSuspended(): boolean                                    │
│ + isInactive(): boolean                                     │
└─────────────────────────────────────────────────────────────┘
```

### UserStatus Enum (정책 메서드 포함)
```java
public enum UserStatus {
    ACTIVE("정상", canLogin=true, canModifyProfile=true),
    INACTIVE("휴면", canLogin=false, canModifyProfile=true),
    SUSPENDED("정지", canLogin=false, canModifyProfile=false),
    DELETED("탈퇴", canLogin=false, canModifyProfile=false);
    
    // 상태 전이 규칙
    public boolean canTransitionTo(UserStatus target);
    public Set<UserStatus> getAllowedTransitions();
}
```

**상태 전이 규칙:**
```
ACTIVE → INACTIVE, SUSPENDED, DELETED
INACTIVE → ACTIVE, DELETED
SUSPENDED → ACTIVE, DELETED
DELETED → (전이 불가, 최종 상태)
```

### Value Objects

#### UserId
```java
public record UserId(String value) {
    public static final String PREFIX = "USR";
    
    public static String generateId();  // Entity 저장 시 호출
    // 형식: USR-xxxxxxxx (예: USR-a1b2c3d4)
}
```

#### Email
```java
public record Email(String value) {
    // 검증: RFC 5322 기반, 최대 255자
    // 정규화: 소문자 변환
    
    public String getDomain();    // @example.com → example.com
    public String getLocalPart(); // user@example.com → user
    public String masked();       // user@example.com → u***r@example.com
}
```

#### PhoneNumber
```java
public record PhoneNumber(String value) {
    // 검증: 한국 휴대폰 번호 (01X-XXXX-XXXX)
    // 정규화: 010-1234-5678 형식으로 변환
    
    public String masked();        // 010-****-5678
    public String withoutHyphen(); // 01012345678
}
```

### Exception 체계

#### UserErrorCode
```java
public enum UserErrorCode implements ErrorCode {
    // 유효성 검증 (400)
    INVALID_EMAIL_FORMAT("USER_001", "유효하지 않은 이메일 형식입니다", 400),
    INVALID_PHONE_FORMAT("USER_002", "유효하지 않은 전화번호 형식입니다", 400),
    INVALID_NAME("USER_003", "이름은 2~50자 사이여야 합니다", 400),
    INVALID_USER_ID_FORMAT("USER_005", "유효하지 않은 사용자 ID 형식입니다", 400),
    
    // 조회 (404)
    USER_NOT_FOUND("USER_010", "사용자를 찾을 수 없습니다", 404),
    
    // 중복 (409)
    EMAIL_ALREADY_EXISTS("USER_020", "이미 사용 중인 이메일입니다", 409),
    
    // 상태 (422)
    CANNOT_MODIFY_DELETED_USER("USER_034", "탈퇴한 사용자는 수정할 수 없습니다", 422),
    CANNOT_MODIFY_SUSPENDED_USER("USER_035", "정지된 사용자는 수정할 수 없습니다", 422),
    INVALID_STATUS_TRANSITION("USER_036", "허용되지 않은 상태 변경입니다", 422);
}
```

#### UserException (팩토리 메서드 패턴)
```java
public class UserException extends BusinessException {
    // 팩토리 메서드
    public static UserException userNotFound(String userId);
    public static UserException emailAlreadyExists(String email);
    public static UserException cannotModifyDeletedUser();
    public static UserException invalidStatusTransition(String from, String to);
    // ...
}
```

---

## 📡 API 명세

### 1. 회원가입
```http
POST /api/v1/users
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "name": "홍길동",
  "phoneNumber": "010-1234-5678",
  "birthDate": "1990-01-15"
}
```

**처리 흐름:**
1. User Service: 프로필 정보 저장 (User)
2. Feign → Auth Server: 인증 정보 저장 (AuthUser)
3. Kafka: `user.created` 이벤트 발행

**Response (201 Created)**
```json
{
  "userId": "USR-a1b2c3d4",
  "email": "user@example.com",
  "name": "홍길동",
  "phoneNumber": "010-****-5678",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00"
}
```

### 2. 사용자 조회
```http
GET /api/v1/users/{userId}
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "userId": "USR-a1b2c3d4",
  "email": "user@example.com",
  "name": "홍길동",
  "phoneNumber": "010-****-5678",
  "birthDate": "1990-01-15",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00"
}
```

### 3. 프로필 수정
```http
PUT /api/v1/users/{userId}
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
Content-Type: application/json

{
  "name": "홍길동(수정)",
  "phoneNumber": "010-9876-5432"
}
```

**도메인 검증:**
- DELETED, SUSPENDED 상태에서는 수정 불가 (UserException 발생)
- 이름 2~50자 검증
- 전화번호 형식 검증

### 4. 회원 탈퇴 (Soft Delete)
```http
DELETE /api/v1/users/{userId}
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
```

**처리:**
1. `user.withdraw()` 호출 → status = DELETED
2. BaseEntity: `isDeleted = true`, `deletedAt`, `deletedBy` 설정
3. Kafka: `user.deleted` 이벤트 발행

### 5. 이메일 중복 확인
```http
GET /api/v1/users/check-email?email=user@example.com
```

**Response (200 OK)**
```json
{
  "email": "user@example.com",
  "available": true
}
```

---

## 📂 패키지 구조

```
com.jun_bank.user_service
├── UserServiceApplication.java
├── global/                              # 전역 설정 레이어
│   ├── config/
│   │   ├── JpaConfig.java               # JPA Auditing 활성화
│   │   ├── QueryDslConfig.java          # QueryDSL JPAQueryFactory
│   │   ├── KafkaProducerConfig.java     # 멱등성 Producer
│   │   ├── KafkaConsumerConfig.java     # 수동 ACK Consumer
│   │   ├── SecurityConfig.java          # 헤더 기반 인증
│   │   ├── FeignConfig.java             # Feign Client 설정
│   │   ├── SwaggerConfig.java           # OpenAPI 문서화
│   │   └── AsyncConfig.java             # 비동기 처리
│   ├── infrastructure/
│   │   ├── entity/
│   │   │   └── BaseEntity.java          # 공통 엔티티 (Audit, Soft Delete)
│   │   └── jpa/
│   │       └── AuditorAwareImpl.java    # JPA Auditing 사용자 정보
│   ├── security/
│   │   ├── UserPrincipal.java           # 인증 사용자 Principal
│   │   ├── HeaderAuthenticationFilter.java
│   │   └── SecurityContextUtil.java
│   ├── feign/
│   │   ├── FeignErrorDecoder.java       # Feign 에러 → BusinessException
│   │   └── FeignRequestInterceptor.java # 인증 헤더 전파
│   └── aop/
│       └── LoggingAspect.java           # 요청/응답 로깅
└── domain/
    └── user/                            # User Bounded Context
        ├── domain/                      # 순수 도메인 ★ 구현 완료
        │   ├── exception/
        │   │   ├── UserErrorCode.java   # 에러 코드 (common-lib ErrorCode 구현)
        │   │   └── UserException.java   # 도메인 예외 (BusinessException 상속)
        │   └── model/
        │       ├── User.java            # Aggregate Root
        │       ├── UserStatus.java      # 상태 Enum (정책 메서드)
        │       └── vo/
        │           ├── UserId.java      # 사용자 ID VO
        │           ├── Email.java       # 이메일 VO
        │           └── PhoneNumber.java # 전화번호 VO
        ├── application/                 # 유스케이스 (TODO)
        │   ├── port/
        │   │   ├── in/                  # UseCase 인터페이스
        │   │   └── out/                 # Repository, Feign Port
        │   ├── service/
        │   └── dto/
        ├── infrastructure/              # Adapter Out (TODO)
        │   ├── persistence/
        │   │   ├── entity/              # JPA Entity
        │   │   ├── repository/          # JPA Repository
        │   │   └── adapter/             # Repository Adapter
        │   ├── feign/                   # Auth Server Feign Client
        │   └── kafka/                   # Kafka Producer
        └── presentation/                # Adapter In (TODO)
            ├── controller/
            └── dto/
```

---

## 🔗 서비스 간 통신

### Feign Client (동기 호출)
| 대상 | 용도 | 실패 시 |
|------|------|---------|
| Auth Server | 회원가입 시 인증 정보 생성 | 트랜잭션 롤백 |

### Kafka (비동기 이벤트)
| 이벤트 | 토픽 | 수신 서비스 |
|--------|------|-------------|
| USER_CREATED | user.created | - |
| USER_UPDATED | user.updated | - |
| USER_DELETED | user.deleted | Account, Card, Transfer 등 |

---

## 📝 구현 체크리스트

### Domain Layer ✅
- [x] UserErrorCode (에러 코드 정의)
- [x] UserException (팩토리 메서드 패턴)
- [x] UserStatus (정책 메서드 포함)
- [x] UserId VO
- [x] Email VO
- [x] PhoneNumber VO
- [x] User (Aggregate Root, 감사 필드 포함)

### Application Layer
- [ ] CreateUserUseCase
- [ ] GetUserUseCase
- [ ] UpdateUserUseCase
- [ ] DeleteUserUseCase
- [ ] UserPort (Repository 인터페이스)
- [ ] AuthPort (Feign 인터페이스)
- [ ] UserEventPort (Kafka 인터페이스)
- [ ] DTO 정의

### Infrastructure Layer
- [ ] UserEntity (JPA Entity)
- [ ] UserJpaRepository
- [ ] UserRepositoryAdapter
- [ ] AuthFeignClient
- [ ] AuthFeignAdapter
- [ ] UserKafkaProducer

### Presentation Layer
- [ ] UserController
- [ ] Request/Response DTO
- [ ] Swagger 문서화

### 테스트
- [ ] 도메인 단위 테스트
- [ ] Application 단위 테스트
- [ ] Repository 통합 테스트
- [ ] API 통합 테스트