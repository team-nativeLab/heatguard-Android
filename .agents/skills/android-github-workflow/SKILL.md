---
name: android-github-workflow
description: Manage Android project GitHub work using the team's issue-first workflow. Use when creating or updating GitHub issues, issue-linked branches, commits, pull requests, labels, or release pull requests for this repository. Also use at the start of every new code-change task (even when the user did not ask for GitHub work) and whenever several tasks run at the same time, to check uncommitted changes, pick or propose an issue, and isolate work per issue.
---

# Android GitHub Workflow

## Overview

Follow the Notion Android convention: create an issue before work, include the issue number in the branch, commit, and PR, and merge implementation PRs into `dev`.

이 Skill보다 저장소의 `AGENTS.md` 규칙을 우선한다. 두 문서가 충돌하면 임의로 선택하지 않고 `AGENTS.md` 형식으로 정렬한 뒤, 필요한 규칙 파일 수정 권한을 사용자에게 확인한다.

## 기본 작업 브랜치 및 저장소 기본 브랜치

- 이 저장소의 기본 작업·통합 브랜치는 `dev`다. Issue 작업 브랜치는 `dev`에서 만들고 구현 PR은 `dev`를 대상으로 한다.
- GitHub 저장소 기본 브랜치도 `dev`로 설정하는 것을 원칙으로 한다. `main`은 `dev` 통합 이후 릴리스 PR의 대상으로 유지하며, 일반 작업 브랜치나 구현 PR의 기본값으로 사용하지 않는다.
- GitHub 기본 브랜치가 `dev`가 아니면 현재 설정을 보고하고 사용자 명시 승인을 받은 뒤에만 `dev`로 변경한다. 변경 전후 기본 PR base, branch protection, 필수 체크, Actions 흐름을 확인한다. 권한이 없어 확인할 수 없는 항목은 확인 불가로 보고한다.

## 작업 시작 점검 (Pre-work Check)

사용자가 GitHub 작업을 따로 지시하지 않았더라도, 코드 변경 작업을 새로 시작할 때(한 번에 여러 작업을 요청받은 경우 포함) 파일을 수정하기 전에 반드시 아래 순서로 점검한다.

1. **현재 상태 확인**: `git status`, 현재 branch, `git worktree list`, 열려 있는 Issue와 PR을 확인한다.
2. **미커밋 변경 처리**: 미커밋 변경이 있으면 새 작업을 시작하지 않는다. 변경 파일 목록과 각 변경이 어느 작업·Issue에 속하는 것으로 보이는지 정리해 보고하고, 처리 방법(해당 Issue branch에 커밋 등)을 사용자에게 확인받는다. 임의로 삭제·`stash`·덮어쓰기·다른 branch로 옮기지 않는다.
3. **Issue 선택**: 재사용할 수 있는 기존 Issue 후보(번호·제목·재사용 이유)를 먼저 제시한다. 적합한 Issue가 없을 때만 새 Issue 초안(제목·label·본문)을 제시하고, **사용자 승인 후에만 생성**한다.
4. **Branch 준비**: 사용할 branch 이름(기존 branch 재사용 또는 새 branch)을 제시하고, **사용자 승인 후에만 생성·전환**한다.

Issue 생성, branch 생성, 커밋은 각각 별도의 단계로 구분해 단계마다 사용자에게 묻는다. 한 번의 승인으로 여러 단계를 묶어 진행하거나, 스스로 판단해 자동으로 진행하지 않는다.

## 커밋·Push·PR 확인 규칙

- 논리 단위 작업이 끝나면 변경 파일 목록과 커밋 메시지 초안(`<type>: [#<issue-number>] <작업 요약>`)을 제시하고, **사용자 승인 후에만 커밋**한다.
- 다음 작업으로 넘어가기 전에 이전 작업의 미커밋 변경이 남아 있으면 커밋 여부를 먼저 묻는다. 미커밋 변경을 남긴 채 다른 Issue 작업을 시작하지 않는다.
- Push와 Draft PR 생성은 자동으로 하지 않는다.
- 단, 다음 작업이 이전 branch의 변경에 의존하거나, 같은 파일을 수정하거나, 이전 branch가 Push·PR 없이 남아 충돌·누락 위험이 있으면 **다음 작업을 시작하기 전에** Push와 Draft PR 생성 여부를 사용자에게 묻는다.

## 동시 작업 (git worktree)

- 여러 작업을 동시에 진행할 때는 Issue마다 별도의 `git worktree`에서 작업한다. 예: `git worktree add ../<repository-name>-worktrees/<issue-number>-<summary> -b feature/#<issue-number>-<summary> dev` (branch 생성은 위 승인 절차를 따른다)
- 하나의 worktree에는 하나의 Issue·하나의 branch만 둔다. 다른 Issue의 변경을 섞지 않는다.
- 서브에이전트로 병렬 작업할 때도 Issue별 worktree로 격리한다.
- 진행 상황과 완료 보고에는 어느 worktree·branch에서 한 작업인지 명시한다.
- worktree 삭제(`git worktree remove`)는 해당 PR이 병합된 뒤 사용자 확인을 받고 진행한다.

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

1. Run the `작업 시작 점검 (Pre-work Check)` above first. Confirm the repository, issue scope, and whether the change is a feature, fix, refactor, chore, or hotfix.
2. Create or select the GitHub issue using the required template, only after the user approves. Apply exactly one applicable label.
3. After the user approves the branch name, create the work branch from `dev` as `feature/#<issue-number>-<kebab-case-summary>`, `fix/#<issue-number>-<kebab-case-summary>`, or `refactor/#<issue-number>-<kebab-case-summary>`.
4. Make and validate the Android change. Run relevant Gradle build, unit-test, lint, and UI/Preview checks.
5. After the user approves, commit as `<type>: [#<issue-number>] <작업 요약>`.
6. When the user requests it (or approves after the risk check in `커밋·Push·PR 확인 규칙`), push the branch and open a draft PR targeting `dev` with the required title and body.
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
