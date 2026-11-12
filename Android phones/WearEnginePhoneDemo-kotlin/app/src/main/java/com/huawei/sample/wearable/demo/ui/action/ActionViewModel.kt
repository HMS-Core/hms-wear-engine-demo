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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class ActionViewModel : ViewModel() {

    companion object {
        private val LOG_TAG = ActionViewModel::class.java.simpleName
    }

    private val _textLog = MutableLiveData<String>().apply {
        value = ""
    }
    val textLog: LiveData<String> = _textLog

    fun addLog(text: String) {
        if (textLog.value.isEmpty()) {
            _textLog.postValue(text)
        } else {
            _textLog.postValue(textLog.value + "\n" + text)
        }
    }
}