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
  ├─ <App>AuthNavDisplay (로그인 전, 보통 단일 back stack. 로그인/회원가입/비밀번호 재설정처럼 선형 플로우면 충분하며,
  │   인증 플로우 안에 독립적으로 전환되는 여러 섹션이 실제로 필요해질 때만 아래 '여러 독립 플로우의 back stack 관리'를 적용한다)
  └─ <App>MainNavDisplay (로그인 후, 하단 탭마다 독립 back stack 예시 — 아래 '여러 독립 플로우의 back stack 관리' 참고)
→ Route
→ Screen
```

`Route`/`Screen`의 책임 분리는 `$android-compose-ui`를 그대로 따른다. Navigation 3에서도 **Screen은 NavKey, back stack, NavDisplay를 전혀 몰라야 한다.** Route만 back stack을 조작한다.

**네비게이션 계층(Navigator/NavHost/NavDisplay)은 "어떤 화면이 어떤 순서로 쌓여 있는가"만 책임진다.**
데이터를 만들거나(API 호출, DB 쓰기), 검증하거나, 언제 이동해야 하는지 판단하는 로직(예: "새 대화를 만들지 말지",
"이 입력값이 유효한지")은 ViewModel/UseCase의 책임이다. Navigator의 함수(`push`, `navigateToTab`, `goBack` 등)는
"이 NavKey로 이동하라"는 이미 결정된 요청을 받아 back stack만 조작해야 하며, 그 요청을 내릴지 말지 스스로 판단하지 않는다.
새 Navigator 함수를 추가할 때 "이 함수가 back stack 조작 외에 다른 일(데이터 생성, 조건 판단 이상의 도메인 로직)을 하고 있지 않은가"를
항상 점검한다.

같은 이유로 **네비게이션 계층은 화면의 UI 표현 자체를 건드리지 않는다.** `NavDisplay`/`entryProvider`가 하는 일은 "지금 어떤 Screen을
보여줄지" 스위칭하는 것뿐이며, 그 안에 그려지는 레이아웃·색상·컴포넌트 구성 같은 실제 UI는 Screen/Route가 전담한다("Screen은 NavKey를
모른다"는 원칙의 반대편 대칭이다). `SceneStrategy`로 "이 목적지가 다이얼로그냐 풀스크린이냐" 같은 표시 방식을 결정하는 것까지는
네비게이션 책임이지만, 그 표시 방식 안의 구체적 UI는 여전히 Screen 몫이다. Navigator나 NavDisplay 코드에 `Modifier`, 색상, 텍스트,
컴포넌트 배치처럼 UI 세부사항이 직접 등장한다면 그 로직이 Screen/Route로 내려가야 하는 건 아닌지 점검한다.

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

## 여러 독립 플로우의 back stack 관리

하단 탭(Home/List/Settings)처럼 **서로 독립적인 이동 기록을 유지해야 하는 여러 섹션**이 있을 때 쓰는 패턴이다.
하단 탭이 가장 흔한 적용 사례지만, 탭이 아니어도(예: 온보딩 내부의 여러 갈래, 기능별로 나뉜 여러 위저드가 화면 하나에서 오갈 때 등)
"섹션을 벗어나도 그 섹션의 이동 기록이 유지되어야 하는가"가 이 패턴을 쓸지 판단하는 기준이다. 필요 없다면(섹션 전환 시
이동 기록을 매번 초기화해도 무방하다면) 이 패턴 대신 단일 back stack + 필요 시 초기화 로직만으로 충분하다.

`<App>MainNavigationState`/`<App>MainNavigator`(`<App>MainNavigationState.kt`)가 이 역할을 한다.
세 요소의 책임은 분명히 나뉜다: **`<App>MainNavigationState`는 상태(백스택들, 현재 탭)를 보관만 하고, `<App>MainNavigator`는
그 상태를 변경만 하며, `toEntries()`는 그 상태를 `NavEntry` 목록으로 변환해 `NavDisplay`에 전달하는 역할만 한다** — 상태 보관과
상태 변경과 렌더링용 변환을 한 클래스가 같이 하지 않는다.

### 구조

- `backStacks: Map<Destination, NavBackStack<NavKey>>` — 섹션(탭)마다 독립된 `NavBackStack`을 `rememberNavBackStack(...)`으로 미리 만들어 둔다.
- 현재 활성 섹션은 `topLevelRoute`(프로퍼티)가 가리키며, 실제 상태는 `topLevelIndex: Int`를 `rememberSaveable`에 저장해 관리한다.
  **NavKey 객체 자체가 아니라 index를 저장한다** — NavKey는 `@Serializable` 대상이라 Compose 기본 Saver로 담을 수 없기 때문이다.
- `toEntries(entryProvider)` — 모든 섹션의 back stack을 `rememberDecoratedNavEntries`로 각각 디코레이트하고(`rememberViewModelStoreNavEntryDecorator`,
  `rememberSaveableStateHolderNavEntryDecorator`를 부여해 섹션 전환 후 돌아와도 화면별 ViewModel·저장 상태가 유지되게 한다),
  그중 현재 활성 섹션의 엔트리 목록만 `NavDisplay(entries = ...)`에 넘긴다.
  비활성 섹션의 엔트리도 이 계산 자체는 매 리컴포지션마다 함께 수행된다는 점을 유의한다(섹션이 매우 많아지면 고려할 트레이드오프).

### Navigator 책임

- `navigateToTab(destination)` / 섹션 전환용 함수 — `backStacks`에 등록된 섹션이면 `topLevelRoute`만 바꾼다(back stack 자체는 건드리지 않아
  이전 기록이 보존된다). 특정 섹션에 재진입할 때 기록을 초기화하고 싶다면(예: 기록 탭을 다시 누르면 항상 최상단으로) 이 함수 안에서
  해당 섹션의 back stack을 시작 화면 하나만 남을 때까지 pop하는 분기를 추가한다 — 섹션마다 이런 특수 규칙이 있을 수 있음을 새 목적지
  설계 시 염두에 둔다.
- `push(destination)` — 현재 활성 섹션의 back stack에만 push한다.
- `goBack()` — 현재 섹션 back stack의 크기가 1보다 크면 pop한다. 크기가 1(시작 화면만 남음)이고 현재 섹션이 시작 섹션이 아니면
  시작 섹션으로 전환한다. 이미 시작 섹션의 시작 화면이면(더 pop할 곳도, 돌아갈 다른 섹션도 없으면) 아무 동작도 하지 않는다
  (상위 `NavDisplay`의 `onBack`이 시스템 뒤로가기까지 처리해 앱 종료로 이어진다).
- 특정 섹션의 back stack을 완전히 비우고 특정 파라미터를 가진 새 항목 하나로 교체한 뒤 그 섹션으로 전환하는 함수(예: "새 대화 화면으로 이동")도
  같은 Navigator에 추가할 수 있다 — 패턴은 "대상 섹션의 back stack을 모두 pop → 새 키 push → topLevelRoute 전환" 3단계다.
  이때도 Navigator는 "새 대화를 시작해도 되는지", "어떤 초기 데이터로 시작할지" 같은 판단은 하지 않는다 — 그런 판단은 호출부(Route가
  ViewModel/UseCase 결과를 받아)에서 이미 끝낸 뒤, Navigator에는 결정된 NavKey 값만 넘긴다. Navigator 함수의 파라미터는 항상
  "어디로 갈지"를 나타내는 값이어야 하며, "갈지 말지"를 담은 조건이어서는 안 된다.

### 다른 화면에서 진입하는 목적지(딥링크·알림 등) 처리

알려진 제약(딥링크 공식 미지원)과 이어지는 내용이다. `<App>MainNavDisplay`가 이미 구성된 상태에서 알림 등으로 특정 섹션의 특정 화면에
진입시켜야 할 때는, 앱 시작 시점에 back stack을 미리 채우는 방법 외에 **`LaunchedEffect(pendingKey)`로 지연 push**하는 방법도 쓸 수 있다:
대상 섹션으로 `navigateToTab()`을 먼저 호출해 그 섹션으로 전환한 뒤 `push()`로 목적지를 얹는다. 섹션 전환을 먼저 해야 뒤로가기가
자연스러운 순서(목적지 → 그 섹션의 시작 화면)로 쌓인다.

### 새 목적지를 추가할 때

- 이 목적지가 특정 섹션에 속하고 섹션을 벗어나도 기록이 유지돼야 하는지(=그 섹션의 back stack에 push), 아니면 어느 섹션에서 봐도
  같은 자리로 돌아가면 되는 화면인지(=단순 push 대상, 섹션 개념 불필요) 판단한다.
- 여러 NavKey가 공유하는 ViewModel이 필요하면 '화면 간 ViewModel 공유' 섹션을 따른다. 이 ViewModelStoreOwner는 섹션 back stack이
  아니라 해당 Composable(대개 NavDisplay를 감싸는 상위 함수)의 생명주기에 묶인다.

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

- Navigation 3는 이 스킬 작성 시점 기준 딥링크를 공식 지원하지 않는다. 앱 시작 시점에 백스택을 원하는 키 목록으로 미리 채우거나(예: 알림 클릭 시 `[Home, Detail(id)]`로 시작), 이미 구성된 NavDisplay에 `LaunchedEffect(pendingKey)`로 지연 push하는 방식으로 우회한다(후자는 '여러 독립 플로우의 back stack 관리' 참고).
- 목적지별로 서로 다른 전환 애니메이션은 `entry<T>(metadata = ...)`와 `NavDisplay.transitionSpec`/`popTransitionSpec`으로 구현할 수 있으나, 값 타입 추론 문제로 `mapOf(...)`를 바로 쓰면 컴파일이 깨지기 쉽다. 꼭 필요하면 먼저 최소 예제로 타입을 확인한 뒤 적용하고, 전역 `transitionSpec`으로 충분하면 그쪽을 우선한다.

## 완료 체크

- 새 화면이 `<App>Destination`에 `@Serializable`로 선언되어 있다.
- route 문자열, `NavType`, `navArgument`를 새로 만들지 않았다.
- Screen이 NavKey/back stack/NavDisplay를 참조하지 않는다.
- ViewModel이 nav 인자를 받아야 하면 `SavedStateHandle`이 아니라 Route의 `LaunchedEffect(key)` + `initialize(...)` 방식을 썼다.
- 다이얼로그·바텀시트를 로컬 boolean으로 여닫지 않고 실제 `NavKey` 목적지로 만들었다.
- 여러 독립 플로우(탭 등)로 나뉘는 화면은 올바른 섹션의 back stack에 push했고, 섹션 재진입 시 기록 초기화가 필요한지 확인했다.
- 여러 화면이 ViewModel을 공유해야 하면 수동 `ViewModelStoreOwner`를 흐름 상위에 두고, 흐름 종료 시 `clear()`한다.
- `rememberSaveable`에 `@Serializable` NavKey 값을 직접 저장하지 않았다.
