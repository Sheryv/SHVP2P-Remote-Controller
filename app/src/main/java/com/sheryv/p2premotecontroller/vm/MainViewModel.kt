package com.sheryv.p2premotecontroller.vm

import android.content.SharedPreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import com.sheryv.p2premotecontroller.KtorWebsockets
import com.sheryv.p2premotecontroller.Session
import com.sheryv.p2premotecontroller.WebSocketMessage
import com.sheryv.p2premotecontroller.data.FlightControlPayload
import com.sheryv.p2premotecontroller.data.Settings
import com.sheryv.p2premotecontroller.logD
import com.sheryv.p2premotecontroller.logE
import com.sheryv.p2premotecontroller.toJsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.json.JSONObject
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.math.max
import kotlin.math.roundToInt

class MainViewModel : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {

  private var server: KtorWebsockets? = null
  private var senderJob: Job? = null

  private val processLifecycle by lazy { ProcessLifecycleOwner.get() }
  private val _settings = MutableStateFlow(Settings())
  private val _uiState = MutableStateFlow(ViewState())

  val uiState = _uiState.asStateFlow()

  val serverResponseFlow =
    MutableSharedFlow<Triple<WebSocketMessage, List<WebSocketMessage>, Map<Int, Double?>>>(
      extraBufferCapacity = 1,
      onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

  val settings = _settings.asStateFlow()

  fun restartServer() {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        if (server?.isRunning() == true) {
          server!!.stop()
        } else {
          server =
            KtorWebsockets(
              settings,
              ::onWebsocketServerStateChanged,
              ::onWebsocketConnectionChanged,
              ::onWebSocketMessage
            )
          server!!.start()
        }
      } catch (e: Exception) {
        logE(e) { "Cannot start websocket server" }
      }
    }
  }

  fun updateJoystick(roll: Float? = null, pitch: Float? = null, yaw: Float? = null) {
    _uiState.update {
      it.update {
        copy(
          roll = roll?.let {
            calcJoystickValue(
              if (settings.value.rollAxisInvert) -it else it,
              settings.value.rollAxisMinValue,
              settings.value.rollAxisMaxValue,
              settings.value.rollAxisCenterValue
            )
          } ?: this.roll,
          pitch = pitch?.let {
            calcJoystickValue(
              if (settings.value.pitchAxisInvert) -it else it,
              settings.value.pitchAxisMinValue,
              settings.value.pitchAxisMaxValue,
              settings.value.pitchAxisCenterValue
            )
          } ?: this.pitch,
          yaw = yaw?.let {
            calcJoystickValue(
              if (settings.value.yawAxisInvert) -it else it,
              settings.value.yawAxisMinValue,
              settings.value.yawAxisMaxValue,
              settings.value.yawAxisCenterValue
            )
          } ?: this.yaw
        )
      }
    }
  }

  fun updateThrottle(throttle: Int) {
    _uiState.update {
      it.update { copy(throttle = throttle.toShort()) }
    }
  }

  fun updateButton(index: Int, state: Boolean) {
    _uiState.update {
      it.update {
        val newFlags = if (state) {
          flags or (1 shl index).toShort()
        } else {
          flags and ((1 shl index).toShort().inv())
        }
        copy(flags = newFlags)
      }
    }
  }

  private suspend fun onWebSocketMessage(msg: WebSocketMessage) {
    server?.also {
      serverResponseFlow.tryEmit(Triple(msg, it.responseHistory(), it.calcAvgPing()))
    }
  }

  private fun onWebsocketServerStateChanged(newState: Boolean) {
    if (newState) {
      senderJob?.cancel()
      senderJob = MainScope().launch {
        while (this.isActive) {
          sendToDevice()
          val del =
            if (processLifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
              settings.value.sendInterval
            } else {
              max(1000, settings.value.sendInterval)
            }
          delay(del)
        }
      }
    } else {
      senderJob?.cancel()
    }
    _uiState.update {
      it.copy(serverRunning = newState)
    }
  }

  private fun onWebsocketConnectionChanged(index: Int, newState: Boolean) {
    _uiState.update {
      it.copy(connectedClients = server?.activeClients() ?: 0)
    }
  }

  private fun sendToDevice() {
    CoroutineScope(Dispatchers.IO).launch {
      server?.send(uiState.value.controls)
    }
//    logD { "Sending" }
//    server?.send(uiState.value.controls)
  }

  private fun calcJoystickValue(normalizedValue: Float, min: Long, max: Long, center: Long): Short {
    val clampedCenter = Math.clamp(center, min, max)
    val result = if (normalizedValue > 0) {
      (normalizedValue * (max - clampedCenter)).roundToInt() + clampedCenter
    } else if (normalizedValue < 0) {
      (normalizedValue * (-min + clampedCenter)).roundToInt() + clampedCenter
    } else {
      clampedCenter
    }

    return result.toShort()
  }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
    CoroutineScope(Dispatchers.IO).launch {
      val sett = Json.decodeFromJsonElement<Settings>(sharedPreferences.all.toJsonObject())
      _settings.update {
        sett
      }
      updateJoystick(0f, 0f, 0f)
    }
  }
}