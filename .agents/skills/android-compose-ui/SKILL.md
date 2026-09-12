---
name: android-compose-ui
description: Jetpack Compose 화면, Route, Screen, UiState, UiModel, 공용 Component를 구현하거나 리팩터링할 때 사용한다. Scaffold, collectAsStateWithLifecycle, 단방향 데이터 흐름, Preview, accessibility, inset 처리가 포함된 작업에 적용한다.
---

# Android Compose UI Workflow

## 가독성

- Composable 본문, 조건문, 상태 갱신, 객체 생성은 한 줄로 압축하지 않는다. 인자 또는 UI 요소가 둘 이상이면 줄바꿈과 들여쓰기를 사용한다.

## 실행 Hook

이 Skill은 화면 수정 중 Hook을 통과하지 못하면 다음 UI 단계로 진행하지 않는다.

### `BeforeWork`

- 기존 Route·Screen·Component·UiState·ScreenEvent 구조와 적용 중인 디자인 token을 확인한다.
- 다이얼로그·BottomSheet·Navigation 변경이면 관련 Skill을 함께 읽는다.

### `BeforeMutation`

- Route가 상태 수집·이벤트 분기·오버레이 상태를 소유하고 Screen은 stateless UI인지 확정한다.
- 새 UI 문자열·색상·반복 수치는 resource 또는 디자인 token의 기존 경로를 확인한다.

### `AfterChange`

- `Screen → onEvent → Route → ViewModel` 흐름, lifecycle-aware 수집, inset·접근성·Component 파일 분리를 점검한다.
- Screen·Component에 ViewModel·Repository·ApiService·직접 네트워크 호출이 새지 않았는지 확인한다.

### `BeforeHandoff`

- 정상·로딩·오류 상태와 필요한 Preview/UI 검증 여부를 확인하고, 실행하지 못한 검증은 명시한다.

## 기본 구조

```text
Navigation (Navigation 3, `$android-navigation` 참고)
→ Route
→ Screen
→ Content (선택, Component 2개 이상을 묶을 때만)
→ Component
```

상태 흐름은 다음과 같이 유지한다.

```text
ViewModel StateFlow
→ Route가 collectAsStateWithLifecycle()
→ Screen에 UiState와 callback 전달
→ 사용자 이벤트 callback
→ Route가 ViewModel 메소드 호출
```

## 파일 위치

- Route, Screen, feature Component는 `view/` 폴더에 둔다.
- ViewModel, UiState, ScreenEvent, UiModel은 `viewmodel/` 폴더에 둔다.
- Domain·Data 계층을 포함한 전체 패키지 구조는 `$android-server-feature`의 패키지 구조를 따른다.

## Screen 단방향 이벤트

Screen에서 발생하는 사용자 의도는 개별 callback을 여러 개 노출하기보다 화면 전용 `ScreenEvent`의 단일 `onEvent` callback으로 Route에 전달한다.

```text
Screen
→ onEvent(ScreenEvent)
→ Route가 when으로 분기
→ ViewModel 호출 / 화면 상태 변경 / 네비게이션
```

- `ScreenEvent`는 Screen과 같은 presentation 패키지에 `sealed interface`로 선언하고, 이벤트가 필요한 값만 포함한다.
- Screen은 `onEvent`만 호출하며 ViewModel, NavController, 다이얼로그 상태를 직접 참조하거나 변경하지 않는다.
- Route는 `onEvent`를 단일 진입점으로 받아 `when`으로 분기한다. ViewModel 호출, 네비게이션 callback 호출, 다이얼로그·bottom sheet 표시 상태 변경은 모두 여기서 수행한다.
- 입력값 변경, 클릭, 확인·취소처럼 사용자의 모든 화면 이벤트는 같은 `ScreenEvent` 흐름에 포함한다. 단, 재사용 가능한 하위 Component의 내부 callback까지 일괄적으로 이벤트 타입으로 바꾸지는 않는다.
- 일회성 효과는 ViewModel의 `SharedFlow` 또는 `Channel`에서 lifecycle-aware 방식으로 Route가 수집하고, Screen 이벤트와 UiState에 섞지 않는다.

예시:

```kotlin
sealed interface ProjectScreenEvent {
    data object BackClicked : ProjectScreenEvent
    data class QueryChanged(val query: String) : ProjectScreenEvent
}

@Composable
fun ProjectRoute(onNavigateBack: () -> Unit) {
    ProjectScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                ProjectScreenEvent.BackClicked -> onNavigateBack()
                is ProjectScreenEvent.QueryChanged -> viewModel.updateQuery(event.query)
            }
        },
    )
}
```

## Route 규칙

- `hiltViewModel()`로 ViewModel을 가져온다.
- `collectAsStateWithLifecycle()`로 UiState를 수집한다.
- ViewModel event 또는 SharedFlow를 lifecycle-aware 방식으로 수집한다.
- 네비게이션 callback을 처리한다.
- 다이얼로그의 표시 여부와 확인·취소 이벤트를 관리하고, 필요한 다이얼로그를 렌더링한다.
- Screen의 `onEvent`를 ViewModel 메소드·네비게이션 callback·Route 상태 변경에 연결한다.
- 실제 배치와 디자인 코드는 최소화한다.
- Repository, UseCase, ApiService를 직접 호출하지 않는다.

예시 구조:

```kotlin
@Composable
fun ProjectRoute(
    onNavigateBack: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProjectScreen(
        uiState = uiState,
        onRetryClick = viewModel::loadProjects,
        onBackClick = onNavigateBack,
    )
}
```

## Screen 규칙

- 상태와 callback만 매개변수로 받는 stateless UI를 우선한다.
- Screen은 `Scaffold`를 포함해 화면 전체를 조립하는 최상위 레이아웃을 직접 배치한다. 화면 전체를 구성하는 책임은 Screen에 있다.
- Screen은 그 안에서 feature Component(그리고 필요하면 Content)를 조합하고 state와 event를 전달한다. `Text`, `Image`처럼 화면 의미나 스타일을 갖는 표현은 새로 만들지 않고 Component로 분리해 사용한다.
- 최상위 컨테이너는 특별한 이유가 없으면 `Scaffold`를 사용한다.
- `innerPadding`을 실제 content에 전달한다.
- 시스템 바 inset을 `Scaffold`와 content에 중복 적용하지 않는다.
- ViewModel, Repository, Context 기반 저장소를 참조하지 않는다.
- 네트워크 요청이나 장기 Coroutine을 직접 시작하지 않는다.

## UiState

로딩, 성공, 빈 결과, 오류를 모호하지 않게 표현한다.
프로젝트 기존 패턴에 따라 단일 data class 또는 sealed hierarchy를 사용한다.

예시 data class:

```kotlin
data class ProjectUiState(
    val isLoading: Boolean = false,
    val projects: List<ProjectUiModel> = emptyList(),
    val errorMessage: String? = null,
)
```

서로 동시에 존재할 수 없는 상태가 많으면 sealed type을 고려한다.

## UiModel과 UI mapper

- Domain Model에 화면 전용 문자열, resource ID, 색상을 넣지 않는다.
- 화면 표시 구조가 Domain과 다르면 UiModel을 만든다.
- 포맷 문자열은 resource와 결합 가능한 구조로 설계한다.
- 서버 동적 값은 `strings.xml`에 넣지 않는다.

## Component 파일 분리 규칙

- 독립된 Component는 반드시 Component별 별도 Kotlin 파일에 선언한다. 하나의 파일에 여러 Component를 선언해 함께 관리하지 않는다.
- 예외는 해당 파일의 public Composable을 보조하는 `private` 구현뿐이다. 이 private Composable도 재사용 가능성, 독립 Preview 필요성, 독립 interaction 책임 중 하나가 생기면 즉시 별도 Component 파일로 분리한다.
- Component는 Route, ViewModel, NavController, Repository를 참조하지 않는다. 받은 state와 callback으로만 동작한다.

## Content 규칙

- `Content`는 화면 전체를 감싸는 최상위 레이아웃 용도로 만들지 않는다. 화면 전체 조립은 Screen이 직접 담당한다.
- `Content`는 서로 관련된 UI 요소 2개 이상을 하나의 재사용 가능한 논리 단위로 묶을 때만 사용한다. 예: 이메일 입력 필드 아래 비밀번호 입력 필드를 묶은 로그인 폼.
- Component가 하나만 필요한 영역에는 Content를 만들지 않는다.
- Content도 독립된 UI 단위이므로 별도 Kotlin 파일에 선언하며, 이름은 묶는 논리 단위의 책임을 드러내도록 `...Content`로 끝낸다.
- Content는 Route, ViewModel, NavController, Repository를 참조하지 않는다. 받은 state와 callback으로만 동작한다.

## Component 분리 기준

다음 중 하나면 별도 Composable을 고려한다.

- 두 곳 이상에서 재사용
- 독립적인 UI 책임
- 자체 상태나 interaction 규칙
- Preview와 테스트가 유용한 단위

한 줄 Text나 단순 Spacer까지 기계적으로 분리하지 않는다. 단, 해당 UI가 화면 의미를 표현하거나 interaction·스타일 규칙을 가지면 크기와 무관하게 Component로 분리한다.
기능 전용 component는 해당 feature 패키지에 두고, 두 화면 이상에서 재사용 가능하거나 앱 전반의 UI 책임을 가진 component는 `core` 패키지에 둔다.

## Spacer 공백 규칙

- `Spacer`는 앞뒤 UI 코드와 각각 빈 줄 하나로 구분한다.
- 조건문이나 람다 블록 안에서도 같은 규칙을 적용한다.
- `Spacer`가 여러 줄로 작성된 경우에는 호출 전체의 앞뒤에 빈 줄을 둔다.

## Preview

- 실제 서버, Hilt ViewModel, Repository를 연결하지 않는다.
- 샘플 UiState와 callback을 전달한다.
- 로딩, 정상, 빈 결과, 오류 상태 Preview를 필요에 따라 만든다.

## 접근성

- 의미 있는 Image와 Icon에는 적절한 `contentDescription`을 제공한다.
- 장식용 이미지는 `contentDescription = null`을 사용한다.
- 클릭 가능한 영역은 Icon만이 아니라 적절한 touch target을 가진 component로 만든다.
- 텍스트만으로 구분하기 어려운 상태는 semantic 정보를 제공한다.

## 하드코딩 방지

- 고정 문자열: `strings.xml` + `stringResource()`
- 색상: `MaterialTheme.colorScheme`
- 반복 spacing/radius/elevation/duration: 디자인 토큰
- 한 번만 쓰이고 의미가 명확한 값까지 무조건 전역 상수로 만들지 않는다.

## 완료 체크

- Route와 Screen이 분리되어 있다.
- Screen이 화면 전체 조립(Scaffold 포함)을 직접 담당하고, Content는 2개 이상 Component를 묶는 용도로만 쓰였다.
- 각 독립 Component와 Content가 각각 별도 Kotlin 파일에 있다.
- Route·Screen·Component·Content는 `view/`, ViewModel·UiState·ScreenEvent·UiModel은 `viewmodel/` 폴더에 있다.
- 다이얼로그는 Screen이 아닌 Route에서 관리한다.
- Screen은 ViewModel을 모른다.
- UiState 수집은 lifecycle-aware 방식이다.
- Scaffold padding과 system inset이 중복되지 않는다.
- Preview가 실제 의존성 없이 동작한다.
- 문자열과 색상 하드코딩이 없다.
- accessibility 설명이 필요한 요소가 처리되어 있다.
