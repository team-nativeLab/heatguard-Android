# 2026-10-26 멀티모듈화 및 테스트 정리 검증

## 범위와 근거

- 작업: Issue #152, `refactor/#152-modularize-and-prune-tests`.
- 기준 dev: `13cc8074c33de5b3bdf8314b051584e5de17448f`.
- 사용자가 기능 보존, 멀티모듈화, 불필요 테스트 제거 및 dev 이후 main 병합을 요청했다.
- README와 실제 소스·Gradle 설정을 기준으로 조사했다. PRD는 기존 합의대로 생성하지 않았다.

## 변경과 기능 보존

- `:core`: 기존 network/session/di 30개 Kotlin 파일. UI component/util은 app에 유지했다.
- `:domain`: 기존 모델·Repository 계약·UseCase 79개. 기존 Uri 의존성 때문에 Android library로 구성했다.
- `:data`: DTO·Mapper·DataSource·Repository 구현·Hilt binding 106개.
- `:app`: UI·ViewModel·Navigation·리소스와 앱 진입점을 유지한다.
- 의존 방향은 app → core/domain/data, data → core/domain, domain → core다.
- 패키지, API 계약, 직렬화 이름, Hilt scope/qualifier를 유지했다. 사용자 이벤트 → ViewModel → UseCase → Repository → DataSource → API 흐름은 동일하다.
- BASE_URL 생성과 release HTTPS 검증 블록은 app에서 data로 그대로 이동했다. app release task도 data 검증에 의존한다. 속성명·debug 기본 주소·release 값과 미확정 기록 저장 키를 보존한다.
- 모듈 경계를 넘는 스마트 캐스트 두 곳은 지역 val로 고정했다. 메모 표시 조건과 체크리스트 오류 처리 조건은 동일하다.
- Manifest, FileProvider 경로, 백업 제외 설정, 앱 ID 및 리소스는 원본과 동일하다. 장기 수명 객체나 callback을 추가하지 않았다.
- 원본 src 파일 553개를 해시로 비교했다. 템플릿 2개 외 누락·추가 파일이 없으며, 변경된 Kotlin 본문은 data BuildConfig import 12곳과 스마트 캐스트 2곳이다. 보안 검토자도 이동 운영 파일 215개와 보안 설정을 독립 비교했다.

## 테스트 정리

- 제거: `ExampleUnitTest`의 기본 산술 확인, `ExampleInstrumentedTest`의 기본 packageName 확인.
- 보존: 인증·세션 세대·계정 전환·재전송 차단·미확정 제출·서버 응답·ViewModel·반응형 UI 테스트.
- 테스트는 대상 모듈로 이동했다. RecordSingleAttemptTest는 data API까지 검증하므로 data 모듈에 둔다.
- TestSessionManager는 core testFixtures에 두어 운영 APK에 포함하지 않는다.
- CI unit/lint/report와 Spotless 대상은 네 모듈로 확장했다.

## 실행 결과

- PASS: `./gradlew :app:compileDebugKotlin`.
- PASS: core/domain/data unit test 및 app ChecklistViewModel 대상 테스트.
- PASS: `./gradlew spotlessApply`.
- PASS: `./gradlew :app:assembleDebug testDebugUnitTest lintDebug spotlessCheck :app:assembleDebugAndroidTest :data:assembleDebugAndroidTest`.
- 단위 테스트 XML 독립 집계: core 53, domain 18, data 160, app 117 — 합계 348, 실패·오류·skip 0.
- Lint XML 집계: 오류 0, app Warning 59, core/domain/data Warning 0. 경고 전체를 이번 작업에서 해소하지 않았다.
- 계측: API 34 bookOn AVD에서 app 43개 중 최초 41 PASS/2 FAIL. 긴급전화 `systemBackFromConfirmationReturnsToHeadquartersContact`는 개별 재실행 PASS(최초 창 focus 대기 실패). 로그인 `expandedFontAndPasswordErrorRemainReachableByScrolling`는 IME 표시 5초 대기로 재실패했다.
- 독립 기준 비교: 변경 전 dev HEAD를 `/tmp/heartguard-modularization-baseline`으로 tracked build 소스만 export하고 동일 AVD에서 동일 로그인 테스트를 실행했다. 같은 파일 116행/동일 5초 IME 대기로 실패했다. 이번 모듈화로 새로 생긴 실패는 아니며 기존 미해결 테스트로 보존한다. 앱 UI 전체 통과로 보고하지 않는다.
- data 계측 5개 PASS: 기록 영속화·손상·동시성 검증. 최초 runner 누락은 app이 쓰던 기존 Espresso 테스트 의존성을 data에도 연결해 해결했다(운영 의존성 추가 없음).
- 서로 다른 계측 테스트는 재실행 포함 48개 중 47 PASS, 원본 dev에서도 재현되는 로그인 IME 테스트 1 FAIL. 실기기·실서버 검증으로 확대 해석하지 않는다.
- 최초 compile은 cross-module 스마트 캐스트 2곳, 최초 core 테스트 컴파일은 data API 통합 테스트 위치로 실패했다. 해당 원인을 수정한 뒤 위 검증이 통과했다.

## 보안 검토

- 최종 모듈화 diff 판정 CLEAN: 신규 Critical/Warning 및 기존 보안 설정 누락을 발견하지 못했다.
- Keystore/AES-GCM, 세션 세대, 인증·비인증·외부 업로드 클라이언트 분리, App Link 토큰 메모리 보관, 백업 제외, release HTTPS 검증을 보존했다.
- 기존 조건부 위험: `ci-autofix-claude.yml`은 PR의 Gradle 코드를 job 범위 쓰기 토큰과 함께 실행한다. repository Actions 변수 목록이 비어 있어 현재 AUTOFIX_PROVIDER 조건은 비활성이다. 환경 목록도 비어 있다. 활성화 전에 읽기 빌드와 쓰기 게시를 분리해야 한다.
- 기존 운영 설정: dev/main branch protection, branch rules, rulesets가 없다. 이번 변경으로 생성된 문제는 아니다.
- 기존 공급망 개선점: 이동 가능한 Action 태그, gitleaks 다운로드 checksum 검증 부재.
- 한계: 최신 CVE/GHSA·전이 의존성 경보, 운영 서버 인가·토큰 폐기, 실제 release 배포 산출물은 검증하지 않았다. 실서버 전체 기능 검증도 수행하지 않았다.

## 병합 인계

- main 기준 `637c6be1d7cfb937d252b70240d64c7bcda4688b`는 dev의 조상이며 기존 dev 235커밋도 main 통합 범위에 포함된다.
- 사후 코드 리뷰와 최종 보안 diff는 CLEAN이다. GitHub 담당이 최신 head CI와 충돌 상태를 확인하고 승인된 dev, main 순서로 병합한다. 계측의 기존 실패 1개는 위와 같이 공개한다.
