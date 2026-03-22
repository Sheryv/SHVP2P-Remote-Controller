package com.sheryv.p2premotecontroller.data

import com.sheryv.p2premotecontroller.data.FlightControlPayload.Companion.AXIS_LIMIT
import com.sheryv.p2premotecontroller.data.FlightControlPayload.Companion.THROTTLE_LIMIT
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class Settings(
  val serverPort: Long = 9789,
  val sendInterval: Long = 200,
  val throttleMinValue: Long = 0,
  val throttleMaxValue: Long = THROTTLE_LIMIT,
  val pitchAxisOnLeft: Boolean = false,
  val pitchAxisMaxValue: Long = AXIS_LIMIT,
  val pitchAxisCenterValue: Long = 0,
  val pitchAxisMinValue: Long = -AXIS_LIMIT,
  val pitchAxisInvert: Boolean = false,
  val yawAxisMaxValue: Long = AXIS_LIMIT,
  val yawAxisCenterValue: Long = 0,
  val yawAxisMinValue: Long = -AXIS_LIMIT,
  val yawAxisInvert: Boolean = false,
  val rollAxisMaxValue: Long = AXIS_LIMIT,
  val rollAxisCenterValue: Long = 0,
  val rollAxisMinValue: Long = -AXIS_LIMIT,
  val rollAxisInvert: Boolean = false,
  )
