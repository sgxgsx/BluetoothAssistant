package xie.morrowind.tool.btassist;

import android.annotation.SuppressLint;
import android.os.Bundle;

import xie.morrowind.util.LogUtil;

@SuppressLint("MissingPermission")
public class RenameActivity extends BluetoothActivity {
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d();

        Bundle data = getIntent().getExtras();
        if (data != null) {
            name = data.getString("name");
        }
        if (name == null) {
            finishTest(false, "Name is empty.");
            return;
        }
        if (bluetoothAdapter.isEnabled()) {
            if (bluetoothAdapter.setName(name)) {
                finishTest(true);
            } else {
                finishTest(false, "Call setName() failed.");
            }
            return;
        }
        if (!bluetoothAdapter.enable()) {
            return;
        }
        LogUtil.d("Waiting for open...");
    }

    @Override
    void onBluetoothOpen(boolean succ) {
        super.onBluetoothOpen(succ);
        if (!succ) {
            finishTest(false, "Open bluetooth failed.");
        } else {
            if (bluetoothAdapter.setName(name)) {
                finishTest(true);
            } else {
                finishTest(false, "Call setName() failed.");
            }
        }
    }
}
