---
name: android-change-verification
description: Android 코드 변경이 끝난 뒤 빌드, 테스트, lint, formatting, Hilt graph, 계층 경계, 하드코딩, 자원 해제 여부를 검증하고 최종 변경 보고를 작성할 때 사용한다. 중간 아이디어 단계가 아니라 구현 완료 또는 리뷰 직전에 적용한다.
---

# Android Change Verification and Handoff

## 원칙

- 저장소에서 실제 사용하는 명령을 먼저 확인한다.
- 존재하지 않는 Gradle task를 추측해 실행하지 않는다.
- 실패한 검증을 숨기지 않는다.
- 실행하지 못한 항목을 성공했다고 보고하지 않는다.
- 자동 검증과 구조 검토를 모두 수행한다.

## 실행 Hook

이 Skill의 Hook은 최종 보고를 꾸미는 절차가 아니라 변경을 넘기기 위한 통과 게이트다. 실패한 Hook이 있으면 완료로 보고하지 않는다.

### `AfterChange`

- 변경 직후 `git status`, 변경 파일 목록, diff를 확인한다.
- 구조·계층·리소스·수명·보안 검사를 변경 범위에 맞게 수행한다.
- 실제로 존재하는 Gradle task만 실행하고 실패 원인을 남긴다.

### `BeforeHandoff`

- 성공한 테스트·빌드·Lint와 실행하지 못한 검증을 분리해 기록한다.
- 의도하지 않은 파일, 민감 정보, 미완성 TODO, staged되지 않은 의도된 변경을 확인한다.
- 최종 보고에는 실제 명령과 결과만 작성한다.

## 1. 변경 범위 확인

다음을 확인한다.

- `git status`
- 변경 파일 목록
- 예상하지 않은 생성 파일
- 사용하지 않는 import와 dead code
- 변경과 무관한 formatting noise
- 함수·조건문·`when`·객체 생성·컬렉션 변환을 한 줄로 압축한 코드

## 2. 가능한 자동 검증

프로젝트에서 존재하는 task를 찾아 가능한 범위에서 실행한다.

- Gradle sync 또는 compile
- 변경 모듈 unit test
- instrumentation test가 필요한 경우 해당 test
- Kotlin formatting
- lint 또는 detekt
- build variant별 compile

예시일 뿐 실제 task 존재 여부를 확인한다.

```text
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew ktlintCheck
./gradlew detekt
```

## 3. 구조 검증

다음을 코드로 확인한다.

- Hilt binding 누락 여부
- RemoteDataSourceImpl의 API 호출이 `ApiExecutor`를 경유하고 ApiService 직접 호출이 없는지
- RepositoryImpl이 ApiService를 직접 호출하지 않는지
- ViewModel이 DTO, ApiService, Retrofit `Response`를 참조하지 않는지
- RemoteDataSource Interface와 Impl이 실제 호출 흐름에 포함되는지
- 새 서버 기능에 UseCase가 포함되는지
- DTO → Domain → UiModel 경계가 분명한지
- 같은 오류가 여러 계층에서 중복 변환되지 않는지
- Route와 Screen이 분리되어 있는지

## 4. UI와 리소스 검증

- 문자열 하드코딩 여부
- feature 내부 직접 색상 값 여부
- 반복 dp, radius, elevation, duration의 token 사용 여부
- `Scaffold`의 `innerPadding` 적용 여부
- system inset 중복 여부
- Preview가 Hilt나 실제 서버에 의존하지 않는지
- 필요한 `contentDescription` 존재 여부

## 5. 수명과 자원 검증

- Listener 또는 Receiver 등록에 대응하는 해제가 있는지
- WebSocket, Player, Sensor, Location 자원에 종료 경로가 있는지
- 직접 만든 CoroutineScope에 cancel 경로가 있는지
- Singleton이 Activity, View, NavController, Compose state, lambda를 저장하지 않는지
- Application 수명 객체가 `@ApplicationContext`만 보관하는지

## 6. 환경과 보안 검증

- Base URL이 `/`로 끝나는지
- 서버 주소가 feature 코드에 하드코딩되지 않았는지
- `BuildConfig.ENABLE_LOG`가 logging을 제어하는지
- 운영 빌드에서 body log가 비활성화되는지
- 민감값이 Git에 추가되지 않았는지
- BuildConfig 값이 비밀이라는 잘못된 전제가 없는지

## 7. 커밋 메시지

- 기능, 버그 수정, 리팩터링 등 변경 의도를 Conventional Commits 접두사로 명시한다.
- 형식은 `<type>(<scope>): <summary>`를 기본으로 한다. scope가 불필요하면 생략할 수 있다.
- 허용 type은 `feat`, `fix`, `refactor`, `docs`, `test`, `build`, `chore`를 사용한다.
- summary는 현재형 동사로 짧게 작성하고, 변경한 기능 또는 대상이 드러나야 한다.

예시:

```text
feat(calendar): add schedule bottom sheet
fix(calendar): animate schedule sheet dismissal
refactor(theme): extract schedule sheet tokens
```

## 완료 보고 형식

최종 보고는 다음 순서를 사용한다.

### 1. 생성·수정한 계층과 파일

핵심 파일을 계층별로 요약한다.

### 2. 실제 데이터 흐름

사용자 이벤트부터 서버 응답과 화면 갱신까지 실제 구현된 흐름을 한 줄 또는 짧은 단계로 적는다.

### 3. Hilt binding 구조

어떤 Interface가 어떤 구현체에 binding되며 어떤 외부 타입이 `@Provides`되는지 적는다.

### 4. 메모리 누수 위험과 대응

위험 유무와 실제 해제 또는 안전 구조를 적는다.

### 5. BuildConfig 환경 변경

추가·수정한 필드와 dev/stage/prod 영향을 적는다. 변경이 없으면 없다고 적는다.

### 6. 검증 결과

- 실행 성공한 명령
- 실패한 명령과 원인
- 환경 문제로 실행하지 못한 항목
- 남은 수동 검증

## 보고 예시

```text
1. 생성·수정 파일
- domain: Project, ProjectRepository, GetProjectsUseCase
- data: ProjectResponseDto, mapper, ProjectRemoteDataSource, ProjectRepositoryImpl
- presentation: ProjectUiState, ProjectViewModel, ProjectRoute, ProjectScreen

2. 데이터 흐름
ProjectScreen 클릭 → ProjectViewModel → GetProjectsUseCase → ProjectRepositoryImpl → ProjectRemoteDataSourceImpl → ProjectApi → DTO → Domain → UiModel → UiState

3. Hilt
ProjectRepositoryImpl을 ProjectRepository에 @Binds, ProjectRemoteDataSourceImpl을 ProjectRemoteDataSource에 @Binds했다.

4. 메모리
장기 callback이나 Activity 참조를 추가하지 않았다. 새 Singleton은 화면 객체를 보관하지 않는다.

5. BuildConfig
변경 없음.

6. 검증
- 성공: :app:compileDebugKotlin, :app:testDebugUnitTest
- 미실행: instrumentation test — emulator 미연결
```
