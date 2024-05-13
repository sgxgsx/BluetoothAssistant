# Bluetooth assistant(蓝牙测试辅助)

BluetoothAssistant is an Android application that bridges command line commands from Linux/Windows/Mac Operating Systems to Android devices over adb.
The tool was developed by morrowindxie and he agreed to share it. 
I have checked the code for possible vulnerabilities/backdoors and was not able to find any, but you are free to verify the code again.


这是一个安卓应用，用来辅助测试蓝牙设备。

它可接收adb shell发送的指令，帮忙代理执行手机上的蓝牙功能：

- (open)开启蓝牙
- (close)关闭蓝牙
- (pair)与指定的蓝牙设备配对 [注](#remark)
- (unpair)解除与指定设备的配对
- (discovery)搜索指定的设备是否在线
- (rename)修改本机蓝牙名称

编译方法：

- 下载工程代码
- 导入Android Studio编译运行即可

安装方法（Win/Mac/Linux）：

- adb shell install -r [apk的路径]

使用方法（Win/Mac/Linux）：

- 打开电脑上的终端，输入如下任意指令：
  - 打开 adb shell am start -n xie.morrowind.tool.btassist/.MainActivity -e test open
  - 关闭 adb shell am start -n xie.morrowind.tool.btassist/.MainActivity -e test close
  - 配对 adb shell am start -n xie.morrowind.tool.btassist/.MainActivity -e test pair -e device [device name]
  - 解配 adb shell am start -n xie.morrowind.tool.btassist/.MainActivity -e test unpair -e device [device name]
  - 搜索 adb shell am start -n xie.morrowind.tool.btassist/.MainActivity -e test discovery -e device [device name]
  - 命名 adb shell am start -n xie.morrowind.tool.btassist/.MainActivity -e test rename -e name [new name]

其中[device name]为需要配对/解配/搜索的蓝牙设备名称；[new name]为想要重命名的名字。

- 在终端上输入命令后，终端会立刻返回命令提示符。此时，可以循环检查手机存储卡上是否生成了结果文件（/sdcard/bluetooth.txt），若无此文件，说明命令还在执行中，否则说明命令已经执行完毕，执行结果会按如下格式存储在此文件中：
  - 第一行：指令名称；
  - 第二行：执行结果，成功为1，失败为0；
  - 第三行：额外信息，譬如失败的原因等，可能为空。

<a name="remark">注</a>：pair配对功能，需要满足如下条件才可使用：

- 使用手机的系统签名对apk进行重新签名（默认编译出来是sdk自带的签名）。
