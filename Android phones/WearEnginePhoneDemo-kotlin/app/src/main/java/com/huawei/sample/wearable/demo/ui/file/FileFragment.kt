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

package com.huawei.sample.wearable.demo.ui.file

import android.Manifest
import android.app.Activity
import android.graphics.BitmapFactory
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.huawei.sample.wearable.demo.R
import com.huawei.sample.wearable.demo.databinding.FragmentFileBinding
import com.huawei.sample.wearable.demo.model.UriUtil
import com.huawei.sample.wearable.demo.service.MainService
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files


class FileFragment : Fragment() {

    companion object {
        private val LOG_TAG = FileFragment::class.java.simpleName

        // 图片预览的最大解码尺寸（像素），避免大图 OOM
        private const val MAX_IMAGE_SIZE = 1080

        private val PERMISSION_LIST: List<String> = listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
    }

    private var _binding: FragmentFileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var messenger: Messenger? = null

    private lateinit var fileViewModel: FileViewModel

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
                MainService.MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_PROGRESS,
                    -> {
                        val text = parsePayload(message)
                        text?.let { text ->
                            Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, text = $text")
                            fileViewModel.addText("Send progress: $text")
                        }
                    }
                MainService.MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_SUCCESS,
                    -> {
                        val text = parsePayload(message)
                        text?.let { text ->
                            Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, text = $text")
                            fileViewModel.addText("Send success: $text")
                        }
                        showSendButton()
                    }
                MainService.MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL,
                    -> {
                        val text = parsePayload(message)
                        text?.let { text ->
                            Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, text = $text")
                            fileViewModel.addText("Send fail: $text")
                        }
                        showSendButton()
                    }
                MainService.MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH_SUCCESS
                    -> {
                        val text = parsePayload(message)
                        text?.let { text ->
                            Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, text = $text")
                            fileViewModel.addText("Stop send success: $text")
                        }
                        showSendButton()
                    }
                MainService.MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH_FAIL,
                    -> {
                        val text = parsePayload(message)
                        text?.let { text ->
                            Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, text = $text")
                            fileViewModel.addText("Stop send fail: $text")
                        }
                        showSendButton()
                    }
                MainService.MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH_RESULT
                    -> {
                        val text = parsePayload(message)
                        text?.let { text ->
                            Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, text = $text")
                            fileViewModel.addText("Stop send result: $text")
                        }
                        showSendButton()
                    }
                MainService.MESSAGE_SEND_FILE_FROM_WATCH_TO_PHONE,
                    -> {
                        val path = parsePayload(message)
                        path?.let { path ->
                            Log.i(LOG_TAG, "handleMessage() message.what = ${message.what}, text = $path")
                            fileViewModel.addText("Receive file: $path")
                            // 收到文件不再自动弹出预览，仅记录路径，由用户点击后查看
                            // Do not pop up the preview automatically; only record the path
                            fileViewModel.setReceivedPath(path)
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

    // 解析 MainService 回推的载荷：正常为 {"type":2,"data":"..."}。
    // 若对端发送的是非 JSON 纯文本，解析失败时原样返回，避免崩溃。
    private fun parsePayload(message: Message): String? {
        val raw = message.data?.getString(MainService.KEY_DATA) ?: return null
        return try {
            JSONObject(raw).optString(MainService.MESSAGE_DATA_KEY, raw)
        } catch (jsonException: JSONException) {
            Log.w(LOG_TAG, "parsePayload() payload is not json, use raw text", jsonException)
            raw
        }
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Activity.RESULT_OK == result.resultCode) {
            result.data?.let { data ->
                // data.data 可能为空，强转会抛 ClassCastException / NPE
                val uri: Uri = data.data ?: return@let
                val path = UriUtil.getPath(requireContext(), uri)
                path?.let { path ->
                    if (path.indexOf("storage") >= 0) {
                        fileViewModel.setSelectPath(path)
                        fileViewModel.addText(path)

                        // 页面可能已销毁，需判空
                        _binding?.inputData?.text = path
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.choose_from_storage), Toast.LENGTH_SHORT).show()
                    }
                } ?:let {
                    Toast.makeText(requireContext(), getString(R.string.choose_from_storage), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->

    }

    private lateinit var fileFragmentMessenger: Messenger

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 先初始化 ViewModel，再创建 Handler，避免消息先于 ViewModel 到达导致 lateinit 崩溃
        fileViewModel = ViewModelProvider(this).get(FileViewModel::class.java)

        fileFragmentMessenger = Messenger(IncomingHandler(requireContext()))

        _binding = FragmentFileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val buttonSelectFile: Button = binding.buttonSelectFile
        buttonSelectFile.setOnClickListener {
            selectImageFile()
        }

        val textData: TextView = binding.textData
        fileViewModel.textData.observe(viewLifecycleOwner) {
            textData.text = it
        }

        val buttonSendFile: Button = binding.buttonSendFile
        buttonSendFile.setOnClickListener {
            sendFile(fileViewModel.selectPath.value)
        }

        val buttonStopSend: Button = binding.buttonStopSend
        buttonStopSend.setOnClickListener {
            stopSend(fileViewModel.selectPath.value)
        }

        // 收到文件后由用户主动点击查看，避免自动弹出且无法关闭
        // Let the user open the received file manually instead of popping it up automatically
        binding.buttonViewReceived.setOnClickListener {
            fileViewModel.receivedPath.value?.let { path ->
                if (path.isNotEmpty()) {
                    showImage(path)
                }
            }
        }

        // 关闭预览，隐藏图片并释放 Bitmap
        // Close the preview, hide the image and release the bitmap
        binding.buttonCloseImage.setOnClickListener {
            hideImage()
        }

        fileViewModel.receivedPath.observe(viewLifecycleOwner) { path ->
            binding.buttonViewReceived.visibility = if (path.isNotEmpty()) View.VISIBLE else View.GONE
        }

        showSendButton()
        connectService()

        return root
    }

    override fun onStart() {
        super.onStart()
        checkAndAskPermission()
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
                    putExtra(FileFragment::class.java.simpleName, fileFragmentMessenger)
                }

            requireActivity().applicationContext.startService(intent)
            Log.i(LOG_TAG, "connectService() startService")

            requireActivity().applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.i(LOG_TAG, "connectService() bindService Context.BIND_AUTO_CREATE, extra = " + FileFragment::class.java.simpleName)
        }
    }

    private fun disconnectService() {
        messenger?.let { messenger ->
            requireActivity().unbindService(serviceConnection)
        }
    }

    private fun checkAndAskPermission() {
        context?.let { context ->
            val permissionList: MutableList<String> = mutableListOf()

            PERMISSION_LIST.forEach { permission ->
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(permission)
                }
            }

            if (!permissionList.isNullOrEmpty()) {
                requestPermissionLauncher.launch(permissionList.toTypedArray())
            }
        }
    }

    private fun selectImageFile() {
        val intent: Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
        }
        activityResultLauncher.launch(intent)
    }

    private fun sendFile(path: String) {
        messenger?.let { messenger ->
            val message = Message.obtain(null, MainService.MESSAGE_SEND_FILE_FROM_PHONE_TO_WATCH_START)
                .apply {
                    data = Bundle().apply {
                        putString(MainService.KEY_DATA, path)
                        fileViewModel.addText("Start send: " + path)
                    }
                }
            try {
                messenger.send(message)
                showStopButton()
            } catch (remoteException: RemoteException) {
                Log.e(LOG_TAG, "sendFile() exception : ", remoteException)
            }
        }
    }

    private fun stopSend(path: String) {
        messenger?.let { messenger ->
            val message = Message.obtain(null, MainService.MESSAGE_STOP_SEND_FILE_FROM_PHONE_TO_WATCH)
                .apply {
                    data = Bundle().apply {
                        putString(MainService.KEY_DATA, path)
                        fileViewModel.addText("Start stop send: " + path)
                    }
                }
            try {
                messenger.send(message)
                showSendButton()
            } catch (remoteException: RemoteException) {
                Log.e(LOG_TAG, "stopSend() exception : ", remoteException)
            }
        }
    }

    private fun showSendButton() {
        // 页面可能已销毁（Tab 切换），此时 Service 仍会回推消息，需判空避免崩溃
        val binding = _binding ?: return
        val buttonSendFile: Button = binding.buttonSendFile
        val buttonStopSend: Button = binding.buttonStopSend
        buttonSendFile.visibility = View.VISIBLE
        buttonStopSend.visibility = View.GONE
    }

    private fun showStopButton() {
        // 页面可能已销毁，需判空避免崩溃
        val binding = _binding ?: return
        val buttonSendFile: Button = binding.buttonSendFile
        val buttonStopSend: Button = binding.buttonStopSend
        buttonStopSend.visibility = View.VISIBLE
        buttonSendFile.visibility = View.GONE
    }

    private fun showImage(path: String) {
        // 页面可能已销毁（Tab 切换），此时 Service 仍会回推消息，需判空避免崩溃
        val binding = _binding ?: return

        val file = File(path)
        if (!file.exists()) {
            Log.w(LOG_TAG, "showImage() file not exist : $path")
            return
        }

        // 仅对图片文件尝试预览，非图片文件解码必然失败
        val lowerPath = path.lowercase()
        val isImage = lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") ||
            lowerPath.endsWith(".png") || lowerPath.endsWith(".webp") || lowerPath.endsWith(".bmp")
        if (!isImage) {
            Log.i(LOG_TAG, "showImage() not an image file : $path")
            return
        }

        // 按采样率解码，避免大图直接解码导致 OOM 崩溃
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
            options.inJustDecodeBounds = false

            val bitmap = BitmapFactory.decodeFile(path, options)
            if (bitmap != null) {
                binding.imageData.setImageBitmap(bitmap)
                binding.imageData.visibility = View.VISIBLE
                binding.buttonCloseImage.visibility = View.VISIBLE
            } else {
                Log.w(LOG_TAG, "showImage() decode failed : $path")
            }
        } catch (throwable: Throwable) {
            // 解码异常（含 OOM）不应导致应用崩溃
            Log.e(LOG_TAG, "showImage() decode error : $path", throwable)
        }
    }

    // 隐藏预览并释放 Bitmap，避免图片占满页面无法退出
    // Hide the preview and release the bitmap so the page is not blocked by the image
    private fun hideImage() {
        val binding = _binding ?: return
        binding.imageData.setImageDrawable(null)
        binding.imageData.visibility = View.GONE
        binding.buttonCloseImage.visibility = View.GONE
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}