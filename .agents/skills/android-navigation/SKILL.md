---
name: android-navigation
description: 화면 전환, back stack, 화면 간 상태 공유를 다룰 때 사용한다. Navigation 3(NavDisplay/NavBackStack/entryProvider) 기반 새 목적지 추가, 하단 탭 back stack, 여러 화면이 공유하는 ViewModel 설계에 적용한다.
---

# Android Navigation Workflow (Navigation 3)

이 프로젝트는 Jetpack Navigation 3(`androidx.navigation3:navigation3-runtime`, `navigation3-ui`)를 사용한다.
Navigation 2의 `NavHost`/`NavController`/`composable(route: String)`는 더 이상 쓰지 않는다.

아래 클래스명(`<App>NavHost`, `<App>Destination` 등)의 `<App>`은 프로젝트 이름을 나타내는 자리표시자다. 실제 코드에서는 각 프로젝트의 실제 접두사를 쓴다 (예: hopes 프로젝트라면 `<App>` = `Hopes`, 즉 `<App>NavHost`/`<App>Destination`; 다른 프로젝트는 그 프로젝트의 실제 클래스명을 그대로 따른다).

## 기본 구조

```text
MainActivity
→ <App>NavHost (sessionState에 따라 둘 중 하나만 구성)
  ├─ <App>AuthNavDisplay (로그인 전, 단일 back stack)
  └─ <App>MainNavDisplay (로그인 후, 하단 탭마다 독립 back stack)
→ Route
→ Screen
```

`Route`/`Screen`의 책임 분리는 `$android-compose-ui`를 그대로 따른다. Navigation 3에서도 **Screen은 NavKey, back stack, NavDisplay를 전혀 몰라야 한다.** Route만 back stack을 조작한다.

## 실행 Hook

### `BeforeMutation`

- 새 화면이 인증 전·후 어느 NavDisplay와 어느 tab back stack에 속하는지 결정한다.
- Dialog·BottomSheet overlay가 필요하면 NavKey·SceneStrategy·push/pop 수명 구조를 먼저 확정한다.

### `AfterChange`

- route 문자열·`NavType`·`navArgument`가 추가되지 않았는지 확인한다.
- Screen이 NavKey/back stack을 참조하지 않고, overlay 제거 시 닫힘 애니메이션과 back stack 정리가 보장되는지 점검한다.

### `BeforeHandoff`

- 인증 상태 전환, 시스템 뒤로가기, 탭별 back stack, 프로세스 복원 가능성을 확인하고 실행하지 못한 UI 검증을 기록한다.

## NavKey 정의 규칙

모든 화면은 `<App>Destination.kt`의 `@Serializable sealed interface <App>Destination : NavKey` 하위에 선언한다.

```kotlin
@Serializable data object Home : <App>Destination
@Serializable data class Detail(val itemId: Long) : <App>Destination
```

- 인자가 없으면 `data object`, 있으면 `data class`를 쓴다. Navigation 2 시절의 route 문자열과 `NavType`/`navArgument`는 만들지 않는다 — 인자는 항상 생성자 프로퍼티로 타입 안전하게 전달한다.
- `@Serializable`을 반드시 붙인다. 프로세스 데스 이후 back stack을 복원하려면 모든 NavKey가 직렬화 가능해야 한다.
- 인자가 있는 `data class` NavKey는 더 이상 싱글턴이 아니다. `<App>Destination.Detail`처럼 bare value로 쓰던 코드는 `<App>Destination.Detail()`(기본값 생성) 또는 `is <App>Destination.Detail`(when 분기)로 바꿔야 한다.
- 새 목적지를 추가할 때 route 문자열이나 `NavType`/`navArgument`를 다루는 코드를 절대 새로 만들지 않는다. 그런 코드가 필요해 보인다면 설계가 Navigation 2 방식으로 되돌아가고 있다는 신호다.
- ViewModel이 nav 인자를 받아야 하면 `SavedStateHandle`에 의존하지 않는다. Nav 3의 NavKey 생성자 프로퍼티는 `SavedStateHandle`에 자동으로 채워지지 않는다. 대신 Route가 `LaunchedEffect(key)`로 ViewModel의 `initialize(...)` 같은 1회성 메소드를 호출해 인자를 전달한다.

## entryProvider 구조

`NavDisplay`의 `entryProvider = entryProvider { ... }` 블록 안에서 `entry<T> { key -> ... }`로 목적지를 정의한다.

```kotlin
entryProvider {
    entry<<App>Destination.Detail> { key ->
        DetailRoute(
            itemId = key.itemId,
            onBackClick = navigator::goBack,
        )
    }
}
```

- 인증 전 화면(Auth)은 `<App>AuthNavDisplay`의 entryProvider에, 인증 후 화면(Home/List/Settings와 그 하위 화면, 프로젝트별 실제 탭 구성을 따른다)은 `<App>MainNavDisplay`의 entryProvider에 둔다. 두 곳의 back stack은 완전히 분리되어 있으므로 서로의 목적지를 참조하지 않는다.
- back stack 조작은 `backStack.add(key)`(push)와 `backStack.removeLastOrNull()`(pop)만 사용한다. Navigation 2의 `popUpTo`/`launchSingleTop`/`inclusive` 같은 `NavOptions` 개념은 없다 — 필요한 동작은 push/pop 조합과 `<App>MainNavigator` 같은 전용 클래스로 직접 구현한다.

## 다이얼로그

다이얼로그로 띄울 목적지도 로컬 `remember`/`rememberSaveable` boolean으로 여닫지 않고, 실제 `NavKey` 목적지로 만들어 back stack에 push/pop한다. `entry<T>(metadata = DialogSceneStrategy.dialog())`로 표시하고, `NavDisplay`의 `sceneStrategies`에 `DialogSceneStrategy()`를 다른 scene strategy보다 먼저 등록해야 실제로 다이얼로그로 렌더링된다.

```kotlin
NavDisplay(
    ...,
    sceneStrategies = listOf(DialogSceneStrategy(), ...),
    entryProvider = entryProvider {
        entry<SomeDestination.Confirm>(metadata = DialogSceneStrategy.dialog()) {
            ConfirmDialog(onConfirm = ..., onDismiss = navigator::goBack)
        }
    },
)
```

## 바텀시트

Navigation 3(1.1.6 기준)는 다이얼로그와 달리 바텀시트 전용 `SceneStrategy`를 공식 제공하지 않는다. 그렇다고 바텀시트를 Screen 내부의 `AnimatedVisibility`나 직접 만든 드래그 제스처로 흉내내지 않는다 — "화면 위에 뜨는 오버레이"라는 점에서 다이얼로그와 본질이 같으므로, 다이얼로그와 동일하게 실제 `NavKey` 목적지로 만들어 push/pop한다. 공식 API가 없는 부분만 Google 공식 샘플(`AnimatedBottomSheetSceneStrategy`, `navigation3-ui` 샘플 소스에 포함)의 구조를 따라 프로젝트 전용으로 직접 구현한다:

- `SceneStrategy<T>`를 구현하는 클래스를 만들고, `calculateScene`에서 대상 엔트리를 감지하면 `OverlayScene<T>`를 반환한다.
- `OverlayScene.content`에서 `rememberModalBottomSheetState()` + Material3 `ModalBottomSheet(onDismissRequest = onBack, sheetState = sheetState) { entry.Content() }`로 감싼다.
- `OverlayScene.onRemove()`를 override해 `sheetState.hide()`가 끝난 뒤에 엔트리가 제거되도록 한다 — 그렇지 않으면 닫힘 애니메이션이 스킵된다.
- 다이얼로그와 마찬가지로 전용 `NavMetadataKey`와 `.bottomSheet()` 팩토리 함수를 만들어 `entry<T>(metadata = ...BottomSheetSceneStrategy.bottomSheet())`로 표시하고, `NavDisplay`의 `sceneStrategies`에 이 전략을 `DialogSceneStrategy`와 함께 등록한다.

## 하단 탭 멀티 백스택

탭마다 독립된 이동 기록이 필요한 하단 탭(예: Home/List/Settings — 프로젝트별 실제 탭 구성을 따른다)은 `<App>MainNavigationState`/`<App>MainNavigator`(`<App>MainNavigationState.kt`)가 관리한다.

- `tabDestinations`(탭 목적지) 각각에 별도의 `NavBackStack`을 만들고, 현재 활성 탭(`topLevelRoute`)의 back stack만 `NavDisplay`에 전달한다.
- 탭 전환은 `<App>MainNavigator.navigateToTab()`, 탭 내부 push는 `<App>MainNavigator.push()`, 뒤로가기는 `<App>MainNavigator.goBack()`을 사용한다. `goBack()`은 현재 탭 back stack이 시작 화면 하나만 남았을 때 시작 탭(Home)으로 되돌아간다 — 탭 내부 화면에서 시스템 뒤로가기를 눌러도 앱이 종료되지 않고 항상 예측 가능하게 동작해야 한다.
- 현재 어떤 탭이 선택되어 있는지는 `NavKey` 객체 자체가 아니라 `tabDestinations` 안에서의 index로 `rememberSaveable`에 저장한다. `@Serializable` NavKey는 Compose 기본 Saver로 담을 수 없으므로, 새로운 저장 상태를 추가할 때도 NavKey 값 자체를 직접 `rememberSaveable`에 넣지 않는다.
- 새 목적지를 만들 때 이 화면이 탭별로 독립된 기록이 필요한지(=탭 back stack에 push) 아니면 로그인 전처럼 단일 흐름인지 먼저 판단한다.

## 인증 상태 전환

- `SessionState`(로그인 화면뿐 아니라 임의 화면의 401 응답으로도 조용히 바뀔 수 있다 — `core/session`의 `expireSession()` 참고)를 `<App>NavHost`가 `key(sessionState) { ... }`로 감싸 관찰한다. 상태가 바뀌면 `<App>AuthNavDisplay`↔`<App>MainNavDisplay` 전체가 다시 구성되며, 각 NavDisplay 서브트리가 disposed/recreated되므로 별도의 명시적 `navigate(popUpTo(0))` 조작이 필요 없다.
- `AuthRoute`의 `onAuthenticated`나 `SettingsRoute`의 `onLogout` 같은 콜백에서 수동으로 back stack을 비우는 코드를 새로 만들지 않는다. `SessionManager`가 상태를 바꾸는 순간 `key(sessionState)`가 이미 처리한다.
- 이 패턴은 세션 상태가 여러 화면에서 비동기로 바뀔 수 있는 프로젝트에 한정된 선택이다. 로그인/로그아웃 버튼 클릭에서만 인증 상태가 바뀌는 단순한 앱이라면 로컬 `rememberSaveable` boolean 토글이 더 간단할 수 있다 — 새로 도입하기 전에 세션 만료가 발생할 수 있는 경로를 먼저 확인한다.

## 화면 간 ViewModel 공유

Navigation 2의 `navController.getBackStackEntry(route)` + `hiltViewModel(entry)` 패턴은 Navigation 3에 없다. 서로 다른 여러 NavKey가 같은 ViewModel을 공유해야 하면(예: 회원가입 여러 단계) 그 화면들을 감싸는 상위 Composable에서 직접 `ViewModelStoreOwner`를 만들어 스코프를 지정한다.

```kotlin
val flowViewModelStoreOwner = remember {
    object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }
}
DisposableEffect(Unit) {
    onDispose { flowViewModelStoreOwner.viewModelStore.clear() }
}
val sharedViewModel: SomeFlowViewModel =
    hiltViewModel(viewModelStoreOwner = flowViewModelStoreOwner)
```

이렇게 얻은 인스턴스를 흐름에 속한 각 Route에 `viewModel` 파라미터로 명시적으로 전달한다. 상위 Composable이 구성에서 사라지면(=흐름을 완전히 벗어나면) `onDispose`에서 반드시 `clear()`한다 — 그렇지 않으면 다음 시도 때 이전 입력값이 남는다.

이 패턴이 필요한지 판단하는 기준: 한 화면의 `Route`가 기본값으로 `hiltViewModel()`을 써도 되면 그냥 두고, 여러 NavKey가 하나의 인스턴스를 반드시 공유해야 할 때만 위 패턴을 적용한다.

## 알려진 제약

- Navigation 3는 이 스킬 작성 시점 기준 딥링크를 공식 지원하지 않는다. 딥링크가 필요해지면 백스택을 앱 시작 시점에 원하는 키 목록으로 미리 채워 넣는 방식(예: 알림 클릭 시 `[Home, Detail(id)]`로 시작)으로 구현하고, 이 섹션을 갱신한다.
- 목적지별로 서로 다른 전환 애니메이션은 `entry<T>(metadata = ...)`와 `NavDisplay.transitionSpec`/`popTransitionSpec`으로 구현할 수 있으나, 값 타입 추론 문제로 `mapOf(...)`를 바로 쓰면 컴파일이 깨지기 쉽다. 꼭 필요하면 먼저 최소 예제로 타입을 확인한 뒤 적용하고, 전역 `transitionSpec`으로 충분하면 그쪽을 우선한다.

## 완료 체크

- 새 화면이 `<App>Destination`에 `@Serializable`로 선언되어 있다.
- route 문자열, `NavType`, `navArgument`를 새로 만들지 않았다.
- Screen이 NavKey/back stack/NavDisplay를 참조하지 않는다.
- ViewModel이 nav 인자를 받아야 하면 `SavedStateHandle`이 아니라 Route의 `LaunchedEffect(key)` + `initialize(...)` 방식을 썼다.
- 다이얼로그·바텀시트를 로컬 boolean으로 여닫지 않고 실제 `NavKey` 목적지로 만들었다.
- 탭별 독립 기록이 필요한 화면은 올바른 tab back stack에 push된다.
- 여러 화면이 ViewModel을 공유해야 하면 수동 `ViewModelStoreOwner`를 흐름 상위에 두고, 흐름 종료 시 `clear()`한다.
- `rememberSaveable`에 `@Serializable` NavKey 값을 직접 저장하지 않았다.
