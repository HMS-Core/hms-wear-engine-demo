# Wear Engine Phone Demo

English | [中文](README_ZH.md)

## Contents

-   [Overview](#Overview)
-   [Preparations](#Preparations)
-   [Environment Requirements](#Environment Requirements)
-   [License](#License)

## Overview

HUAWEI Wear Engine provides the open capabilities of Huawei watches.

It is designed for developers of apps and services running on phones and Huawei smart watches.

By integrating the Wear Engine, it will be possible for an app or service on a phone to send messages and transfer data to a Huawei smart watches. This also works the other way round, which means that an app or service on a Huawei smart watch is able to send messages and transfer data to a phone.

The Wear Engine pools the phone and the watch's resources and capabilities, which include apps and services, creating benefits for consumers and developers alike. It enables users to use their devices in more diversified scenarios and receive more convenient services, with a smoother experience. It also expands the reach of your business, and takes your apps and services to the next level. 

This document provides the sample code for integrating the Wear Engine to Android devices. This project contains sample code for calling Android APIs of the Wear Engine to send messages and data between your app on the phone and the watch. It involves only simple methods to call the APIs and is for reference only.

The Wear Engine provides the following functions:

-   Authorize

    Obtain user authorization for the openess of device capabilities.

    The code is stored in  **\\app\\src\\main\\java\\com\\huawei\\wearengine\\app\\WearEngineMainActivity.java initData\(\)**.


-   Manage device connections

    Allow you to obtain the list of devices that have been connected to the Huawei Health app.

    The code is stored in  **\\app\\src\\main\\java\\com\\huawei\\wearengine\\app\\WearEngineMainActivity.java getBoundDevices\(View view\)**.

    Result:

    ![](figures/en-us_image_0000001071060016.png)


-   Manage point-to-point \(P2P\) messaging

    Create an app-to-app communications channel between the phone and the watch to receive and send the customized packet messages and files on third-party apps.

    1. Check whether your app on the watch is running

    The code is stored in  **\\app\\src\\main\\java\\com\\huawei\\wearengine\\app\\WearEngineMainActivity.java getBoundDevices\(View view\)**.

    2. Send messages or files from your app on the phone to that on the watch in P2P mode

    The code is stored in  **\\app\\src\\main\\java\\com\\huawei\\wearengine\\app\\WearEngineMainActivity.java sendMessage\(View view\) sendFile\(String sendFilePath\)**.

    3. Receive the messages from your app on the watch

    The code is stored in  **\\app\\src\\main\\java\\com\\huawei\\wearengine\\app\\WearEngineMainActivity.java getBoundDevices\(View view\)**.

    Result:

    ![](figures/en-us_image_0000001070580027.png)


-   Monitor and manage device status

    Monitor or query the real-time connection status between the watch and Huawei Health.

    The code is stored in  **\\app\\src\\main\\java\\com\\huawei\\wearengine\\app\\WearEngineMainActivity.java getBoundDevices\(View view\)**.

    Result:

    ![](figures/en-us_image_0000001070857865.png)

-   Notifications

    Allow you to send notifications to devices that have been connected to the Huawei Health app.

    The code is stored in  **\\app\\src\\main\\java\\com\\huawei\\wearengine\\app\\NotificationActivity.java  sendNotification\(View view\)**.

    Result:

    ![](figures/en-us_image_0000001070431867.png)


## Preparations

Before using the sample code, check whether Integrated Development Environment \(IDE\) has been installed.

1.  Decompress the sample code package.
2.  Copy the code package to the IDE directory and import the code package to the IDE.
3.  Click  **Sync Project with Gradle Files**  to build the IDE.

Before using the functions in the sample code package, you need to set the following parameters. For details, see the Getting Started section and the Preparations section in the development guide.

-   Address of the Wear Engine SDK Maven \(which has been set in the sample code project\):
    -   Go to the project  **build.gradle**  \>  **all projects**  \>  **repositories**, configure the Maven address of HMS SDK:  **maven \{url 'http://developer.huawei.com/repo/'\}**.
    -   Go to the project  **build.gradle**  \>  **buildscript**  \>  **dependencies**, configure the Maven address of HMS SDK:  **maven \{url 'http://developer.huawei.com/repo/'\}**.


-   Build dependencies: Add build dependencies  **implementation 'com.huawei.hms:wearengine:\{version\}'**  to the  **build.gradle**  file at the app level. Modify  **\{version\}**  to the actual version number such as "5.0.1.300".
-   AppId: Add the AppId information generated when the app is created on HUAWEI Developers to the  **AndroidManifest.xml**  file of the app.
-   Package name: Modify the package name to the one you entered when creating the app on HUAWEI Developers.
-   Signature: Import the signature file to the build.gradle file at the app level. Note that the signature file must match the certificate fingerprint entered when creating the app on HUAWEI Developers.
-   Fingerprint on the wearable device: To use the P2P messaging function, enter the fingerprint information of the wearable device in  **p2pClient.setPeerFingerPrint\(""\)**  in the  **WearEngineMainActivity.java**  file.

## Environment Requirements

-   Android Studio version 3.3.2 or later
-   Java SDK 1.8 or later

## License

The sample code is licensed under the  [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

