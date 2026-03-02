# OYE Backend - Spring Boot (Kotlin)

## Quick Reference
- **Build**: `./gradlew compileKotlin`
- **Run**: `./gradlew bootRun`
- **Tech**: Spring Boot 4.0, Kotlin 2.2, JPA/Hibernate, Flyway, Spring AI (OpenAI)
- **Java**: 21
- **Package**: `com.mindbridge.oye`
- **DB**: PostgreSQL (prod), H2 (dev)
- **Deploy**: Railway

## Project Structure
```
src/main/kotlin/com/mindbridge/oye/
├── config/          # Security, JWT, OAuth2, Cache, Filters
├── controller/      # REST controllers (implements api/ interfaces)
├── controller/api/  # API interfaces with Swagger annotations
├── domain/          # JPA entities & enums
├── dto/             # Request/Response DTOs
├── event/           # Event classes & listeners
├── exception/       # Custom exceptions & GlobalExceptionHandler
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic
└── util/            # Utilities (AiResponseParser, UserProfileBuilder, DateUtils)
```

## Coding Patterns

### Entity
- `@Entity`, `@Table`, `@Comment` on every column
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- `@ManyToOne(fetch = FetchType.LAZY)` for relations
- `val` for immutable, `var` for mutable
- Default: `val createdAt: LocalDateTime = LocalDateTime.now()`

### Controller
- API interface in `controller/api/` with Swagger `@Operation` + `@ApiResponse`
- Controller in `controller/` implements the interface
- `@RestController`, `@RequestMapping("/api/v1/...")`
- `AuthenticationResolver.getCurrentUser(principal)` for auth
- Returns `ApiResponse<T>` wrapper (`ApiResponse.success(data)`)

### Service
- Constructor injection (no `@Autowired`)
- `@Transactional(readOnly = true)` for reads, `@Transactional` for writes
- `ApplicationEventPublisher` for domain events
- AI calls: `callAiWithRetry()` pattern with retry logic (see CompatibilityService)

### DTO
- Immutable `data class` with `@Schema` annotations
- `companion object { fun from(entity): Dto }` for conversion

### Exception
- All extend `OyeException(message)` in `FortuneException.kt`
- Handlers in `GlobalExceptionHandler.kt` with `errorResponse(status, message, code)`

### Events
- `@Async @TransactionalEventListener(phase = AFTER_COMMIT)`
- `@Transactional(propagation = REQUIRES_NEW)` for new transaction

### Scheduler
- `@Scheduled(cron = "...", zone = "Asia/Seoul")`
- Batch processing with `BATCH_SIZE = 50`, `Thread.sleep(500)` between batches

### Repository
- `JpaRepository<T, Long>` with method name queries
- `@Query` with JPQL for JOIN FETCH

### Flyway
- Files: `src/main/resources/db/migration/V{N}__description.sql`
- Current latest: V11

## Key Domain Concepts
- **User**: 사용자 (birthDate, gender, mbti, bloodType 등 사주 정보)
- **UserConnection**: 1:1 연결 (LOVER/FRIEND/FAMILY/COLLEAGUE)
- **Group**: 그룹 궁합 (FRIEND/FAMILY/COLLEAGUE, max 10명, inviteCode)
- **Fortune**: AI 일일 운세 (매일 6AM KST 생성)
- **Compatibility**: AI 1:1 궁합 (매일 6:10AM KST 생성)
- **GroupCompatibility**: AI 그룹 내 쌍별 궁합 (매일 6:20AM KST 생성)
- **LottoRecommendation**: AI 로또 번호 추천

## Auth Flow
- Social login (Kakao/Apple) → SDK token → Backend verification → SocialAccount → JWT
- JWT in `Authorization: Bearer` header
- `@AuthenticationPrincipal principal: Any?` → `AuthenticationResolver.getCurrentUser()`
