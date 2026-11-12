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

package com.huawei.sample.wearable.demo.ui.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.huawei.sample.wearable.demo.databinding.FragmentDataBinding
import com.huawei.sample.wearable.demo.service.MainService
import org.json.JSONException
import org.json.JSONObject

class DataFragment : Fragment() {

    companion object {
        private val LOG_TAG = DataFragment::class.java.simpleName
    }

    private var _binding: FragmentDataBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var messenger: Messenger? = null

    private lateinit var dataViewModel: DataViewModel

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messenger = Messenger(service)
            Log.i(LOG_TAG, "onServiceConnected() name = ${name?.shortClassName}")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messenger = null
            Log.i(LOG_TAG, "onServiceDisconnected() name = ${name?.shortClassName}")
        }
    }

    inner class IncomingHandler(
        context: Context,
        private val applicationContext: Context = context.applicationContext
    ) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            Log.i(LOG_TAG, "handleMessage() message.what = " + message.what)

            when (message.what) {
                MainService.MESSAGE_SEND_JSON_FROM_PHONE_TO_WATCH_SUCCESS,
                MainService.MESSAGE_SEND_TEXT_FROM_WATCH_TO_PHONE
                    -> {
                        val dataString = message.data?.getString(MainService.KEY_DATA)
                        dataString?.let { dataString ->
                            Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, dataString = $dataString")

                            // 手表端可能发送非 JSON 的纯文本，解析失败时原样展示，避免崩溃
                            val displayText = try {
                                JSONObject(dataString).optString(MainService.MESSAGE_DATA_KEY, dataString)
                            } catch (jsonException: JSONException) {
                                Log.w(LOG_TAG, "handleMessage() payload is not json, show raw text", jsonException)
                                dataString
                            }
                            dataViewModel.addText(displayText)
                        }
                    }
                MainService.MESSAGE_SEND_JSON_FROM_PHONE_TO_WATCH_FAIL
                    -> {
                        Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}")
                        dataViewModel.addText("Send text fail")
                    }
                else
                    -> {
                        Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}")
                        super.handleMessage(message)
                    }
            }
        }
    }

    private lateinit var dataFragmentMessenger: Messenger

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 先初始化 ViewModel，再创建 Handler，避免消息先于 ViewModel 到达导致 lateinit 崩溃
        dataViewModel = ViewModelProvider(this).get(DataViewModel::class.java)

        dataFragmentMessenger = Messenger(IncomingHandler(requireContext()))

        _binding = FragmentDataBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textData: TextView = binding.textData
        dataViewModel.textData.observe(viewLifecycleOwner) {
            textData.text = it
        }

        val buttonSendText: Button = binding.buttonSendText
        buttonSendText.setOnClickListener {
            val inputData: EditText = binding.inputData
            if (inputData.text.toString().trim().isNotEmpty()) {
                sendText(inputData.text.toString().trim())
                inputData.setText("")
            }
        }

        connectService()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun connectService() {
        Log.i(LOG_TAG, "connectService() messenger = $messenger")
        if (null == messenger) {
            val intent = Intent(requireContext(), MainService::class.java)
                .apply {
                    putExtra(DataFragment::class.java.simpleName, dataFragmentMessenger)
                }

            requireActivity().applicationContext.startService(intent)
            Log.i(LOG_TAG, "connectService() startService")

            requireActivity().applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.i(LOG_TAG, "connectService() bindService Context.BIND_AUTO_CREATE, extra = " + DataFragment::class.java.simpleName)
        }
    }

    private fun disconnectService() {
        messenger?.let { messenger ->
            requireActivity().unbindService(serviceConnection)
        }
    }

    private fun sendText(text: String) {
        messenger?.let { messenger ->
            val message = Message.obtain(null, MainService.MESSAGE_SEND_TEXT_FROM_PHONE_TO_WATCH)
                .apply {
                    data = Bundle().apply {
                        putString(MainService.KEY_DATA, text)
                    }
                }
            try {
                messenger.send(message)
            } catch (remoteException: RemoteException) {
                Log.e(LOG_TAG, "sendText() exception : ", remoteException)
            }
        }
    }
}