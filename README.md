# 지금참여 — Backend

> AI 기반 정치 참여 플랫폼 **지금참여**의 백엔드. 국회 의안·국민동의청원·입법예고 공공데이터를 수집·임베딩하고, pgvector 개인화 추천과 직접 구현한 AI 에이전트(분석·챗봇)를 제공합니다.

단국대학교 오픈소스 기초 프로젝트 (5조) · [조직 홈](https://github.com/dku-jigeum) · [문서(Confluence)](https://dankook-opensource-project.atlassian.net/wiki/spaces/MFS/overview)

## 기술 스택

- **언어/런타임**: Java 17
- **프레임워크**: Spring Boot 4.0.6 (Web · Security · Data JPA · Batch)
- **DB**: PostgreSQL + pgvector (벡터 유사도 검색)
- **마이그레이션**: Flyway
- **인증**: Spring Security + JWT
- **임베딩**: OpenAI `text-embedding-3-small` (1536차원)
- **AI 에이전트 LLM**: OpenAI-compatible 엔드포인트 — 로컬 **EXAONE 3.5**(Ollama) 또는 OpenAI. Planner-Executor + ReAct 직접 구현
- **빌드**: Gradle

## 주요 기능 (REST API)

| 그룹 | 경로 | 설명 |
| --- | --- | --- |
| 인증 | `/api/auth` | 회원가입·로그인 (JWT 발급) |
| 사용자 프로필 | `/api/users/profile` | 온보딩(관심사·직업) → 프로필 임베딩 |
| 피드 | `/api/feed`, `/api/feed/trending` | pgvector 개인화 추천 + 트렌딩 |
| 이슈 | `/api/bills`, `/api/petitions`, `/api/legislation` | 법안·청원·입법예고 목록/상세 |
| 상세 분석 에이전트 | `/api/agent/analyze` (+`/stream`) | Planner-Executor 분석 카드 (SSE) |
| 챗봇 | `/api/agent/chat` (+`/stream`) | 자율 ReAct 챗봇 (SSE) |
| 심화 도구 | `/api/events/*` | 유사 이슈·비교·의견 초안·캘린더 |
| 북마크 | `/api/bookmarks` | 관심 이슈 저장 |

> 전체 명세: [Backend API 명세서 (Confluence)](https://dankook-opensource-project.atlassian.net/wiki/spaces/MFS/pages/11927574)

## 데이터 수집 (Spring Batch)

- 공공 API → Reader / Processor / Writer 구조로 수집 → DB 저장
- 별도 임베딩 잡(`Bill/Petition/LegislationEmbeddingJob`)이 본문을 벡터화해 pgvector에 적재
- `spring.batch.job.enabled=false` — 잡은 스케줄러/수동 트리거로 실행

## 프로젝트 구조

```
src/main/java/com/dku/opensource/be
├─ api/            REST 컨트롤러
├─ agent/          AI 에이전트 (model/ExaoneClient · react/ReActLoop · tool/*)
├─ batch/          Spring Batch 수집·임베딩 잡
├─ recommendation/ 임베딩·추천 (EmbeddingService · RecommendationService)
├─ domain/         엔티티·리포지토리
├─ security/       JWT · Spring Security
└─ common/         공통 응답·예외
```

## 로컬 실행

### 요구사항
- Java 17, PostgreSQL 15+ (pgvector 확장), (선택) Ollama + EXAONE 3.5

### 1) DB 준비
```sql
CREATE DATABASE jigeumchamyeo;
\c jigeumchamyeo
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2) 시크릿 설정
`src/main/resources/application-secret.yml.example` 를 복사해 `application-secret.yml` 로 저장하고 값을 채웁니다(git 제외). 또는 환경 변수로 주입:

| 변수 | 설명 |
| --- | --- |
| `DB_URL` · `DB_USERNAME` · `DB_PASSWORD` | DB 접속 (기본 `jdbc:postgresql://localhost:5432/jigeumchamyeo`) |
| `OPENAI_API_KEY` | 임베딩(`text-embedding-3-small`) |
| `AGENT_BASE_URL` · `AGENT_API_KEY` · `AGENT_MODEL_NAME` | 에이전트 LLM. EXAONE 로컬이면 Ollama OpenAI-호환 주소 지정 |
| `JWT_SECRET` | JWT 서명 키 |
| `public-data.bill/petition/legislation-api-key` | 의안·청원·입법예고 공공 API 키 (open.assembly.go.kr / data.go.kr) |
| `firebase.credentials-path` | (선택) FCM |

### 3) 실행
```bash
./gradlew bootRun     # 기본 포트 8082
```

## ⚠️ 마이그레이션 주의 (KAN-43)

- 현재 **Flyway 자동 실행이 동작하지 않습니다**(Spring Boot 4 호환 이슈). 신규 마이그레이션은 `src/main/resources/db/migration/V*.sql` 를 **수동 적용**해야 합니다.
- 기존 테이블과 baseline 정합 후 자동화 예정 → [KAN-43](https://dankook-opensource-project.atlassian.net/browse/KAN-43).

### 트러블슈팅
- Postgres가 scram 인증을 요구해 접속이 막히면 `pg_hba.conf`/드라이버 인증 방식을 확인하세요.
- Docker로 띄울 때 IPv6 바인딩 때문에 접속이 안 되면 호스트를 `127.0.0.1` 로 명시하세요.

## 브랜치 전략

- **`develop`** 이 활성 개발 base(최신 기능 포함), `main`은 통합 시점 반영.
- 작업 브랜치 `feature/KAN-번호-기능명`, PR 제목 `[KAN-번호] 작업 내용`.

## 관련 링크

- [FE 저장소](https://github.com/dku-jigeum/FE) · [Jira KAN 보드](https://dankook-opensource-project.atlassian.net/jira/software/projects/KAN/boards/1) · [Confluence 문서](https://dankook-opensource-project.atlassian.net/wiki/spaces/MFS/overview)
