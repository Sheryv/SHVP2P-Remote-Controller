package com.sheryv.p2premotecontroller.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreference
import com.sheryv.p2premotecontroller.R
import com.sheryv.p2premotecontroller.Utils
import com.sheryv.p2premotecontroller.data.FlightControlPayload
import com.sheryv.p2premotecontroller.data.FlightControlPayload.Companion.AXIS_LIMIT
import com.sheryv.p2premotecontroller.data.FlightControlPayload.Companion.THROTTLE_LIMIT
import com.sheryv.p2premotecontroller.data.Settings


class SettingsActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.settings_activity)
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings)) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }
    if (savedInstanceState == null) {
      supportFragmentManager
        .beginTransaction()
        .replace(R.id.settings, SettingsFragment())
        .commit()
    }
    supportActionBar?.setDisplayHomeAsUpEnabled(true)


  }

  class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var ipAddress: String? = null
    private var wsAddress: String = ""

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      val connManager = requireActivity().getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
      ipAddress = Utils.getDeviceIpAddress(connManager).orEmpty()

      val screen = preferenceManager.createPreferenceScreen(preferenceManager.context)

      val getValueMax: (String, Long) -> ((EditTextPreference) -> Long) = { key, default ->
        { (preferenceManager.sharedPreferences?.getString(key, null)?.toLongOrNull() ?: default) - 1 }
      }
      val getValueMin: (String, Long) -> ((EditTextPreference) -> Long) = { key, default ->
        { (preferenceManager.sharedPreferences?.getString(key, null)?.toLongOrNull() ?: default) + 1 }
      }

      screen.apply {
        category("Global").apply {
          addPreference(Preference(context).apply {
            key = "address"
            title = "Address: "
            summary = "Tap to copy"
            isIconSpaceReserved = false
            setOnPreferenceClickListener {
              copyToClipboard(wsAddress)
              true
            }
          })

          editNumber("Server port number", "serverPort", DEFAULTS.serverPort, null, 1025, 32767)
          editNumber("Sent interval in milliseconds", "sendInterval", DEFAULTS.sendInterval, null, 10, 10000)
        }

        category("Throttle adjustments").apply {
          editNumber(
            "Throttle min value",
            "throttleMinValue",
            DEFAULTS.throttleMinValue,
            null,
            { 0 },
            getValueMax("throttleMaxValue", THROTTLE_LIMIT)
          )
          editNumber(
            "Throttle max value",
            "throttleMaxValue",
            DEFAULTS.throttleMaxValue,
            null,
            getValueMin("throttleMinValue", 0),
            { THROTTLE_LIMIT })

        }
        category("Joystick adjustments").apply {
          switch("Move pitch axis to left joystick", "pitchAxisOnLeft", DEFAULTS.pitchAxisOnLeft)
          switch("Invert roll axis", "rollAxisInvert", DEFAULTS.rollAxisInvert)
          editNumber("Roll axis min value", "rollAxisMinValue", DEFAULTS.rollAxisMinValue, null, -AXIS_LIMIT, 0)
          editNumber("Roll axis center value", "rollAxisCenterValue", DEFAULTS.rollAxisCenterValue, null, -AXIS_LIMIT, AXIS_LIMIT)
          editNumber("Roll axis max value", "rollAxisMaxValue", DEFAULTS.rollAxisMaxValue, null, 0, AXIS_LIMIT)
          switch("Invert pitch axis", "pitchAxisInvert", DEFAULTS.pitchAxisInvert)
          editNumber("Pitch axis min value", "pitchAxisMinValue", DEFAULTS.pitchAxisMinValue, null, -AXIS_LIMIT, 0)
          editNumber("Pitch axis center value", "pitchAxisCenterValue", DEFAULTS.pitchAxisCenterValue, null, -AXIS_LIMIT, AXIS_LIMIT)
          editNumber("Pitch axis max value", "pitchAxisMaxValue", DEFAULTS.pitchAxisMaxValue, null, 0, AXIS_LIMIT)
          switch("Invert yaw axis", "yawAxisInvert", DEFAULTS.yawAxisInvert)
          editNumber("Yaw axis min value", "yawAxisMinValue", DEFAULTS.yawAxisMinValue, null, -AXIS_LIMIT, 0)
          editNumber("Yaw axis center value", "yawAxisCenterValue", DEFAULTS.yawAxisCenterValue, null, -AXIS_LIMIT, AXIS_LIMIT)
          editNumber("Yaw axis max value", "yawAxisMaxValue", DEFAULTS.yawAxisMaxValue, null, 0, AXIS_LIMIT)
        }
      }

      preferenceScreen = screen
      updateAddress(preferenceManager.sharedPreferences!!)
    }

    fun copyToClipboard(text: String) {
      val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText("Address", text)
      clipboard.setPrimaryClip(clip)

      Toast.makeText(context, "Address copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
      updateAddress(sharedPreferences)
    }

    private fun updateAddress(sharedPreferences: SharedPreferences) {
      val port = sharedPreferences.getString("port", null)?.toIntOrNull() ?: DEFAULTS.serverPort
      wsAddress = "ws://${ipAddress}:$port/pipe"

      findPreference<Preference>("address")?.title = "Address: $wsAddress"
    }

    fun PreferenceGroup.category(title: String): PreferenceCategory {
      val preference = PreferenceCategory(context).apply {
        this.title = title
        isIconSpaceReserved = false
      }
      addPreference(preference)
      return preference
    }

    fun PreferenceGroup.switch(
      title: String,
      key: String,
      default: Boolean = false,
      summary: String = ""
    ): SwitchPreference {
      val preference = SwitchPreference(context).apply {
        this.key = key
        this.title = title
        this.summary = summary
        setDefaultValue(default)
        isIconSpaceReserved = false
      }
      addPreference(preference)
      return preference
    }

    fun PreferenceGroup.editText(
      title: String,
      key: String,
      default: String = "",
      summary: String? = null,
    ): EditTextPreference {
      val preference = EditTextPreference(context).apply {
        this.key = key
        this.title = title
        this.dialogTitle = title
        if (default.isNotBlank())
          this.dialogMessage = "Default is $default"
        if (summary != null) {
          this.summary = summary
        } else {
          summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        }
        isIconSpaceReserved = false
        setDefaultValue(default)
      }
      addPreference(preference)
      return preference
    }

    fun PreferenceScreen.editNumber(
      title: String,
      key: String,
      default: Long = 0,
      summary: String? = null,
      min: Long? = null,
      max: Long? = null,
    ): EditTextPreference {
      val minProvider: ((EditTextPreference) -> Long)? = min?.let { m -> ({ m }) }
      val maxProvider: ((EditTextPreference) -> Long)? = max?.let { m -> ({ m }) }
      return editNumber(title, key, default, summary, minProvider, maxProvider)
    }

    fun PreferenceGroup.editNumber(
      title: String,
      key: String,
      default: Long = 0,
      summary: String? = null,
      min: ((EditTextPreference) -> Long)? = null,
      max: ((EditTextPreference) -> Long)? = null,
    ): EditTextPreference {

      val description = { minValue: Long?, maxValue: Long? ->
        when {
          minValue != null && maxValue != null -> "Only numbers between $minValue and $maxValue are allowed"
          minValue != null -> "Only numbers greater or equal to $minValue are allowed"
          maxValue != null -> "Only numbers less or equal to $maxValue are allowed"
          else -> "Only numbers are allowed"
        }
      }

      val validate = { text: String?, min: Long?, max: Long? ->
        !text.isNullOrBlank()
            && max?.let { m -> text.toLongOrNull()?.let { it <= m } ?: false } ?: true
            && min?.let { m -> text.toLongOrNull()?.let { it >= m } ?: false } ?: true
      }

      val preference = EditTextPreference(context).apply {
        this.key = key
        this.title = title
        this.dialogTitle = title
        this.dialogMessage = "Default is $default"
        if (summary != null) {
          this.summary = summary
        } else {
          summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        }
        isIconSpaceReserved = false
        setDefaultValue(default.toString())
        setOnBindEditTextListener { editText ->
          editText.inputType = InputType.TYPE_NUMBER_FLAG_SIGNED
          val maxValue = max?.invoke(this)
          val minValue = min?.invoke(this)
          val desc = description(minValue, maxValue)
          editText.doAfterTextChanged { edit ->
            if (edit != null && validate(edit.toString(), minValue, maxValue)) {
              editText.error = null
            } else {
              editText.error = "Incorrect value. $desc"
            }
          }
        }
        setOnPreferenceChangeListener { p, v ->
          validate(v as? String, min?.invoke(this), max?.invoke(this))
        }
      }
      addPreference(preference)
      return preference
    }


  }
}

private val DEFAULTS = Settings()