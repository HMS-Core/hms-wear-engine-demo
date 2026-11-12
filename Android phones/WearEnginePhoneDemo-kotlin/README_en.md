# Huawei Wear Engine Sample Project (Android)

[中文](README.md) | English | [日本語](README_ja.md)

## Contents

- [Introduction](#introduction)
- [Features](#features)
- [Preparation](#preparation)
- [Development Environment](#development-environment)
- [Test Environment](#test-environment)
- [Implementation Notes](#implementation-notes)
- [References](#references)

## Introduction

Huawei Wear Engine is a capability open service for Huawei smart watches. It is designed for developers of both phone and watch applications, enabling message communication and data transfer between third-party applications on the phone and the watch.

This project is the Android phone-side sample code of Wear Engine, implemented in **Kotlin** (`WearEnginePhoneDemo`). It calls the Wear Engine service APIs provided by the Huawei Health app and demonstrates how to use each open capability. The sample code only shows simple API calls and is for reference or limited use only.

## Features

| Feature | Description | Code Location |
| --- | --- | --- |
| Authorization | Request user authorization for device capabilities (DEVICE_MANAGER / NOTIFY / SENSOR) | `ui/device/DeviceViewModel.kt` `checkPermission()` `requestPermission()` |
| Device connection | Obtain the list of devices bound to Huawei Health and their connection state | `ui/device/DeviceViewModel.kt` `getConnectedDevice()` |
| Device status query | Query available storage, power status and charge status, and monitor low-power/charge events | `ui/device/DeviceViewModel.kt` `getAvailableKbytes()` `queryMonitorPowerStatus()` `registerMonitor()` |
| P2P communication | Check whether the watch-side app is installed (distinguishing smart watch from Lite wearable), send and receive custom messages, send and receive files (including cancel and progress), and send direction-key actions | `service/MainService.kt` `wearEngineInit()` `sendText()` `sendFile()` `stopSendFile()` `Receiver.onReceiveMessage()`, `ui/file/FileFragment.kt`, `ui/action/ActionFragment.kt` |
| Sensor | Get the sensor list, read asynchronously and stop reading | `ui/sensor/SensorActivity.kt`, `ui/sensor/SensorViewModel.kt` |
| Notification | Send notifications to the watch with 0/1/2/3-button templates | `ui/notify/NotifyActivity.kt`, `ui/notify/NotifyViewModel.kt` |

## Preparation

Complete the following configuration before using this sample. Every unconfigured item is represented by an all-uppercase placeholder.

### 1. Import the project

1. Extract the sample package.
2. Open the `WearEnginePhoneDemo` directory with Android Studio.
3. Click **Sync Project with Gradle Files** to finish setting up the build environment.

### 2. Configure the AppId

In `app/src/main/AndroidManifest.xml`, replace `${APP_ID}` with the AppId generated when you create the app in HUAWEI Developer:

```xml
<meta-data
    android:name="com.huawei.hms.client.appid"
    android:value="${APP_ID}" />
```

### 3. Configure the package name

In `app/build.gradle.kts`, change `applicationId` to the package name registered in HUAWEI Developer:

```kotlin
defaultConfig {
    applicationId = "com.example.packagename"
    ...
}
```

> `namespace` is the source code package name and is different from `applicationId`. It usually does not need to be changed.

### 4. Configure signing

This sample provides no signing configuration. `assembleDebug` uses the debug keystore generated automatically by Android Studio, and `assembleRelease` produces an unsigned APK.

To release the app or use a formal signature, add your own `signingConfigs` in `app/build.gradle.kts`, and make sure the certificate fingerprint of the signing file matches the one registered in HUAWEI Developer.

### 5. Configure the watch-side app information

P2P message, file transfer, sensor and notification all depend on the watch-side app information. Replace the following 4 placeholders in `app/src/main/java/com/huawei/sample/wearable/demo/model/Constant.kt`:

| Constant | Description |
| --- | --- |
| `WEARABLE_APP_PACKAGE_NAME` | Package name of the smart watch app |
| `WEARABLE_APP_FINGER_PRINT` | Fingerprint of the smart watch app (that is, the APP ID of the watch app) |
| `LITE_WEARABLE_APP_PACKAGE_NAME` | Package name of the Lite wearable app |
| `LITE_WEARABLE_APP_FINGER_PRINT` | Fingerprint of the Lite wearable app (format: `{package name}_{base64 of public key}`) |

The procedure for generating the Lite wearable fingerprint is described in the comments of `Constant.kt`.

> Note: The sample detects the watch-side app through `isAppInstalled`. If the package names above are still placeholders, the detection always fails, and P2P communication, sensor and notification will not work.

### 6. Dependency configuration

The Wear Engine SDK is introduced as a Maven dependency. The HMS Core repository is already configured:

```kotlin
implementation("com.huawei.hms:wearengine:5.0.3.304")
```

## Development Environment

- Android Studio Meerkat | 2024.3.1 or later
- Android Gradle Plugin 8.9.1
- Gradle 8.11.1
- Kotlin 1.9.0
- JDK 17
- compileSdk 36 / minSdk 29 / targetSdk 36

## Test Environment

### Android

| Item | Contents |
| --- | --- |
| Device Name | HUAWEI P30 lite |
| Model | MAR-LX2J |

## Implementation Notes

- When implementing Wear Engine, you must pass the correct package name and fingerprint to `P2pClient`. The fingerprint in particular is a bit confusing. The procedure for obtaining the fingerprint is written in `Constant.kt`, so please refer to that.
- The project uses a two-process architecture: `MainService` runs in the `:mainservice` process and communicates with the pages through `Messenger`, holding the `P2pClient` in one place.
- The sensor and notification pages are standalone `Activity` classes that use `HiWear.getSensorClient()` / `HiWear.getNotifyClient()` directly, without going through `MainService`.

## References

- Wear Engine - Android Phone App Development (https://developer.huawei.com/consumer/en/doc/connectivity-Guides/phone-dev-0000001086797354)
