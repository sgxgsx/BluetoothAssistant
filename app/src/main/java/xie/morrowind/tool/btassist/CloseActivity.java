package xie.morrowind.tool.btassist;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;

import xie.morrowind.util.LogUtil;

@SuppressLint("MissingPermission")
public class CloseActivity extends BluetoothActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            finishTest(true, "Already closed.");
            return;
        }
        if (!bluetoothAdapter.disable()) {
            finishTest(false, "Invoke BluetoothAdapter.disable() failed.");
            return;
        }
        LogUtil.d("Waiting for close...");
    }

    @Override
    protected void onBluetoothClose(boolean succ) {
        super.onBluetoothClose(succ);
        finishTest(succ);
    }
}
