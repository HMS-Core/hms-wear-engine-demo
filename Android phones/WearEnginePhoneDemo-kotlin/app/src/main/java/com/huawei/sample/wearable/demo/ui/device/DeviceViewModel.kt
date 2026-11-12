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

package com.huawei.sample.wearable.demo.ui.device

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.huawei.sample.wearable.demo.model.Constant
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.common.WearEngineErrorCode
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.monitor.MonitorItem
import com.huawei.wearengine.monitor.MonitorListener
import com.huawei.wearengine.p2p.PingCallback
import java.util.Timer
import kotlin.concurrent.timer


class DeviceViewModel : ViewModel() {

    companion object {
        private val LOG_TAG = DeviceViewModel::class.java.simpleName

        private val PERMISSION_ARRAY = arrayOf(
            Permission.DEVICE_MANAGER,
            Permission.NOTIFY,
            Permission.SENSOR,
            Permission.MOTION_SENSOR
        )

        private val MONITOR_CHARGE_STATUS_DETAIL: Map<Int, String> = mapOf(
            1 to "Charge start",
            2 to "Charge stop",
            3 to "Charge finish"
        )

        private val MONITOR_ITEM_LIST = listOf<MonitorItem>(MonitorItem.MONITOR_ITEM_LOW_POWER, MonitorItem.MONITOR_CHARGE_STATUS)

        const val PING_PERIOD: Long = 1000 * 30
    }

    private val _textHasAvailableDevices = MutableLiveData<String>().apply {
        value = "Devices : Unknown"
    }
    val textHasAvailableDevices: LiveData<String> = _textHasAvailableDevices

    private val _textCheckPermission = MutableLiveData<String>().apply {
        value = "Permission : Unknown"
    }
    val textCheckPermission: LiveData<String> = _textCheckPermission

    private val _textDeviceName = MutableLiveData<String>().apply {
        value = "Device name : Unknown"
    }
    val textDeviceName: LiveData<String> = _textDeviceName

    private val _textDeviceUuid = MutableLiveData<String>().apply {
        value = "Device UUID : Unknown"
    }
    val textDeviceUuid: LiveData<String> = _textDeviceUuid

    private val _textDeviceAvailableStorage = MutableLiveData<String>().apply {
        value = "Available storage : Unknown"
    }
    val textDeviceAvailableStorage: LiveData<String> = _textDeviceAvailableStorage

    private val _textDeviceMonitorPowerStatus = MutableLiveData<String>().apply {
        value = "Power status : Unknown"
    }
    val textDeviceMonitorPowerStatus: LiveData<String> = _textDeviceMonitorPowerStatus

    private val _textDeviceMonitorChargeStatus = MutableLiveData<String>().apply {
        value = "Charge status : Unknown"
    }
    val textDeviceMonitorChargeStatus: LiveData<String> = _textDeviceMonitorChargeStatus

    private val _textDeviceMonitorItemWear = MutableLiveData<String>().apply {
        value = "Item wear : Unknown"
    }
    val textDeviceMonitorItemWear: LiveData<String> = _textDeviceMonitorItemWear

    private val _textDeviceMonitorItemPowerMode = MutableLiveData<String>().apply {
        value = "Item power mode : Unknown"
    }
    val textDeviceMonitorItemPowerMode: LiveData<String> = _textDeviceMonitorItemPowerMode

    private val _textDeviceType = MutableLiveData<String>().apply {
        value = "Device type : Unknown"
    }
    val textDeviceType: LiveData<String> = _textDeviceType

    private val _textIsAppInstalled = MutableLiveData<String>().apply {
        value = "Package name : Unknown"
    }
    val textIsAppInstalled: LiveData<String> = _textIsAppInstalled

    private val _textPing = MutableLiveData<String>().apply {
        value = "Ping : Unknown"
    }
    val textPing: LiveData<String> = _textPing
    
    private val _textLog = MutableLiveData<String>().apply {
        value = ""
    }
    val textLog: LiveData<String> = _textLog

    private var connectedDevice: Device? = null
    private var targetWatchAppPackageName: String? = null
    private var targetWatchAppFingerPrint: String? = null

    // 供传感器/通知页面获取当前已连接设备
    // Expose the connected device for the sensor and notification pages
    fun currentDevice(): Device? = connectedDevice

    // 供传感器/通知页面获取当前目标手表应用包名
    // Expose the target watch app package name for the sensor and notification pages
    fun targetWatchAppPackageName(): String? = targetWatchAppPackageName

    // 供页面追加日志
    // Append a log line for the pages
    fun addLog(text: String) {
        Log.i(LOG_TAG, text)
        _textLog.value = (_textLog.value ?: "") + text + "\n"
    }

    private var pingTimer: Timer? = null

    // 检查设备是否存在
    // Check if the device is present
    fun hasAvailableDevices(context: Context) {
        HiWear.getDeviceClient(context)
            .hasAvailableDevices()
            .addOnSuccessListener { result ->
                // 有设备
                // there is a device
                if (result) {
                    _textHasAvailableDevices.postValue("Devices : Available")
                } else
                // 无设备
                // No device
                {
                    _textHasAvailableDevices.postValue("Devices : Non available")
                }

                _textLog.value = _textLog.value + "update() addOnSuccessListener() result = $result" + "\n"
                Log.i(LOG_TAG, "update() addOnSuccessListener() result = $result")
            }
            .addOnFailureListener { execption ->
                _textHasAvailableDevices.postValue("Devices : Unknown")

                _textLog.value = _textLog.value + "update() addOnFailureListener() execption = $execption" + "\n"
                Log.e(LOG_TAG, "update() addOnFailureListener() : ", execption)
            }
    }

    // 检查 Wear Engine 是否有权限
    // Check if Wear Engine has permission
    fun checkPermission(context: Context) {
        HiWear.getAuthClient(context)
            .checkPermissions(PERMISSION_ARRAY)
            .addOnSuccessListener { resultArray ->
                var resultText = ""
                if (resultArray.size >= 4) {
                    resultText += "Basic device infomation = " + resultArray[0] + ", "
                    resultText += "Message notification = " + resultArray[1] + ", "
                    resultText += "Sensor = " + resultArray[2] + ", "
                    resultText += "Motion sensor = " + resultArray[3]
                }

                _textCheckPermission.postValue("Permission : $resultText")

                _textLog.value = _textLog.value + "checkPermission() addOnSuccessListener() result = $resultText\n"
                Log.i(LOG_TAG, "checkPermission() addOnSuccessListener() result = $resultText")

                resultArray.forEach { result ->
                    if (!result) {
                        requestPermission(context)
                        return@addOnSuccessListener
                    }
                }
            }
            .addOnFailureListener { execption ->
                _textCheckPermission.postValue("Permission : Unknown")

                _textLog.value = _textLog.value + "checkPermission() addOnFailureListener() execption = $execption" + "\n"
                Log.e(LOG_TAG, "checkPermission() addOnFailureListener() : ", execption)
            }
    }

    // 获取用户的Wear Engine权限
    // Ask the user for Wear Engine permissions
    private fun requestPermission(context: Context) {
        HiWear.getAuthClient(context)
            .requestPermission(
                object : AuthCallback {
                    override fun onOk(permissions: Array<out Permission>?) {
                        _textLog.value = _textLog.value + "requestPermission() onOk() permissions = " + permissions?.joinToString(separator = ",", transform = { it.name }) + "\n"
                        Log.i(LOG_TAG, "requestPermission() onOk() permissions = " + permissions?.joinToString(separator = ",", transform = { it.name }))
                    }

                    override fun onCancel() {
                        _textLog.value = _textLog.value + "requestPermission() onCancel()\n"
                        Log.i(LOG_TAG, "requestPermission() onCancel()")
                    }
                },
                Permission.DEVICE_MANAGER,
                Permission.NOTIFY,
                Permission.SENSOR,
                Permission.MOTION_SENSOR
            )
            .addOnSuccessListener {
                _textLog.value = _textLog.value + "requestPermission() addOnSuccessListener()\n"
                Log.i(LOG_TAG, "requestPermission() addOnSuccessListener()")
            }
            .addOnFailureListener { execption ->
                _textLog.value = _textLog.value + "requestPermission() addOnFailureListener() execption = $execption" + "\n"
                Log.e(LOG_TAG, "requestPermission() addOnFailureListener() : ", execption)
            }
    }

    // 获取可以进行P2P通信的设备
    // Get devices that can communicate P2P
    fun getConnectedDevice(context: Context) {
        HiWear.getDeviceClient(context)
            .bondedDevices
            .addOnSuccessListener { deviceList ->
                _textLog.value = _textLog.value + "getConnectedDevice() addOnSuccessListener() deviceList = " + deviceList.joinToString{it.name} + "\n"
                Log.i(LOG_TAG, "getConnectedDevice() addOnSuccessListener() deviceList = " + deviceList.joinToString{it.name})

                // 进一步限制当前连接到目标设备的设备数量。
                // *此示例假设当前连接到目标设备的设备数量为 1。
                // Further limit the number of devices currently connected to the target device.
                // *This sample assumes that the number of devices currently connected to the target device is one.
                deviceList.forEach { device ->
                    if (device.isConnected) {
                        connectedDevice = device
                        _textDeviceName.postValue("Device name : " + device.name)
                        _textDeviceUuid.postValue("Device UUID : " + device.uuid)

                        _textLog.value = _textLog.value + "getConnectedDevice() addOnSuccessListener() connected device = " + device.name + ", UUID = " + device.uuid + "\n"
                        Log.i(LOG_TAG, "getConnectedDevice() addOnSuccessListener() connected device = " + device.name + ", UUID = " + device.uuid)

                        // 依次查询设备状态，避免多个查询并发时回调被覆盖
                        // Query device status sequentially to avoid callbacks being overwritten
                        getAvailableKbytes(context) {
                            // 检查设备的电池状态
                            // Check the device's battery status
                            queryMonitorPowerStatus(context)
                        }

                        // 注册监听器
                        // Register the listener
                        registerMonitor(context, MONITOR_ITEM_LIST)
                    }
                }
            }
            .addOnFailureListener { execption ->
                _textDeviceName.postValue("Device name : Unknown")
                _textDeviceUuid.postValue("Device UUID : Unknown")

                _textLog.value = _textLog.value + "getConnectedDevice() addOnFailureListener() execption = $execption" + "\n"
                Log.e(LOG_TAG, "getConnectedDevice() addOnFailureListener() : ", execption)
            }
    }

    // 检查设备上的可用空间（KB）
    // Check the free space (KB) on the device
    private fun getAvailableKbytes(context: Context, onComplete: () -> Unit = {}) {
        connectedDevice?.let { connectedDevice ->
            if (!connectedDevice.isConnected) {
                _textDeviceAvailableStorage.postValue("Available storage : Unknown")

                _textLog.value = _textLog.value + "getAvailableKbytes() connectedDevice.isConnected = $connectedDevice.isConnected\n"
                Log.i(LOG_TAG, "getAvailableKbytes() connectedDevice.isConnected = $connectedDevice.isConnected")
                onComplete()
            } else {
                HiWear.getDeviceClient(context)
                    .getAvailableKbytes(connectedDevice)
                    .addOnSuccessListener { kb ->
                        _textDeviceAvailableStorage.postValue("Available storage : $kb KB = ${kb / 1024} MB = ${kb / 1024 / 1024} GB")

                        _textLog.value = _textLog.value + "getAvailableKbytes() addOnSuccessListener() Available KB = $kb\n"
                        Log.i(LOG_TAG, "getAvailableKbytes() addOnSuccessListener() Available KB = $kb")

                        checkApp(context)
                        onComplete()
                    }
                    .addOnFailureListener { execption ->
                        _textDeviceAvailableStorage.postValue("Available storage : Unknown")

                        _textLog.value = _textLog.value + "getAvailableKbytes() addOnFailureListener() execption = $execption" + "\n"
                        Log.e(LOG_TAG, "getAvailableKbytes() addOnFailureListener() : ", execption)
                        onComplete()
                    }
            }
        } ?: let {
            _textDeviceAvailableStorage.postValue("Available storage : Unknown")

            _textLog.value = _textLog.value + "getAvailableKbytes() connectedDevice is null\n"
            Log.i(LOG_TAG, "getAvailableKbytes() connectedDevice is null")
            onComplete()
        }
    }

    // 检查设备的电池状态
    // Check the device's battery status
    private fun queryMonitorPowerStatus(context: Context) {
        connectedDevice?.let { connectedDevice ->
            if (!connectedDevice.isConnected) {
                _textDeviceMonitorPowerStatus.postValue("Power status : Unknown")

                _textLog.value = _textLog.value + "queryMonitorPowerStatus() connectedDevice.isConnected = $connectedDevice.isConnected\n"
                Log.i(LOG_TAG, "queryMonitorPowerStatus() connectedDevice.isConnected = $connectedDevice.isConnected")
            } else {
                HiWear.getMonitorClient(context)
                    .query(connectedDevice, MonitorItem.MONITOR_POWER_STATUS)
                    .addOnSuccessListener { monitorData ->
                        _textDeviceMonitorPowerStatus.postValue("Power status : ${monitorData.asInt()}")

                        _textLog.value = _textLog.value + "queryMonitorPowerStatus() addOnSuccessListener() Power status = ${monitorData.asInt()}\n"
                        Log.i(LOG_TAG, "queryMonitorPowerStatus() addOnSuccessListener() Power status = ${monitorData.asInt()}")

                        // 还检查设备的充电状态
                        // Also check the device's charging status
                        queryMonitorChargeStatus(context)
                    }
                    .addOnFailureListener { execption ->
                        _textDeviceMonitorPowerStatus.postValue("Power status : Unknown")

                        _textLog.value = _textLog.value + "queryMonitorPowerStatus() addOnFailureListener() execption = $execption" + "\n"
                        Log.e(LOG_TAG, "queryMonitorPowerStatus() addOnFailureListener() : ", execption)

                        // 还检查设备的充电状态
                        // Also check the device's charging status
                        queryMonitorChargeStatus(context)
                    }
            }
        } ?: let {
            _textDeviceMonitorPowerStatus.postValue("Power status : Unknown")

            _textLog.value = _textLog.value + "queryMonitorPowerStatus() connectedDevice is null\n"
            Log.i(LOG_TAG, "queryMonitorPowerStatus() connectedDevice is null")
        }
    }

    // 检查设备的充电状态
    // Check the device's charging status
    private fun queryMonitorChargeStatus(context: Context) {
        connectedDevice?.let { connectedDevice ->
            if (!connectedDevice.isConnected) {
                _textDeviceMonitorChargeStatus.postValue("Charge status : Unknown")

                _textLog.value = _textLog.value + "queryMonitorChargeStatus() connectedDevice.isConnected = $connectedDevice.isConnected\n"
                Log.i(LOG_TAG, "queryMonitorChargeStatus() connectedDevice.isConnected = $connectedDevice.isConnected")
            } else {
                HiWear.getMonitorClient(context)
                    .query(connectedDevice, MonitorItem.MONITOR_CHARGE_STATUS)
                    .addOnSuccessListener { monitorData ->
                        val result = MONITOR_CHARGE_STATUS_DETAIL[monitorData.asInt()]
                        result?.let { let ->
                            _textDeviceMonitorChargeStatus.postValue("Charge status : $result")
                        } ?: let {
                            _textDeviceMonitorChargeStatus.postValue("Charge status : Unknown")
                        }

                        _textLog.value = _textLog.value + "queryMonitorChargeStatus() addOnSuccessListener() Power status = ${monitorData.asInt()}\n"
                        Log.i(LOG_TAG, "queryMonitorChargeStatus() addOnSuccessListener() Power status = ${monitorData.asInt()}")
                    }
                    .addOnFailureListener { execption ->
                        _textDeviceMonitorChargeStatus.postValue("Charge status : Unknown")

                        _textLog.value = _textLog.value + "queryMonitorChargeStatus() addOnFailureListener() execption = $execption" + "\n"
                        Log.e(LOG_TAG, "queryMonitorChargeStatus() addOnFailureListener() : ", execption)
                    }
            }
        } ?: let {
            _textDeviceMonitorChargeStatus.postValue("Charge status : Unknown")

            _textLog.value = _textLog.value + "queryMonitorChargeStatus() connectedDevice is null\n"
            Log.i(LOG_TAG, "queryMonitorChargeStatus() connectedDevice is null")
        }
    }

    // 监听设备状态的监听器
    // A listener to monitor the device status
    val monitorListener = MonitorListener { errorCode, monitorItem, monitorData ->
        when (monitorItem.name) {
            // 电池状态
            // battery status
            MonitorItem.MONITOR_ITEM_LOW_POWER.name -> {
                _textDeviceMonitorPowerStatus.postValue("Power status : ${monitorData.asInt()}")
            }
            // 充电状态
            // charging status
            MonitorItem.MONITOR_CHARGE_STATUS.name -> {
                val result = MONITOR_CHARGE_STATUS_DETAIL[monitorData.asInt()]
                result?.let { let ->
                    _textDeviceMonitorChargeStatus.postValue("Charge status : $result")
                } ?: let {
                    _textDeviceMonitorChargeStatus.postValue("Charge status : Unknown")
                }
            }
            else -> {

            }
        }
    }

    // 注册监听器る
    // Register the listener
    private fun registerMonitor(context: Context, monitorItemArray: List<MonitorItem>) {
        unregisterMonitor()

        connectedDevice?.let { connectedDevice ->
            if (!connectedDevice.isConnected) {
                _textLog.value = _textLog.value + "registerMonitor() connectedDevice.isConnected = $connectedDevice.isConnected\n"
                Log.i(LOG_TAG, "registerMonitor() connectedDevice.isConnected = $connectedDevice.isConnected")
            } else {
                HiWear.getMonitorClient(context)
                    .register(connectedDevice, monitorItemArray, monitorListener)
                    .addOnSuccessListener {
                        _textLog.value = _textLog.value + "registerMonitor() addOnSuccessListener()\n"
                        Log.i(LOG_TAG, "registerMonitor() addOnSuccessListener()")
                    }
                    .addOnFailureListener { execption ->
                        _textLog.value = _textLog.value + "registerMonitor() addOnFailureListener() execption = $execption" + "\n"
                        Log.e(LOG_TAG, "registerMonitor() addOnFailureListener() : ", execption)
                    }
            }
        } ?: let {
            _textLog.value = _textLog.value + "registerMonitor() connectedDevice is null\n"
            Log.i(LOG_TAG, "registerMonitor() connectedDevice is null")
        }
    }

    // 移除监听器
    // Remove the listener
    fun unregisterMonitor() {
        _textLog.value = _textLog.value + "unregisterMonitor()\n"
        Log.i(LOG_TAG, "unregisterMonitor()")
    }

    // 检查目标应用是否安装在设备上
    // Check if the target app is installed on the device
    private fun checkApp(context: Context) {
        connectedDevice?.let { connectedDevice ->
            if (!connectedDevice.isConnected) {
                _textLog.value = _textLog.value + "checkApp() connectedDevice.isConnected = $connectedDevice.isConnected\n"
                Log.i(LOG_TAG, "checkApp() connectedDevice.isConnected = $connectedDevice.isConnected")
            } else {
                // 首先，检查设备上是否安装了智能表应用程序
                // First, check if the Wearable app is installed on the device.
                HiWear.getP2pClient(context)
                    .isAppInstalled(connectedDevice, Constant.WEARABLE_APP_PACKAGE_NAME)
                    .addOnSuccessListener { isAppInstalled ->
                        // 如果已经安装
                        // If installed
                        if (isAppInstalled) {
                            _textDeviceType.postValue("Device type : Wearable (Smart watch)")

                            _textLog.value = _textLog.value + "checkApp() addOnSuccessListener() Wearable (Smart watch)\n"
                            Log.i(LOG_TAG, "checkApp() addOnSuccessListener() Wearable (Smart watch)")

                            _textIsAppInstalled.postValue(Constant.WEARABLE_APP_PACKAGE_NAME + " : Installed")

                            _textLog.value = _textLog.value + "checkApp() addOnSuccessListener() ${Constant.WEARABLE_APP_PACKAGE_NAME} = $isAppInstalled\n"
                            Log.i(LOG_TAG, "checkApp() addOnSuccessListener() ${Constant.WEARABLE_APP_PACKAGE_NAME} = $isAppInstalled")

                            // 假设设备是智能表
                            // 使用智能表应用的包名称和指纹。
                            // Assume the device is a wearable
                            // Use the package name and fingerprint of the wearable app.
                            targetWatchAppPackageName = Constant.WEARABLE_APP_PACKAGE_NAME
                            targetWatchAppFingerPrint = Constant.WEARABLE_APP_FINGER_PRINT

                            // 尝试与设备通信（尝试 ping）
                            // Try to communicate with the device (try pinging)
                            targetWatchAppPackageName?.let { packageName ->
                                ping(context, packageName)
                            }
                        } else {
                            // 检查设备上是否安装了运动表应用
                            // Check if the Lite Wearable app is installed on the device
                            HiWear.getP2pClient(context)
                                .isAppInstalled(connectedDevice, Constant.LITE_WEARABLE_APP_PACKAGE_NAME)
                                .addOnSuccessListener { isAppInstalled ->
                                    // 如果安装了
                                    // If installed
                                    if (isAppInstalled) {
                                        _textDeviceType.postValue("Device type : Lite wearable (Sport watch)")

                                        _textLog.value = _textLog.value + "checkApp() addOnSuccessListener() Lite wearable (Sport watch)\n"
                                        Log.i(LOG_TAG, "checkApp() addOnSuccessListener() Lite wearable (Sport watch)")

                                        _textIsAppInstalled.postValue(Constant.LITE_WEARABLE_APP_PACKAGE_NAME + " : Installed")

                                        _textLog.value = _textLog.value + "checkApp() addOnSuccessListener() ${Constant.LITE_WEARABLE_APP_PACKAGE_NAME} = $isAppInstalled\n"
                                        Log.i(LOG_TAG, "checkApp() addOnSuccessListener() ${Constant.LITE_WEARABLE_APP_PACKAGE_NAME} = $isAppInstalled")

                                        // 假设设备是运动表
                                        // 使用运动表应用的包名和指纹。
                                        // Assume the device is a Lite Wearable
                                        // Use the package name and fingerprint of the Lite Wearable app.
                                        targetWatchAppPackageName = Constant.LITE_WEARABLE_APP_PACKAGE_NAME
                                        targetWatchAppFingerPrint = Constant.LITE_WEARABLE_APP_FINGER_PRINT

                                        // 尝试与设备通信（尝试 ping）
                                        // Try to communicate with the device (try ping)
                                        targetWatchAppPackageName?.let { packageName ->
                                            ping(context, packageName)
                                        }
                                    } else
                                    // 无法确定设备是智能表还是运动表
                                    // Unable to determine if device is Wearable or Lite Wearable
                                    {
                                        _textDeviceType.postValue("Device type : Unknown")
                                        _textIsAppInstalled.postValue(Constant.WEARABLE_APP_PACKAGE_NAME + " & " + Constant.LITE_WEARABLE_APP_PACKAGE_NAME + " : Not exist")
                                    }
                                }
                                .addOnFailureListener { execption ->
                                    _textDeviceType.postValue("Device type : Unknown")
                                    _textIsAppInstalled.postValue(Constant.WEARABLE_APP_PACKAGE_NAME + " & " + Constant.LITE_WEARABLE_APP_PACKAGE_NAME + " : Not exist")

                                    _textLog.value = _textLog.value + "checkApp() addOnFailureListener() execption = $execption" + "\n"
                                    Log.e(LOG_TAG, "checkApp() addOnFailureListener() : ", execption)
                                }
                        }
                    }
                    .addOnFailureListener { execption ->
                        _textDeviceType.postValue("Device type : Unknown")
                        _textIsAppInstalled.postValue(Constant.WEARABLE_APP_PACKAGE_NAME + " & " + Constant.LITE_WEARABLE_APP_PACKAGE_NAME + " : Not exist")

                        _textLog.value = _textLog.value + "checkApp() addOnFailureListener() execption = $execption" + "\n"
                        Log.e(LOG_TAG, "checkApp() addOnFailureListener() : ", execption)
                    }
            }
        } ?: let {
            _textDeviceType.postValue("Device type : Unknown")
            _textIsAppInstalled.postValue(Constant.WEARABLE_APP_PACKAGE_NAME + " & " + Constant.LITE_WEARABLE_APP_PACKAGE_NAME + " : Not exist")

            Log.i(LOG_TAG, "checkApp() connectedDevice is null")
        }
    }

    // 尝试与设备通信（尝试 ping）
    // Try to communicate with the device (try ping)
    private fun ping(context: Context, packageName: String) {
        connectedDevice?.let { connectedDevice ->
            if (!connectedDevice.isConnected) {
                _textLog.value = _textLog.value + "ping() connectedDevice.isConnected = $connectedDevice.isConnected\n"
                Log.i(LOG_TAG, "ping() connectedDevice.isConnected = $connectedDevice.isConnected")
            } else {
                HiWear.getP2pClient(context)
                    .setPeerPkgName(packageName)
                    .ping(connectedDevice, object : PingCallback {
                        override fun onPingResult(errCode: Int) {
                            _textPing.postValue("Ping : $errCode - ${WearEngineErrorCode.getErrorMsgFromCode(errCode)}")
                        }
                    })
                    .addOnSuccessListener {
                        _textLog.value = _textLog.value + "ping() addOnSuccessListener()\n"
                        Log.i(LOG_TAG, "ping() addOnSuccessListener()")
                    }
                    .addOnFailureListener { execption ->
                        _textPing.postValue("Ping : Unknown")

                        _textLog.value = _textLog.value + "ping() addOnFailureListener() execption = $execption" + "\n"
                        Log.e(LOG_TAG, "ping() addOnFailureListener() : ", execption)
                    }
            }
        } ?: let {
            _textPing.postValue("Ping : Unknown")

            _textLog.value = _textLog.value + "ping() connectedDevice is null\n"
            Log.i(LOG_TAG, "ping() connectedDevice is null")
        }
    }

    // 智能手机不断发送 ping 信号，以防止手表应用程序被终止
    // The smartphone keeps sending pings to prevent the watch app from being terminated
    fun startPingTimer(context: Context, period: Long) {
        targetWatchAppPackageName?.let { packageName ->
            stopPingTimer()

            pingTimer = timer(period = period) {
                ping(context, packageName)
            }
        }
    }

    // 停止发送 PING
    // Stop sending PING
    fun stopPingTimer() {
        pingTimer?.let { pingTimer ->
            pingTimer.cancel()
        }
        pingTimer = null
    }
}