# User Service

> 사용자 관리 서비스 - 회원가입, 프로필 관리, 회원 탈퇴

## 📋 개요

| 항목 | 내용 |
|------|------|
| 포트 | 8087 |
| 데이터베이스 | user_db (PostgreSQL) |
| 주요 역할 | 사용자 생명주기 관리 |

## 🎯 학습 포인트

### 1. 기본 CRUD 패턴
- **JPA Entity 설계**: `@Entity`, `@Table`, `@Column` 활용
- **Repository 패턴**: Spring Data JPA 기본 사용법
- **DTO 변환**: Entity ↔ DTO 분리 (보안, 유연성)
- **Validation**: `@Valid`, `@NotBlank`, `@Email` 등 Bean Validation

### 2. 이벤트 발행 (Kafka Producer)
```
회원가입 완료 → user.created 이벤트 발행 → Auth Server 수신
회원 탈퇴 → user.deleted 이벤트 발행 → 모든 서비스 수신 (연관 데이터 정리)
```

### 3. 보안 고려사항
- 비밀번호 암호화 (BCrypt)
- 민감 정보 마스킹 (주민번호, 전화번호)
- API 응답에서 비밀번호 제외

---

## 🗄️ 도메인 모델

### User Entity

```
┌─────────────────────────────────────────────┐
│                    User                      │
├─────────────────────────────────────────────┤
│ id: Long (PK, Auto)                         │
│ email: String (Unique, Not Null)            │
│ password: String (Encrypted)                │
│ name: String                                │
│ phoneNumber: String                         │
│ birthDate: LocalDate                        │
│ status: UserStatus (ACTIVE/INACTIVE/DELETED)│
│ createdAt: LocalDateTime                    │
│ updatedAt: LocalDateTime                    │
│ version: Long (@Version - 낙관적 락)         │
└─────────────────────────────────────────────┘
```

### UserStatus Enum
```java
public enum UserStatus {
    ACTIVE,     // 정상
    INACTIVE,   // 휴면
    SUSPENDED,  // 정지
    DELETED     // 탈퇴 (Soft Delete)
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

**Response (201 Created)**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "phoneNumber": "010-****-5678",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00"
}
```

**이벤트 발행**: `user.created`
```json
{
  "eventId": "uuid",
  "eventType": "USER_CREATED",
  "timestamp": "2024-01-15T10:30:00",
  "payload": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동"
  }
}
```

---

### 2. 사용자 조회 (단건)
```http
GET /api/v1/users/{userId}
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "phoneNumber": "010-****-5678",
  "birthDate": "1990-01-15",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00"
}
```

---

### 3. 사용자 정보 수정
```http
PUT /api/v1/users/{userId}
X-User-Id: 1
X-User-Role: USER
Content-Type: application/json

{
  "name": "홍길동(수정)",
  "phoneNumber": "010-9876-5432"
}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동(수정)",
  "phoneNumber": "010-****-5432",
  "status": "ACTIVE",
  "updatedAt": "2024-01-15T11:00:00"
}
```

**이벤트 발행**: `user.updated`

---

### 4. 비밀번호 변경
```http
PUT /api/v1/users/{userId}/password
X-User-Id: 1
X-User-Role: USER
Content-Type: application/json

{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!"
}
```

**Response (200 OK)**
```json
{
  "message": "비밀번호가 변경되었습니다."
}
```

---

### 5. 회원 탈퇴 (Soft Delete)
```http
DELETE /api/v1/users/{userId}
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "message": "회원 탈퇴가 완료되었습니다."
}
```

**이벤트 발행**: `user.deleted`
```json
{
  "eventId": "uuid",
  "eventType": "USER_DELETED",
  "timestamp": "2024-01-15T12:00:00",
  "payload": {
    "userId": 1
  }
}
```

---

### 6. 사용자 목록 조회 (관리자)
```http
GET /api/v1/users?page=0&size=20&status=ACTIVE
X-User-Id: 999
X-User-Role: ADMIN
```

**Response (200 OK)**
```json
{
  "content": [
    {
      "id": 1,
      "email": "user1@example.com",
      "name": "홍길동",
      "status": "ACTIVE"
    },
    {
      "id": 2,
      "email": "user2@example.com",
      "name": "김철수",
      "status": "ACTIVE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

---

### 7. 이메일 중복 확인
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
├── global/                          # 전역 설정 레이어
│   ├── config/                      # 설정 클래스
│   │   ├── JpaConfig.java           # JPA Auditing 활성화
│   │   ├── QueryDslConfig.java      # QueryDSL JPAQueryFactory 빈
│   │   ├── KafkaProducerConfig.java # Kafka Producer (멱등성, JacksonJsonSerializer)
│   │   ├── KafkaConsumerConfig.java # Kafka Consumer (수동 ACK, JacksonJsonDeserializer)
│   │   ├── SecurityConfig.java      # Spring Security (헤더 기반 인증)
│   │   ├── FeignConfig.java         # Feign Client 설정
│   │   ├── SwaggerConfig.java       # OpenAPI 문서화
│   │   └── AsyncConfig.java         # 비동기 처리 (ThreadPoolTaskExecutor)
│   ├── infrastructure/
│   │   ├── entity/
│   │   │   └── BaseEntity.java      # 공통 엔티티 (Audit, Soft Delete)
│   │   └── jpa/
│   │       └── AuditorAwareImpl.java # JPA Auditing 사용자 정보
│   ├── security/
│   │   ├── UserPrincipal.java       # 인증 사용자 Principal
│   │   ├── HeaderAuthenticationFilter.java # Gateway 헤더 인증 필터
│   │   └── SecurityContextUtil.java # SecurityContext 유틸리티
│   ├── feign/
│   │   ├── FeignErrorDecoder.java   # Feign 에러 → BusinessException 변환
│   │   └── FeignRequestInterceptor.java # 인증 헤더 전파
│   └── aop/
│       └── LoggingAspect.java       # 요청/응답 로깅 AOP
└── domain/
    └── user/                        # User 도메인
        ├── domain/                  # 순수 도메인 (Entity, VO, Enum)
        ├── application/             # 유스케이스, Port, DTO
        ├── infrastructure/          # Adapter (Out) - Repository, Kafka
        └── presentation/            # Adapter (In) - Controller
```

---

## 🔧 Global 레이어 상세

### Config 설정

| 클래스 | 설명 |
|--------|------|
| `JpaConfig` | JPA Auditing 활성화 (`@EnableJpaAuditing`) |
| `QueryDslConfig` | `JPAQueryFactory` 빈 등록 |
| `KafkaProducerConfig` | 멱등성 Producer (ENABLE_IDEMPOTENCE=true, ACKS=all) |
| `KafkaConsumerConfig` | 수동 ACK (MANUAL_IMMEDIATE), group-id: user-service-group |
| `SecurityConfig` | Stateless 세션, 헤더 기반 인증, CSRF 비활성화 |
| `FeignConfig` | 로깅 레벨 BASIC, 에러 디코더, 요청 인터셉터 |
| `SwaggerConfig` | OpenAPI 3.0 문서화 설정 |
| `AsyncConfig` | ThreadPoolTaskExecutor (core=5, max=10, queue=25) |

### Security 설정

| 클래스 | 설명 |
|--------|------|
| `HeaderAuthenticationFilter` | `X-User-Id`, `X-User-Role`, `X-User-Email` 헤더 → SecurityContext |
| `UserPrincipal` | `UserDetails` 구현체, 인증된 사용자 정보 |
| `SecurityContextUtil` | 현재 사용자 조회 유틸리티 |

### BaseEntity (Soft Delete 지원)

```java
@MappedSuperclass
public abstract class BaseEntity {
    private LocalDateTime createdAt;      // 생성일시 (자동)
    private LocalDateTime updatedAt;      // 수정일시 (자동)
    private String createdBy;             // 생성자 (자동)
    private String updatedBy;             // 수정자 (자동)
    private LocalDateTime deletedAt;      // 삭제일시
    private String deletedBy;             // 삭제자
    private Boolean isDeleted = false;    // 삭제 여부
    
    public void delete(String deletedBy);  // Soft Delete
    public void restore();                 // 복구
}
```

---

## 🔗 서비스 간 통신

### 발행 이벤트 (Kafka Producer)
| 이벤트 | 토픽 | 수신 서비스 | 설명 |
|--------|------|-------------|------|
| USER_CREATED | user.created | Auth Server | 계정 생성 트리거 |
| USER_UPDATED | user.updated | - | 정보 변경 알림 |
| USER_DELETED | user.deleted | All Services | 연관 데이터 정리 |

### 수신 이벤트 (Kafka Consumer)
| 이벤트 | 토픽 | 발신 서비스 | 설명 |
|--------|------|-------------|------|
| - | - | - | (현재 없음) |

### Feign Client 호출
| 대상 서비스 | 용도 | 비고 |
|-------------|------|------|
| Auth Server | 계정 상태 동기화 | 선택적 |

---

## ⚙️ 설정

### application.yml (서비스 내부)
- 포트: 8087
- Eureka 등록
- Config Server 연결

### config-repo (Config Server)
- DB 접속 정보: user_db
- Kafka 토픽 정의
- 서비스 고유 설정 (비밀번호 정책 등)

---

## 🧪 테스트 시나리오

### 1. 회원가입 테스트
```bash
# 정상 케이스
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test1234!","name":"테스트"}'

# 중복 이메일 (409 Conflict 예상)
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test1234!","name":"테스트2"}'

# 유효성 검증 실패 (400 Bad Request 예상)
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"email":"invalid-email","password":"123","name":""}'
```

### 2. Kafka 이벤트 확인
```bash
# Kafka 토픽 메시지 확인
docker exec -it kafka-1 kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic user.created \
  --from-beginning
```

---

## 📝 구현 체크리스트

- [ ] Entity, Repository 생성
- [ ] Service 레이어 구현
- [ ] Controller 구현
- [ ] DTO, Mapper 구현
- [ ] Kafka Producer 구현
- [ ] 예외 처리 (GlobalExceptionHandler)
- [ ] 유효성 검증 (@Valid)
- [ ] 비밀번호 암호화 (BCrypt)
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] API 문서화 (Swagger)