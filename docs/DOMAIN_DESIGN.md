---
level: 2
mode: committed
status: active
last_verified: 2026-05-29
confidence: 0.85
owners: [박세현]
description: Entity 모델·DB 스키마·Repository 규칙 — Entity 추가/변경·Flyway 마이그레이션 추가 시 반드시 갱신.
---

# DOMAIN_DESIGN — 도메인 모델 · DB 스키마 (L2)

---

## 1. Entity 목록

| Entity | 패키지 | 테이블 | 설명 |
|---|---|---|---|
| `User` | `domain.auth` | `users` | 인증 사용자 (JWT) |
| `UserProfile` | `domain.user` | `user_profile` | 사용자 관심사·임베딩·나이·직업 |
| `Bill` | `domain.bill` | `bill` | 국회 의안 (위원회 포함) |
| `Petition` | `domain.petition` | `petition` | 국민동의청원 (임베딩 포함) |
| `LegislationNotice` | `domain.legislation` | `legislation_notice` | 입법예고 (임베딩 포함) |

---

## 2. 핵심 설계 결정

- **pgvector 임베딩 컬럼**: `UserProfile`, `Bill`, `Petition`, `LegislationNotice` 모두 벡터 컬럼 보유 — 코사인 유사도 추천에 사용
- **Entity는 반드시 `domain/` 패키지** — `api/`, `batch/`, `recommendation/` 에 Entity 선언 금지
- **DTO 분리**: 공공 API 응답용 DTO는 `batch/step/dto/` 에 위치, Entity와 분리
- **Repository**: Spring Data JPA. pgvector 쿼리는 `@Query(nativeQuery = true)` 로 Native Query 작성

---

## 3. Flyway 마이그레이션 원칙

- 파일명 형식: `V{번호}__{설명}.sql` (언더스코어 2개)
- 기존 마이그레이션 파일 수정 금지 — 수정이 필요하면 새 버전 추가
- 첫 마이그레이션(V1)에 `CREATE EXTENSION IF NOT EXISTS vector;` 포함 (pgvector 의존)
- 새 컬럼/테이블 추가 시 이 문서 §1 표 갱신 필수

---

## 4. 도메인 불변식

- `Bill.billNo` — 국회 고유 법안 번호, 중복 저장 금지 (Batch 멱등성 기준)
- `Petition` — 동일 청원 중복 저장 금지
- `LegislationNotice` — 동일 입법예고 중복 저장 금지
- 임베딩 컬럼은 Batch 임베딩 Job 실행 후 채워짐 — null 허용 (초기 수집 시)
