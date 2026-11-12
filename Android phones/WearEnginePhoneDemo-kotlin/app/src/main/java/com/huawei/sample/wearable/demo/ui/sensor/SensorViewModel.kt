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

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.sensor.DataResult
import com.huawei.wearengine.sensor.Sensor
import com.huawei.wearengine.sensor.SensorReadCallback
import com.huawei.wearengine.sensor.SensorStopCallback

class SensorViewModel : ViewModel() {

    companion object {
        private val LOG_TAG = SensorViewModel::class.java.simpleName

        private const val MAX_LOG_LENGTH = 32 * 1024
    }

    private val _textLog = MutableLiveData<String>().apply {
        value = ""
    }
    val textLog: LiveData<String> = _textLog

    // 传感器回调频率高，用 StringBuilder 同步累积，避免 postValue 读改写丢行
    // Sensor callbacks are frequent; accumulate synchronously to avoid losing lines
    private val logBuffer = StringBuilder()

    private val _sensorList = MutableLiveData<List<Sensor>>().apply {
        value = emptyList()
    }
    val sensorList: LiveData<List<Sensor>> = _sensorList

    private var currentDevice: Device? = null
    private var selectedSensor: Sensor? = null

    fun setDevice(device: Device?) {
        currentDevice = device
    }

    fun setSelectedSensor(index: Int) {
        val list = _sensorList.value ?: return
        if (index >= 0 && index < list.size) {
            selectedSensor = list[index]
        } else {
            addLog("checked sensor item is error.")
        }
    }

    // 获取传感器列表
    // Obtain the sensor list
    fun getSensorList(context: Context) {
        val device = currentDevice
        if (device == null || !device.isConnected) {
            addLog("getSensorList: The currentDevice status is error.")
            return
        }
        HiWear.getSensorClient(context)
            .getSensorList(device)
            .addOnSuccessListener { sensors ->
                if (sensors.isNullOrEmpty()) {
                    addLog("getSensorList list is null or list size is 0.")
                    return@addOnSuccessListener
                }
                val supported = sensors.filter {
                    Sensor.NAME_ACC == it.name || Sensor.NAME_PPG == it.name
                }
                supported.forEach { sensor ->
                    addLog("sensor name:${sensor.name}")
                    addLog("sensor id:${sensor.id}")
                }
                _sensorList.postValue(supported)
                selectedSensor = supported.firstOrNull()
                addLog("getSensorList size = ${supported.size}")
            }
            .addOnFailureListener { exception ->
                addLog("getSensorList Exception.")
                Log.e(LOG_TAG, "getSensorList() addOnFailureListener() : ", exception)
            }
    }

    // 异步读取传感器数据
    // Asynchronous reading of the sensor data
    fun asyncReadSensorData(context: Context) {
        val device = currentDevice
        val sensor = selectedSensor
        if (sensor == null || device == null || !device.isConnected) {
            addLog("asyncReadSensorData: The device or sensor object is empty, or the device is disconnected.")
            return
        }
        val callback = object : SensorReadCallback {
            override fun onReadResult(errorCode: Int, dataResult: DataResult?) {
                addLog("asyncReadSensorData errorCode:$errorCode")
                if (dataResult == null || dataResult.sensor == null) {
                    return
                }
                val resultSensor = dataResult.sensor
                addLog("sensor name:${resultSensor.name}")
                when (resultSensor.type) {
                    Sensor.TYPE_PPG -> {
                        dataResult.asList()?.forEach { item ->
                            addLog("PPG data:${item.asFloats()?.joinToString()}")
                        }
                    }
                    Sensor.TYPE_ACC -> {
                        addLog("ACC data:${dataResult.asFloats()?.joinToString()}")
                    }
                    else -> {
                        addLog("data:${dataResult.asFloats()?.joinToString()}")
                    }
                }
            }
        }
        HiWear.getSensorClient(context)
            .asyncRead(device, sensor, callback)
            .addOnSuccessListener {
                addLog("asyncRead task submission success sensor name is:${sensor.name}")
            }
            .addOnFailureListener { exception ->
                addLog("asyncRead task error. sensor name is:${sensor.name}")
                Log.e(LOG_TAG, "asyncRead() addOnFailureListener() : ", exception)
            }
    }

    // 停止异步读取传感器数据
    // Stop the asynchronous reading of the sensor data
    fun stopReadSensorData(context: Context) {
        val device = currentDevice
        val sensor = selectedSensor
        if (sensor == null || device == null || !device.isConnected) {
            addLog("stopReadSensorData: The device or sensor object is empty, or the device is disconnected.")
            return
        }
        val callback = object : SensorStopCallback {
            override fun onStopResult(errorCode: Int) {
                addLog("stopAsyncRead result:$errorCode")
            }
        }
        HiWear.getSensorClient(context)
            .stopAsyncRead(device, sensor, callback)
            .addOnSuccessListener {
                addLog("stopAsyncRead task submission success sensor name is:${sensor.name}")
            }
            .addOnFailureListener { exception ->
                addLog("stopAsyncRead task error. sensor name is:${sensor.name}")
                Log.e(LOG_TAG, "stopAsyncRead() addOnFailureListener() : ", exception)
            }
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
            // 限制日志长度，避免长时间读取传感器导致内存无限增长
            // Cap the log size so long sensor reads do not grow memory without bound
            if (logBuffer.length > MAX_LOG_LENGTH) {
                logBuffer.delete(0, logBuffer.length - MAX_LOG_LENGTH)
            }
            logBuffer.toString()
        }
        _textLog.postValue(snapshot)
    }
}
