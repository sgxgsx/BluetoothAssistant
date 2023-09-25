package xie.morrowind.tool.btassist;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.text.TextUtils;

import xie.morrowind.util.LogUtil;

@SuppressLint("MissingPermission")
public class DiscoveryActivity extends BluetoothActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LogUtil.d();
        if (TextUtils.isEmpty(deviceName)) {
            finishTest(false, "Device name not specified.");
            return;
        }

        if (bluetoothAdapter.isEnabled()) {
            // TODO: 判断蓝牙是否已连接，如果连接的设备是想要发现的设备，直接返回成功。
            LogUtil.d("Already opened, start discovery now...");
            bluetoothAdapter.startDiscovery();
        } else {
            LogUtil.d("Waiting open bluetooth first...");
            bluetoothAdapter.enable();
        }
    }

    @Override
    protected void onBluetoothOpen(boolean succ) {
        super.onBluetoothOpen(succ);
        if (succ) {
            LogUtil.d("Bluetooth opened, start discovery now...");
            bluetoothAdapter.startDiscovery();
        }
    }

    @Override
    protected void onBluetoothDeviceFound(BluetoothDevice device) {
        super.onBluetoothDeviceFound(device);
        finishTest(true);
    }
}
