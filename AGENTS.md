# Android Repository Agent Contract

## 1. 적용 범위와 우선순위

이 파일은 저장소 전체의 기본 계약이며, 동일한 내용이 여러 Android 저장소(hopes, BookOn, HeartGuard, moil)에 공통으로 적용된다.
작업 디렉터리와 더 가까운 위치에 `AGENTS.override.md` 또는 `AGENTS.md`가 있으면 더 구체적인 규칙을 우선한다.
이 저장소의 규칙은 `.agents/skills/*/SKILL.md`에 나뉘어 정의되어 있다. 코드 작업, 커밋, 브랜치, PR 등 어떤 작업을 시작하기 전에도 관련 분야의 `SKILL.md`를 먼저 확인하고 그 규칙을 따른다.

작업을 시작하기 전에 다음을 먼저 확인한다.

- 프로젝트 루트와 `docs/`의 `README`, `PRD`(있다면 반드시 읽고 목적·범위·용어를 작업에 반영한다)
- 현재 패키지 구조와 모듈 경계
- 기존 공통 네트워크 결과 타입과 예외 처리 방식
- 현재 직렬화 도구와 Retrofit 설정
- 기존 Hilt Module과 scope
- 프로젝트에서 실제로 사용하는 빌드 및 검증 명령

기존 코드베이스가 이미 같은 책임의 공통 추상화를 제공하면 중복 타입을 만들지 않는다.
이 계약과 다른 구조를 적용해야 하면 코드 수정 전에 이유와 영향 범위를 설명한다.
README와 PRD가 서로 충돌하거나, 이 파일과 `SKILL.md`가 충돌하면 임의로 판단하지 말고 사용자에게 확인한다.

## 2. 기본 기술 계약

아래는 이 저장소들의 공통 기본값이다. 저장소가 실제로 다른 도구를 쓰면(예: DI나 서버 통신이 없는 저장소) 기존 프로젝트 설정을 따르고, 해당하지 않는 항목의 규칙은 적용하지 않는다.

- 언어: Kotlin
- UI: Jetpack Compose + Material 3 (Compose를 쓰는 저장소에 XML Layout을 새로 도입하지 않는다)
- 의존성 주입: Hilt (사용하는 저장소에 한함)
- 상태 관리: ViewModel + StateFlow + 단방향 데이터 흐름
- 화면 구조: Route와 Screen 분리
- 비동기 처리: Kotlin Coroutines + Flow
- 서버 통신: Retrofit + OkHttp (서버 통신이 있는 저장소에 한함)
- 아키텍처: MVVM + Presentation / Domain / Data / Core 계층 분리
- 직렬화: 기존 프로젝트 설정을 따른다. 한 기능 안에서 Gson과 Kotlin Serialization을 임의로 혼용하지 않는다.

## 3. 필수 스킬 라우팅

작업은 하나 이상의 스킬을 함께 사용할 수 있다. 아래에 없는 스킬이 `.agents/skills` 아래에 추가되면 관련 작업 시작 전 디렉터리를 확인하고 따른다.

| 작업 조건 | 반드시 적용할 스킬 |
|---|---|
| 새 Retrofit 서버 기능 추가, 기존 서버 기능의 전체 흐름 수정 | `$android-server-feature` |
| 계층 설계, Repository·UseCase·RemoteDataSource 역할 판단, 구조 리뷰 | `$android-layer-boundaries` |
| Hilt binding, Module, scope, qualifier, 주입 오류 수정 | `$android-hilt-di` |
| Context 저장, Singleton, Listener, Callback, Receiver, Socket, Player, 직접 만든 CoroutineScope 등 장기 수명 작업 | 구현 전에 `$android-memory-safety` |
| Compose 화면, Route, Screen, UiState, Component 구현 (Figma 수치 → 레이아웃 변환 포함) | `$android-compose-ui` |
| 화면 전환, back stack, 하단 탭, 화면 간 상태 공유, 새 목적지 추가 | `$android-navigation` |
| 색상, 테마 모드, 문자열, spacing, radius, elevation 등 디자인 시스템 수정 | `$android-design-system` |
| 다이얼로그·바텀시트 배경 블러 추가·수정 | `$android-dialog-blur` |
| 변수·함수·클래스·파일·패키지·UseCase 이름을 새로 짓거나 검토 | `$android-code-naming` |
| BuildConfig, product flavor, Retrofit, OkHttp, Interceptor, Authenticator, timeout, 서버 주소 수정 | `$android-network-environment` |
| Coroutine 취소, Flow, UiState 오류 상태, 이벤트 효과 처리 | `$android-coroutine-errors` |
| Google·Kakao·Apple 로그인, OAuth/OIDC, PKCE, 토큰 저장, Redirect URI 등 인증 보안 | `$android-oauth-security` |
| FCM 알림·리마인더, Firebase Installation ID(FID) 등록·서버 동기화·수신 처리 | `$android-fcm-fid-notifications` |
| 코드 변경이 끝난 뒤 컴파일·테스트·lint·구조 검증 및 완료 보고 | `$android-change-verification` |
| Issue·브랜치·커밋·PR·라벨·릴리스 등 GitHub 작업 | `$android-github-workflow` (아래 6번 규칙과 함께 적용) |

새 서버 기능은 최소한 다음 순서로 스킬을 적용한다.

1. `$android-memory-safety`로 위험 여부 점검
2. `$android-server-feature`로 전체 기능 구현
3. 필요에 따라 계층·Hilt·UI·네트워크·Coroutine 스킬 병행
4. `$android-change-verification`으로 완료 전 검증

## 4. 항상 지켜야 하는 핵심 규칙

- 새 서버 기능에서는 `UseCase`와 `RemoteDataSource Interface / Impl`을 생략하지 않는다.
- ViewModel은 DTO, Retrofit `Response`, ApiService를 직접 참조하지 않는다.
- RepositoryImpl은 ApiService를 직접 호출하지 않고 RemoteDataSource를 통한다.
- Repository Interface는 Domain Model 또는 프로젝트 공통 Domain 결과 타입만 노출한다.
- (Hilt 사용 시) 직접 생성 가능한 프로젝트 클래스는 `@Inject constructor`를 우선한다.
- (Hilt 사용 시) Interface 연결에는 `@Binds`, 외부 Builder나 직접 생성 규칙이 필요한 타입에는 `@Provides`를 우선한다.
- Application 수명 객체에 Activity, Fragment, View, NavController, Compose 상태 또는 Composable lambda를 저장하지 않는다.
- 화면 최상위는 특별한 이유가 없으면 `Scaffold`를 사용한다.
- 고정 UI 문자열은 `strings.xml`, 색상은 `MaterialTheme.colorScheme`, 반복 수치는 디자인 토큰 또는 의미 있는 상수로 관리한다.
- 같은 범위에서 의미가 겹치거나 모호한 이름(`data`, `value`, `item`, `result`, `temp` 등)을 쓰지 않는다. 상세 규칙은 `$android-code-naming`을 따른다.
- 주요 메소드에는 역할, 호출 시점, 중요한 매개변수, 반환 또는 실패 동작을 한국어 주석으로 설명한다. 코드를 그대로 번역하는 주석은 쓰지 않는다.
- 함수·조건문·`when`·객체 생성·컬렉션 변환을 한 줄로 압축하지 않는다. 짧더라도 제어 흐름 또는 인자가 둘 이상이면 줄바꿈과 들여쓰기로 의도를 드러낸다.
- `CancellationException`을 삼키지 않는다.
- 새 dependency나 라이브러리는 기존 dependency로 해결할 수 없을 때만 추가하며, 추가 전에 이유·유지보수 상태·호환성·라이선스·보안·앱 크기 영향을 설명하고 사용자 확인을 받는다.
- 요청하지 않은 기존 기능을 임의로 삭제하거나 변경하지 않는다. 현재 요청 범위를 벗어나는 문제를 발견하면 먼저 사용자에게 알린다.
- 스킬에 없는 새 아키텍처·라이브러리·패턴, 인증·토큰·개인정보·암호화·네트워크 보안, 성능에 영향을 주는 변경은 구현 전에 현재 상황, 필요한 변경, 이유, 영향 범위를 설명하고 사용자 확인을 받는다.
- 코드 규모가 커져 단일 모듈이 유지보수에 불리해지면 멀티모듈 전환을 제안하되, 사용자 허락 없이 구조를 바꾸지 않는다.
- 모르는 내용은 추측하지 않고 모른다고 말한다. 요구사항이 애매하면 임의로 가정하지 않고 사용자에게 확인한다.
- 검증하지 않은 항목을 검증했다고 보고하지 않는다.

## 5. 기본 작업 방식

1. 관련 파일과 기존 패턴을 조사한다. 코드베이스를 충분히 이해하지 못했다면 추측하지 말고 사용자에게 질문한다.
2. README·PRD와 수명, 보안, 호환성, 마이그레이션 위험을 먼저 확인한다.
3. 필요한 스킬을 선택하고 그 절차를 따른다.
4. 실제 코드·리소스·구조를 변경하기 전에 구현 계획과 검증 순서를 작성한다(5.1 참고).
5. 기존 구조를 존중하면서 책임이 분리된 최소 변경을 구현한다.
6. 변경 후 스스로 다시 검토하고(요구사항 충족, 계층 경계, 중복·불필요 코드, 기존 기능 훼손, 보안·성능, nullable·에러 처리, import·빌드 오류 가능성), 폴더·파일 구조를 바꿨다면 삭제·이동한 파일을 참조하는 코드가 남지 않았는지도 확인한다.
7. 가능한 검증 명령을 실행한다. (`$android-change-verification`)
8. 완료 보고에는 변경 파일, 데이터 흐름, Hilt binding(해당 시), 메모리 위험, BuildConfig 변경(해당 시), 검증 결과를 포함한다.

### 5.1 작업 전 계획 수립

- 계획에는 변경 대상, 기존 동작 보호 사항, 구현 순서, 실패 가능성, 검증 명령을 포함한다.
- 여러 파일·계층에 걸치는 작업은 `work_planner` 에이전트가 있으면 read-only로 실행해 계획을 받고, 그 결과를 반영한 뒤에만 파일을 수정한다. 에이전트를 쓸 수 없으면 메인 작업자가 같은 항목으로 계획을 작성한다.
- 단순 질의, 문서 확인, 상태 조회, 한두 줄의 국소 수정에는 별도 계획 에이전트를 실행하지 않는다.

### 5.2 README·PRD

- README와 PRD가 있으면 작업 전에 읽고 참고한다.
- README가 없거나, 제목 한 줄 수준이거나, 실제 구조와 어긋나 있으면 `readme_writer`(Claude에서는 `readme-writer`) 에이전트가 있을 때 먼저 작성·갱신한 뒤 본 작업을 진행한다. 단순 질의나 단일 파일 변경에서는 생략한다.
- PRD가 없으면 임의로 만들지 않고 사용자에게 확인한다. README에는 저장소에서 확인한 사실만 쓰고, 확인하지 못한 항목은 `확인 필요`로 표시하며, API Key·토큰 같은 비밀 값은 쓰지 않는다.

## 6. GitHub Issue·브랜치·Pull Request 게시 규칙

코드 작업을 시작하거나 GitHub 게시를 요청받으면 먼저 열려 있는 Issue와 Pull Request를 확인한다. 새 Issue는 독립적인 새 작업일 때만 생성하고, 이미 열려 있는 Issue의 범위에 속하는 작업에는 새 Issue를 만들지 않는다.

### Issue 선택과 작성

- 같은 기능·화면·사용자 흐름·PR에 포함되는 세부 작업, 또는 기존 체크리스트에 추가할 수 있는 작은 작업은 기존 Issue를 사용한다.
- 독립적으로 완료하거나 별도 PR·담당자·일정이 필요한 기능 또는 버그, 기존 Issue 범위를 명확히 벗어난 작업만 새 Issue로 만든다.
- 큰 기능 안에서 별도 진행 관리가 필요하면 부모 Issue의 Sub-issue로 만든다.
- Issue 제목은 `[Feature]`, `[Fix]`, `[Refactor]` 접두사를 사용하고, 본문에는 `목적`, 체크리스트 형식의 `작업 내용`, `완료 조건`을 작성한다.

### 브랜치와 커밋

- Issue 번호를 포함해 `feature/#<issue-number>-<short-description>`, `fix/#<issue-number>-<short-description>`, `refactor/#<issue-number>-<short-description>` 형식으로 최신 기본 브랜치에서 생성한다.
- 커밋 메시지는 `<type>: [#<issue-number>] <작업 요약>` 형식을 사용한다. type은 `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `style` 중 하나를 사용한다.
- 커밋은 하나의 논리적 변경만 포함하고 Issue 번호를 참조만 한다. `Closes`, `Fixes`, `Resolves`는 커밋 메시지에 사용하지 않는다.

### Pull Request와 Issue 종료

- PR 제목은 `<type>: <전체 작업 요약>`으로 작성하며, 본문에는 작업 내용, 주요 변경 사항, 테스트 결과, 관련 Issue를 포함한다.
- Issue 전체를 완료한 PR 본문에만 `Closes #<issue-number>`를 `관련 Issue` 영역에 작성한다. 일부 작업만 완료했다면 `Related to #<issue-number>` 또는 `Refs #<issue-number>`를 사용한다.
- 여러 Issue를 모두 완료했으면 각 줄에 `Closes #<issue-number>`를 각각 작성한다.
- Issue는 PR 병합 전 수동으로 닫지 않는다. 기본 브랜치로 병합된 뒤 자동 종료 여부를 확인한다.
- 사용자의 명시적 요청 없이 PR을 병합하지 않는다.

### 금지 및 보고

- 파일·함수·Composable 단위의 Issue 생성, 같은 기능의 중복 Issue, 설명 없는 Issue, 서로 무관한 작업을 하나의 PR에 넣는 것을 금지한다.
- GitHub에 게시한 뒤 Issue, 브랜치, 커밋, PR, Issue 연결 방식, 병합 상태를 보고한다. 기존 Issue를 썼다면 새 Issue를 만들지 않은 이유도 한 줄로 밝힌다.

## 7. 에이전트 파일·스킬 파일 보호와 서브에이전트

- `AGENTS.md`와 `.agents/skills/**/SKILL.md`는 읽기 전용 규칙 파일로 취급한다. 수정·삭제·생성·`.agents/skills` 구조 변경은 먼저 이유를 설명하고 사용자의 명시적 허락을 받은 뒤에만 한다.
- 이 파일은 여러 저장소에 동일하게 배포되므로, 한 저장소에서만 필요한 규칙은 이 파일이 아니라 그 저장소의 `AGENTS.override.md`에 둔다.
- 서브에이전트(`codebase_analyzer`, `design_analyzer`, `work_planner`, `readme_writer`, `github_workflow` 등)는 사용할 수 있을 때, 작업의 복잡도와 정확성에 실질적인 도움이 될 때만 활용한다. 결과를 받은 뒤 최종 판단은 메인 작업자가 한다.
- GitHub 게시(Issue·브랜치·커밋·Push·PR·라벨·병합·릴리스)에 `github_workflow` 에이전트가 있으면 그 에이전트에 위임하고, 위 6번 규칙과 `$android-github-workflow`를 적용한다.
