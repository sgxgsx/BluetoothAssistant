package xie.morrowind.tool.btassist;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import xie.morrowind.util.LogUtil;

public class ConfigActivity extends Activity {

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.init(this);
        LogUtil.d();
        
        setContentView(R.layout.activity_config);

        // Init service button state.
        Button serviceButton = findViewById(R.id.btn_toggle_service);
        serviceButton.setOnClickListener(serviceButtonClickListener);
        boolean inService = isServiceRunning(this, BluetoothService.class.getName());
        if (inService) {
            serviceButton.setText(R.string.label_stop_service);
            serviceButton.setBackgroundColor(
                    getResources().getColor(android.R.color.holo_red_light));
        } else {
            serviceButton.setText(R.string.label_start_service);
            serviceButton.setBackgroundColor(
                    getResources().getColor(android.R.color.holo_blue_light));
        }
        serviceButton.setTag(inService); // remember on service status
        
        // Register service on/off broadcast.
        IntentFilter filter = new IntentFilter(BluetoothService.ACTION_BLUETOOTH_SERVICE_CHANGED);
        registerReceiver(serviceStateReceiver, filter);

        Intent serviceIntent = new Intent();
        serviceIntent.setAction("android.bluetooth.IBluetooth");
        serviceIntent.setPackage("com.android.bluetooth.btservice");
        ComponentName comp = new ComponentName("com.android.bluetooth.btservice", "com.android.bluetooth.btservice.AdapterService");
        //serviceIntent.setComponent(comp);
        bindService(serviceIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LogUtil.d();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                LogUtil.d();
            }
        }, Context.BIND_AUTO_CREATE);

        List<String> permList = new ArrayList<>();
        // Android 版本大于等于 12 时，申请新的蓝牙权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permList.add(Manifest.permission.BLUETOOTH_SCAN);
            permList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            permList.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            permList.add(Manifest.permission.BLUETOOTH);
            permList.add(Manifest.permission.BLUETOOTH_ADMIN);
            permList.add(Manifest.permission.BLUETOOTH_PRIVILEGED);
            permList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            permList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permList.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        } else {
            permList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    protected void onStop() {
        finish();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(serviceStateReceiver);
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    private final ServiceStateReceiver serviceStateReceiver = new ServiceStateReceiver(
            new ServiceStateReceiver.OnServiceStateChangedListener() {
                
        @Override
        public void onServiceStateChanged(boolean inService) {
            LogUtil.d("onServiceStateChanged, inService = "+inService);
            Button serviceButton = findViewById(R.id.btn_toggle_service);
            serviceButton.setTag(inService);
            serviceButton.setClickable(true);
            if (inService) {
                serviceButton.setText(R.string.label_stop_service);
                serviceButton.setBackgroundColor(
                        getResources().getColor(android.R.color.holo_red_light));
                
//                Toast.makeText(context, R.string.text_service_started, Toast.LENGTH_SHORT).show();
                
            } else {
                serviceButton.setText(R.string.label_start_service);
                serviceButton.setBackgroundColor(
                        getResources().getColor(android.R.color.holo_blue_light));
//                Toast.makeText(context, R.string.text_service_stopped, Toast.LENGTH_SHORT).show();
            }
        }
    });
    
    private final View.OnClickListener serviceButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(ConfigActivity.this, BluetoothService.class);
            Button serviceButton = findViewById(R.id.btn_toggle_service);
            boolean inService = (boolean) serviceButton.getTag();
            if (inService) {
                serviceButton.setClickable(false);
                stopService(intent);
                
            } else {
                serviceButton.setClickable(false);
                startService(intent);
            }
            
        }
    };

    private boolean isServiceRunning(Context context, String serviceClassName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        try {
            List<ActivityManager.RunningServiceInfo> runningServices = am.getRunningServices(Integer.MAX_VALUE);
            if (runningServices.size() == 0) {
                return false;
            }
            for (ActivityManager.RunningServiceInfo si : runningServices) {
                if (si.service.getClassName().equals(serviceClassName)) {
                    return true;
                }
            }

        } catch (SecurityException e) {
            LogUtil.x(e);
        }
        return false;
    }
}
