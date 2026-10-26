# 작업 인계: Figma 미구현 화면

## 2026-10-27: 체크리스트 서버 연동 및 화면 정리 (#173, #174)

- 체크리스트 작업은 `feature/#173-checklist-progress-and-round-status`, 화면 정리는 `fix/#174-profile-inquiry-site-photo-ui`에서 최신 `dev` (`d007c916`) 기준으로 분리했다. 기존 CI workflow 변경이 있는 기본 체크아웃에는 손대지 않았다.
- Draft PR #175는 체크리스트 서버 항목 완료 수/전체 수로 진행률을 계산하고, 일정 기준 회차 완료 수와 분리한다. 현재 회차만 상세로 진입할 수 있고 홈 일정 흐름은 유지한다. 체크리스트 API는 날짜 단위라 당일 회차끼리 항목 상태를 공유한다.
- Draft PR #176은 연락처 시트의 navigation bar inset/스크롤, 전화 취소 버튼·표시 포맷, 사진 문구, 현장 제목을 정리했다. 010으로 시작하는 11자리 번호만 표시 포맷하며 발신에는 서버 원본 번호를 사용한다. 프로필·문의 입력 배경은 기존 공용 토큰 적용을 유지했다.
- 로컬 검증: 두 작업 모두 `:app:compileDebugKotlin`, `:app:assembleDebug`, `:app:lintDebug`, `spotlessCheck`, `git diff --check` 통과. 테스트 소스가 없어 `:app:testDebugUnitTest`는 `NO-SOURCE`다. 연결된 Android 기기/에뮬레이터가 없어 작은 화면·큰 글꼴에서 시트와 UI를 실기기 검수하지 못했다.
- GitHub CI: PR #175의 Format Check, PR Rules, Secret Scan, CI Build & Lint 성공. PR #176의 같은 네 체크도 성공했다. 두 PR은 `dev` 대상 Draft이며 미병합이고, 연결 이슈 #173·#174는 PR 병합 전 상태로 열려 있다.
- 다음 작업은 PR diff와 사용자 리뷰 확인이다. 사용자가 병합을 요청하기 전에는 두 PR을 병합하지 않는다. UI 런타임 검수는 기기나 에뮬레이터를 사용할 수 있을 때 수행한다.

## 2026-10-26: 멀티모듈화 및 템플릿 테스트 정리 (#152)

- 작업 브랜치: `refactor/#152-modularize-and-prune-tests`, 기준 dev `13cc8074c33de5b3bdf8314b051584e5de17448f`.
- 사용자 승인 범위는 기능 보존 멀티모듈화, 불필요 테스트 제거, 보안 검토, dev 반영 후 main 병합이다.
- app은 화면/Navigation/리소스, core는 network/session/di, domain은 모델/계약/UseCase, data는 DTO/DataSource/Repository/Hilt binding을 소유한다. app core/util/component는 유지했다.
- 템플릿 산술·packageName 테스트 2개만 삭제했다. 실제 회귀 테스트를 모듈별로 옮기고 core testFixtures로 테스트 도우미를 공유한다.
- data에서 기존 BASE_URL 생성/검증을 단일 소유한다. 값·저장 키·release HTTPS 검증 및 앱 Manifest/보안 XML은 보존했다.
- 전체 단위 테스트 348개, assembleDebug, lintDebug(오류0/app경고59), Spotless, app/data 계측 APK 빌드 통과. 계측 최종 상태는 아래 검증 문서에 기록한다.
- 사후 코드 리뷰 및 최종 보안 diff CLEAN. 기존 비활성 Claude autofix CI의 쓰기 토큰 위험은 활성화 전에 수정해야 한다. dev/main 보호 규칙도 없는 상태다.
- [상세 검증](verification/2026-10-26-modularization.md)을 우선 참고한다. 실서버 기능과 최신 CVE/GHSA는 이번에 재검증하지 않았다.
- 게시 담당은 #152 연결 PR의 최신 CI/충돌 여부를 확인해 dev→main 순으로 병합한다. main 통합에는 기존 dev 235커밋도 포함되며 사용자에게 알렸다.


## 목표와 범위

Figma 파일 `이음산업건설`의 체크리스트 화면과 기록 취소 확인창을 구현했다. 서버 연동은 범위 밖이며, 체크리스트는 데모 상태를 로컬에서만 변경한다.

## 구현 내용

- 홈의 체크리스트 요약 영역에서 체크리스트 화면을 연다.
- 새 진입은 3/6으로 시작하고, 여섯 항목을 행 전체에서 토글한다. 회전 중에는 상태를 유지하고 홈으로 나갔다가 다시 들어오면 3/6으로 초기화한다.
- 모든 항목을 확인하면 6/6 완료 스타일과 활성 완료 버튼을 보여주고, 완료 버튼은 홈으로 이동한다.
- 기록 편집 화면에서 뒤로가기로 기록 흐름 전체를 나갈 때 확인 다이얼로그를 표시한다. 계속 작성·홈으로 나가기, 바깥 탭/뒤로가기 dismiss, 저장 중 홈 나가기 비활성 동작을 지원한다.
- 다이얼로그 딤은 해당 Window에 0.4를 적용하고 닫힐 때 기존 Window dim 설정을 복원한다.
- 좁은 화면에서 다이얼로그 좌우 20dp 여백을 보장한다. 표준 폭에서는 최대 322dp를 유지한다.
- 서버/API/새 dependency는 추가하지 않았다.

## 주요 파일

- 체크리스트: `app/src/main/java/com/nativelap/heartguard/view/screen/checklist/ChecklistScreen.kt`, `view/route/checklist/ChecklistRoute.kt`, `viewmodel/checklist/ChecklistViewModel.kt`, `view/component/checklist/`
- 기록 취소 확인: `view/route/record/RecordCancelConfirmRoute.kt`, `view/component/record/RecordCancelConfirmDialogContent.kt`
- 연결·스타일: `navigation/HeartGuardNavHost.kt`, `navigation/HeartGuardDestination.kt`, `view/screen/home/HomeScreen.kt`, `view/route/home/HomeRoute.kt`, `view/component/home/HomeCheckTimeline.kt`, `ui/theme/Dimension.kt`, `ui/theme/Theme.kt`, `app/src/main/res/values/strings.xml`

## 검증

- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` 성공.
- 최종 `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` 성공.
- 에뮬레이터에서 1080×2400 기본 화면의 3/6·6/6·취소 다이얼로그를 확인했다. 6/6 완료 버튼이 홈으로 이동하고 재진입 시 3/6으로 초기화되는 것을 확인했다.
- 320×640dp, 글자 크기 200%에서 체크리스트 본문 스크롤과 고정 CTA, 기록 취소 다이얼로그 텍스트·버튼 배치를 확인했다.
- 바깥 탭으로 다이얼로그를 닫은 뒤 배경 밝기가 복원되는 것을 확인했다.
- UIAutomator 접근성 트리에서 홈 체크 카드가 `Button` 하나로 노출되고, 이름이 제목과 다음 점검 요약으로 제한되는 것을 확인했다. 실제 TalkBack 발화는 확인하지 않았다.
- 사후 코드 리뷰: Critical/Blocker 없음. 행/칩 의미론 중복과 긴 홈 버튼 이름 우려를 수정했다. 새 화면 전용 회귀 테스트는 추가하지 않았다.
- 기본 체크 완료 글자색 `#9CA3AF`는 Figma 값을 유지했다. 대비율이 낮다는 리뷰 우려는 비차단 디자인 제한으로 보고한다.
- 시각 리뷰에서 기존 현장 사진 화면 상단이 Figma 캡처보다 위로 보이는 차이가 확인됐다. 캡처 뷰포트 높이가 달라 정확한 정규화 비교가 필요하며, 해당 기존 화면 레이아웃은 이번 누락 화면 작업 범위에서 변경하지 않았다. 취소 다이얼로그 자체와 체크리스트는 시각 QA 통과했다.

## 최신 작업 상태 (2026-10-25)

- 최신 `dev`는 `df697c56773b8c40e4c5dae20d81ff6d684aaa5c`이며 PR #148까지 병합됐다. 체크리스트 데모는 제거됐고 팀 체크리스트 서버 화면이 연결돼 있다.
- PR #139, #140, #141, #144, #145도 `dev`에 병합됐다. 현장 선택 UI 연결, 앱 계정 현장 데이터의 세션 세대 보호, 로그인 화면 계정 선택 제거가 이미 반영돼 있다.
- 이번 후속 변경은 로그인·회원가입·현장 연결 기능을 Team 세션과 Team API 계약으로 통일하고, 기존 `APP_ACCOUNT` 세션은 권한을 바꾸지 않고 만료시켜 재로그인하도록 한다. 현장 범위 API는 전송 시점에 최신 선택 현장 ID를 읽으며 인증 세션 태그가 없거나 바뀌면 요청을 차단한다.
- 가입·이메일 인증·현장 연결 Team 경로는 서버가 구현해야 할 기대 계약이다. 서버 구현 및 실환경 응답은 아직 검증되지 않았다.
- Team 현장 관리 진입에서 연결 목록·선택·해제·대시보드 UI를 유지한다.
- 후속 변경의 compile, 전체 unit test, assemble, lint, Spotless 검사가 모두 통과했다.
- Issue #138을 계속 사용한다. 게시 전 최신 GitHub 체크와 리뷰를 확인하고, 병합 후 `dev` SHA와 현재 Android Studio checkout을 동기화한다.

## 현재 저장소 상태와 이어서 할 일

- 작업 브랜치: `fix/#58-figma-review-and-hardcoded-values`
- 구현은 커밋 `2feffb1` (`feat: [#58] Figma 체크리스트와 기록 취소 확인 화면 구현`)으로 게시했다.
- 기존 Issue #58을 사용했고, 새 이슈는 만들지 않았다.
- Draft PR [#59](https://github.com/team-nativeLab/heatguard-Android/pull/59)는 `dev` 대상이며 OPEN 상태다. 병합하지 않았다.
- PR API는 `DIRTY` / `CONFLICTING`을 반환했지만 `git merge-tree`에는 파일 충돌이 없었다. 두 상태가 일치하지 않아 추가 확인이 필요하다. 새 head에는 GitHub 체크가 아직 보고되지 않았고, `sync-check.sh`의 저장소 조회는 `UNKNOWN`이었다. dev 동기화·충돌 해결·병합은 보류했다.
- 코드 사후 리뷰와 Figma 대조를 마쳤다. 코드 리뷰상 Critical/Blocker는 없다.
- 다음 제품 단계는 서버 연동이다. 체크리스트 항목, 초기 완료 수, 서버 저장 정책은 API/제품 결정에 맞춰 로컬 데모 모델을 교체한다.
# 작업 인계: 테스트 전면 제거 및 멀티모듈 확인

## 2026-10-26: 네 모듈 테스트 소스 전면 제거 (#158)

- 작업 브랜치: `refactor/#158-remove-all-tests`, 기준 dev `3149907dd018f98ded429f7c3e050b5a3ef719f7`.
- `:app`, `:core`, `:domain`, `:data` 모듈과 `src/main` 운영 코드는 유지했다. 저장소에는 66개 `src/test`, `src/androidTest`, `src/testFixtures` 소스를 더 이상 두지 않는다.
- 네 모듈의 test runner, test/testFixtures 의존성 및 설정과 테스트 전용 version catalog 항목을 제거했다. 디버그 Compose tooling과 운영 의존성은 유지했다.
- CI에서 테스트 태스크·보고서·테스트 성공 문구를 없애고 `CI Build & Lint`로 이름을 바꿨다. Claude/Codex `workflow_run` 트리거도 새 이름에 맞췄다. Issue #157의 auto-fix 쓰기 토큰·runner 격리는 별도 이슈로 남아 있다.
- README는 현재 테스트가 없고 과거 테스트 결과는 재실행할 수 없는 이력임을 명시한다. 당시 테스트가 보호하던 인증·세션·권한 분리·기록 제출 안전성을 현재 CI가 자동 확인하지 못한다.
- 보안 diff 검토: Critical 없음. 테스트 삭제로 관련 보안 회귀 탐지가 사라지는 Warning과, CI 상태 이름 변경 후 필수 상태 검사를 확인해야 한다는 제안이 있다. 이는 테스트 제거 요청에 따른 검증 범위 축소이며 현재 운영 코드 변경으로 인한 취약점은 확인되지 않았다.
- Gradle 프로젝트 목록에서 네 모듈을 확인했다. `:app:compileDebugKotlin`, `:app:assembleDebug`, `lintDebug`, `spotlessCheck`가 통과했고 debug/release runtime 의존성에 JUnit·Espresso·MockWebServer·Compose UI Test가 남지 않았다. 단위·계측 테스트는 제거 요청 때문에 실행하지 않았다.
- 사후 코드 리뷰 판정은 CONCERNS이며 Critical/Blocker는 없다. 테스트 제거에 따른 인증·세션·기록 회귀 검증 공백, CI 체크 이름 변경 확인, 별도 이슈 #157의 기존 autofix 토큰 격리 위험을 보고했다.
- `dev`와 `main`에는 branch protection/ruleset과 필수 상태 체크가 없다(읽기 전용 GitHub API 조회). 따라서 현재 설정에서는 이전 또는 새 CI 체크 이름이 필수로 요구되지 않는다.
- 변경은 아직 commit/PR/push 되지 않았다. 게시 단계 승인을 받은 뒤 `dev` PR부터 진행하고, 체크·보안 경고를 검토한 다음 사용자가 요청한 `main` 통합을 이어간다. #157 보안 격리는 별도 브랜치에서 이어서 구현하던 중 자동 검토가 보안 민감 workflow 수정을 차단해 멈췄다. #157 브랜치에는 CI workflow 이름을 갱신한 두 줄만 있으며, 나머지 격리 구현은 자동 검토 승인 후 계속한다.

# 이전 작업 인계: Figma 미구현 화면
