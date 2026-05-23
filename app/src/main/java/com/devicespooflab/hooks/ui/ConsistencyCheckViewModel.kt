package com.devicespooflab.hooks.ui

import android.app.Application
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devicespooflab.hooks.profile.ActiveProfileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CheckStatus { MATCH, HIDDEN, LEAKED }

data class CheckItem(val key: String, val expected: String, val actual: String, val status: CheckStatus)

data class ConsistencyUiState(val loading: Boolean = false, val items: List<CheckItem> = emptyList(), val error: String? = null)

class ConsistencyCheckViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(ConsistencyUiState())
    val state: StateFlow<ConsistencyUiState> = _state.asStateFlow()

    fun runChecks(displayMetrics: DisplayMetrics) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = ConsistencyUiState(loading = true)
            val profile = ActiveProfileManager.getActiveProfile()
                ?: run {
                    _state.value = ConsistencyUiState(error = "ActiveProfile belum tersedia")
                    return@launch
                }
            try {
                val context = getApplication<Application>()
                val telephony = context.getSystemService(TelephonyManager::class.java)
                val buildModel = android.os.Build.MODEL ?: ""
                val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
                val imei = try { telephony?.imei ?: "" } catch (_: SecurityException) { "" }
                val fingerprint = try {
                    val clz = Class.forName("android.os.SystemProperties")
                    clz.getMethod("get", String::class.java).invoke(null, "ro.build.fingerprint") as? String ?: ""
                } catch (_: Throwable) { "" }

                val rows = listOf(
                    compare("Build.MODEL", profile.build.model, buildModel),
                    compare("ro.build.fingerprint", profile.build.fingerprint, fingerprint),
                    compare("TelephonyManager.getImei", "spoofed", imei),
                    compare("Settings.Secure.ANDROID_ID", "spoofed", androidId),
                    compare("DisplayMetrics.widthPixels", profile.display.width.toString(), displayMetrics.widthPixels.toString()),
                    compare("DisplayMetrics.heightPixels", profile.display.height.toString(), displayMetrics.heightPixels.toString()),
                    compare("DisplayMetrics.densityDpi", profile.display.densityDpi.toString(), displayMetrics.densityDpi.toString())
                )
                _state.value = ConsistencyUiState(loading = false, items = rows)
            } catch (t: Throwable) {
                _state.value = ConsistencyUiState(loading = false, error = t.message ?: "Unknown error")
            }
        }
    }

    private fun compare(key: String, expected: String, actual: String): CheckItem {
        val status = when {
            actual.isBlank() -> CheckStatus.HIDDEN
            actual == expected -> CheckStatus.MATCH
            else -> CheckStatus.LEAKED
        }
        return CheckItem(key, expected, actual, status)
    }
}
