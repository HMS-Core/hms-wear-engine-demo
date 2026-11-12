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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.huawei.sample.wearable.demo.databinding.FragmentDeviceBinding
import com.huawei.sample.wearable.demo.ui.notify.NotifyActivity
import com.huawei.sample.wearable.demo.ui.sensor.SensorActivity

class DeviceFragment : Fragment() {

    private var _binding: FragmentDeviceBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var deviceViewModel: DeviceViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        deviceViewModel = ViewModelProvider(this).get(DeviceViewModel::class.java)

        _binding = FragmentDeviceBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textHasAvailableDevices: TextView = binding.textHasAvailableDevices
        deviceViewModel.textHasAvailableDevices.observe(viewLifecycleOwner) {
            textHasAvailableDevices.text = it
        }

        val textCheckPermission: TextView = binding.textCheckPermission
        deviceViewModel.textCheckPermission.observe(viewLifecycleOwner) {
            textCheckPermission.text = it
        }

        val textDeviceName: TextView = binding.textDeviceName
        deviceViewModel.textDeviceName.observe(viewLifecycleOwner) {
            textDeviceName.text = it
        }

        val textDeviceUuid: TextView = binding.textDeviceUuid
        deviceViewModel.textDeviceUuid.observe(viewLifecycleOwner) {
            textDeviceUuid.text = it
        }

        val textDeviceAvailableStorage: TextView = binding.textDeviceAvailableStorage
        deviceViewModel.textDeviceAvailableStorage.observe(viewLifecycleOwner) {
            textDeviceAvailableStorage.text = it
        }

        val textDeviceMonitorPowerStatus: TextView = binding.textDeviceMonitorPowerStatus
        deviceViewModel.textDeviceMonitorPowerStatus.observe(viewLifecycleOwner) {
            textDeviceMonitorPowerStatus.text = it
        }

        val textDeviceMonitorChargeStatus: TextView = binding.textDeviceMonitorChargeStatus
        deviceViewModel.textDeviceMonitorChargeStatus.observe(viewLifecycleOwner) {
            textDeviceMonitorChargeStatus.text = it
        }

        val textDeviceType: TextView = binding.textDeviceType
        deviceViewModel.textDeviceType.observe(viewLifecycleOwner) {
            textDeviceType.text = it
        }

        val textIsAppInstalled: TextView = binding.textIsAppInstalled
        deviceViewModel.textIsAppInstalled.observe(viewLifecycleOwner) {
            textIsAppInstalled.text = it
        }

        val textPing: TextView = binding.textPing
        deviceViewModel.textPing.observe(viewLifecycleOwner) {
            textPing.text = it
        }


        val textLog: TextView = binding.textLog
        deviceViewModel.textLog.observe(viewLifecycleOwner) {
            textLog.text = it
        }

        val buttonUpdate: Button = binding.buttonCheckStatus
        buttonUpdate.setOnClickListener {
            deviceViewModel.hasAvailableDevices(requireContext())
            deviceViewModel.getConnectedDevice(requireContext())
        }

        val switchWeakup: SwitchMaterial = binding.switchWeakup
        switchWeakup.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                deviceViewModel.startPingTimer(requireContext(), DeviceViewModel.PING_PERIOD)
            } else {
                deviceViewModel.stopPingTimer()
            }
        }

        binding.buttonSensor.setOnClickListener {
            val device = deviceViewModel.currentDevice()
            if (device == null || !device.isConnected) {
                deviceViewModel.addLog("No available device or the device has not been connected.")
                return@setOnClickListener
            }
            startActivity(
                Intent(requireContext(), SensorActivity::class.java)
                    .putExtra(SensorActivity.EXTRA_CURRENT_DEVICE, device)
            )
        }

        binding.buttonNotify.setOnClickListener {
            val device = deviceViewModel.currentDevice()
            if (device == null || !device.isConnected) {
                deviceViewModel.addLog("No available device or the device has not been connected.")
                return@setOnClickListener
            }
            startActivity(
                Intent(requireContext(), NotifyActivity::class.java)
                    .putExtra(NotifyActivity.EXTRA_CURRENT_DEVICE, device)
                    .putExtra(NotifyActivity.EXTRA_PACKAGE_NAME, deviceViewModel.targetWatchAppPackageName())
            )
        }

        return root
    }

    override fun onStart() {
        super.onStart()
        deviceViewModel.checkPermission(requireContext())
    }

    override fun onPause() {
        super.onPause()

        val deviceViewModel =
            ViewModelProvider(this).get(DeviceViewModel::class.java)

        deviceViewModel.unregisterMonitor()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}