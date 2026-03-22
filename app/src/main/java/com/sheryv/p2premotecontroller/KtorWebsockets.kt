package com.sheryv.p2premotecontroller

import com.sheryv.p2premotecontroller.data.FlightControlPayload
import com.sheryv.p2premotecontroller.data.Settings
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.StateFlow
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

class KtorWebsockets(
  private val settings: StateFlow<Settings>,
  private val stateChanged: (Boolean) -> Unit,
  private val clientConnectionChanged: (Int, Boolean) -> Unit,
  private val responseHandler: suspend (WebSocketMessage) -> Unit,
  ) {
  private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? =
    null

  private val sessionsById = ConcurrentHashMap<Int, Session>()
  private val nextIndex = AtomicInteger(0)

  private val responseHistoryQueue: ArrayDeque<WebSocketMessage> = ArrayDeque()

  fun start() {
    server = embeddedServer(Netty, port = settings.value.serverPort.toInt()) {
      install(WebSockets) {
        pingPeriod = null
        timeout = 15.seconds
        maxFrameSize = 128
        masking = false
      }

      routing {
        webSocket("/pipe") {
          val i = nextIndex.getAndIncrement()
          val session = Session(i, this)
          sessionsById[i] = session
          logD { "Connection [$i] open" }
          try {
            clientConnectionChanged(i, true)

            for (frame in incoming) {
              val timestamp = System.currentTimeMillis()
              val ping = session.lastSentTime?.let { timestamp - it }
              val response = when (frame) {
                is Frame.Text -> WebSocketMessage.Text(
                  i,
                  timestamp,
                  ping,
                  frame.readText()
                )

                is Frame.Binary -> WebSocketMessage.Binary(
                  i,
                  timestamp,
                  ping,
                  frame.data
                )

                else -> continue
              }

              session.lastResponse = response
              addHistory(response)
              responseHandler(response)
            }
          } catch (ignored: CancellationException) {
          } catch (e: Exception) {
            val timestamp = System.currentTimeMillis()
            val ping = session.lastSentTime?.let { timestamp - it }
            responseHandler(WebSocketMessage.Error(i, timestamp, ping, e))
            logE(e) { "Connection [$i] error" }
          } finally {
            logD { "Connection [$i] closed" }
            sessionsById.remove(i)
            clientConnectionChanged(i, false)
          }
        }
      }
    }.start(wait = false)
    logD { "Server started" }
    stateChanged(true)
  }

  suspend fun send(data: FlightControlPayload, clientId: Int? = null) {
    if (clientId != null) {
      val session = sessionsById[clientId]
      session?.socket?.send(data.toBytes())
      session?.lastSentTime = System.currentTimeMillis()
    } else {
      val bytes = data.toBytes()
      sessionsById.values.forEach {
        it.socket.send(bytes)
        it.lastSentTime = System.currentTimeMillis()
      }
    }
  }

  fun isRunning() = server != null

  fun stop() {
    server?.stop()
    server = null
    stateChanged(false)
  }

  fun session(clientId: Int) = sessionsById[clientId]

  fun activeClients() = sessionsById.size

  fun responseHistory(): List<WebSocketMessage> = responseHistoryQueue

  fun calcAvgPing(): Map<Int, Double?> {
    return sessionsById.mapValues { (id, s) ->
      val avg = responseHistoryQueue.mapNotNull {
        if (it.clientId != id) {
          return@mapNotNull null
        }
        it.ping
      }.average()
      if (avg <= 0) {
        return@mapValues null
      }
      return@mapValues avg
    }
  }

  private fun addHistory(msg: WebSocketMessage) {
    responseHistoryQueue.addLast(msg)
    if (responseHistoryQueue.size > 100) {
      responseHistoryQueue.removeFirstOrNull()
    }
  }
}

sealed class WebSocketMessage(val clientId: Int, val timestamp: Long, val ping: Long?) {
  abstract fun dataAsString(): String

  class Binary(clientId: Int, timestamp: Long, ping: Long?, val data: ByteArray) :
    WebSocketMessage(clientId, timestamp, ping) {

    override fun dataAsString() = String(data, StandardCharsets.UTF_8)

    override fun toString(): String {
      return "#$clientId, ${data.toHexString(HexFormat.UpperCase)}"
    }
  }

  class Text(clientId: Int, timestamp: Long, ping: Long?, val data: String) :
    WebSocketMessage(clientId, timestamp, ping) {

    override fun dataAsString() = data

    override fun toString(): String {
      return "#$clientId, ${data}"
    }
  }

  class Error(clientId: Int, timestamp: Long, ping: Long?, val error: Exception) :
    WebSocketMessage(clientId, timestamp, ping) {

    override fun dataAsString() = "ERR: ${error.message}"

    override fun toString(): String {
      return "#$clientId, ${error.stackTraceToString()}"
    }
  }
}

class Session(val clientId: Int, val socket: DefaultWebSocketServerSession) {
  var lastSentTime: Long? = null
  var lastResponse: WebSocketMessage? = null
}