/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.

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

package com.huawei.wearengine.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.wearengine.HiWear;
import com.huawei.wearengine.device.Device;
import com.huawei.wearengine.notify.Action;
import com.huawei.wearengine.notify.Notification;
import com.huawei.wearengine.notify.NotificationConstants;
import com.huawei.wearengine.notify.NotificationTemplate;
import com.huawei.wearengine.notify.NotifyClient;

import java.util.HashMap;

/**
 * Notification Page
 *
 * @since 2020-11-17
 */
public class NotificationActivity extends AppCompatActivity {
    private static final int SCROLL_HIGH = 50;

    private static final String NOTIFY_TEMPLATE_NO_BUTTON = "50";

    private static final String NOTIFY_TEMPLATE_ONE_BUTTON = "51";

    private static final String NOTIFY_TEMPLATE_TWO_BUTTON = "52";

    private static final String NOTIFY_TEMPLATE_THREE_BUTTON = "53";

    private Device checkedDevice;

    private NotifyClient mNotifyClient;

    private EditText etTemplateId;

    private EditText etPackage;

    private EditText etTitle;

    private EditText etText;

    private EditText etButtonContentFirst;

    private EditText etButtonContentSecond;

    private EditText etButtonContentThird;

    private TextView notifyResultTextView;

    private TextView tvDevice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notify);
        initView();
        initData();
    }

    /**
     * Init view.
     */
    private void initView() {
        etTemplateId = findViewById(R.id.edit_notify_template);
        etPackage = findViewById(R.id.edit_notify_package_name);
        etTitle = findViewById(R.id.edit_notify_title);
        etText = findViewById(R.id.edit_notify_content);
        etButtonContentFirst = findViewById(R.id.edit_notify_button_one);
        etButtonContentSecond = findViewById(R.id.edit_notify_button_two);
        etButtonContentThird = findViewById(R.id.edit_notify_button_three);
        notifyResultTextView = findViewById(R.id.result_shows);
        notifyResultTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvDevice = findViewById(R.id.text_notify_device);
    }

    /**
     * Init data.
     */
    private void initData() {
        checkedDevice = getIntent().getParcelableExtra("currentDevice");
        String packageName = getIntent().getStringExtra("packageName");
        if (packageName != null) {
            etPackage.setText(packageName);
        }
        if (checkedDevice != null && checkedDevice.getName() != null) {
            tvDevice.setText(checkedDevice.getName());
        } else {
            finish();
            return;
        }
        mNotifyClient = HiWear.getNotifyClient(NotificationActivity.this);
    }

    /**
     * Send notification.
     *
     * @param view UI object
     */
    public void sendNotification(View view) {
        Notification.Builder builder = new Notification.Builder();
        String packageName = etPackage.getText().toString().trim();
        if (TextUtils.isEmpty(packageName)) {
            printOperationResult("please input packageName for send!");
        }
        builder.setPackageName(packageName);
        String title = etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            printOperationResult("please input title for send!");
        }
        builder.setTitle(title);
        String text = etText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            printOperationResult("please input notification content for send!");
        }
        builder.setText(text);
        setButtonContentAndTemplate(builder);
        Action action = getNotificationAction();
        builder.setAction(action);
        Notification notification = builder.build();
        mNotifyClient.notify(checkedDevice, notification).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void successVoid) {
                printOperationResult("send notification success.");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                printOperationResult("send notification fail, exception.");
            }
        });
    }

    /**
     * Create the Action object of the Notification.
     *
     * @return Action Object
     */
    private Action getNotificationAction() {
        return new Action() {
            @Override
            public void onResult(Notification notification, int feedback) {
                String result =
                    "getNotificationAction feedback:" + feedback + ",notification title:" + notification.getTitle();
                printOperationResult(result);
            }

            @Override
            public void onError(Notification notification, int errorCode, String errorMsg) {
                String result = "getNotificationAction onError errorCode:" + errorCode + ",errorMsg:" + errorMsg;
                printOperationResult(result);
            }
        };
    }

    /**
     * Set the number and content of notification buttons.
     *
     * @param builder Construct the Builder object of the Notification object.
     */
    private void setButtonContentAndTemplate(Notification.Builder builder) {
        HashMap<Integer, String> buttonContents = new HashMap<>();
        String templateId = etTemplateId.getText().toString().trim();
        String button1Text = etButtonContentFirst.getText().toString().trim();
        String button2Text = etButtonContentSecond.getText().toString().trim();
        String button3Text = etButtonContentThird.getText().toString().trim();
        switch (templateId) {
            case NOTIFY_TEMPLATE_NO_BUTTON:
                builder.setTemplateId(NotificationTemplate.NOTIFICATION_TEMPLATE_NO_BUTTON);
                break;
            case NOTIFY_TEMPLATE_ONE_BUTTON:
                if (TextUtils.isEmpty(button1Text)) {
                    printOperationResult("please input button1Text for send!");
                }
                builder.setTemplateId(NotificationTemplate.NOTIFICATION_TEMPLATE_ONE_BUTTON);
                buttonContents.put(NotificationConstants.BUTTON_ONE_CONTENT_KEY, button1Text);
                break;
            case NOTIFY_TEMPLATE_TWO_BUTTON:
                if (TextUtils.isEmpty(button1Text) || TextUtils.isEmpty(button2Text)) {
                    printOperationResult("please input button1Text and button2Text for send!");
                }
                builder.setTemplateId(NotificationTemplate.NOTIFICATION_TEMPLATE_TWO_BUTTONS);
                buttonContents.put(NotificationConstants.BUTTON_ONE_CONTENT_KEY, button1Text);
                buttonContents.put(NotificationConstants.BUTTON_TWO_CONTENT_KEY, button2Text);
                break;
            case NOTIFY_TEMPLATE_THREE_BUTTON:
                if (TextUtils.isEmpty(button1Text) || TextUtils.isEmpty(button2Text)
                    || TextUtils.isEmpty(button3Text)) {
                    printOperationResult(
                        "please input button1Text and button2Text and button3Text for send!");
                }
                builder.setTemplateId(NotificationTemplate.NOTIFICATION_TEMPLATE_THREE_BUTTONS);
                buttonContents.put(NotificationConstants.BUTTON_ONE_CONTENT_KEY, button1Text);
                buttonContents.put(NotificationConstants.BUTTON_TWO_CONTENT_KEY, button2Text);
                buttonContents.put(NotificationConstants.BUTTON_THREE_CONTENT_KEY, button3Text);
                break;
            default:
                builder.setTemplateId(NotificationTemplate.NOTIFICATION_TEMPLATE_NO_BUTTON);
                break;
        }
        if (!buttonContents.isEmpty()) {
            builder.setButtonContents(buttonContents);
        }
    }

    /**
     * Clear the result printing box.
     *
     * @param view UI object
     */
    public void clearResult(View view) {
        notifyResultTextView.setText("");
        notifyResultTextView.scrollTo(0, 0);
    }

    /**
     * Print the result.
     *
     * @param string Print result character string
     */
    private void printOperationResult(final String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyResultTextView.append(string + System.lineSeparator());
                int offset = notifyResultTextView.getLineCount() * notifyResultTextView.getLineHeight();
                if (offset > notifyResultTextView.getHeight()) {
                    notifyResultTextView.scrollTo(0, offset - notifyResultTextView.getHeight() + SCROLL_HIGH);
                }
            }
        });
    }
}