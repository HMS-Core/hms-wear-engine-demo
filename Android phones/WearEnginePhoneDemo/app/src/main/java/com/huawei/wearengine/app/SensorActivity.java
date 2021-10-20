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
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.wearengine.HiWear;
import com.huawei.wearengine.device.Device;
import com.huawei.wearengine.sensor.DataResult;
import com.huawei.wearengine.sensor.Sensor;
import com.huawei.wearengine.sensor.SensorClient;
import com.huawei.wearengine.sensor.SensorReadCallback;
import com.huawei.wearengine.sensor.SensorStopCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sensor Page
 *
 * @since 2020-11-16
 */
public class SensorActivity extends AppCompatActivity {
    private static final int SCROLL_HIGH = 50;

    private SensorClient mSensorClient;

    private Device mCurrentReadDevice;

    private Sensor mSensor;

    private List<Sensor> mSensorList = new ArrayList<>();

    private TextView sensorResultTextView;

    private RadioGroup sensorsRadioGroup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);
        sensorResultTextView = findViewById(R.id.result_shows);
        sensorsRadioGroup = findViewById(R.id.sensor_radio_group);
        TextView deviceNameTextView = findViewById(R.id.textView_device_name);
        mSensorClient = HiWear.getSensorClient(this);
        mCurrentReadDevice = getIntent().getParcelableExtra("currentDevice");
        if (mCurrentReadDevice == null) {
            printOperationResult("mCurrentReadDevice is null.");
            return;
        }
        if (mCurrentReadDevice.getName() != null) {
            deviceNameTextView.setText(mCurrentReadDevice.getName());
        }
        addListener();
    }

    /**
     * Add listener.
     */
    private void addListener() {
        sensorResultTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        sensorsRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int index = checkedId;
                if (index >= 0 && index < mSensorList.size()) {
                    mSensor = mSensorList.get(index);
                } else {
                    printOperationResult("checked radioButton item is error.");
                }
            }
        });
    }

    /**
     * Obtaining the sensor list.
     *
     * @param view UI object
     */
    public void getSensorList(View view) {
        if (mCurrentReadDevice == null || !mCurrentReadDevice.isConnected()) {
            printOperationResult("getSensorList: The mCurrentReadDevice status is error.");
            return;
        }
        mSensorClient.getSensorList(mCurrentReadDevice).addOnSuccessListener(new OnSuccessListener<List<Sensor>>() {
            @Override
            public void onSuccess(List<Sensor> sensors) {
                if (sensors == null || sensors.size() == 0) {
                    printOperationResult("getSensorList list is null or list size is 0.");
                    return;
                }
                mSensorList.clear();
                for (Sensor item : sensors) {
                    if (Sensor.NAME_ACC.equals(item.getName()) || Sensor.NAME_PPG.equals(item.getName())) {
                        printOperationResult("sensor name:" + item.getName());
                        printOperationResult("sensor id:" + item.getId());
                        mSensorList.add(item);
                    }
                }
                updateSensorListBox();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                printOperationResult("getSensorList Exception.");
            }
        });
    }

    /**
     * Asynchronous reading of the sensor data.
     *
     * @param view UI object
     */
    public void asyncReadSensorData(View view) {
        if (checkParams()) {
            printOperationResult(
                "asyncReadSensorData: The device or sensor object is empty, or the device is disconnected.");
            return;
        }
        SensorReadCallback sensorReadCallback = new SensorReadCallback() {
            @Override
            public void onReadResult(int errorCode, DataResult dataResult) {
                printOperationResult("asyncReadSensorData errorCode:" + errorCode);
                if (dataResult != null && dataResult.getSensor() != null) {
                    printOperationResult("sensor name:" + dataResult.getSensor().getName());
                    float[] data;
                    switch (dataResult.getSensor().getType()) {
                        case Sensor.TYPE_PPG:
                            List<DataResult> dataResults = dataResult.asList();
                            for (DataResult item : dataResults) {
                                data = item.asFloats();
                                printOperationResult("PPG data:" + Arrays.toString(data));
                            }
                            break;
                        case Sensor.TYPE_ACC:
                            data = dataResult.asFloats();
                            printOperationResult("ACC data:" + Arrays.toString(data));
                            break;
                        default:
                            data = new float[0];
                            printOperationResult("data:" + Arrays.toString(data));
                            break;
                    }
                }
            }
        };
        mSensorClient.asyncRead(mCurrentReadDevice, mSensor, sensorReadCallback)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void success) {
                    printOperationResult("asyncRead task submission success sensor name is:" + mSensor.getName());
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    printOperationResult("asyncRead task error. sensor name is:" + mSensor.getName());
                }
            });
    }

    /**
     * Stop the asynchronous reading of the sensor data.
     *
     * @param view UI object
     */
    public void stopReadSensorData(View view) {
        if (checkParams()) {
            printOperationResult(
                "stopReadSensorData: The device or sensor object is empty, or the device is disconnected.");
            return;
        }
        SensorStopCallback sensorStopCallback = new SensorStopCallback() {
            @Override
            public void onStopResult(int errorCode) {
                String result = "stopAsyncRead result:" + errorCode;
                printOperationResult(result);
            }
        };
        mSensorClient.stopAsyncRead(mCurrentReadDevice, mSensor, sensorStopCallback)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void success) {
                    printOperationResult("stopAsyncRead task submission success sensor name is:" + mSensor.getName());
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    printOperationResult("stopAsyncRead task error. sensor name is:" + mSensor.getName());
                }
            });
    }

    /**
     * Clear the result printing box.
     *
     * @param view UI object
     */
    public void clearResult(View view) {
        sensorResultTextView.setText("");
        sensorResultTextView.scrollTo(0, 0);
    }

    /**
     * Update the sensor display box.
     */
    private void updateSensorListBox() {
        sensorsRadioGroup.removeAllViews();
        for (int index = 0; index < mSensorList.size(); index++) {
            Sensor sensor = mSensorList.get(index);
            if (index == 0) {
                mSensor = sensor;
            }
            printOperationResult(sensor.getName());
            RadioButton deviceRadioButton = new RadioButton(this);
            setRaidButton(deviceRadioButton, sensor.getName(), index);
            sensorsRadioGroup.addView(deviceRadioButton);
        }
    }

    /**
     * Set the device list.
     */
    private void setRaidButton(final RadioButton radioButton, String text, int id) {
        radioButton.setChecked(id == 0);
        radioButton.setId(id);
        radioButton.setText(text);
    }

    /**
     * Check whether there are available devices or sensors for connection.
     *
     * @return true:Not available，false:available
     */
    private boolean checkParams() {
        return mSensor == null || mCurrentReadDevice == null || !mCurrentReadDevice.isConnected();
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
                sensorResultTextView.append(string + System.lineSeparator());
                int offset = sensorResultTextView.getLineCount() * sensorResultTextView.getLineHeight();
                if (offset > sensorResultTextView.getHeight()) {
                    sensorResultTextView.scrollTo(0, offset - sensorResultTextView.getHeight() + SCROLL_HIGH);
                }
            }
        });
    }
}