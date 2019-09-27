package xie.morrowind.tool.btassist;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import xie.morrowind.util.LogUtil;

public class PairActivity extends BluetoothActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d();

        if (bluetoothAdapter.isEnabled()) {
            if (BluetoothUtil.isBonded(deviceName)) {
                finishTest(true, "\""+deviceName+"\" already bonded.");
                return;
            }
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
            if (BluetoothUtil.isBonded(deviceName)) {
                finishTest(true, "\""+deviceName+"\" already bonded.");
                return;
            }
            LogUtil.d("Bluetooth opened, start discovery now...");
            bluetoothAdapter.startDiscovery();
        } else {
            finishTest(false, "Open bluetooth failed.");
        }
    }

    @Override
    protected void onBluetoothDeviceFound(BluetoothDevice device) {
        super.onBluetoothDeviceFound(device);
        if (!device.createBond()) {
            finishTest(false, "createBond failed.");
        } else {
            LogUtil.d("Waiting for bond...");
        }
    }

    @Override
    protected void onBluetoothBonded(BluetoothDevice device, boolean succ) {
        super.onBluetoothBonded(device, succ);
        finishTest(true);
    }
}
