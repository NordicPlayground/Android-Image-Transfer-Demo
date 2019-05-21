# android-image-transfer-demo

This app allows images to be streamed from an nRF52/nRF52840 kit with a connected camera sensor to the application, showing the image and measuring the transfer speed in the process. 

Different image resolutions can be selected in the app, and the BLE phy can be changed between 1Mbps and 2Mbps to demonstrate the difference (this requires a phone that supports 2Mbps). 

**Tested on:** 
	
| Phone | Speed | 2M high speed support |
| ----- | ----- | ----- |
| Samsung Galaxy S10 | ~1200kbps | Yes |
| Samsung Galaxy S8 | ~1200kbps | Yes |
| Samsung Galaxy S6 | ~90kbps | No |
| Nexus 5X | ~540kbps | No |
| Nexus 5 | ~42kbps | No |
| Huawei P20 Pro | ~460kbps | Poor (only 1 packet per con int in 2M mode, leading to worse throughput) |
| Lenovo Tab 4 10 Wifi | ~57kbps | No |


### Note
- Android 4.3 or later is required.
- Android Studio supported 
