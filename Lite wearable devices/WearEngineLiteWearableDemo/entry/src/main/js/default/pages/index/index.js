import brightness from '@system.brightness'
import {P2pClient, Message, Builder} from "../wearEngine/wearengine.js"
import app from '@system.app'

var messgeClient = new P2pClient();
var infoClient = new Message();
var builderClient = new Builder();

module.exports = {
    data: {
        operateMessage: 'Operation info',
        receiveMessgeOK: 'Received message',
    },
    onInit() {
        brightness.setKeepScreenOn({
            keepScreenOn: true,
            success: function () {
                console.log('screen on success');
            },
            fail: function () {
                console.log('screen on failed');
            },
        });

        messgeClient.setPeerPkgName("com.huawei.hiwear.test");
    },
    onDestroy() {},
    registerMessage() {
        var flash = this;
        console.log('Register message button click');
        flash.operateMessage = "Register message button click";
        messgeClient.registerReceiver({
            onSuccess: function () {
                flash.receiveMessgeOK = "Message receive success";
            },
            onFailure: function () {
                flash.receiveMessgeOK = "Message receive fail";
            },
            onReceiveMessage: function (data) {
                if (data && data.isFileType) {
                    flash.receiveMessgeOK = "Receive file name:" + data.name;
                }else{
                    flash.receiveMessgeOK = "Receive message info:" + data;
                }
            },
        });
    },
    unregisterMessage() {
        this.operateMessage = "Register message button click";
        var flash = this;
        messgeClient.unregisterReceiver({
            onSuccess: function () {
                flash.operateMessage = "Stop receiving messages is sent";
            },
        });
    },
    sendMessage() {
        builderClient.setDescription("hello wearEngine");
        infoClient.builder = builderClient;
        this.operateMessage = "Send message button click";
        console.log("testBuilder" + infoClient.getData());
        var flashlight = this;
        messgeClient.send(infoClient, {
            onSuccess: function () {
                flashlight.operateMessage = "Message sent successfully";
            },
            onFailure: function () {
                flashlight.operateMessage = "Failed to send message";
            },
            onSendResult: function (resultCode) {
                console.log(resultCode.data + resultCode.code);
            },
            onSendProgress: function (count) {
                console.log(count);
            },
        });

    },
    sendFile() {
        var testFile = {
            "name" : "internal://app/ccc.png",
            "mode" : "",
            "mode2" : "",
        };

        builderClient.setPayload(testFile);
        console.log("setPayload");

        infoClient.builder = builderClient;
        this.operateMessage = "Send file button click";
        console.log("testFileBuilder: " + infoClient.getFile().name);
        var flashlight = this;

        messgeClient.send(infoClient,{
            onSuccess: function() {
                flashlight.operateMessage = "File sent successfully";
            },
            onFailure: function() {
                flashlight.operateMessage = "Failed to send file";
            },
            onSendResult: function(resultCode) {
                console.log(resultCode.data + resultCode.code);
            },
            onSendProgress: function(count) {
                console.log("Progress:" + count);
            },
        });
    },
    pingRight() {
        console.log("ping right");
        var flashlight = this;
        messgeClient.setPeerPkgName("com.huawei.hiwear.test");
        flashlight.operateMessage = "Ping correct APP";
        messgeClient.ping({
            onSuccess: function () {
                flashlight.operateMessage = flashlight.operateMessage + "success";
            },
            onFailure: function () {
                flashlight.operateMessage = flashlight.operateMessage + "fail";
            },
            onPingResult: function (resultCode) {
                flashlight.operateMessage = "result code:" + resultCode.code + ", the app already have installed";
            },
        });

    },
    pingFalse() {
        console.log("ping false");
        var flashlight = this;
        messgeClient.setPeerPkgName("com.huawei.mywearengine.test1111");
        flashlight.operateMessage = "Ping wrong APP";
        messgeClient.ping({
            onSuccess: function () {
                flashlight.operateMessage = flashlight.operateMessage + "success";
            },
            onFailure: function () {
                flashlight.operateMessage = flashlight.operateMessage + "fail";
            },
            onPingResult: function (resultCode) {
                flashlight.operateMessage = "result code:" + resultCode.code + ", the app not installed";
            },
        });

    },
    swipeEvent(e) {
        if (e.direction == "right") {
            app.terminate();
        }
    },
}
