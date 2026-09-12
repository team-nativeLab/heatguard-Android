---
name: android-dialog-blur
description: Jetpack Compose 다이얼로그가 열릴 때 배경 블러를 추가·수정하는 작업에 사용한다. Android Window 배경 블러, ModalBottomSheet처럼 별도 레이어인 배경, DisposableEffect 기반 설정·해제, 재사용 가능한 Modifier 블러 처리가 필요한 경우 적용한다.
---

# Android Dialog Blur

## 실행 Hook

### `BeforeMutation`

- Dialog·ModalBottomSheet가 실제 Window overlay인지 확인하고, 공용 blur Composable·Modifier 재사용 여부를 결정한다.
- blur radius token과 Window API 경계를 먼저 확인한다.

### `AfterChange`

- `DisposableEffect`의 등록·해제 쌍과 `onDispose`의 Window 복원 동작을 점검한다.
- `SideEffect`로 수명 관리하지 않았는지, 독립 backdrop마다 동일한 fallback Modifier가 적용되는지 확인한다.

## Rules

- Create one reusable Window blur Composable in `core/component/overlay` instead of duplicating `DialogWindowProvider` casts in each dialog.
- Use `DisposableEffect(dialogWindow, blurRadiusPx)` to add the blur flag and radius, then clear the flag and reset the radius in `onDispose`.
- Use a reusable `Modifier` extension for visual fallback blur. Apply one shared Modifier from the Route to every independently rendered backdrop, such as a screen and a `ModalBottomSheet`.
- Keep blur radius in a role-based dimension token; convert `Dp` to pixels only at the Window API boundary.
- Do not use `SideEffect` for Window blur lifecycle management.

## Pattern

```kotlin
@Composable
fun ApplyDialogWindowBackgroundBlur(blurRadius: Dp) {
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
    val blurRadiusPx = with(LocalDensity.current) { blurRadius.toPx().roundToInt() }

    DisposableEffect(dialogWindow, blurRadiusPx) {
        dialogWindow?.apply { /* register blur */ }

        onDispose {
            dialogWindow?.apply { /* clear blur */ }
        }
    }
}
```
