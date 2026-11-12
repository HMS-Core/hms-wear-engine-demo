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

package com.huawei.sample.wearable.demo.ui.action

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
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.huawei.sample.wearable.demo.databinding.FragmentActionBinding
import com.huawei.sample.wearable.demo.service.MainService
import org.json.JSONException
import org.json.JSONObject
import java.util.Timer

class ActionFragment : Fragment() {

    companion object {
        private val LOG_TAG = ActionFragment::class.java.simpleName

        private const val ACTION_OPEN = "ACTION_OPEN"
        private const val ACTION_UP = "ACTION_UP"
        private const val ACTION_LEFT = "ACTION_LEFT"
        private const val ACTION_RIGHT = "ACTION_RIGHT"
        private const val ACTION_DOWN = "ACTION_DOWN"

        private const val PING_PERIOD: Long = 1000 * 30
    }

    private var _binding: FragmentActionBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var messenger: Messenger? = null

    private lateinit var actionViewModel: ActionViewModel

    private var timer: Timer? = null

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
                            actionViewModel.addLog(displayText)
                        }
                    }
                MainService.MESSAGE_SEND_JSON_FROM_PHONE_TO_WATCH_FAIL
                    -> {
                        Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}")
                        actionViewModel.addLog("Send json fail")
                    }
                MainService.MESSAGE_WEAR_ENGINE_INIT_FINISH
                    -> {
                        Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}")
                        startPingTimer(PING_PERIOD)

                        // 本来ウォッチ側のWear Engineの初期化が完了するまで待たなければならないが、このサンプルでは簡略化のため、１秒間隔でタスクを５回繰り返すことにする
                        runRepeatedTask(5, 1000) {
                            sendAction(ACTION_OPEN)
                        }
                    }
                else
                    -> {
                        Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}")
                        super.handleMessage(message)
                    }
            }
        }
    }

    private lateinit var actionFragmentMessenger: Messenger

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 先初始化 ViewModel，再创建 Handler，避免消息先于 ViewModel 到达导致 lateinit 崩溃
        actionViewModel = ViewModelProvider(this).get(ActionViewModel::class.java)

        actionFragmentMessenger = Messenger(IncomingHandler(requireContext()))

        _binding = FragmentActionBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textLog: TextView = binding.textLog
        actionViewModel.textLog.observe(viewLifecycleOwner) {
            textLog.text = it
        }

        val buttonUp: Button = binding.buttonUp
        buttonUp.setOnClickListener {
            sendAction(ACTION_UP)
        }

        val buttonLeft: Button = binding.buttonLeft
        buttonLeft.setOnClickListener {
            sendAction(ACTION_LEFT)
        }

        val buttonRight: Button = binding.buttonRight
        buttonRight.setOnClickListener {
            sendAction(ACTION_RIGHT)
        }

        val buttonDown: Button = binding.buttonDown
        buttonDown.setOnClickListener {
            sendAction(ACTION_DOWN)
        }

        connectService()

        return root
    }

    override fun onStart() {
        super.onStart()

        startPingTimer(PING_PERIOD)

            sendAction(ACTION_OPEN)
    }

    override fun onStop() {
        super.onStop()
        stopPingTimer()
        timer?.cancel()
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
                    putExtra(ActionFragment::class.java.simpleName, actionFragmentMessenger)
                }

            requireActivity().applicationContext.startService(intent)
            Log.i(LOG_TAG, "connectService() startService")

            requireActivity().applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.i(LOG_TAG, "connectService() bindService Context.BIND_AUTO_CREATE, extra = " + ActionFragment::class.java.simpleName)
        }
    }

    private fun disconnectService() {
        messenger?.let { messenger ->
            requireActivity().unbindService(serviceConnection)
        }
    }

    private fun sendAction(action: String) {
        messenger?.let { messenger ->
            val message = Message.obtain(null, MainService.MESSAGE_SEND_ACTION_FROM_PHONE_TO_WATCH)
                .apply {
                    data = Bundle().apply {
                        putString(MainService.KEY_DATA, action)
                    }
                }
            try {
                messenger.send(message)
            } catch (remoteException: RemoteException) {
                Log.e(LOG_TAG, "sendAction() exception : ", remoteException)
            }
        }
    }

    private fun startPingTimer(period: Long) {
        messenger?.let { messenger ->
            val message = Message.obtain(null, MainService.MESSAGE_SEND_AUTO_PING_FROM_PHONE_TO_WATCH)
                .apply {
                    data = Bundle().apply {
                        putLong(MainService.KEY_DATA, period)
                    }
                }
            try {
                messenger.send(message)
            } catch (remoteException: RemoteException) {
                Log.e(LOG_TAG, "startPingTimer() exception : ", remoteException)
            }
        }
    }

    private fun stopPingTimer() {
        messenger?.let { messenger ->
            val message = Message.obtain(null, MainService.MESSAGE_STOP_SEND_AUTO_PING_FROM_PHONE_TO_WATCH)
            try {
                messenger.send(message)
            } catch (remoteException: RemoteException) {
                Log.e(LOG_TAG, "stopPingTimer() exception : ", remoteException)
            }
        }
    }

    fun runRepeatedTask(times: Int, intervalMillis: Long, task: () -> Unit) {
        timer?.cancel()

        var count = 0
        timer = Timer()

        timer?.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                task()
                count++
                if (count >= times) {
                    timer?.cancel()
                }
            }
        }, intervalMillis, intervalMillis)
    }
}