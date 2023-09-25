package xie.morrowind.tool.btassist;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import xie.morrowind.util.LogUtil;

@SuppressLint("MissingPermission")
public class UnpairActivity extends BluetoothActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!BluetoothUtil.isBonded(this, deviceName)) {
            finishTest(true, "\"" + deviceName + "\" already unpaired.");
            return;
        }

        if (bluetoothAdapter.isEnabled()) {
            LogUtil.d("Already opened, start unpair \"" + deviceName + "\"...");
            if (!BluetoothUtil.removeBond(this, deviceName)) {
                finishTest(false, "Invoke removeBond() failed.");
            } else {
                LogUtil.d("Waiting for unpair...");
            }
        } else {
            LogUtil.d("Waiting open bluetooth first...");
            bluetoothAdapter.enable();
        }
    }

    @Override
    protected void onBluetoothOpen(boolean succ) {
        super.onBluetoothOpen(succ);
        if (succ) {
            LogUtil.d("Bluetooth opened, start unpair \"" + deviceName + "\"...");
            if (!BluetoothUtil.removeBond(this, deviceName)) {
                finishTest(false, "Invoke removeBond() failed.");
            } else {
                LogUtil.d("Waiting for unpair...");
            }
        } else {
            finishTest(false, "Open bluetooth failed.");
        }
    }

    @Override
    protected void onBluetoothUnbind(BluetoothDevice device) {
        super.onBluetoothUnbind(device);
        finishTest(true);
    }
}
