# Bluetooth
蓝牙模块HC05和Android通信的App

### HC05蓝牙模块

作为Server，设置成从模式

### 手机

作为Client，在Android中给定蓝牙设备的MAC地址去连接，UUID要设置成串口操作专用的00001101-0000-1000-8000-00805F9B34FB。 连接上蓝牙设备之后通过Handler向主线程传送连接状态、收发消息。主线程更新UI