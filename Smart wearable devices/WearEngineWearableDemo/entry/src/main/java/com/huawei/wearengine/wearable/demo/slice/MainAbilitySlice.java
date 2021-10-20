/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.wearengine.wearable.demo.slice;

import com.huawei.watch.kit.hiwear.HiWear;
import com.huawei.watch.kit.hiwear.p2p.HiWearMessage;
import com.huawei.watch.kit.hiwear.p2p.P2pClient;
import com.huawei.watch.kit.hiwear.p2p.Receiver;
import com.huawei.watch.kit.hiwear.p2p.SendCallback;
import com.huawei.wearengine.wearable.demo.ResourceTable;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.Text;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Sample code of app-to-app communications when developing apps on smart wearable devices
 *
 * @since 2020-10-28
 */
public class MainAbilitySlice extends AbilitySlice implements Component.ClickedListener {
    private static final int MY_MODULE = 0x00201;

    private static final HiLogLabel LABEL = new HiLogLabel(HiLog.LOG_APP, MY_MODULE, "MainAbilitySlice");

    private static final String RESULT_PREFIX = "result: ";

    private P2pClient mP2pClient;

    private Text mPingResult;

    private Text mReceivedMsg;

    private Text mSendFileResult;

    private Text mSendMsgResult;

    private Text mSendFileProgress;

    private Button mBtnPing;

    private Button mBtnRegisterReceiver;

    private Button mBtnSendData;

    private Button mBtnSendFile;

    private Button mBtnUnregisterReceiveData;

    private Button mBtnClearData;

    private Receiver mReceiver;

    // Set the package name of your app on the phone
    private static final String TEST_PEER_PACKAGE = "";
    // Set the fingerprint information for the phone app
    private static final String TEST_FINGERPRINT = "";

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_demo_layout);

        // Button and result display component of the ping function
        mBtnPing = (Button) findComponentById(ResourceTable.Id_ping);
        mPingResult = (Text) findComponentById(ResourceTable.Id_ping_result);

        // Button and result display component for sending messages and files in P2P mode
        mBtnSendData = (Button) findComponentById(ResourceTable.Id_send_data);
        mBtnSendFile = (Button) findComponentById(ResourceTable.Id_send_file);
        mSendMsgResult = (Text) findComponentById(ResourceTable.Id_send_data_result);
        mSendFileResult = (Text) findComponentById(ResourceTable.Id_send_file_result);
        mSendFileProgress = (Text) findComponentById(ResourceTable.Id_send_file_progress);

        // Registry reception button for sending and receiving messages and files in P2P mode
        mBtnRegisterReceiver = (Button) findComponentById(ResourceTable.Id_register_receiver);
        mBtnUnregisterReceiveData = (Button) findComponentById(ResourceTable.Id_cancel_receive_data);
        mReceivedMsg = (Text) findComponentById(ResourceTable.Id_received_msg);

        // Clear button, which is used to clear the display result of each component
        mBtnClearData = (Button) findComponentById(ResourceTable.Id_clear_data);

        // Set a listener to listen for button events
        mBtnPing.setClickedListener(this);
        mBtnRegisterReceiver.setClickedListener(this);
        mBtnUnregisterReceiveData.setClickedListener(this);
        mBtnSendData.setClickedListener(this);
        mBtnSendFile.setClickedListener(this);
        mBtnClearData.setClickedListener(this);

        // Step 1: Obtain a point-to-point communication object
        mP2pClient = HiWear.getP2pClient(this);

        // Step 2: Set the package name of your app on the phone
        mP2pClient.setPeerPkgName(TEST_PEER_PACKAGE);

        // Step 3: Set the fingerprint information for the phone app
        mP2pClient.setPeerFingerPrint(TEST_FINGERPRINT);
    }

    @Override
    public void onActive() {
        super.onActive();
    }

    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
    }

    @Override
    public void onClick(Component component) {
        switch (component.getId()) {
            case ResourceTable.Id_ping:
                HiLog.info(LABEL, "start to ping");
                startPing();
                break;
            case ResourceTable.Id_register_receiver:
                HiLog.info(LABEL, "start to register receiver");
                registerReceiver();
                break;
            case ResourceTable.Id_send_data:
                HiLog.info(LABEL, "start to send data");
                sendP2pMessage();
                break;
            case ResourceTable.Id_send_file:
                HiLog.info(LABEL, "start to send file");
                sendFile();
                break;
            case ResourceTable.Id_cancel_receive_data:
                HiLog.info(LABEL, "start to unregister receiver");
                unregisterReceiver();
                break;
            case ResourceTable.Id_clear_data:
                HiLog.info(LABEL, "start to clear data");
                clearData();
                break;
            default:
                HiLog.info(LABEL, "nothing to click");
                break;
        }
    }

    /**
     * Check whether your app on the phone is running by using the ping function
     */
    private void startPing() {
        mP2pClient.ping(result -> {
            HiLog.info(LABEL, "ping result: %{public}d", result);

            // Display the callback result
            showText(mPingResult, "ping result: " + result);
        });
    }

    /**
     * Send a short message to your app on the phone
     */
    private void sendP2pMessage() {
        // Build a Message object
        String messageStr = "Hello, Wear Engine!";
        HiWearMessage.Builder builder = new HiWearMessage.Builder();
        builder.setPayload(messageStr.getBytes(StandardCharsets.UTF_8));
        HiWearMessage sendMessage = builder.build();

        // Create the callback method
        SendCallback sendCallback = new SendCallback() {
            @Override
            public void onSendResult(int resultCode) {
                HiLog.info(LABEL, "sendP2PMessage, resultCode:  %{public}d", resultCode);
                showText(mSendMsgResult, RESULT_PREFIX + resultCode);
            }

            @Override
            public void onSendProgress(long progress) {
                HiLog.info(LABEL, "sendP2PMessage, progress is:  %{public}d", progress);
            }
        };

        mP2pClient.send(sendMessage, sendCallback);
    }

    /**
     * Send a file to your app on the phone
     */
    private void sendFile() {
        // Construct the file to be sent
        String cachePath;
        try {
            cachePath = getCacheDir().getCanonicalPath();
        } catch (IOException e) {
            HiLog.error(LABEL, "cannot get canonical path");
            return;
        }
        File dir = new File(cachePath, "hiwear");
        HiWearMessage.Builder builder = new HiWearMessage.Builder();
        builder.setPayload(new File(dir, "demo.txt"));
        HiWearMessage sendMessage = builder.build();

        // Create the callback method
        SendCallback sendCallback = new SendCallback() {
            @Override
            public void onSendResult(int resultCode) {
                HiLog.info(LABEL, "sendFile, resultCode: %{public}d", resultCode);
                showText(mSendFileResult, RESULT_PREFIX + resultCode);
            }

            @Override
            public void onSendProgress(long progress) {
                HiLog.info(LABEL, "sendFile, progress is: %{public}d", progress);
                showText(mSendFileProgress, "progress: " + progress);
            }
        };
        mP2pClient.send(sendMessage, sendCallback);
    }

    /**
     * Receive the message or file from your app on the phone
     */
    private void registerReceiver() {
        mReceiver = message -> {
            switch (message.getType()) {
                // Receive the short message
                case HiWearMessage.MESSAGE_TYPE_DATA: {
                    HiLog.info(LABEL, "receive P2P message");
                    if (message.getData() != null) {
                        String msg = new String(message.getData(), StandardCharsets.UTF_8);
                        showText(mReceivedMsg, "received msg: " + msg);
                    }
                    break;
                }
                // Receive the file
                case HiWearMessage.MESSAGE_TYPE_FILE: {
                    HiLog.info(LABEL, "receive file");
                    File file = message.getFile();
                    if (file != null && file.exists()) {
                        showText(mReceivedMsg, "file name: " + file.getName());
                    }
                    break;
                }
                default:
                    HiLog.error(LABEL, "unsupported message type");
            }
        };
        mP2pClient.registerReceiver(mReceiver);
    }

    /**
     * Cancel the reception of the message or file from your app on the phone
     */
    private void unregisterReceiver() {
        if (mP2pClient != null && mReceiver != null) {
            mP2pClient.unregisterReceiver(mReceiver);
        }
    }

    /**
     * Clear display results of components
     */
    private void clearData() {
        mPingResult.setText("ping result：");
        mSendMsgResult.setText(RESULT_PREFIX);
        mReceivedMsg.setText("received msg: ");
    }

    /**
     * Display the specified character string on the specified Text component
     *
     * @param target Target text components
     * @param content Character string to be displayed
     */
    private void showText(Text target, String content) {
        if (target == null) {
            return;
        }
        getUITaskDispatcher().syncDispatch(() -> {
            target.setText(content);
            target.setTruncationMode(Text.TruncationMode.AUTO_SCROLLING);
            target.startAutoScrolling();
        });
    }
}