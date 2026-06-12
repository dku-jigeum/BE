# HANDOFF — 2026-06-12 세션

> 다음 세션 시작 시 이 문서를 먼저 읽을 것.

## 이번 세션에서 완료한 것

### 1. main 동기화
- origin/main 13커밋 fast-forward (`049c5ff` → `f38ffff`)
- 받아온 주요 내용: KAN-12 에이전트 전체 구현 (`agent/` 패키지, Tool 10개, Planner+ToolExecutor 구조), 캘린더 도메인, 입법예고 본문 백필 배치, Flyway V10~V12

### 2. KAN-41 — 챗봇 Q&A 에이전트 (신규 등록 → 구현 → 완료)
- **Jira**: [KAN-41](https://dankook-opensource-project.atlassian.net/browse/KAN-41) — 완료 전환됨
- **PR**: [#26](https://github.com/DKU-OpenSource/BE/pull/26) — `feature/KAN-41-chat-qa` → `main`, **리뷰/머지 대기 중**
- **커밋**: `2ee9362` (기본 구현), `e1c7779` (현재 이슈 컨텍스트 주입)

**구현 내용:**
| 파일 | 역할 |
|---|---|
| `agent/tool/SearchIssuesTool.java` | `search_issues` — 질문 임베딩 → pgvector로 법안3+청원2+입법예고2 검색 |
| `agent/tool/GetIssueDetailTool.java` | `get_issue_detail` — `type:id` 입력 → 제목/본문(800자)/마감일 |
| `agent/AgentChatService.java` | 단일 턴 파이프라인: 검색 → 상위 2건 본문 → EXAONE 답변. **Planner 미경유** (검색이 항상 필요하므로) |
| `api/AgentController.java` | `POST /api/agent/chat` 추가 |
| `test/.../AgentChatServiceTest.java` | 단위 테스트 6건 (Mockito) — 전체 빌드 통과 |

**설계 결정 (사용자 확정):**
- 단일 턴 (대화 이력 없음), Web fetch 없음 (본문은 DB에 이미 수집됨)
- 챗봇은 **상시 노출** + 상세페이지에서 질문 시 `issueId`/`issueType`(optional)을 전달하면 해당 이슈를 "현재 보고 있는 이슈"로 프롬프트에 주입 — "이 법안" 지시어 해석 지원

```json
// 일반: { "question": "최저임금 관련 법안 뭐 있어?" }
// 상세페이지: { "question": "이 법안 영향 있어?", "issueId": "2200063", "issueType": "bill" }
```

### 3. GitHub 인증 정리
- remote URL에 평문 노출돼 있던 `ghp_` 토큰 제거 → `https://github.com/DKU-OpenSource/BE.git`
- 해당 토큰은 **API에서 401 (무효)** 상태였음 — 폐기 권장 (GitHub Settings > Tokens)
- 현재 인증: **gh CLI 로그인 (parksehyn, keyring)** + `gh auth setup-git` 적용 — push/PR 모두 gh 경유
- PR 생성 시 한글 본문은 `gh pr create --body-file <UTF-8 파일>` 사용

## 미해결 / 주의사항

1. **PR #26 머지 전 실서버 검증 권장** — 단위 테스트만 했음. OpenAI 임베딩 키 + EXAONE 엔드포인트 필요. `bootRun` 후 `/api/agent/chat`에 실제 질문 1회 확인할 것
2. **로컬 미커밋 변경 (의도적으로 커밋 안 함)**:
   - `SecurityConfig.java` — `/`, `/index.html` permitAll 1줄 (이전 로컬 테스트 잔재, 처리 방침 미정)
   - 추적 안 되는 파일: `bootrun.log`, `bootrun_collect.log`, `C:temp_output.txt`
3. **BE/CLAUDE.md의 Jira 이슈 표는 구버전** — Jira 직접 조회가 기준 (KAN-9/10/12/13/14/28/37 모두 완료됨)

## 다음 작업: KAN-11 — FCM 마감 임박 푸시 알림 (D-7/D-3/D-1)

마지막 남은 Jira 이슈. 시작 전 확인할 것:
1. **Firebase 서비스 계정 JSON 키** 발급 여부 → `application-secret.yml` 등록 필요
2. **북마크 도메인 존재 여부** — 이슈 설명은 "북마크한 사용자에게 발송"인데 북마크 기능이 코드에 있는지 확인. 없으면 `UserCalendarEvent`(캘린더 등록) 기반으로 대체할지 사용자와 상의
3. FE에서 FCM 토큰 받는 API (`POST /api/users/fcm-token`) 필요 — `UserProfile.fcmToken` 컬럼은 이미 존재
4. 브랜치: `feature/KAN-11-fcm-notification`
