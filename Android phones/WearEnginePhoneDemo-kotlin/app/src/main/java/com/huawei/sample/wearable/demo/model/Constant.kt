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

package com.huawei.sample.wearable.demo.model

class Constant {
    companion object {
        // 有关指纹识别的更多信息，请参见此处
        // For more information about fingerprints, see here
        // https://developer.huawei.com/consumer/cn/blog/topic/03188242414955227

        // 智能表应用包名（请替换为手表侧应用的包名）
        // Package name of the Wearable app (replace with your watch app package name)
        const val WEARABLE_APP_PACKAGE_NAME: String = "WEARABLE_APP_PACKAGE_NAME"

        // 智能表应用指纹 = {WearableアプリのAPP ID}（请替换为手表侧应用的 APP ID）
        // Wearable app fingerprint = {Wearable app APP ID} (replace with your watch app APP ID)
        const val WEARABLE_APP_FINGER_PRINT: String = "WEARABLE_APP_FINGER_PRINT"

        // 运动表应用包名（请替换为运动表侧应用的包名）
        // Package name of the Lite wearable app (replace with your Lite wearable app package name)
        const val LITE_WEARABLE_APP_PACKAGE_NAME = "LITE_WEARABLE_APP_PACKAGE_NAME"

        // 运动表应用指纹
        // https://developer.huawei.com/consumer/en/doc/connectivity-guides/signature-0000001053969657
        // 生成过程：
        // 1. 打开运动表项目中使用的cer证书，复制最后的BEGIN CERTIFICATE块，并创建一个新的cer文件。
        // 2. 打开（1）中创建的cer文件，复制“公钥”的值
        // 3. 使用 base64 工具（例如 https://www.tomeko.net/online_tools/hex_to_base64.php）将公钥值转换为 base64
        // 4. 运动表应用指纹 = {运动表应用包名}_{公钥的base64值}
        // 例子：
        // {运动表应用的包名}_{公钥的base64值} = com.huawei.sample.wearable.sportwatch.demo_{公钥的base64值}
        // Lite wearable app fingerprint (replace with your Lite wearable app fingerprint)
        const val LITE_WEARABLE_APP_FINGER_PRINT = "LITE_WEARABLE_APP_FINGER_PRINT"
    }
}