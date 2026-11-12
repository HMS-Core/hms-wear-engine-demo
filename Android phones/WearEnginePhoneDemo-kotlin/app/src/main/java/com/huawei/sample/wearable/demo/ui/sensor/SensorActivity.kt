/*
 * Copyright 2026. Huawei Technologies Co., Ltd. All rights reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.sample.wearable.demo.ui.sensor

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.huawei.sample.wearable.demo.R
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.sensor.Sensor

/**
 * 传感器页面
 * Sensor page
 *
 * 从 DeviceFragment 跳转，通过 Intent extra "currentDevice" 传入目标设备。
 * Launched from DeviceFragment with the target device in the "currentDevice" extra.
 */
class SensorActivity : AppCompatActivity() {

    companion object {
        private val LOG_TAG = SensorActivity::class.java.simpleName

        const val EXTRA_CURRENT_DEVICE = "currentDevice"
    }

    private lateinit var sensorViewModel: SensorViewModel

    private lateinit var textDeviceName: TextView
    private lateinit var sensorRadioGroup: RadioGroup
    private lateinit var textLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)

        sensorViewModel = ViewModelProvider(this).get(SensorViewModel::class.java)

        textDeviceName = findViewById(R.id.text_device_name)
        sensorRadioGroup = findViewById(R.id.sensor_radio_group)
        textLog = findViewById(R.id.text_log)

        val currentDevice: Device? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_CURRENT_DEVICE, Device::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_CURRENT_DEVICE)
        }
        sensorViewModel.setDevice(currentDevice)

        if (currentDevice == null) {
            sensorViewModel.addLog("currentDevice is null.")
        } else {
            textDeviceName.text = currentDevice.name ?: ""
        }

        sensorViewModel.textLog.observe(this) {
            textLog.text = it
        }
        sensorViewModel.sensorList.observe(this) { sensors ->
            updateSensorListBox(sensors)
        }

        sensorRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            // RadioButton 的 id 从 1 开始，避免 0 与 View.NO_ID 冲突
            // RadioButton ids start from 1 so that 0 does not clash with View.NO_ID
            sensorViewModel.setSelectedSensor(checkedId - 1)
        }

        findViewById<View>(R.id.button_get_sensor_list).setOnClickListener {
            sensorViewModel.getSensorList(this)
        }
        findViewById<View>(R.id.button_async_read).setOnClickListener {
            sensorViewModel.asyncReadSensorData(this)
        }
        findViewById<View>(R.id.button_stop_read).setOnClickListener {
            sensorViewModel.stopReadSensorData(this)
        }
        findViewById<View>(R.id.button_clear_log).setOnClickListener {
            sensorViewModel.clearLog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 页面退出时停止读取，避免传感器回调持续到达
        // Stop reading when the page is destroyed so callbacks do not keep coming
        sensorViewModel.stopReadSensorData(applicationContext)
        Log.i(LOG_TAG, "onDestroy() stopReadSensorData")
    }

    private fun updateSensorListBox(sensors: List<Sensor>) {
        sensorRadioGroup.removeAllViews()
        sensors.forEachIndexed { index, sensor ->
            val radioButton = RadioButton(this).apply {
                id = index + 1
                text = sensor.name
                isChecked = index == 0
            }
            sensorRadioGroup.addView(radioButton)
        }
    }
}
