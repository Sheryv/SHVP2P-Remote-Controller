@file:OptIn(FlowPreview::class)

package com.sheryv.p2premotecontroller

import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.Inet4Address
import java.net.InetAddress


object Utils {
  var loggingEnabled = true

  fun getDeviceIpAddress(connectivityManager: ConnectivityManager): String? {
    val linkProperties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
    var inetAddress: InetAddress?
    for (linkAddress in linkProperties?.linkAddresses ?: emptyList()) {
      inetAddress = linkAddress.address
      if (inetAddress is Inet4Address && !inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
        return inetAddress.hostAddress
      }
    }
    return null
  }
}


fun LifecycleOwner.observe(
  state: Lifecycle.State = Lifecycle.State.STARTED,
  block: suspend CoroutineScope.() -> Unit
) {
  lifecycleScope.launch {
    repeatOnLifecycle(state) {
      block()
    }
  }
}

fun <T> LifecycleOwner.observeState(
  state: StateFlow<T>,
  lifecycle: Lifecycle.State = Lifecycle.State.STARTED,
  throttle: Long = 100,
  block: suspend (T) -> Unit
) {
  lifecycleScope.launch {
    repeatOnLifecycle(lifecycle) {
      state.transform {
        emit(it)
        delay(throttle)
      }.collect(block)
    }
  }
}

fun Any.log(msg: () -> Any?) {
  if (Utils.loggingEnabled)
    Log.i(this.javaClass.name, msg()?.toString().orEmpty())
}

fun Any.logE(error: Exception? = null, msg: () -> Any?) {
  if (Utils.loggingEnabled)
    Log.e(this.javaClass.name, msg()?.toString().orEmpty(), error)
}

fun Any.logD(msg: () -> Any?) {
  if (Utils.loggingEnabled)
    Log.d(this.javaClass.name, msg()?.toString().orEmpty())
}

private fun Any?.toJsonElement(): JsonElement {
  return when (this) {
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Array<*> -> this.toJsonArray()
    is List<*> -> this.toJsonArray()
    is Map<*, *> -> this.toJsonObject()
    is JsonElement -> this
    else -> JsonNull
  }
}

fun Array<*>.toJsonArray(): JsonArray {
  val array = mutableListOf<JsonElement>()
  this.forEach { array.add(it.toJsonElement()) }
  return JsonArray(array)
}

fun List<*>.toJsonArray(): JsonArray {
  val array = mutableListOf<JsonElement>()
  this.forEach { array.add(it.toJsonElement()) }
  return JsonArray(array)
}

fun Map<*, *>.toJsonObject(): JsonObject {
  val map = mutableMapOf<String, JsonElement>()
  this.forEach {
    if (it.key is String) {
      map[it.key as String] = it.value.toJsonElement()
    }
  }
  return JsonObject(map)
}