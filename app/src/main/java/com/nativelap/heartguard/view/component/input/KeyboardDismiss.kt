package com.nativelap.heartguard.view.component.input

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

internal class KeyboardInputBounds {
    private val inputBounds = mutableMapOf<Any, Rect>()

    fun update(
        key: Any,
        bounds: Rect,
    ) {
        inputBounds[key] = bounds
    }

    fun remove(key: Any) {
        inputBounds.remove(key)
    }

    fun contains(position: Offset): Boolean = inputBounds.values.any { bounds -> bounds.contains(position) }
}

private val LocalKeyboardInputBounds = staticCompositionLocalOf<KeyboardInputBounds?> { null }

@Composable
internal fun Modifier.keyboardInputBounds(): Modifier {
    val bounds = LocalKeyboardInputBounds.current ?: return this
    val key = remember { Any() }
    DisposableEffect(bounds, key) {
        onDispose { bounds.remove(key) }
    }
    return onGloballyPositioned { coordinates -> bounds.update(key, coordinates.boundsInRoot()) }
}

@Composable
internal fun KeyboardDismissContainer(content: @Composable () -> Unit) {
    val bounds = remember { KeyboardInputBounds() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    CompositionLocalProvider(LocalKeyboardInputBounds provides bounds) {
        Box(
            modifier =
                Modifier.fillMaxSize().pointerInput(bounds, focusManager, keyboardController) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                        val startedInInput = bounds.contains(down.position)
                        var dragged = false
                        var multiTouch = false
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            if (event.changes.size > 1) multiTouch = true
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) break
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                dragged = true
                            }
                        } while (change.pressed)
                        if (!startedInInput && !dragged && !multiTouch) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    }
                },
        ) {
            content()
        }
    }
}
