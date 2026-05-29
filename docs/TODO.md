---
level: 3
mode: committed
status: active
last_verified: 2026-05-29
confidence: 0.8
owners: [박세현]
description: 미완료 작업·외부 설정 체크리스트. Jira(KAN)가 상세 이슈의 단일 원천 — 여기엔 외부 설정·비밀값·환경별 체크리스트만.
---

# TODO — 미완료 사항 · 외부 설정 체크리스트 (L3)

> 작업 이슈는 Jira(KAN 프로젝트)가 단일 원천. 여기엔 Jira에 담기 어려운 외부 설정·주의사항만.

---

## 미구현 패키지

| 패키지 | Jira | 상태 |
|---|---|---|
| `notification/` | KAN-11 | 해야 할 일 |
| `agent/` | KAN-12 | 해야 할 일 |
| 입법예고 Batch | KAN-14 | 해야 할 일 |

---

## 외부 설정 체크리스트

- [ ] `application-secret.yml` — `bill-api-key`, `petition-api-key`, `legislation-api-key` 값 확인
- [ ] 입법예고 (`open.lawmaking.go.kr`) 별도 포털 키 필요 여부 확인 — 인증 오류 발생 시
- [ ] FCM 서비스 계정 키 (`google-services.json` 또는 서비스 계정 JSON)
- [ ] EXAONE 3.5 API 엔드포인트·키 설정
- [ ] Flyway 자동 실행 미작동 원인 확인 (V1~V3 수동 적용 이력 있음) — 프로덕션 배포 전 필수
- [ ] pgvector `CREATE EXTENSION` 프로덕션 DB 적용 확인

---

## Agent 설계 방향 (KAN-12)

Spring AI + OpenAI GPT-4o 오케스트레이터. 툴 목록:

| 툴 | 트리거 |
|---|---|
| 법안 Q&A | 채팅 입력 |
| 캘린더 등록 | 법안 클릭 → `.ics` 반환 |
| 3줄 요약 | 법안 상세 진입 |
| 관련 법안 | 법안 상세 하단 |
| 관련 청원 연결 | 법안 상세 |
| 법안 비교 | 두 법안 선택 |

캘린더: `GET /api/bills/{billNo}/calendar` → `.ics` 파일 반환. Google Calendar API 연동은 2차.
