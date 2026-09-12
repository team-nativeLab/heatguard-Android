---
name: android-code-naming
description: Android Kotlin·Jetpack Compose 코드의 변수, 함수, 클래스, 파일, 패키지, UseCase 명명 규칙을 새로 작성하거나 검토할 때 사용한다.
---

# Android Code Naming

## 실행 Hook

### `BeforeMutation`

- 새 public 타입·파일·함수·상수의 역할과 기존 명명 패턴을 먼저 확인한다.
- 이름만 보고 값과 동작을 이해할 수 있는지 검토한 뒤 생성·수정을 시작한다.

### `AfterChange`

- 파일명과 대표 public 타입명이 일치하는지 확인한다.
- `StateFlow` backing property, Boolean 접두사, UseCase 접미사, 패키지 규칙을 재검사한다.

## 기본 규칙

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 일반 변수/프로퍼티 | `camelCase`, 명사 또는 명사구 | `userName`, `selectedBook` |
| 함수 | `camelCase`, 동사 또는 동사구 | `loadBooks()`, `saveUser()` |
| 클래스/인터페이스 | `PascalCase` | `BookRepository` |
| 상수 | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT` |
| private backing property | `_` + 공개 프로퍼티 이름 | `_uiState` → `uiState` |
| Composable | `PascalCase`, 명사처럼 작성 | `BookCard()` |
| 패키지 | 모두 소문자, `_` 금지 | `feature.bookdetail` |
| UseCase | 동사 + 대상 + `UseCase` | `GetBookDetailUseCase` |

## 파일 이름

- 파일명은 파일의 대표 공개 타입과 같은 `PascalCase`로 작성한다. 예: `BookDetailViewModel.kt`, `GetBookDetailUseCase.kt`.
- 역할을 나타내는 접미사를 생략하지 않는다. 예: `BookDetailUiState.kt`, `BookDetailRoute.kt`, `BookDetailScreen.kt`, `BookRepositoryImpl.kt`.
- 하나의 파일에는 서로 무관한 대표 공개 타입을 섞지 않는다. 작은 `private` 보조 타입은 같은 파일에 둘 수 있다.
- 패키지 경로는 모두 소문자로 작성하며 단어를 연결할 때 `_` 대신 소문자를 사용한다. 예: `feature.bookdetail`.

## 적용

1. 새 타입과 파일을 만들기 전에 기존 feature의 명명 패턴을 확인한다.
2. 약어보다 도메인 의미가 드러나는 이름을 사용한다. 단, DTO·URL처럼 널리 통용되는 약어는 허용한다.
3. Boolean은 상태나 의도가 드러나도록 `is`, `has`, `can`, `should`로 시작한다. 예: `isLoading`, `hasNextPage`.
4. backing property를 노출할 때는 `_uiState`는 `MutableStateFlow`, `uiState`는 읽기 전용 `StateFlow`처럼 변경 가능 범위를 분리한다.
5. 기존 코드의 확립된 public API 명명과 충돌하면 호환성을 우선하고, 새 코드부터 이 규칙을 적용한다.

## 완료 체크

- 변수와 함수 이름만으로 값과 동작의 의미를 파악할 수 있다.
- 파일명과 대표 공개 타입명이 일치한다.
- Composable, UseCase, UiState 등 계층 역할을 이름으로 식별할 수 있다.
- package에 대문자나 `_`가 없다.
