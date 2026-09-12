---
name: android-memory-safety
description: Context를 필드에 저장하거나 Singleton, Listener, Callback, BroadcastReceiver, WebSocket, Sensor, Location, MediaPlayer, ExoPlayer, 직접 만든 CoroutineScope, Activity·View·NavController·Compose 상태의 장기 참조를 추가·수정할 때 구현 전에 반드시 사용한다.
---

# Android Memory Safety Preflight

## 실행 Hook

### `BeforeWork`

- Context 필드, Singleton, Listener, Callback, 직접 만든 CoroutineScope, 네트워크 연결 등 장기 수명 객체 추가 여부를 먼저 검색한다.
- 대상·owner·종료 시점을 확인하고, 수명 분석이 끝나기 전에는 코드를 수정하지 않는다.

### `BeforeMutation`

- 장기 객체가 참조할 대상, 수명 불일치 가능성, 누수 원인, 해제 방법을 네 항목으로 기록한다.
- 화면 객체·Activity Context·Composable lambda를 Application 수명 객체에 저장하지 않는 구조를 확정한다.

### `AfterChange`

- 등록과 해제, `DisposableEffect`의 `onDispose`, Coroutine 취소, `close`·`release` 경로가 모두 존재하는지 확인한다.

### `BeforeHandoff`

- Singleton이 Activity·View·NavController·화면 callback을 보관하지 않는지 최종 확인한다.

## 가독성

- 등록·해제와 수명 분기를 한 줄로 압축하지 않아, 참조 생성과 해제 경로를 쉽게 검토할 수 있게 한다.

## 적용 시점

이 스킬은 코드 작성 후가 아니라 코드 작성 전에 적용한다.
다음 중 하나라도 포함되면 수명 검사를 먼저 수행한다.

- Context를 필드에 저장하는 클래스
- `@Singleton` 또는 Application 수명 객체
- Listener, Callback, BroadcastReceiver 등록
- WebSocket, Sensor, Location, MediaPlayer, ExoPlayer 연결
- `CoroutineScope(...)` 직접 생성
- Activity, Fragment, View, NavController, Compose state, lambda 장기 보관
- Adapter, Manager, Repository가 화면 객체를 참조

## 필수 분석

구현 전에 다음 네 가지를 명시한다.

1. 장기 수명 객체가 어떤 대상을 참조하는지
2. 참조 대상이 종료되어도 왜 참조가 남을 수 있는지
3. 어떤 생명주기 불일치로 누수가 생기는지
4. 적용할 해제 방법 또는 더 안전한 구조

## 경고 형식

위험이 있으면 코드보다 먼저 다음 형식으로 보고한다.

```text
메모리 누수 위험
- 장기 수명 객체:
- 참조 대상:
- 수명 불일치:
- 종료 후 참조가 남는 이유:
- 적용할 대응:
```

위험이 없더라도 Context나 장기 객체를 새로 도입한다면 안전한 이유를 한두 문장으로 기록한다.

## 안전 원칙

### Context

- Application 수명 객체에는 `@ApplicationContext`만 저장한다.
- Activity Context는 Activity 범위에서만 사용한다.
- Activity Context, View, Window를 Singleton에 넣지 않는다.
- UI를 띄우는 작업이 아니라면 Application Context로 충분한지 먼저 판단한다.

### Listener와 Receiver

- 등록 메소드와 해제 메소드를 한 쌍으로 설계한다.
- `register...()`가 있으면 대응하는 `unregister...()` 또는 `removeListener()` 경로를 만든다.
- owner가 파괴될 때 반드시 해제되도록 lifecycle에 연결한다.

### WebSocket, Player, Sensor, Location

- `disconnect()`, `close()`, `release()`, `stop()` 등 명시적인 종료 메소드를 제공한다.
- 연결 owner와 종료 owner가 동일해야 한다.
- 재연결 전에 기존 연결을 정리한다.
- Callback 내부에서 Activity나 Composable lambda를 장기 저장하지 않는다.

### Compose

등록과 해제는 `DisposableEffect`에서 한 쌍으로 관리한다.

```kotlin
DisposableEffect(key1 = owner) {
    listener.register()

    onDispose {
        listener.unregister()
    }
}
```

단순 데이터 로드는 ViewModel로 이동하고 `LaunchedEffect`에 장기 연결 객체를 보관하지 않는다.

### Coroutine

- ViewModel 작업은 `viewModelScope`
- Lifecycle owner 작업은 `lifecycleScope`
- Compose 화면 수명 작업은 `LaunchedEffect` 또는 `rememberCoroutineScope`
- 직접 만든 Scope가 꼭 필요하면 owner, Job 취소 시점, `close()` 경로를 명시한다.

## 수명 불일치 예시

### 위험

```text
Singleton WebSocketManager
→ Activity callback 저장
→ Activity 종료
→ WebSocketManager는 앱 종료까지 살아 있음
→ 종료된 Activity가 GC되지 못함
```

### 안전한 방향

```text
Singleton WebSocketManager
→ 데이터 이벤트를 Flow로 노출
→ ViewModel이 viewModelScope에서 수집
→ 화면은 lifecycle-aware 방식으로 UiState 수집
→ 화면 callback을 Manager에 저장하지 않음
```

## 완료 체크

- 장기 참조 대상과 owner가 분명하다.
- 등록과 해제 경로가 모두 존재한다.
- Scope 취소 또는 자원 release 시점이 코드에 있다.
- Singleton이 Activity, View, NavController, Composable lambda를 보관하지 않는다.
- Compose side effect는 lifecycle에 맞게 정리된다.
