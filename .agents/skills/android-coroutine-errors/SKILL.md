---
name: android-coroutine-errors
description: Android에서 Coroutine, Flow, StateFlow, SharedFlow, Channel, 네트워크 예외, UiState의 로딩·성공·빈 결과·오류 처리, 서버 요청 실패(첫 로딩·새로고침·변경 요청)와 서버 상태 변경 반영, 취소 전파를 구현하거나 리뷰할 때 사용한다.
---

# Android Coroutine and Error Handling

## 가독성

- `launch`, `when`, 오류 분기, 상태 갱신을 한 줄로 압축하지 않는다. 취소와 성공·실패 흐름이 드러나도록 각 분기를 줄바꿈한다.

## 실행 Hook

### `BeforeMutation`

- Coroutine·Flow의 owner와 취소 시점, 로딩·성공·빈 결과·오류 상태를 먼저 결정한다.
- 네트워크 호출은 공통 `ApiExecutor` 오류 정책과 중복되지 않도록 확인한다.

### `AfterChange`

- `CancellationException`이 보존되는지, 직접 만든 Scope에 종료 경로가 있는지 확인한다.
- `MutableStateFlow`가 외부에 노출되지 않고 일회성 효과가 StateFlow와 섞이지 않았는지 확인한다.

### `BeforeHandoff`

- 성공·실패·취소·중복 호출 시나리오를 검토하고 관련 단위 테스트 결과를 기록한다.

## Coroutine 수명

- ViewModel 작업은 `viewModelScope`를 우선한다.
- Activity 또는 Fragment 작업은 `lifecycleScope`를 우선한다.
- Compose 화면 수명 작업은 `LaunchedEffect` 또는 `rememberCoroutineScope`를 사용한다.
- 직접 `CoroutineScope`를 만들면 owner와 취소 시점을 명확히 하고 종료 메소드를 제공한다.

## 취소 보존

`CancellationException`을 일반 오류로 처리하거나 삼키지 않는다.

```kotlin
try {
    repository.load()
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Throwable) {
    // 프로젝트 정책에 따라 오류 상태로 변환한다.
}
```

`runCatching`을 사용할 때도 취소 예외가 실패 상태로 흡수되지 않는지 확인한다.
프로젝트에 취소 보존 helper가 있으면 그 규칙을 따른다.

## 오류 변환 위치

동일 오류를 여러 계층에서 중복 변환하지 않는다.

네트워크 호출은 프로젝트 공통 `ApiExecutor`를 사용한다. `ApiExecutor`가 취소를 다시 던지고 HTTP·IO·serialization 오류를 공통 결과로 정규화하는 책임을 가진다면, 호출부는 `try/catch` 또는 `runCatching`으로 이를 다시 감싸지 않는다.

권장 분리:

- ApiExecutor 또는 Core Network: HTTP·IO·serialization 오류를 프로젝트 공통 데이터 오류로 정규화하고 `CancellationException`을 전파
- RemoteDataSource: `ApiExecutor` 결과를 소비하고 API별 응답 본문을 추출
- RepositoryImpl: 데이터 소스 조합 중 발생한 의미 있는 data 오류 처리
- UseCase: 업무 규칙 위반 표현
- ViewModel 또는 UI mapper: 사용자 표시 메시지 선택

내부 원인과 사용자 메시지를 같은 문자열로 취급하지 않는다.

## UiState

다음 상태를 구분한다.

- 로딩
- 성공
- 빈 결과
- 오류

상태가 모순되지 않도록 갱신한다.
예를 들어 `isLoading = true`와 새 오류 메시지가 동시에 남지 않도록 현재 상태를 명시적으로 copy한다.

## 서버 요청 실패 처리

실패는 "어떤 요청이 실패했는가"에 따라 UiState를 다르게 만든다.

| 실패한 요청 | UiState 처리 |
|---|---|
| 화면 첫 로딩(GET) | 전체 오류 상태 + 재시도 액션. 서버 값 자리에 샘플·이전 값을 채우지 않고 `--`, 빈 상태, 오류 문구로 표시한다. |
| 새로고침·페이지네이션 | 이미 받은 데이터는 유지하고 일회성 이벤트(Snackbar 등)로 실패를 알리며 재시도 수단을 준다. 기존 목록을 오류 화면으로 덮지 않는다. |
| 변경 요청(POST·PUT·PATCH·DELETE) | 낙관적 업데이트를 했다면 이전 상태로 롤백한다. 입력값은 보존하고 버튼을 다시 활성화한다. |

- 변경 요청이 진행 중이면 `isSubmitting` 같은 상태로 중복 제출을 막고, 성공·실패 어느 쪽이든 반드시 해제한다.
- 오류는 기능별 앱 오류 `sealed interface`(예: Network, Timeout, Forbidden, NotFound, Conflict, Server, Unknown)로 받아 UI 문구와 재시도 가능 여부를 정한다. 서버 코드 매핑은 `android-server-feature`의 "서버 에러 코드"를 따른다.
- 401은 화면에서 처리하지 않는다. Authenticator → `SessionManager.expireSession()` 경로로만 세션을 종료하고, 화면은 세션 상태 변경에 따라 로그인 화면으로 교체된다(`android-network-environment`).
- 재시도는 사용자가 누르는 재시도를 기본으로 한다. 자동 재시도는 멱등한 GET에만 횟수·간격 제한을 두고 적용하며, 변경 요청은 자동 재시도하지 않는다.
- 실패를 로그만 남기고 UiState에 반영하지 않거나, 로딩 표시가 계속 남는 상태를 만들지 않는다. 모든 요청 경로는 성공·빈 결과·오류 중 하나로 끝난다.

## 서버 상태 변경 반영

서버 데이터는 앱 밖에서도 바뀐다(다른 기기·관리자 처리·시간 경과). 앱이 들고 있는 값이 최신이라고 가정하지 않는다.

- 변경 요청 성공 후에는 서버 응답 본문 또는 재조회 결과로 상태를 확정한다. 앱이 추정한 값으로 확정하지 않는다.
- 409 Conflict, 이미 처리됨, 상태 불일치 같은 오류는 최신 데이터를 다시 조회해 화면을 갱신하고 사용자에게 상태가 바뀌었음을 알린다.
- 서버 상태값(enum)은 `UNKNOWN`을 두고 안전하게 파싱한다. 모르는 상태값 때문에 크래시하거나 임의의 기본 상태로 표시하지 않는다(`android-server-feature`의 "서버 고정 코드값과 enum").
- 서버 상태가 바뀔 수 있는 화면은 재진입·포그라운드 복귀 시 재조회 정책을 명시한다. Compose에서는 `LifecycleResumeEffect` 또는 `repeatOnLifecycle(Lifecycle.State.RESUMED)`를 사용하고, 과도한 호출을 막기 위해 최소 간격을 둔다.
- 같은 데이터에 대한 새 요청이 시작되면 이전 Job을 취소하거나 `flatMapLatest`를 사용해, 늦게 도착한 이전 응답이 최신 상태를 덮어쓰지 않게 한다.
- 사용자가 바뀌면(로그아웃·계정 전환) 이전 사용자의 응답을 반영하지 않는다(`android-oauth-security` 8-1).

## StateFlow와 이벤트

### StateFlow

현재 화면 상태처럼 재구독 시 다시 받아야 하는 값에 사용한다.

- 목록
- 로딩 여부
- 입력값
- 선택 상태
- 오류 화면 상태

### SharedFlow 또는 Channel

한 번만 처리해야 하는 효과에 사용한다.

- 네비게이션
- Snackbar
- Toast 요청
- 권한 화면 열기

이벤트성 효과를 UiState boolean으로 섞어 중복 소비가 발생하지 않게 한다.
프로젝트가 SharedFlow 또는 Channel 중 하나를 이미 채택했다면 그 규칙을 따른다.

## Flow 수집

- Compose에서는 `collectAsStateWithLifecycle()`을 우선한다.
- ViewModel에서 여러 Flow를 합칠 때 `combine`, `stateIn`, `shareIn`의 시작 정책을 기존 프로젝트와 일치시킨다.
- 무한 Flow를 일반 suspend 반환처럼 `first()`로 잘라야 하는지 신중히 판단한다.
- 수집 Job이 owner 종료 시 취소되는지 확인한다.

## 완료 체크

- `CancellationException`이 다시 던져진다.
- API 호출이 `ApiExecutor`를 경유하고, 그 결과를 호출부에서 중복 변환하지 않는다.
- 직접 만든 Scope에 명확한 취소 경로가 있다.
- 상태와 이벤트가 분리되어 있다.
- 오류가 계층마다 중복 변환되지 않는다.
- 사용자 메시지와 내부 오류가 분리되어 있다.
- lifecycle-aware 수집을 사용한다.
- 첫 로딩·새로고침·변경 요청 실패가 각각 오류 상태·기존 데이터 유지·롤백으로 구분되어 처리되고, 로딩 표시가 남지 않는다.
- 변경 요청 중복 제출이 막혀 있고, 성공 후 서버 응답 또는 재조회로 상태를 확정한다.
- 서버 상태 변경(409, 알 수 없는 상태값, 재진입 시 변경)이 크래시 없이 최신 상태로 반영된다.
