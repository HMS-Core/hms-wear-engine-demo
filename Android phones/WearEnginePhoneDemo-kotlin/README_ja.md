# ファーウェイWear Engineのサンプルプロジェクト（Android）

[中文](README.md) | [English](README_en.md) | 日本語

## 目次

- [概要](#概要)
- [機能一覧](#機能一覧)
- [開発準備](#開発準備)
- [開発環境](#開発環境)
- [テスト環境](#テスト環境)
- [実装の注意事項](#実装の注意事項)
- [参考資料](#参考資料)

## 概要

Huawei Wear Engineは、ファーウェイのスマートウォッチ向け能力開放サービスです。スマートフォンとウォッチの両方のアプリ開発者向けに、スマートフォンとウォッチ上のサードパーティアプリ間でのメッセージ通信やデータ転送などの機能を提供します。

本プロジェクトはWear EngineサービスのAndroidスマートフォン側サンプルコードで、**Kotlin**（`WearEnginePhoneDemo`）で実装されています。ファーウェイのヘルスケアアプリが提供するWear EngineサービスのAPIを呼び出し、各オープン能力の利用方法を示します。本サンプルコードはAPIの簡単な呼び出しのみを提供するもので、参考または限定的な使用を目的としています。

## 機能一覧

| 機能 | 説明 | コード位置 |
| --- | --- | --- |
| 認可 | デバイス能力開放のユーザー認可を取得（DEVICE_MANAGER / NOTIFY / SENSOR） | `ui/device/DeviceViewModel.kt` `checkPermission()` `requestPermission()` |
| デバイス接続管理 | ヘルスケアアプリにバインドされたデバイス一覧と接続状態を取得 | `ui/device/DeviceViewModel.kt` `getConnectedDevice()` |
| デバイス状態照会 | 空き容量、バッテリー状態、充電状態を照会し、低電力・充電イベントを監視 | `ui/device/DeviceViewModel.kt` `getAvailableKbytes()` `queryMonitorPowerStatus()` `registerMonitor()` |
| P2P通信 | ウォッチ側アプリのインストール検出（スマートウォッチとLiteウェアラブルを自動判別）、カスタムメッセージの送受信、ファイルの送受信（送信キャンセルと進捗を含む）、方向キーアクションの送信 | `service/MainService.kt` `wearEngineInit()` `sendText()` `sendFile()` `stopSendFile()` `Receiver.onReceiveMessage()`、`ui/file/FileFragment.kt`、`ui/action/ActionFragment.kt` |
| センサー | センサー一覧の取得、非同期読み取り、読み取り停止 | `ui/sensor/SensorActivity.kt`、`ui/sensor/SensorViewModel.kt` |
| メッセージ通知 | ウォッチ側へ通知を送信。ボタンなし/1/2/3ボタンの4種類のテンプレートに対応 | `ui/notify/NotifyActivity.kt`、`ui/notify/NotifyViewModel.kt` |

## 開発準備

本サンプルコードを使用する前に、以下の設定を完了してください。未設定の項目はすべて大文字のプレースホルダーで表されています。

### 1. プロジェクトのインポート

1. サンプルコードのパッケージを解凍します。
2. Android Studioで `WearEnginePhoneDemo` ディレクトリを開きます。
3. **Sync Project with Gradle Files** をクリックして開発環境の構築を完了します。

### 2. AppIdの設定

`app/src/main/AndroidManifest.xml` 内の `${APP_ID}` を、ファーウェイデベロッパーでアプリを作成した際に生成されるAppIdに置き換えます。

```xml
<meta-data
    android:name="com.huawei.hms.client.appid"
    android:value="${APP_ID}" />
```

### 3. パッケージ名の設定

`app/build.gradle.kts` の `applicationId` を、ファーウェイデベロッパーで登録したパッケージ名に変更します。

```kotlin
defaultConfig {
    applicationId = "com.example.packagename"
    ...
}
```

> `namespace` はソースコードのパッケージ名であり、`applicationId` とは別の概念です。通常は変更する必要はありません。

### 4. 署名の設定

本サンプルには署名設定が含まれていません。`assembleDebug` はAndroid Studioが自動生成するdebug署名を使用し、`assembleRelease` は未署名のAPKを出力します。

リリースや正式署名を使用する場合は、`app/build.gradle.kts` に独自の `signingConfigs` を追加し、署名ファイルの証明書フィンガープリントがファーウェイデベロッパーで登録したものと一致することを確認してください。

### 5. ウォッチ側アプリ情報の設定

P2Pメッセージ、ファイル転送、センサー、メッセージ通知はすべてウォッチ側アプリの情報に依存します。`app/src/main/java/com/huawei/sample/wearable/demo/model/Constant.kt` 内の以下の4つのプレースホルダーを置き換えてください。

| 定数 | 説明 |
| --- | --- |
| `WEARABLE_APP_PACKAGE_NAME` | スマートウォッチ側アプリのパッケージ名 |
| `WEARABLE_APP_FINGER_PRINT` | スマートウォッチ側アプリのフィンガープリント（ウォッチ側アプリのAPP ID） |
| `LITE_WEARABLE_APP_PACKAGE_NAME` | Liteウェアラブル側アプリのパッケージ名 |
| `LITE_WEARABLE_APP_FINGER_PRINT` | Liteウェアラブル側アプリのフィンガープリント（形式：`{パッケージ名}_{公開鍵のbase64値}`） |

Liteウェアラブル側フィンガープリントの生成手順は `Constant.kt` のコメントに記載されています。

> 注意：本サンプルは `isAppInstalled` でウォッチ側アプリを検出します。上記のパッケージ名がプレースホルダーのままだと検出は必ず失敗し、P2P通信、センサー、通知機能は使用できません。

### 6. 依存関係の設定

Wear Engine SDKはMaven依存関係として導入されています。HMS Coreリポジトリは設定済みです。

```kotlin
implementation("com.huawei.hms:wearengine:5.0.3.304")
```

## 開発環境

- Android Studio Meerkat | 2024.3.1 以降
- Android Gradle Plugin 8.9.1
- Gradle 8.11.1
- Kotlin 1.9.0
- JDK 17
- compileSdk 36 / minSdk 29 / targetSdk 36

## テスト環境

### Android

| 項目 | 内容 |
| --- | --- |
| デバイス名 | HUAWEI P30 lite |
| 機種 | MAR-LX2J |

## 実装の注意事項

- Wear Engineを実装するときに、P2pClientに正しいパッケージ名とフィンガープリントを渡す必要があります。特にフィンガープリントについてちょっとわかりにくいです。Constant.ktにフィンガープリントの取得手順が書かれているので、それを参考にしていただければと思います。
- 本プロジェクトは2プロセス構成です。`MainService` は `:mainservice` プロセスで動作し、`Messenger` を介して各ページと通信し、`P2pClient` を一元管理します。
- センサーとメッセージ通知のページは独立した `Activity` であり、`HiWear.getSensorClient()` / `HiWear.getNotifyClient()` を直接使用し、`MainService` を経由しません。

## 参考資料

- Wear Engine - Android Phone App Development (https://developer.huawei.com/consumer/en/doc/connectivity-Guides/phone-dev-0000001086797354)
