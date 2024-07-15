package com.neronguyenvn.reorderablelauncher.core.common.coroutine

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: ClDispatcher)

enum class ClDispatcher { Default, IO }