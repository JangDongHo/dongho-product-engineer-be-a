# Epic - Story - Task 분해

**개발 기간:** 2026-04-25 (토) ~ 2026-04-28 (화)

---

## 일정 요약

| 날짜      | 목표                                         |
| --------- | -------------------------------------------- |
| 4/25 (토) | Epic 1 프로젝트 초기 설정 + Epic 2 강의 관리 |
| 4/26 (일) | Epic 3 수강 신청 관리                        |
| 4/27 (월) | Epic 4 정원 관리 & 동시성 제어 + 테스트 보강 |
| 4/28 (화) | Epic 5 선택 구현 + Epic 6 마무리 & 문서화    |

---

## Epic 1. 프로젝트 초기 설정

> **일정:** 4/25 오전
> **목표:** 개발 환경 구성 및 공통 컴포넌트 준비

### Story 1-1. Spring Boot 프로젝트 구성

- [x] **Task** `build.gradle` 의존성 추가
  - Spring Web, Spring Data JPA, MySQL Driver, Validation, Lombok
- [x] **Task** `application.yml` 환경 설정
  - DB 연결, JPA DDL 설정, 로깅 레벨
- [x] **Task** 레이어드 패키지 구조 설계
  - Presentation, Business, Persistence 패키지 구조 설계

### Story 1-2. Docker Compose 환경 구성

- [x] **Task** `docker-compose.yml` 작성
  - MySQL 컨테이너 설정 (이미지, 포트, 환경 변수)
  - 볼륨 마운트 설정 (데이터 영속성)
- [x] **Task** `application.yml` DB 연결 정보 연동
  - `docker-compose.yml` 환경 변수와 일치하도록 설정
- [x] **Task** 로컬 실행 검증
  - `docker compose up` 후 애플리케이션 정상 기동 확인

### Story 1-3. 공통 컴포넌트 구성

- [x] **Task** 커스텀 예외 클래스 정의
  - `BusinessException`, `ErrorCode` enum
  - 비즈니스 예외 vs 시스템 예외 분리
- [x] **Task** 글로벌 예외 핸들러 구현
  - `@RestControllerAdvice` + `@ExceptionHandler`
  - 에러 응답 포맷 통일 (`code`, `message`)
- [x] **Task** 공통 응답 포맷 정의 (선택)
  - `ApiResponse<T>` 래퍼 클래스

---

## Epic 2. 강의(Class) 관리

> **일정:** 4/25 오후
> **목표:** 강의 등록, 상태 전이, 목록/상세 조회 API 완성
> **관련 시나리오:** 시나리오 1, 2

### Story 2-1. 강의 도메인 모델 설계

- [x] **Task** `Class` 엔티티 설계
  - 필드: `id`, `creatorId`, `title`, `description`, `price`, `capacity`, `startDate`, `endDate`, `status`, `currentEnrollment`, `createdAt`, `updatedAt`
  - `price`: **KRW 원 단위 정수**(`long` 등)만 사용, **소수(소수점 금액)는 지원하지 않음**
  - PK 전략: Auto Increment
- [x] **Task** `ClassStatus` Enum 정의
  - `DRAFT`, `OPEN`, `CLOSED`
- [x] **Task** 인덱스 설계
  - 페이지네이션을 고려한 `(status, id)` 복합 인덱스 추가

### Story 2-2. 강의 등록 API

- [x] **Task** `POST /classes` API 구현
  - Request Body: `title`, `description`, `price`, `capacity`, `startDate`, `endDate` (`price` 는 **원 단위 정수**, JSON 숫자)
  - 초기 상태: `DRAFT`
- [x] **Task** 입력값 유효성 검증
  - `@NotBlank`, `@Positive`, `@NotNull` 등 Bean Validation 적용
  - 400 Bad Request 응답
- [x] **Task** 단위 테스트 작성
  - 정상 등록 케이스
  - 필수 항목 누락 케이스

### Story 2-3. 강의 상태 전이 API

- [x] **Task** `PATCH /classes/{id}/status` API 구현
  - Request Body: `status`
- [x] **Task** 허용된 상태 전이 규칙 구현
  - `DRAFT → OPEN`, `OPEN → CLOSED` 만 허용
  - 그 외 전이 시도 → 400 Bad Request
- [x] **Task** 단위 테스트 작성
  - 정상 전이 케이스 (DRAFT→OPEN, OPEN→CLOSED)
  - 불허 전이 케이스 (DRAFT→CLOSED, CLOSED→OPEN)

### Story 2-4. 강의 목록 & 상세 조회 API

- [x] **Task** `GET /classes` API 구현
  - Query Param: `status` (선택, 없으면 전체 반환)
  - 응답 목록: 강의 `createdAt` 기준 **최신순** 정렬
- [x] **Task** `GET /classes/{id}` API 구현
  - 현재 신청 인원 `currentEnrollment` 반환
  - 존재하지 않는 ID → 404 Not Found
- [x] **Task** 단위 테스트 작성
  - 상태 필터 조회 케이스
  - 현재 신청 인원 정확성 검증

---

## Epic 3. 수강 신청(Enrollment) 관리

> **일정:** 4/26
> **목표:** 수강 신청, 결제 확정, 취소, 내 목록 조회 API 완성
> **관련 시나리오:** 시나리오 3, 5, 6

### Story 3-1. 수강 신청 도메인 모델 설계

- [x] **Task** `Enrollment` 엔티티 설계
  - 필드: `id`, `userId`, `classId`, `status`, `confirmedAt`, `createdAt`, `updatedAt`
- [x] **Task** `EnrollmentStatus` Enum 정의
  - `PENDING`, `CONFIRMED`, `CANCELLED`
- [x] **Task** 인덱스 설계
  - `(userId, classId)` 유니크 제약 → 중복 신청 방지

### Story 3-2. 수강 신청 API

- [x] **Task** `POST /enrollments` API 구현
  - Request Body: `userId`, `classId`
  - 결과 상태: `PENDING`
  - 성공 시 `Lecture.currentEnrollment` 1 증가
- [x] **Task** 사전 조건 검증 로직 구현
  - 강의 상태가 `OPEN`인지 확인 → 아니면 400
  - 동일 강의 중복 신청 확인 → 409 Conflict
  - 정원 초과 확인 → 409 Conflict (Epic 4에서 동시성 강화)
- [x] **Task** 단위 테스트 작성
  - 정상 신청 케이스
  - DRAFT/CLOSED 강의 신청 시도
  - 중복 신청 시도
  - `currentEnrollment` 1 증가 검증

### Story 3-3. 결제 확정 API

- [x] **Task** `PATCH /enrollments/{id}/confirm` API 구현
  - `PENDING → CONFIRMED` 상태 전이
  - `confirmedAt` 타임스탬프 기록 (취소 기간 계산 기준)
  - `currentEnrollment`는 신청(`PENDING`) 시점에 정원이 반영되므로, 확정 시 **증감 없음** (이중 집계 방지)
- [x] **Task** 예외 처리
  - `PENDING`이 아닌 상태에서 확정 시도 → 400
- [x] **Task** 단위 테스트 작성
  - 정상 확정 케이스
  - 이미 CONFIRMED / CANCELLED 상태 확정 시도
  - 확정 시 `currentEnrollment` 변화 없음

### Story 3-4. 수강 취소 API

- [x] **Task** `PATCH /enrollments/{id}/cancel` API 구현
- [x] **Task** 취소 분기 로직 구현
  - `PENDING` 상태: 제한 없이 취소 가능
  - `CONFIRMED` 상태: `confirmedAt` 기준 7일 이내만 취소 가능
  - 7일 초과 시 → 400 Bad Request
  - 취소 성공 시 `Lecture.currentEnrollment` 1 감소
- [x] **Task** 단위 테스트 작성
  - PENDING 취소 케이스
  - CONFIRMED + 7일 이내 취소 케이스
  - CONFIRMED + 7일 초과 취소 케이스 (경계값 포함)
  - 취소 시 `currentEnrollment` 1 감소 검증

### Story 3-5. 내 수강 신청 목록 조회 API

- [x] **Task** `GET /enrollments?userId={userId}` API 구현
  - 응답: 강의 정보 + 신청 상태 포함, 수강 신청 `createdAt` 기준 **최신순** 정렬
- [x] **Task** 단위 테스트 작성

---

## Epic 4. 정원 관리 & 동시성 제어

> **일정:** 4/27
> **목표:** 정원 초과 방지 + 동시 요청 상황에서도 정원 무결성 보장
> **관련 시나리오:** 시나리오 4

### Story 4-1. 정원 초과 방지

- [x] **Task** `Lecture.currentEnrollment`로 정원·현재 인원을 관리
  - `PENDING` / `CONFIRMED`가 정원에 반영되는 규칙에 맞게, 수강 신청·확정·취소 흐름에서 `currentEnrollment`를 트랜잭션 내에서 일관되게 증감 갱신
- [x] **Task** 신청 시 정원 검증 로직 구현
  - `currentEnrollment >= capacity`이면 409 Conflict

### Story 4-2. 동시성 제어

- [x] **Task** 동시성 제어 방식 결정 및 근거 문서화
  - 비관적 락(`SELECT ... FOR UPDATE`) vs 낙관적 락(`@Version`) 비교
- [x] **Task** 선택한 방식으로 수강 신청 트랜잭션 구현
- [x] **Task** 동시 신청 통합 테스트 작성
  - 잔여 1석에 N명 동시 신청 → 정확히 1명만 성공 검증
  - `CountDownLatch` + `ExecutorService` 멀티스레드 테스트

---

## Epic 5. 선택 구현

> **일정:** 4/28 오전
> **목표:** 추가 점수 항목 구현 (우선순위 순)

### Story 5-1. 강의별 수강생 목록 조회 (크리에이터 전용)

- [ ] **Task** `GET /classes/{id}/enrollments` API 구현
  - `CONFIRMED` 상태 수강생만 반환
- [ ] **Task** 크리에이터 소유권 검증
  - 요청한 `creatorId`가 강의 소유자인지 확인 → 아니면 403 Forbidden
- [ ] **Task** 단위 테스트 작성

### Story 5-2. 신청 내역 페이지네이션

- [ ] **Task** 내 수강 신청 목록 API에 페이지네이션 적용
  - Cursor 기반 또는 Offset 기반 결정
  - Query Param: `page`, `size` (또는 `cursor`, `size`)

### Story 5-3. 대기열(Waitlist) 기능

- [ ] **Task** `Waitlist` 도메인 모델 설계
  - 필드: `id`, `userId`, `classId`, `waitOrder`, `createdAt`
- [ ] **Task** 정원 초과 시 대기열 자동 등록 로직 구현
- [ ] **Task** 수강 취소 시 대기자 자동 승격 로직 구현
  - 대기 순서 1번 수강생의 Enrollment를 `PENDING`으로 생성
- [ ] **Task** 단위 테스트 작성

---

## Epic 6. 테스트 & 마무리

> **일정:** 4/28 오후
> **목표:** 전체 시나리오 검증 + README 문서화 완성

### Story 6-1. 통합 테스트 & 엣지 케이스 검증

- [ ] **Task** 엣지 케이스 체크리스트 기반 테스트 작성
  - [ ] `PENDING` 상태에서 결제 없이 취소
  - [ ] 동일 강의 중복 신청 시도
  - [ ] `DRAFT` / `CLOSED` 강의에 신청 시도
  - [ ] 정원 1석에 동시 다수 신청 (동시성)
  - [ ] 결제 후 정확히 7일째 되는 날 취소 시도 (경계값)
  - [ ] 존재하지 않는 강의/신청 ID로 요청

### Story 6-2. README 문서화 완성

- [ ] **Task** 설계 결정 사항 작성
  - 페이지네이션 방식
  - 인덱스 전략
  - 트랜잭션 범위
  - 동시성 제어 방식 선택 이유
  - 예외 처리 전략
- [ ] **Task** 데이터 모델 ERD 정리
- [ ] **Task** AI 활용 범위 작성

---

## 우선순위 & 리스크

| 우선순위 | 항목                                              | 비고                        |
| -------- | ------------------------------------------------- | --------------------------- |
| 🔴 필수  | Epic 1~4                                          | 과제 필수 구현 범위         |
| 🟡 권장  | Story 5-1 (수강생 목록), Story 5-2 (페이지네이션) | 추가 점수, 구현 난이도 낮음 |
| 🟢 선택  | Story 5-3 (대기열)                                | 추가 점수, 구현 복잡도 높음 |

**리스크:**

- 동시성 제어(Story 4-2)는 구현 + 테스트 검증에 시간이 많이 소요될 수 있음 → 4/27 오전 중에 반드시 완료
- 대기열(Story 5-3)은 시간 부족 시 과감히 제외
