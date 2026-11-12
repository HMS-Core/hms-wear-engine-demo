# 华为可穿戴引擎示例项目 (Android)

中文 | [English](README_en.md) | [日本語](README_ja.md)

## 目录

- [简介](#简介)
- [功能说明](#功能说明)
- [开发准备](#开发准备)
- [开发环境](#开发环境)
- [测试环境](#测试环境)
- [实现说明](#实现说明)
- [参考文献](#参考文献)

## 简介

Huawei Wear Engine 是华为提供的智能手表能力开放服务，面向手机和手表的应用开发者，实现手机与手表上第三方应用间的消息通信、数据传输等能力。

本项目是 Wear Engine 服务的 Android 手机侧示例代码，使用 **Kotlin** 实现（`WearEnginePhoneDemo`），调用华为运动健康 App 提供的 Wear Engine 服务接口，演示各项开放能力的调用方式。示例代码只提供 API 的简单调用，仅供参考或受限使用。

## 功能说明

| 功能 | 说明 | 代码位置 |
| --- | --- | --- |
| 授权 | 获取设备能力开放的用户授权（DEVICE_MANAGER / NOTIFY / SENSOR） | `ui/device/DeviceViewModel.kt` `checkPermission()` `requestPermission()` |
| 设备连接管理 | 获取已与华为运动健康 App 绑定的设备列表及连接状态 | `ui/device/DeviceViewModel.kt` `getConnectedDevice()` |
| 设备状态查询 | 查询设备剩余空间、电量、充电状态，并监听低电量/充电事件 | `ui/device/DeviceViewModel.kt` `getAvailableKbytes()` `queryMonitorPowerStatus()` `registerMonitor()` |
| 点对点通信 | 检测手表侧应用是否安装（自动区分智能表与运动表）、收发自定义报文、收发文件（含取消发送与进度）、下发方向键动作 | `service/MainService.kt` `wearEngineInit()` `sendText()` `sendFile()` `stopSendFile()` `Receiver.onReceiveMessage()`、`ui/file/FileFragment.kt`、`ui/action/ActionFragment.kt` |
| 传感器 | 获取传感器列表、异步读取、停止读取 | `ui/sensor/SensorActivity.kt`、`ui/sensor/SensorViewModel.kt` |
| 消息通知 | 向手表侧发送通知，支持无按钮/1/2/3 按钮四种模板 | `ui/notify/NotifyActivity.kt`、`ui/notify/NotifyViewModel.kt` |

## 开发准备

使用本示例代码前，请先完成以下配置。未替换的配置项均以全大写占位符表示。

### 1. 导入工程

1. 解压示例代码包。
2. 使用 Android Studio 打开工程目录 `WearEnginePhoneDemo`。
3. 点击 **Sync Project with Gradle Files** 完成开发环境构建。

### 2. 配置 AppId

在 `app/src/main/AndroidManifest.xml` 中，将 `${APP_ID}` 替换为在华为开发者联盟创建应用时生成的 AppId：

```xml
<meta-data
    android:name="com.huawei.hms.client.appid"
    android:value="${APP_ID}" />
```

### 3. 配置包名

在 `app/build.gradle.kts` 中，将 `applicationId` 修改为在开发者联盟创建应用时填写的包名：

```kotlin
defaultConfig {
    applicationId = "com.example.packagename"
    ...
}
```

> `namespace` 是源码包名，与 `applicationId` 是两个概念，通常无需修改。

### 4. 配置签名

示例工程未提供签名配置，`assembleDebug` 使用 Android Studio 自动生成的 debug 签名，`assembleRelease` 产出未签名 APK。

如需发布或使用正式签名，请在 `app/build.gradle.kts` 中自行添加 `signingConfigs`，并确保签名文件的证书指纹与开发者联盟创建应用时填写的一致。

### 5. 配置穿戴设备侧应用信息

点对点消息、文件传输、传感器、消息通知均依赖手表侧应用信息。请在 `app/src/main/java/com/huawei/sample/wearable/demo/model/Constant.kt` 中替换以下 4 个占位符：

| 常量 | 说明 |
| --- | --- |
| `WEARABLE_APP_PACKAGE_NAME` | 智能表侧应用包名 |
| `WEARABLE_APP_FINGER_PRINT` | 智能表侧应用指纹（即手表侧应用的 APP ID） |
| `LITE_WEARABLE_APP_PACKAGE_NAME` | 运动表侧应用包名 |
| `LITE_WEARABLE_APP_FINGER_PRINT` | 运动表侧应用指纹（格式：`{包名}_{公钥的base64值}`） |

运动表侧指纹的生成流程见 `Constant.kt` 中的注释说明。

> 说明：示例代码通过 `isAppInstalled` 自动检测手表侧应用，若上述包名为占位符，检测必然失败，点对点通信与传感器、通知功能均无法使用。

### 6. 依赖配置

Wear Engine SDK 通过 Maven 依赖引入，工程已配置 HMS Core 仓库：

```kotlin
implementation("com.huawei.hms:wearengine:5.0.3.304")
```

## 开发环境

- Android Studio Meerkat | 2024.3.1 及以上版本
- Android Gradle Plugin 8.9.1
- Gradle 8.11.1
- Kotlin 1.9.0
- JDK 17
- compileSdk 36 / minSdk 29 / targetSdk 36

## 测试环境

### Android

| 项目 | 内容 |
| --- | --- |
| 设备名称 | HUAWEI P30 lite |
| 型号 | MAR-LX2J |

## 实现说明

- 实现 Wear Engine 时，必须将正确的包名称和指纹传递给 `P2pClient`。指纹信息可能比较容易混淆，获取指纹的流程写在 `Constant.kt` 文件中，请参考该文件。
- 工程使用双进程架构：`MainService` 运行在 `:mainservice` 进程，通过 `Messenger` 与各页面通信，统一持有 `P2pClient`。
- 传感器与消息通知页面为独立 `Activity`，直接使用 `HiWear.getSensorClient()` / `HiWear.getNotifyClient()`，不经过 `MainService`。

## 参考文献

- 穿戴能力 - Android 手机侧应用开发 (https://developer.huawei.com/consumer/cn/doc/connectivity-Guides/phone-dev-0000001086797354)
