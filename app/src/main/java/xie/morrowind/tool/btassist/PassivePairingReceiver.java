package xie.morrowind.tool.btassist;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import xie.morrowind.util.LogUtil;

public final class PassivePairingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return;
        }
        LogUtil.d("Received event: " + action.substring(action.lastIndexOf('.')+1));
        switch (action) {
            case BluetoothDevice.ACTION_PAIRING_REQUEST:
                abortBroadcast();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                LogUtil.d("Pairing with " + device.getName());
                boolean ret = BluetoothUtil.setPairingConfirmation(device, true);
                LogUtil.d("setPairingConfirmation = " + ret);
                if (ret) {
                    ret = BluetoothUtil.setPin(device, "0000");
                    LogUtil.d("setPin = " + ret);
                }
                break;
            default:
                break;
        }
    }
}
