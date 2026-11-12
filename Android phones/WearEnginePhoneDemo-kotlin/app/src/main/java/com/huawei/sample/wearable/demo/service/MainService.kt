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

package com.huawei.sample.wearable.demo.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import com.huawei.sample.wearable.demo.model.Constant
import com.huawei.sample.wearable.demo.ui.action.ActionFragment
import com.huawei.sample.wearable.demo.ui.data.DataFragment
import com.huawei.sample.wearable.demo.ui.device.DeviceViewModel
import com.huawei.sample.wearable.demo.ui.file.FileFragment
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.common.WearEngineErrorCode
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.p2p.CancelFileTransferCallBack
import com.huawei.wearengine.p2p.FileIdentification
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.PingCallback
import com.huawei.wearengine.p2p.Receiver
import com.huawei.wearengine.p2p.SendCallback
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Timer
import kotlin.concurrent.timer


class MainService : Service() {

    companion object {
        private val LOG_TAG = MainService::class.java.simpleName

        ////////////////////////////////////////////////////////////////
        // Fragment 和服务通信命令
        //（与 Wear Engine 无关，仅用于 Fragment 和服务通信。
        // Fragment and service communication commands
        // (Not related to Wear Engine, just for fragment and service interaction)
        ////////////////////////////////////////////////////////////////

        const val MESSAGE_SEND_JSON_FROM_PHONE_TO_WATCH_SUCCESS = 101
        const val MESSAGE_SEND_JSON_FROM_PHONE_TO_WATCH_FAIL = 102

        const val MESSAGE_SEND_TEXT_FROM_PHONE_TO_WATCH = 201

        const val MESSAGE_SEND_ACTION_FROM_PHONE_TO_WATCH = 301
        const val MESSAGE_SEND_AUTO_PING_FROM_PHONE_TO_WATCH = 302
        const val MESSAGE_STOP_SEND_AUTO_PING_FROM_PHONE_TO_WATCH = 303

        const val MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_START = 401
        const val MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_PROGRESS = 402
        const val MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_SUCCESS = 403
        const val MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL = 404

        const val MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH = 411
        const val MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH_SUCCESS = 412
        const val MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL = 413
        const val MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH_RESULT = 414

        const val MESSAGE_SEND_TEXT_FROM_WATCH_TO_PHONE = 1201

        const val MESSAGE_SEND_FILE_FROM_WATCH_TO_PHONE = 1401

        const val MESSAGE_WEAR_ENGINE_INIT_FINISH = 10001


        const val MESSAGE_TYPE_KEY = "type"
        const val MESSAGE_TYPE_VALUE_JSON_STRING = 1
        const val MESSAGE_TYPE_VALUE_TEXT = 2
        const val MESSAGE_TYPE_VALUE_ACTION = 3
        const val MESSAGE_DATA_KEY = "data"

        const val KEY_DATA = "Data"

    }

    private lateinit var p2pClient: P2pClient

    private lateinit var messenger: Messenger

    private var connectedDevice: Device? = null
    private var targetWatchAppPackageName: String? = null
    private var targetWatchAppFingerPrint: String? = null

    private var dataFragmentMessenger: Messenger? = null
    private var fileFragmentMessenger: Messenger? = null
    private var actionFragmentMessenger: Messenger? = null

    private var pingTimer: Timer? = null

    // 处理从片段收到的消息
    // Process messages received from fragments
    inner class IncomingHandler(
        context: Context,
        private val applicationContext: Context = context.applicationContext
    ) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            when (message.what) {
                // 将手机上的短信发送到手表
                // Send text from your phone to your watch
                MESSAGE_SEND_TEXT_FROM_PHONE_TO_WATCH -> {
                    // 获取已发送消息的文本
                    // Get the text of the message
                    val text = message.data.getString(KEY_DATA)
                    text?.let { text ->
                        // テキストを送信する
                        // send text
                        sendText(text)
                    }
                    Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, text = $text")
                }
                // 将文件从手机发送到手表
                // Send files from your phone to your watch
                MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_START -> {
                    // 获取消息的文件路径
                    // Get the file path of the message
                    val path = message.data.getString(KEY_DATA)
                    path?.let { path ->
                        // 通过传递文件路径发送文件
                        // Pass the file path to send the file
                        sendFile(path)
                    }
                    Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, path = $path")
                }
                // 停止从手机向手表发送文件
                // Stop sending files from your phone to your watch
                MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH -> {
                    // 获取消息的文件路径
                    // Get the file path of the message
                    val path = message.data.getString(KEY_DATA)
                    path?.let { path ->
                        // 停止发送文件
                        // Stop sending the file
                        stopSendFile(path)
                    }
                    Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, path = $path")
                }
                // 将手机上的操作发送到手表
                // Sending actions from your phone to your watch
                MESSAGE_SEND_ACTION_FROM_PHONE_TO_WATCH -> {
                    // 获取消息的操作
                    // Get the action of the message
                    val action = message.data.getString(KEY_DATA)
                    action?.let { action ->
                        // 提交操作
                        // Submit action
                        sendAction(action)
                    }
                    Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, action = $action")
                }
                // 继续从手机向手表发送 ping 信号
                // Continue sending ping from the phone to the watch
                MESSAGE_SEND_AUTO_PING_FROM_PHONE_TO_WATCH -> {
                    // 获取消息的操作
                    // Get the action of the message
                    val period = message.data.getLong(KEY_DATA)
                    period?.let { period ->
                        startPingTimer(period)
                    }
                    Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, period = $period")
                }
                // 阻止手机持续向手表发送提示音
                // Stop your phone from continuously sending pings to your watch
                MESSAGE_STOP_SEND_AUTO_PING_FROM_PHONE_TO_WATCH -> {
                    stopPingTimer()
                    Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}")
                }
                // 其他
                // Other
                else -> {
                    Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}")
                    super.handleMessage(message)
                }
            }
        }
    }



    // 从手表收到的消息
    // Message received from the watch
    private val receiver = object : Receiver {
        override fun onReceiveMessage(p2pMessage: com.huawei.wearengine.p2p.Message) {
            when (p2pMessage.type) {
                // 消息类型：数据
                // Message type: data
                com.huawei.wearengine.p2p.Message.MESSAGE_TYPE_DATA -> {
                    // data 可能为空，直接调用 toString 会抛 NPE 导致 :mainservice 进程崩溃
                    val payload = p2pMessage.data ?: return
                    Log.i(LOG_TAG, "onReceiveMessage() data = " + payload.toString(Charsets.UTF_8) + ", description = " + p2pMessage.description)

                    // 获取消息数据内容
                    // Get the message data
                    val jsonString = payload.toString(Charsets.UTF_8)

                    // 将数据内容传输到片段
                    // Transfer the data content to the fragment
                    noticeDataTypeResult(MESSAGE_SEND_TEXT_FROM_WATCH_TO_PHONE, jsonString)
                }
                // 消息类型：文件
                // Message type: file
                com.huawei.wearengine.p2p.Message.MESSAGE_TYPE_FILE -> {
                    // file / absolutePath 可能为空，直接访问会抛 NPE
                    val file = p2pMessage.file ?: return
                    val absolutePath = file.absolutePath ?: return
                    Log.i(LOG_TAG, "onReceiveMessage() absolutePath = " + absolutePath)

                    // 将接收到的文件的绝对路径传递给片段
                    // Transfer the absolute path of the received file to the fragment
                    noticeFileTypeResult(MESSAGE_SEND_FILE_FROM_WATCH_TO_PHONE, absolutePath)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(LOG_TAG, "onCreate()")

        // 创建一个信使来处理从片段收到的消息
        // Create a messenger to handle messages received from the fragment
        messenger = Messenger(IncomingHandler(applicationContext))

        // Wear Engine初始化
        // Initialize Wear Engine
        wearEngineInit()
    }

    override fun onBind(intent: Intent): IBinder {
        Log.i(LOG_TAG, "onBind()")

        intent?.let { intent ->
            // 注册一个消息器来发送给 DataFragment
            // Register a messenger to send to DataFragment
            if (intent.hasExtra(DataFragment::class.java.simpleName)) {
                dataFragmentMessenger = intent.getParcelableExtra(DataFragment::class.java.simpleName)
                Log.i(LOG_TAG, "onBind() dataFragmentMessenger = $dataFragmentMessenger")
            }
            // 注册一个消息器来发送给 FileFragment
            // Register a messenger to send to FileFragment
            if (intent.hasExtra(FileFragment::class.java.simpleName)) {
                fileFragmentMessenger = intent.getParcelableExtra(FileFragment::class.java.simpleName)
                Log.i(LOG_TAG, "onBind() fileFragmentMessenger = $fileFragmentMessenger")
            }
            // 注册一个消息器来发送给 ActionFragment
            // Register a messenger to send to ActionFragment
            if (intent.hasExtra(ActionFragment::class.java.simpleName)) {
                actionFragmentMessenger = intent.getParcelableExtra(ActionFragment::class.java.simpleName)
                Log.i(LOG_TAG, "onBind() actionFragmentMessenger = $actionFragmentMessenger")
            }
        }

        return messenger.binder
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)

        Log.i(LOG_TAG, "onRebind()")

        intent?.let { intent ->
            // 注册一个消息器来发送给 DataFragment
            // Register a messenger to send to DataFragment
            if (intent.hasExtra(DataFragment::class.java.simpleName)) {
                dataFragmentMessenger = intent.getParcelableExtra(DataFragment::class.java.simpleName)
                Log.i(LOG_TAG, "onRebind() dataFragmentMessenger = $dataFragmentMessenger")
            }
            // 注册一个消息器来发送给 FileFragment
            // Register a messenger to send to FileFragment
            if (intent.hasExtra(FileFragment::class.java.simpleName)) {
                fileFragmentMessenger = intent.getParcelableExtra(FileFragment::class.java.simpleName)
                Log.i(LOG_TAG, "onRebind() fileFragmentMessenger = $fileFragmentMessenger")
            }
            // 注册一个消息器来发送给 ActionFragment
            // Register a messenger to send to ActionFragment
            if (intent.hasExtra(ActionFragment::class.java.simpleName)) {
                actionFragmentMessenger = intent.getParcelableExtra(ActionFragment::class.java.simpleName)
                Log.i(LOG_TAG, "onBind() actionFragmentMessenger = $actionFragmentMessenger")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(LOG_TAG, "onStartCommand()")

        intent?.let { intent ->
            // 注册一个消息器来发送给 DataFragment
            // Register a messenger to send to DataFragment
            if (intent.hasExtra(DataFragment::class.java.simpleName)) {
                dataFragmentMessenger = intent.getParcelableExtra(DataFragment::class.java.simpleName)
                Log.i(LOG_TAG, "onStartCommand() dataFragmentMessenger = $dataFragmentMessenger")
            }
            // 注册一个消息器来发送给 FileFragment
            // Register a messenger to send to FileFragment
            if (intent.hasExtra(FileFragment::class.java.simpleName)) {
                fileFragmentMessenger = intent.getParcelableExtra(FileFragment::class.java.simpleName)
                Log.i(LOG_TAG, "onStartCommand() fileFragmentMessenger = $fileFragmentMessenger")
            }
            // 注册一个消息器来发送给 ActionFragment
            // Register a messenger to send to ActionFragment
            if (intent.hasExtra(ActionFragment::class.java.simpleName)) {
                actionFragmentMessenger = intent.getParcelableExtra(ActionFragment::class.java.simpleName)
                Log.i(LOG_TAG, "onBind() actionFragmentMessenger = $actionFragmentMessenger")
            }
        }

        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.i(LOG_TAG, "onDestroy()")

        // Wear Engine解锁接收器
        // Unlock the Wear Engine receiver
        HiWear.getP2pClient(this).unregisterReceiver(receiver)
            .addOnSuccessListener {
                Log.i(LOG_TAG, "onDestroy() unregisterReceiver() addOnSuccessListener")
            }
            .addOnFailureListener { execption ->
                Log.e(LOG_TAG, "onDestroy() unregisterReceiver() addOnFailureListener : ", execption)
            }
    }

    // Wear Engine初始化
    // Initialize Wear Engine
    private fun wearEngineInit() {
        // 创建一个 P2pClient
        p2pClient = HiWear.getP2pClient(applicationContext)

        // 获取注册设备
        // Create a P2pClient
        HiWear.getDeviceClient(applicationContext)
            .bondedDevices
            .addOnSuccessListener { deviceList ->
                Log.i(LOG_TAG, "init() addOnSuccessListener() deviceList = " + deviceList.joinToString{it.name})

                // 在已注册的设备上执行以下操作：
                // Get registered devices
                deviceList.forEach { device ->
                    // 对于连接的设备执行以下操作：
                    // Perform the following process for the connected device:
                    if (device.isConnected) {
                        connectedDevice = device

                        Log.i(LOG_TAG, "init() addOnSuccessListener() connected device = " + device.name + ", UUID = " + device.uuid)

                        // 检查设备上是否安装了指定的应用程序（包）
                        // 首先，检查智能表应用程序的包名称。
                        // Check if the specified app (package) is installed on the device.
                        // First, check the package name of the wearable app.
                        p2pClient
                            .isAppInstalled(connectedDevice, Constant.WEARABLE_APP_PACKAGE_NAME)
                            .addOnSuccessListener { isAppInstalled ->
                                // 如果智能表应用程序已安装
                                // If the Wearable app is already installed
                                if (isAppInstalled) {
                                    targetWatchAppPackageName = Constant.WEARABLE_APP_PACKAGE_NAME
                                    targetWatchAppFingerPrint = Constant.WEARABLE_APP_FINGER_PRINT

                                    // 设置智能表应用的包名称
                                    // Set the package name of the Wearable app
                                    p2pClient.setPeerPkgName(targetWatchAppPackageName)
                                    // 设置智能表应用程序的指纹
                                    // Set the fingerprint of the Wearable app
                                    p2pClient.setPeerFingerPrint(targetWatchAppFingerPrint)
                                    // 注册 Wear Engine 接收器
                                    // Register the receiver in Wear Engine
                                    p2pClient.registerReceiver(connectedDevice, receiver)
                                    Log.i(LOG_TAG, "init() registerReceiver()")

                                    Log.i(LOG_TAG, "init() addOnSuccessListener() Wearable (Smart watch)")
                                    Log.i(LOG_TAG, "init() addOnSuccessListener() ${Constant.WEARABLE_APP_PACKAGE_NAME} = $isAppInstalled")
                                } else {
                                    // 检查运动表应用的包名称
                                    // Check the package name of the Lite Wearable app
                                    p2pClient
                                        .isAppInstalled(connectedDevice, Constant.LITE_WEARABLE_APP_PACKAGE_NAME)
                                        .addOnSuccessListener { isAppInstalled ->
                                            // 如果运动表应用已安装
                                            // If the Lite Wearable app is already installed
                                            if (isAppInstalled) {
                                                targetWatchAppPackageName = Constant.LITE_WEARABLE_APP_PACKAGE_NAME
                                                targetWatchAppFingerPrint = Constant.LITE_WEARABLE_APP_FINGER_PRINT

                                                // 设置运动表应用的包名称
                                                // Set the package name of the Lite Wearable app
                                                p2pClient.setPeerPkgName(targetWatchAppPackageName)
                                                // 设置运动表应用的指纹
                                                // Set the fingerprint of the Lite Wearable app
                                                p2pClient.setPeerFingerPrint(targetWatchAppFingerPrint)
                                                // 注册 Wear Engine 接收器
                                                // Register the Lite Wear Engine receiver
                                                p2pClient.registerReceiver(connectedDevice, receiver)
                                                Log.i(LOG_TAG, "init() registerReceiver()")

                                                Log.i(LOG_TAG, "init() addOnSuccessListener() Lite wearable (Sport watch)")
                                                Log.i(LOG_TAG, "init() addOnSuccessListener() ${Constant.LITE_WEARABLE_APP_PACKAGE_NAME} = $isAppInstalled")

                                                noticeDataTypeResult(MESSAGE_WEAR_ENGINE_INIT_FINISH, "")
                                                noticeFileTypeResult(MESSAGE_WEAR_ENGINE_INIT_FINISH, "")
                                                noticeActionTypeResult(MESSAGE_WEAR_ENGINE_INIT_FINISH, "")
                                            }
                                        }
                                        .addOnFailureListener { execption ->
                                            Log.e(LOG_TAG, "init() addOnFailureListener() : ", execption)
                                        }
                                }
                            }
                            .addOnFailureListener { execption ->
                                Log.e(LOG_TAG, "init() addOnFailureListener() : ", execption)
                            }

                        return@addOnSuccessListener
                    }
                }
            }
            .addOnFailureListener { execption ->
                Log.e(LOG_TAG, "init() addOnFailureListener() : ", execption)
            }
    }

    // 发送文本
    // send text
    private fun sendText(text: String) {
        val jsonObject = JSONObject().apply {
            this.put(MESSAGE_TYPE_KEY, MESSAGE_TYPE_VALUE_TEXT)
            this.put(MESSAGE_DATA_KEY, text)
        }
        // 将文本转换为 JsonString 并将其发送到设备
        // Convert the text to a JsonString and send it to the device
        sendJsonString(jsonObject.toString())
    }

    // 发送行动
    // send action
    private fun sendAction(action: String) {
        val jsonObject = JSONObject().apply {
            this.put(MESSAGE_TYPE_KEY, MESSAGE_TYPE_VALUE_ACTION)
            this.put(MESSAGE_DATA_KEY, action)
        }
        // 将文本转换为 JsonString 并将其发送到设备
        // Convert the text to a JsonString and send it to the device
        sendJsonString(jsonObject.toString())
    }

    // 将文本转换为 JsonString 并发送到手表
    // Convert the text to a JsonString and send it to the watch
    private fun sendJsonString(jsonString: String) {
        if (jsonString.isBlank()) {
            return
        }

        connectedDevice?.let { connectedDevice ->
            if (!connectedDevice.isConnected) {
                Log.i(LOG_TAG, "sendJsonString() connectedDevice.isConnected = $connectedDevice.isConnected")
            } else {
                // 生成消息
                // generate message
                val sendMessage = com.huawei.wearengine.p2p.Message.Builder()
                    .setPayload(jsonString.toByteArray(StandardCharsets.UTF_8))
                    .build()

                // 向手表发送消息
                // Send a message to the watch
                p2pClient
                    .send(connectedDevice, sendMessage, object : SendCallback {
                        override fun onSendResult(resultCode: Int) {
                            // 发送成功
                            // Send successfully
                            if (WearEngineErrorCode.ERROR_CODE_COMM_SUCCESS == resultCode) {
                                Log.i(LOG_TAG, "sendJsonString() onSendResult() succeeded - jsonString = $jsonString, resultCode = $resultCode")

                                // 在fragment中体现手表端收到的传输结果
                                // Reflect the transmission result received from the watch side in the fragment
                                noticeDataTypeResult(MESSAGE_SEND_JSON_FROM_PHONE_TO_WATCH_SUCCESS, jsonString)
                            } else
                            // 发送失败
                            // Send failure
                            {
                                Log.i(LOG_TAG, "sendJsonString() onSendResult() failed - jsonString = $jsonString, resultCode = $resultCode ${WearEngineErrorCode.getErrorMsgFromCode(resultCode)}")

                                // 在fragment中体现手表端收到的传输结果
                                // Reflect the transmission result received from the watch side in the fragment
                                noticeDataTypeResult(MESSAGE_SEND_JSON_FROM_PHONE_TO_WATCH_FAIL, "")
                            }
                        }

                        override fun onSendProgress(progress: Long) {
                            Log.i(LOG_TAG, "sendJsonString() onSendProgress() progress = $progress")
                        }
                    })
                    .addOnSuccessListener {
                        Log.i(LOG_TAG, "sendJsonString() addOnSuccessListener()")
                    }
                    .addOnFailureListener { execption ->
                        Log.e(LOG_TAG, "sendJsonString() addOnFailureListener() : ", execption)

                        // 传输失败
                        // 将结果反映到Fragment
                        // Transmission failed
                        // Reflect the transmission result in the fragment
                        noticeDataTypeResult(MESSAGE_SEND_JSON_FROM_PHONE_TO_WATCH_FAIL, "")
                    }
            }
        } ?: let {
            Log.i(LOG_TAG, "sendJsonString() connectedDevice is null")
        }
    }

    // 将文件发送到手表
    // 注意：如果您没有文件访问权限，则发送文件将失败。例如，如果您只有媒体访问权限，则可以传输 png 文件，但传输 bin 和 txt 文件将失败
    // Send a file to the watch
    // Note: If you do not have file access permissions, sending the file will fail. For example, if you only have media access permissions, you can transfer png files, but not bin or txt files.
    private fun sendFile(path: String) {
        // 如果文件路径为空
        // If the file path is empty
        if (path.isBlank()) {
            // 将错误反映到Fragment
            // Reflect the error to the fragment
            noticeFileTypeResult(MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL, path)
            return
        }

        val file = File(path)
        // 如果文件路径无法打开
        // If the file path cannot be opened
        if (!file.exists()) {
            Log.i(LOG_TAG, "sendFile() $path not exist")
            // 将错误反映到Fragment
            // Reflect the error to the fragment
            noticeFileTypeResult(MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL, path)
            return
        }

        // 如果不是文件
        // if not a file
        if (!file.isFile) {
            Log.i(LOG_TAG, "sendFile() $path not file")
            // 将错误反映到Fragment
            // Reflect the error to the fragment
            noticeFileTypeResult(MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL, path)
            return
        }

        connectedDevice?.let { connectedDevice ->
            if (!connectedDevice.isConnected) {
                Log.i(LOG_TAG, "sendFile() connectedDevice.isConnected = $connectedDevice.isConnected")
            } else {
                // 生成消息
                // generate message
                val sendMessage = com.huawei.wearengine.p2p.Message.Builder()
                    .setPayload(file)
                    .build()

                p2pClient
                    .send(connectedDevice, sendMessage, object : SendCallback {
                        override fun onSendResult(resultCode: Int) {
                            // 发送成功
                            // Send successfully
                            if (WearEngineErrorCode.ERROR_CODE_COMM_SUCCESS == resultCode) {
                                Log.i(LOG_TAG, "sendFile() onSendResult() succeeded - path = $path, resultCode = $resultCode")

                                // 将结果反映到Fragment
                                // Reflect the transmission result received from the watch side in the fragment
                                noticeFileTypeResult(MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_SUCCESS, path)
                            } else
                            // 送信失敗
                            // Send failure
                            {
                                Log.i(LOG_TAG, "sendFile() onSendResult() failed - path = $path, resultCode = $resultCode ${WearEngineErrorCode.getErrorMsgFromCode(resultCode)}")

                                // 将结果反映到Fragment
                                // Reflect the transmission result received from the watch side in the fragment
                                noticeFileTypeResult(MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL, path)
                            }
                        }

                        override fun onSendProgress(progress: Long) {
                            Log.i(LOG_TAG, "sendFile() onSendProgress() progress = $progress")

                            // 将结果反映到Fragment
                            // Reflect the file sending progress in the fragment
                            noticeFileTypeResult(MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_PROGRESS, progress.toString())
                        }
                    })
                    .addOnSuccessListener {
                        Log.i(LOG_TAG, "sendFile() addOnSuccessListener()")
                    }
                    .addOnFailureListener { execption ->
                        Log.e(LOG_TAG, "sendFile() addOnFailureListener() : ", execption)

                        // 传输失败
                        // 在Fragment中反映传输结果
                        // Transmission failed
                        // Reflect the transmission result in the fragment
                        noticeFileTypeResult(MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL, path)
                    }
            }
        } ?: let {
            Log.i(LOG_TAG, "sendFile() connectedDevice is null")
        }
    }

    // 停止发送文件
    // Stop sending files
    private fun stopSendFile(path: String) {
        // 如果文件路径为空
        // If the file path is empty
        if (path.isBlank()) {
            // 将错误反映到Fragment
            // Reflect the error to the fragment
            noticeFileTypeResult(MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL, path)
            return
        }

        val file = File(path)
        // 如果文件路径无法打开
        // If the file path cannot be opened
        if (!file.exists()) {
            Log.i(LOG_TAG, "stopSendFile() $path not exist")
            // 将错误反映到Fragment
            // Reflect the error to the fragment
            noticeFileTypeResult(MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL, path)
            return
        }

        // 如果不是文件
        // if not a file
        if (!file.isFile) {
            Log.i(LOG_TAG, "stopSendFile() $path not file")
            // 将错误反映到Fragment
            // Reflect the error to the fragment
            noticeFileTypeResult(MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL, path)
            return
        }

        connectedDevice?.let { connectedDevice ->
            if (!connectedDevice.isConnected) {
                Log.i(LOG_TAG, "stopSendFile() connectedDevice.isConnected = $connectedDevice.isConnected")
            } else {
                val fileIdentification = FileIdentification.Builder()
                    .setFile(file)
                    .build()

                // 发送消息停止发送文件
                // Send a message to stop sending files
                p2pClient
                    .cancelFileTransfer(connectedDevice, fileIdentification, object : CancelFileTransferCallBack {
                        override fun onCancelFileTransferResult(errCode: Int) {
                            Log.i(LOG_TAG, "stopSendFile() onCancelFileTransferResult() path = $path, errCode = $errCode")

                            // 在Fragment中反映文件传输停止结果
                            // Reflect the file transmission stop result in the fragment
                            noticeFileTypeResult(MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH_RESULT, "path = $path, errCode = $errCode")
                        }
                    })
                    .addOnSuccessListener {
                        Log.i(LOG_TAG, "stopSendFile() addOnSuccessListener()")

                        // 在Fragment中反映文件传输停止结果
                        // Reflect the file transmission stop result in the fragment
                        noticeFileTypeResult(MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH_SUCCESS, path)
                    }
                    .addOnFailureListener { execption ->
                        Log.e(LOG_TAG, "stopSendFile() addOnFailureListener() : ", execption)

                        // 在Fragment中反映文件传输停止结果
                        // Reflect the file transmission stop result in the fragment
                        noticeFileTypeResult(MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL, path)
                    }
            }
        } ?: let {
            Log.i(LOG_TAG, "stopSendFile() connectedDevice is null")
        }
    }

    // 将结果反映在DataFragment中
    // Reflect the results in DataFragment
    private fun noticeDataTypeResult(type: Int, text: String) {
        dataFragmentMessenger?.let { dataFragmentMessenger ->
            val replyMessage = Message.obtain(null, type)
                .apply {
                    data = Bundle().apply {
                        putString(KEY_DATA, text)
                    }
                }
            try {
                dataFragmentMessenger.send(replyMessage)
                Log.i(LOG_TAG, "noticeDataTypeResult() type = $type, text = $text")
            } catch (remoteException: RemoteException) {
                Log.e(LOG_TAG, "sendMessage() exception : ", remoteException)
            }
        }
    }

    // 将结果反映在FileFragment中
    // Reflect the results in FileFragment
    private fun noticeFileTypeResult(type: Int, text: String) {
        fileFragmentMessenger?.let { fileFragmentMessenger ->
            val jsonObject = JSONObject().apply {
                this.put(MESSAGE_TYPE_KEY, MESSAGE_TYPE_VALUE_TEXT)
                this.put(MESSAGE_DATA_KEY, text)
            }

            val replyMessage = Message.obtain(null, type)
                .apply {
                    data = Bundle().apply {
                        putString(KEY_DATA, jsonObject.toString())
                    }
                }
            try {
                fileFragmentMessenger.send(replyMessage)
                Log.i(LOG_TAG, "noticeFileTypeResult() type = $type, text = $text")
            } catch (remoteException: RemoteException) {
                Log.e(LOG_TAG, "sendMessage() exception : ", remoteException)
            }
        }
    }

    private fun noticeActionTypeResult(type: Int, text: String) {
        actionFragmentMessenger?.let { actionFragmentMessenger ->
            val replyMessage = Message.obtain(null, type)
                .apply {
                    data = Bundle().apply {
                        putString(KEY_DATA, text)
                    }
                }
            try {
                actionFragmentMessenger.send(replyMessage)
                Log.i(LOG_TAG, "noticeActionTypeResult() type = $type, text = $text")
            } catch (remoteException: RemoteException) {
                Log.e(LOG_TAG, "sendMessage() exception : ", remoteException)
            }
        }
    }

    // 尝试与设备通信（尝试 ping 命令）
    //Attempt to communicate with the device (try pinging)
    private fun ping(packageName: String) {
        connectedDevice?.let { connectedDevice ->
            if (!connectedDevice.isConnected) {
                Log.i(LOG_TAG, "ping() connectedDevice.isConnected = $connectedDevice.isConnected")
            } else {
                HiWear.getP2pClient(applicationContext)
                    .setPeerPkgName(packageName)
                    .ping(connectedDevice, object : PingCallback {
                        override fun onPingResult(errCode: Int) {
                        }
                    })
                    .addOnSuccessListener {
                        Log.i(LOG_TAG, "ping() addOnSuccessListener()")
                    }
                    .addOnFailureListener { execption ->
                        Log.e(LOG_TAG, "ping() addOnFailureListener() : ", execption)
                    }
            }
        } ?: let {
            Log.i(LOG_TAG, "ping() connectedDevice is null")
        }
    }

    // 智能手机不断发送 ping 信号，以防止手表应用程序被终止。
    // The smartphone keeps sending ping to prevent the watch app from being terminated.
    private fun startPingTimer(period: Long) {
        targetWatchAppPackageName?.let { packageName ->
            stopPingTimer()

            pingTimer = timer(period = period) {
                ping(packageName)
            }
        }
    }

    // 停止发送 PING
    // Stop sending PING
    private fun stopPingTimer() {
        pingTimer?.let { pingTimer ->
            pingTimer.cancel()
        }
        pingTimer = null
    }
}