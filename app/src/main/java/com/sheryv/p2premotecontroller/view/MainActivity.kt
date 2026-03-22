package com.sheryv.p2premotecontroller.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.sheryv.p2premotecontroller.R
import com.sheryv.p2premotecontroller.Utils
import com.sheryv.p2premotecontroller.observeState
import com.sheryv.p2premotecontroller.vm.MainViewModel
import com.yoimerdr.android.virtualjoystick.views.JoystickView
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
  val viewModel: MainViewModel by viewModels()

  @OptIn(FlowPreview::class)
  @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_main)
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }


    val textLastPackets: TextView = findViewById(R.id.text_last_packets)
    val textState: TextView = findViewById(R.id.text_state)
    val textLeftJoy: TextView = findViewById(R.id.text_left_joy)
    val textRightJoy: TextView = findViewById(R.id.text_right_joy)
    val textThrottle: TextView = findViewById(R.id.text_throttle)
    val buttonConnect: Button = findViewById(R.id.button_connect)
    val buttonSettings: Button = findViewById(R.id.settings_button)
    val buttonBit0: Button = findViewById(R.id.button_bit0)
    val switchBit1: SwitchMaterial = findViewById(R.id.switch_bit1)
    val switchBit2: SwitchMaterial = findViewById(R.id.switch_bit2)
    val sliderThrottle: Slider = findViewById(R.id.slider_throttle)
    val leftJoystick: JoystickView = findViewById(R.id.left_joystick)
    val rightJoystick: JoystickView = findViewById(R.id.right_joystick)

    buttonSettings.setOnClickListener {
      this.startActivity(Intent(this, SettingsActivity::class.java))
    }
    buttonConnect.setOnClickListener {
      viewModel.restartServer()
    }
    buttonBit0.setOnTouchListener { v: View?, event: MotionEvent? ->
      when (event?.action) {
        MotionEvent.ACTION_DOWN -> {
          viewModel.updateButton(0, true)
        }

        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
          viewModel.updateButton(0, false)
        }
      }
      false
    }
    switchBit1.setOnCheckedChangeListener { _, state ->
      viewModel.updateButton(1, state)
    }
    switchBit2.setOnCheckedChangeListener { _, state ->
      viewModel.updateButton(2, state)
    }
    sliderThrottle.addOnChangeListener { _, value, _ ->
      viewModel.updateThrottle(value.toInt())
    }
    leftJoystick.setMoveListener {
      val position = leftJoystick.ndcPosition
      if (viewModel.settings.value.pitchAxisOnLeft) {
        viewModel.updateJoystick(yaw = position.x, pitch = position.y)
      } else {
        viewModel.updateJoystick(yaw = position.x)
      }
    }
    leftJoystick.setMoveEndListener {
      if (viewModel.settings.value.pitchAxisOnLeft) {
        viewModel.updateJoystick(yaw = 0f, pitch = 0f)
      } else {
        viewModel.updateJoystick(yaw = 0f)
      }
    }
    rightJoystick.setMoveListener {
      val position = rightJoystick.ndcPosition
      if (viewModel.settings.value.pitchAxisOnLeft) {
        viewModel.updateJoystick(roll = position.x)
      } else {
        viewModel.updateJoystick(roll = position.x, pitch = position.y)
      }
    }
    rightJoystick.setMoveEndListener {
      if (viewModel.settings.value.pitchAxisOnLeft) {
        viewModel.updateJoystick(roll = 0f)
      } else {
        viewModel.updateJoystick(roll = 0f, pitch = 0f)
      }
    }
    textLastPackets.text = ""
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.serverResponseFlow.conflate().onEach { delay(100) }.collect { (last, history, pings) ->
          val avg = pings.values.filterNotNull().average().toInt()

          textLastPackets.text = "Avg ping: ${avg}\n" + history.takeLast(4).reversed().joinToString("\n") {
            "#${it.clientId}: last: ${it.ping ?: "-"} | ${it.dataAsString()}"
          }
        }
      }
    }

    observeState(viewModel.uiState, throttle = 50) {
//      logD { "State update: $it" }

      textState.text =
        if (it.serverRunning) "Server running, clients: " + it.connectedClients else "Server stopped"
      buttonConnect.text = if (it.serverRunning) "Stop" else "Start server"
      textLeftJoy.text = "  Yaw: ${it.controls.yaw}"
      textRightJoy.text = " Roll: ${it.controls.roll}\nPitch: ${it.controls.pitch}"
      textThrottle.text = "Throttle: ${it.controls.throttle}\nFlags: ${it.controls.flagsAsBits()}"
    }

    observeState(viewModel.settings) {
      sliderThrottle.value =
        Math.clamp(sliderThrottle.value, it.throttleMinValue.toFloat(), it.throttleMaxValue.toFloat())
      sliderThrottle.valueFrom = it.throttleMinValue.toFloat()
      sliderThrottle.valueTo = it.throttleMaxValue.toFloat()
    }

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    sharedPreferences.registerOnSharedPreferenceChangeListener(viewModel)
    viewModel.onSharedPreferenceChanged(sharedPreferences, null)
  }
}