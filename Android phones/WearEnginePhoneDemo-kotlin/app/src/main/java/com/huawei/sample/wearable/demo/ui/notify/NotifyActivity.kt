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

package com.huawei.sample.wearable.demo.ui.notify

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.huawei.sample.wearable.demo.R
import com.huawei.wearengine.device.Device

/**
 * 消息通知页面
 * Notification page
 *
 * 从 DeviceFragment 跳转，通过 Intent extra "currentDevice" 传入目标设备，
 * 通过 "packageName" 传入手表侧应用包名。
 * Launched from DeviceFragment with the target device in the "currentDevice" extra
 * and the watch side package name in the "packageName" extra.
 */
class NotifyActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CURRENT_DEVICE = "currentDevice"
        const val EXTRA_PACKAGE_NAME = "packageName"
    }

    private lateinit var notifyViewModel: NotifyViewModel

    private lateinit var editPackageName: EditText
    private lateinit var editTitle: EditText
    private lateinit var editContent: EditText
    private lateinit var editTemplateId: EditText
    private lateinit var editButtonOne: EditText
    private lateinit var editButtonTwo: EditText
    private lateinit var editButtonThree: EditText
    private lateinit var textLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notify)

        notifyViewModel = ViewModelProvider(this).get(NotifyViewModel::class.java)

        val textDevice = findViewById<TextView>(R.id.text_notify_device)
        editPackageName = findViewById(R.id.edit_notify_package_name)
        editTitle = findViewById(R.id.edit_notify_title)
        editContent = findViewById(R.id.edit_notify_content)
        editTemplateId = findViewById(R.id.edit_notify_template)
        editButtonOne = findViewById(R.id.edit_notify_button_one)
        editButtonTwo = findViewById(R.id.edit_notify_button_two)
        editButtonThree = findViewById(R.id.edit_notify_button_three)
        textLog = findViewById(R.id.text_log)

        val currentDevice: Device? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_CURRENT_DEVICE, Device::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_CURRENT_DEVICE)
        }
        notifyViewModel.setDevice(currentDevice)

        if (currentDevice == null) {
            notifyViewModel.addLog("currentDevice is null.")
            finish()
            return
        }
        textDevice.text = currentDevice.name ?: ""

        intent.getStringExtra(EXTRA_PACKAGE_NAME)?.let { packageName ->
            editPackageName.setText(packageName)
        }

        notifyViewModel.textLog.observe(this) {
            textLog.text = it
        }

        findViewById<Button>(R.id.button_send_notify).setOnClickListener {
            notifyViewModel.sendNotification(
                this,
                editPackageName.text.toString(),
                editTitle.text.toString(),
                editContent.text.toString(),
                editTemplateId.text.toString(),
                listOf(
                    editButtonOne.text.toString(),
                    editButtonTwo.text.toString(),
                    editButtonThree.text.toString()
                )
            )
        }
        findViewById<Button>(R.id.button_clear_log).setOnClickListener {
            notifyViewModel.clearLog()
        }
    }
}
