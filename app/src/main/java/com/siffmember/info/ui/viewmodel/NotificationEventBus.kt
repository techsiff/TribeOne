package com.siffmember.info.ui.viewmodel

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object NotificationEventBus {
    private val _events = MutableSharedFlow<String>() // or custom data
    val events: SharedFlow<String> = _events

    suspend fun sendEvent(message: String) {
        _events.emit(message)
    }
}