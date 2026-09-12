---
name: android-github-workflow
description: Manage Android project GitHub work using the team's issue-first workflow. Use when creating or updating GitHub issues, issue-linked branches, commits, pull requests, labels, or release pull requests for this repository.
---

# Android GitHub Workflow

## Overview

Follow the Notion Android convention: create an issue before work, include the issue number in the branch, commit, and PR, and merge implementation PRs into `dev`.

이 Skill보다 저장소의 `AGENTS.md` 규칙을 우선한다. 두 문서가 충돌하면 임의로 선택하지 않고 `AGENTS.md` 형식으로 정렬한 뒤, 필요한 규칙 파일 수정 권한을 사용자에게 확인한다.

## 실행 Hook

GitHub Hook은 외부 상태를 변경하기 전에 실행한다. 검증이 끝나지 않았거나 식별자가 불일치하면 다음 GitHub 작업을 실행하지 않는다.

### `BeforeGitHubMutation`

- 저장소·Issue 범위·작업 유형·허용 label·현재 branch와 원격 대상 branch를 확인한다.
- Issue → branch → commit → draft PR → ready → `dev` merge 순서를 지킨다.
- AGENTS.md에 정의된 제목·본문·commit 형식으로 issue 번호를 연결하고 `codex/` 접두사를 사용하지 않는다.

### `AfterGitHubMutation`

- 생성·수정한 Issue, branch, commit, PR의 실제 상태와 대상 branch를 다시 조회한다.
- PR 병합 후 merge commit과 Issue 종료 상태를 확인하고, 자동 종료되지 않으면 완료 상태를 명시적으로 반영한다.

## Workflow

1. Confirm the repository, issue scope, and whether the change is a feature, fix, refactor, chore, or hotfix.
2. Create or select the GitHub issue using the required template. Apply exactly one applicable label.
3. Create the work branch from `dev` as `feature/#<issue-number>-<kebab-case-summary>`, `fix/#<issue-number>-<kebab-case-summary>`, or `refactor/#<issue-number>-<kebab-case-summary>`.
4. Make and validate the Android change. Run relevant Gradle build, unit-test, lint, and UI/Preview checks.
5. Commit as `<type>: [#<issue-number>] <작업 요약>`.
6. Push the branch and open a draft PR targeting `dev` with the required title and body.
7. After integration testing on `dev`, create a release chore issue and a release branch for the `dev` to `main` PR.

## Issue Convention

Use this format:

```md
제목: [Feature] 구현할 작업

## 목적
작업이 필요한 이유와 해결하려는 문제를 작성합니다.

## 작업 내용
- [ ]

## 완료 조건
- [ ]
- [ ]

## 참고 사항
- 브랜치: `feature/#<issue-number>-<summary>` 또는 작업 유형에 맞는 `fix`·`refactor` 브랜치
- PR 대상: `dev`
```

## Label Convention

- `✨ feature`: 새로운 기능 구현
- `🚨 fix`: 버그 수정
- `♻️ refactor`: 동작 변경 없는 구조 개선
- `🔧 chore`: 설정, 빌드, dependency 작업
- `🔥 hotfix`: `main`에 반영된 긴급 수정

## Commit Convention

AGENTS.md의 Conventional Commits 형식을 사용한다.

```text
<type>: [#<issue-number>] <작업 요약>
```

긴급 운영 수정도 같은 형식을 사용하며 `type`은 `fix`로 작성한다. 각 commit은 연결된 Issue 범위 하나만 포함한다.

## Pull Request Convention

Target `dev` for implementation work. Use this title and body:

```md
제목: <type>: <전체 작업 요약>

## 변경 사항
-

## 검증
- [ ] Build
- [ ] Unit Test
- [ ] Lint
- [ ] UI / Preview 확인

## 체크리스트
- [ ] DTO·ApiService가 Presentation/Domain에 노출되지 않음
- [ ] 문자열·색상·간격 하드코딩 없음
- [ ] Hilt binding 확인
- [ ] 민감 정보 포함 없음

## 관련 이슈
Closes #<issue-number>
```

Use a draft PR unless the user explicitly requests ready-for-review. Verify the title, issue number, `dev` base branch, selected label, and `Closes` directive before opening it.

Issue, Commit, PR 본문과 리뷰는 한국어를 기본으로 작성한다. Label은 위에 정의한 다섯 개만 생성하고 사용한다.

## Release Convention

Create a `🔧 chore` issue for each release. Branch from `dev` as `chore/#<issue-number>-release-<version>`, validate the release candidate, and open the release PR from that branch to `main`.
