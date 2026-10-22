package com.priosort.app.capture

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CaptureStatus(
    val accessGranted: Boolean = false,
    val serviceConnected: Boolean = false,
    val lastCapturedAtMs: Long? = null
)

object CaptureStateManager {
    private val mutableStatus = MutableStateFlow(CaptureStatus())
    val status = mutableStatus.asStateFlow()

    fun refreshPermission(context: Context) {
        mutableStatus.value = mutableStatus.value.copy(
            accessGranted = isNotificationAccessGranted(context)
        )
    }

    fun setServiceConnected(connected: Boolean, context: Context? = null) {
        mutableStatus.value = mutableStatus.value.copy(
            accessGranted = context?.let(::isNotificationAccessGranted)
                ?: mutableStatus.value.accessGranted,
            serviceConnected = connected
        )
    }

    fun markNotificationCaptured(atMs: Long = System.currentTimeMillis()) {
        mutableStatus.value = mutableStatus.value.copy(lastCapturedAtMs = atMs)
    }

    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners.orEmpty().split(":").any { component ->
            ComponentName.unflattenFromString(component)?.packageName == context.packageName
        }
    }
}
