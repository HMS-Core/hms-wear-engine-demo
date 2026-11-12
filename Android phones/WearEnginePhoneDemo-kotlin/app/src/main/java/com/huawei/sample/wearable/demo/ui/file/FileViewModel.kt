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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FileViewModel : ViewModel() {
    companion object {
        private val LOG_TAG = FileViewModel::class.java.simpleName
    }

    private val _selectPath = MutableLiveData<String>().apply {
        value = ""
    }
    val selectPath: LiveData<String> = _selectPath

    private val _textData = MutableLiveData<String>().apply {
        value = ""
    }
    val textData: LiveData<String> = _textData

    // 最近一次从手表收到的文件路径，供用户手动查看
    // Path of the most recently received file, for manual viewing
    private val _receivedPath = MutableLiveData<String>().apply {
        value = ""
    }
    val receivedPath: LiveData<String> = _receivedPath

    fun setSelectPath(path: String) {
        _selectPath.postValue(path)
    }

    fun setReceivedPath(path: String) {
        _receivedPath.postValue(path)
    }

    fun setText(text: String) {
        _textData.postValue(text)
    }

    fun addText(text: String) {
        if (textData.value.isEmpty()) {
            _textData.postValue(text)
        } else {
            _textData.postValue(textData.value + "\n" + text)
        }
    }
}