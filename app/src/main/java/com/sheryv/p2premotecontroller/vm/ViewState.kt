package com.sheryv.p2premotecontroller.vm

import com.sheryv.p2premotecontroller.data.FlightControlPayload

data class ViewState(
  val serverRunning: Boolean = false,
  val connectedClients: Int = 0,
  val lastPacketsStatus: String = "",
  val controls: FlightControlPayload = FlightControlPayload(),

) {
  fun update(block: FlightControlPayload.() -> FlightControlPayload) =
    copy(controls = block(controls))

  override fun toString(): String {
    return "R=${serverRunning} | $controls"
  }
}