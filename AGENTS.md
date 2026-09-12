# Android Repository Agent Contract

## 1. 적용 범위와 우선순위

이 파일은 저장소 전체의 기본 계약이다.
작업 디렉터리와 더 가까운 위치에 `AGENTS.override.md` 또는 `AGENTS.md`가 있으면 더 구체적인 규칙을 우선한다.

작업을 시작하기 전에 다음을 먼저 확인한다.

- 현재 패키지 구조와 모듈 경계
- 기존 공통 네트워크 결과 타입과 예외 처리 방식
- 현재 직렬화 도구와 Retrofit 설정
- 기존 Hilt Module과 scope
- 프로젝트에서 실제로 사용하는 빌드 및 검증 명령

기존 코드베이스가 이미 같은 책임의 공통 추상화를 제공하면 중복 타입을 만들지 않는다.
이 계약과 다른 구조를 적용해야 하면 코드 수정 전에 이유와 영향 범위를 설명한다.

## 2. 기본 기술 계약

- 언어: Kotlin
- UI: Jetpack Compose + Material 3
- 의존성 주입: Hilt
- 상태 관리: ViewModel + StateFlow + 단방향 데이터 흐름
- 화면 구조: Route와 Screen 분리
- 비동기 처리: Kotlin Coroutines + Flow
- 서버 통신: Retrofit + OkHttp
- 아키텍처: MVVM + Presentation / Domain / Data / Core 계층 분리
- 직렬화: 기존 프로젝트 설정을 따른다. 한 기능 안에서 Gson과 Kotlin Serialization을 임의로 혼용하지 않는다.

## 3. 필수 스킬 라우팅

작업은 하나 이상의 스킬을 함께 사용할 수 있다.

| 작업 조건 | 반드시 적용할 스킬 |
|---|---|
| 새 Retrofit 서버 기능 추가, 기존 서버 기능의 전체 흐름 수정 | `$android-server-feature` |
| 계층 설계, Repository·UseCase·RemoteDataSource 역할 판단, 구조 리뷰 | `$android-layer-boundaries` |
| Hilt binding, Module, scope, qualifier, 주입 오류 수정 | `$android-hilt-di` |
| Context 저장, Singleton, Listener, Callback, Receiver, Socket, Player, 직접 만든 CoroutineScope 등 장기 수명 작업 | 구현 전에 `$android-memory-safety` |
| Compose 화면, Route, Screen, UiState, Component 구현 | `$android-compose-ui` |
| 색상, 테마 모드, 문자열, spacing, radius, elevation 등 디자인 시스템 수정 | `$android-design-system` |
| BuildConfig, product flavor, Retrofit, OkHttp, Interceptor, Authenticator, timeout, 서버 주소 수정 | `$android-network-environment` |
| Coroutine 취소, Flow, UiState 오류 상태, 이벤트 효과 처리 | `$android-coroutine-errors` |
| 코드 변경이 끝난 뒤 컴파일·테스트·lint·구조 검증 및 완료 보고 | `$android-change-verification` |

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
- 직접 생성 가능한 프로젝트 클래스는 `@Inject constructor`를 우선한다.
- Interface 연결에는 `@Binds`, 외부 Builder나 직접 생성 규칙이 필요한 타입에는 `@Provides`를 우선한다.
- Application 수명 객체에 Activity, Fragment, View, NavController, Compose 상태 또는 Composable lambda를 저장하지 않는다.
- 화면 최상위는 특별한 이유가 없으면 `Scaffold`를 사용한다.
- 고정 UI 문자열은 `strings.xml`, 색상은 `MaterialTheme.colorScheme`, 반복 수치는 디자인 토큰 또는 의미 있는 상수로 관리한다.
- 주요 메소드에는 역할, 호출 시점, 중요한 매개변수, 반환 또는 실패 동작을 한국어 주석으로 설명한다.
- 함수·조건문·`when`·객체 생성·컬렉션 변환을 한 줄로 압축하지 않는다. 짧더라도 제어 흐름 또는 인자가 둘 이상이면 줄바꿈과 들여쓰기로 의도를 드러낸다.
- `CancellationException`을 삼키지 않는다.
- 검증하지 않은 항목을 검증했다고 보고하지 않는다.

## 5. 기본 작업 방식

1. 관련 파일과 기존 패턴을 조사한다.
2. 수명, 보안, 호환성, 마이그레이션 위험을 먼저 확인한다.
3. 필요한 스킬을 선택하고 그 절차를 따른다.
4. 기존 구조를 존중하면서 책임이 분리된 최소 변경을 구현한다.
5. 가능한 검증 명령을 실행한다.
6. 완료 보고에는 변경 파일, 데이터 흐름, Hilt binding, 메모리 위험, BuildConfig 변경, 검증 결과를 포함한다.

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
