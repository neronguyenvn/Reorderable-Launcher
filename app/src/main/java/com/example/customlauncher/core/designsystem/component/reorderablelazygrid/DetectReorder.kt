/*
 * Copyright 2022 André Claßen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.customlauncher.core.designsystem.component.reorderablelazygrid

import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

@Composable
fun Modifier.detectPressOrDragAndReorder(
    state: ReorderableState<*>,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
): Modifier {
    val itemPosition = remember { mutableStateOf(Offset.Zero) }
    return this then Modifier
        .onGloballyPositioned { itemPosition.value = it.positionInWindow() }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                awaitLongPressOrCancellation(down.id)?.let { start ->
                    onLongClick()
                    awaitDragOrCancellation(down.id)

                    val relativePosition = itemPosition.value -
                            state.layoutWindowPosition.value + start.position
                    state.onDragStart(relativePosition.x.toInt(), relativePosition.y.toInt())
                }
            }
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown()
                waitForUpOrCancellation()?.run {
                    if (uptimeMillis - previousUptimeMillis < 300) {
                        onClick()
                    }
                }
            }
        }
}
