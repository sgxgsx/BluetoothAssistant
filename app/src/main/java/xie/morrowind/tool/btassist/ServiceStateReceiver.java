
package xie.morrowind.tool.btassist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceStateReceiver extends BroadcastReceiver {
    
    public interface OnServiceStateChangedListener {
        
        void onServiceStateChanged(boolean onService);
        
    }
    
    private final OnServiceStateChangedListener listener;
    
    public ServiceStateReceiver(OnServiceStateChangedListener listener) {
        super();
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(BluetoothService.ACTION_BLUETOOTH_SERVICE_CHANGED.equals(action) && listener != null) {
            boolean onService = intent.getBooleanExtra(BluetoothService.EXTRA_SERVING_STATE, false);
            listener.onServiceStateChanged(onService);
        }
    }

}
