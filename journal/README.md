---
level: 3
mode: log
status: active
last_verified: 2026-05-29
confidence: 0.9
owners: [박세현]
description: 하루 작업 로그·세션 인수인계. 세션 시작 직후 가장 최근 파일을 읽어 흐름을 이어받는다.
---

# journal/

하루 작업 로그와 세션 인수인계 문서 디렉터리.

## 파일 명명 규칙

- 일일 로그: `YYYY-MM-DD.md`
- 인수인계: `YYYY-MM-DD-{주제}-handoff.md`

## 세션 시작 시

가장 최근 파일을 읽어 "다음 할 일"을 확인한다.

## 항목 형식 (일일 로그)

```markdown
# YYYY-MM-DD

## 오늘 한 일
- ...

## 결정 사항
- ...

## 다음 할 일
- ...

## 막힌 것 / 미결
- ...
```
