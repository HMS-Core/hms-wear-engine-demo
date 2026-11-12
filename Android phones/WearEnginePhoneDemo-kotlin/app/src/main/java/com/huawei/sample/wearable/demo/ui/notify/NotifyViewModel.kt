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

import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.notify.Action
import com.huawei.wearengine.notify.Notification
import com.huawei.wearengine.notify.NotificationConstants
import com.huawei.wearengine.notify.NotificationTemplate

class NotifyViewModel : ViewModel() {

    companion object {
        private val LOG_TAG = NotifyViewModel::class.java.simpleName

        const val NOTIFY_TEMPLATE_NO_BUTTON = "50"
        const val NOTIFY_TEMPLATE_ONE_BUTTON = "51"
        const val NOTIFY_TEMPLATE_TWO_BUTTON = "52"
        const val NOTIFY_TEMPLATE_THREE_BUTTON = "53"
    }

    private val _textLog = MutableLiveData<String>().apply {
        value = ""
    }
    val textLog: LiveData<String> = _textLog

    private val logBuffer = StringBuilder()

    private var currentDevice: Device? = null

    fun setDevice(device: Device?) {
        currentDevice = device
    }

    /**
     * 发送通知
     * Send a notification
     */
    fun sendNotification(
        context: Context,
        packageName: String,
        title: String,
        text: String,
        templateId: String,
        buttonContents: List<String>
    ) {
        val device = currentDevice
        if (device == null) {
            addLog("currentDevice is null.")
            return
        }

        val trimmedPackageName = packageName.trim()
        if (TextUtils.isEmpty(trimmedPackageName)) {
            addLog("please input packageName for send!")
            return
        }
        val trimmedTitle = title.trim()
        if (TextUtils.isEmpty(trimmedTitle)) {
            addLog("please input title for send!")
            return
        }
        val trimmedText = text.trim()
        if (TextUtils.isEmpty(trimmedText)) {
            addLog("please input notification content for send!")
            return
        }

        val builder = Notification.Builder()
            .setPackageName(trimmedPackageName)
            .setTitle(trimmedTitle)
            .setText(trimmedText)

        if (!setButtonContentAndTemplate(builder, templateId.trim(), buttonContents)) {
            return
        }

        builder.setAction(createAction())
        val notification = builder.build()

        HiWear.getNotifyClient(context)
            .notify(device, notification)
            .addOnSuccessListener {
                addLog("send notification success.")
            }
            .addOnFailureListener { exception ->
                addLog("send notification fail, exception.")
                Log.e(LOG_TAG, "notify() addOnFailureListener() : ", exception)
            }
    }

    /**
     * 通知按钮的回调
     * Callback for the notification buttons
     */
    private fun createAction(): Action {
        return object : Action {
            override fun onResult(notification: Notification?, feedback: Int) {
                addLog("getNotificationAction feedback:$feedback,notification title:${notification?.title}")
            }

            override fun onError(notification: Notification?, errorCode: Int, errorMsg: String?) {
                addLog("getNotificationAction onError errorCode:$errorCode,errorMsg:$errorMsg")
            }
        }
    }

    /**
     * 设置通知按钮数量与内容
     * Set the number and the content of the notification buttons
     *
     * @return true 表示参数合法可以继续发送，false 表示应中断
     */
    private fun setButtonContentAndTemplate(
        builder: Notification.Builder,
        templateId: String,
        buttonContents: List<String>
    ): Boolean {
        val button1Text = buttonContents.getOrNull(0)?.trim() ?: ""
        val button2Text = buttonContents.getOrNull(1)?.trim() ?: ""
        val button3Text = buttonContents.getOrNull(2)?.trim() ?: ""
        val contents = HashMap<Int, String>()

        when (templateId) {
            NOTIFY_TEMPLATE_NO_BUTTON -> {
                builder.setTemplateId(NotificationTemplate.NOTIFICATION_TEMPLATE_NO_BUTTON)
            }
            NOTIFY_TEMPLATE_ONE_BUTTON -> {
                if (TextUtils.isEmpty(button1Text)) {
                    addLog("please input button1Text for send!")
                    return false
                }
                builder.setTemplateId(NotificationTemplate.NOTIFICATION_TEMPLATE_ONE_BUTTON)
                contents[NotificationConstants.BUTTON_ONE_CONTENT_KEY] = button1Text
            }
            NOTIFY_TEMPLATE_TWO_BUTTON -> {
                if (TextUtils.isEmpty(button1Text) || TextUtils.isEmpty(button2Text)) {
                    addLog("please input button1Text and button2Text for send!")
                    return false
                }
                builder.setTemplateId(NotificationTemplate.NOTIFICATION_TEMPLATE_TWO_BUTTONS)
                contents[NotificationConstants.BUTTON_ONE_CONTENT_KEY] = button1Text
                contents[NotificationConstants.BUTTON_TWO_CONTENT_KEY] = button2Text
            }
            NOTIFY_TEMPLATE_THREE_BUTTON -> {
                if (TextUtils.isEmpty(button1Text) || TextUtils.isEmpty(button2Text)
                    || TextUtils.isEmpty(button3Text)
                ) {
                    addLog("please input button1Text and button2Text and button3Text for send!")
                    return false
                }
                builder.setTemplateId(NotificationTemplate.NOTIFICATION_TEMPLATE_THREE_BUTTONS)
                contents[NotificationConstants.BUTTON_ONE_CONTENT_KEY] = button1Text
                contents[NotificationConstants.BUTTON_TWO_CONTENT_KEY] = button2Text
                contents[NotificationConstants.BUTTON_THREE_CONTENT_KEY] = button3Text
            }
            else -> {
                builder.setTemplateId(NotificationTemplate.NOTIFICATION_TEMPLATE_NO_BUTTON)
            }
        }

        if (contents.isNotEmpty()) {
            builder.setButtonContents(contents)
        }
        return true
    }

    fun clearLog() {
        synchronized(logBuffer) {
            logBuffer.setLength(0)
        }
        _textLog.postValue("")
    }

    fun addLog(text: String) {
        Log.i(LOG_TAG, text)
        val snapshot = synchronized(logBuffer) {
            logBuffer.append(text).append('\n')
            logBuffer.toString()
        }
        _textLog.postValue(snapshot)
    }
}
