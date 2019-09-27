package xie.morrowind.tool.btassist;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;

import xie.morrowind.util.LogUtil;

public class OpenActivity extends BluetoothActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            finishTest(true, "Already opened.");
            return;
        }
        if (!bluetoothAdapter.enable()) {
            finishTest(false, "Invoke BluetoothAdapter.enable() failed.");
            return;
        }
        LogUtil.d("Waiting for open...");
    }

    @Override
    protected void onBluetoothOpen(boolean succ) {
        super.onBluetoothOpen(succ);
        finishTest(succ);
    }
}
