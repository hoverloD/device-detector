## Environment

IDEA 2019，Android SDK Platform 8.1(API level 27)。

Test environment:
- OnePlus 8T，raspberry pi 3B+
- ARCore 1.36.230390483


## Use

At first use [unity part of this project](https://github.com/hoverloD/devicedetector-unity) to build Unity3D AR project, export Android Engineering Project, put this in unityLibrary folder. Remember to change the mac defined in UnityPlayerActivity to the mac of your raspberry pi.


Build and install the apk, grant authority of camera and GPS, open bluetooth. 

Run `sudo node start-ble.js`([BLE part here](https://github.com/hoverloD/device-detector-ble)), then open this apk, bluetooth will connect automatically, you'll see VIO data sent to raspberry pi, printed to console and saved in vioData.json. 

Use your algorithm to get the device location, write it in deviceLocation.json, then monitor code will find the file has changed, then notify the smartphone, the apk will show a toast in about 2 seconds: "new device discovered, please open the list to view". Open the device list to see the locators.



## Bluetooth

raspberry: nodejs 8.9.0, bleno 0.5.0

android：FastBLE 2.4.0


nodejs:
	curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
	sudo apt-get install -y nodejs

npm:
	sudo apt-get -f install npm

Bleno:
	sudo npm install bleno --unsafe-perm 

change the version of nodejs:
	sudo npm install -g n
	sudo n 8.9.0

bluetooth-hci-socket:
	sudo npm install bluetooth-hci-socket --unsafe-perm 


## Tips

Make sure the OS of raspberry pi has a corresponding version of nodejs. Use `dpkg --print-architecture` to view system architecture, and check https://registry.npmmirror.com/binary.html?path=node/v8.9.0/ to confirm.


## APK

If you need a fast preview, download the APK that has been built:

[device-detector.apk](https://github.com/hoverloD/device-detector/raw/main/device-detector.apk)