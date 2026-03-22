package com.sheryv.p2premotecontroller

import com.sheryv.p2premotecontroller.data.FlightControlPayload
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class WebSocketServer(
  port: Int,
  private val handler: (Int, String?, Exception?) -> Unit,
  private val stateChanged: (Boolean) -> Unit,
  private val clientConnectionChanged: (Int, Boolean) -> Unit
) : WebSocketServer(InetSocketAddress("0.0.0.0", port)) {

  var running: Boolean = false
    private set
  private val nextIndex = AtomicInteger(0)
  private val sockets: MutableMap<Int, WebSocket> = ConcurrentHashMap()

  fun send(msg: FlightControlPayload, index: Int? = null) {
    if (index != null) {
      if (sockets[index]?.isOpen == true) {
        sockets[index]!!.send(msg.toBytes())
      }
    } else {
      broadcast(msg.toBytes())
    }
  }

  fun clients() = sockets.keys.toSet()

  override fun onOpen(p0: WebSocket, p1: ClientHandshake?) {
    val index = nextIndex.getAndIncrement()

    sockets[index] = p0
    clientConnectionChanged(index, true)

    logD { "Websocket [$index] client connected. Current clients: ${sockets.keys}" }
  }

  override fun onClose(p0: WebSocket, p1: Int, p2: String?, byRemote: Boolean) {
    val index = sockets.entries.first { it.value == p0 }.key
    log { "Websocket [$index] connection closed" + (if (byRemote) " by remote host" else "") }
    sockets.remove(index)
    clientConnectionChanged(index, false)
  }

  override fun onMessage(conn: WebSocket, message: String?) {
    val index = sockets.entries.firstOrNull { it.value == conn }?.key ?: 0
//    logD { "OnMessage [$index]:" + message.orEmpty() }
    try {
      handler(index, message ?: "", null)
    } catch (e: Exception) {
      logE(e) { "Error parsing websocket message" }
      handler(index, null, e)
    }
  }

  override fun onMessage(p0: WebSocket, p1: ByteBuffer?) {
    try {
      onMessage(p0, StandardCharsets.UTF_8.decode(p1).toString())
    } catch (e: Exception) {
      logE(e) { "Error parsing websocket message" }
    }
  }

  override fun onError(p0: WebSocket?, p1: java.lang.Exception?) {
    val index = sockets.entries.firstOrNull { it.value == p0 }?.key
    logE(p1) { "Websocket [${index ?: "-"}] error" }
  }

  override fun onStart() {
    log { "Websocket server started" }
    running = true
    stateChanged(true)
  }


  fun close() {
    stateChanged(false)
    running = false
    super.stop()
  }
}