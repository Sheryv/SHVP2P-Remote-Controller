package com.sheryv.p2premotecontroller.data

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.math.min


data class FlightControlPayload(
  // signed byte (1 byte)
  val flags: Short = 0,
  // signed byte (1 byte)
  val other: Short = 0,
  // signed shorts (2 bytes each)
  val roll: Short = 0,
  val pitch: Short = 0,
  val yaw: Short = 0,
  val throttle: Short = 0,
  // String (4 bytes)
  val code: String? = null,
) {
  // unsigned short (represented as int to handle 0-65535, then cast)
  val msgId: Int = id()

  fun toBytes(): ByteArray {
    // Total size: 4 + 1 + 1 + 2 + 2 + 2 + 2 + 2 = 16 bytes
    val buffer = ByteBuffer.allocate(16)
    buffer.order(ByteOrder.BIG_ENDIAN)

    // 4s - 4 byte string (padded or truncated to 4 bytes)
    val codeBytes = ByteArray(4)
    if (code != null) {
      val source = code!!.toByteArray(StandardCharsets.US_ASCII)
      System.arraycopy(source, 0, codeBytes, 0, min(source.size, 4))
    }
    buffer.put(codeBytes)

    // 2b - Two signed bytes
    buffer.put(flags.toUByte().toByte())
    buffer.put(other.toUByte().toByte())

    // 4h - Four signed shorts
    buffer.putShort(roll)
    buffer.putShort(pitch)
    buffer.putShort(yaw)
    buffer.putShort(throttle)

    // 1H - One unsigned short
    buffer.putShort((msgId and 0xFFFF).toShort())

    return buffer.array()

  }

  fun flagsAsBits() = Integer.toBinaryString(flags.toInt()).takeLast(8).padStart(8, '0')

  fun otherAsBits() = Integer.toBinaryString(other.toInt()).takeLast(8).padStart(8, '0')

  override fun toString() =
    "${flagsAsBits()}, ${other}, roll=${roll}, pitch=${pitch}, yaw=${yaw}, throttle=${throttle}, id=${msgId}, code=${code}"

  companion object {
    private fun id() = (System.currentTimeMillis() % 65535).toInt()

    const val THROTTLE_LIMIT = 100L
    const val AXIS_LIMIT = 100L
  }
}

