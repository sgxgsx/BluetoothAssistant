package xie.morrowind.tool.btassist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import xie.morrowind.util.LogUtil;
import xie.morrowind.util.ReflectUtil;

public class BluetoothService extends Service {
    public final static String ACTION_BLUETOOTH_SERVICE_CHANGED =
            "xie.morrowind.intent.action.BLUETOOTH_SERVICE_CHANGED";
    public final static String EXTRA_SERVING_STATE = "service state";

    private boolean hook = false;
    private BroadcastReceiver receiver = null;
    private IntentFilter filter = null;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.e();

        // Set notification to avoid low memory kill.
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent pendingintent = PendingIntent.getActivity(this, 0,
                new Intent(this, ConfigActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(this, "channel_service");

            NotificationChannel channel = new NotificationChannel("channel_service", BluetoothService.class.getSimpleName(), NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableVibration(true);
            channel.enableLights(true);
            nm.createNotificationChannel(channel);
        } else {
            b = new Notification.Builder(this);
        }
        b.setCategory(Notification.CATEGORY_SERVICE)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.label_service_started_title))
                .setContentText(getText(R.string.text_service_started_content))
                .setContentIntent(pendingintent);
        Notification n = b.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LogUtil.e("Call startForeground, sdk = " + Build.VERSION.SDK_INT);
            startForeground(1, n);
        } else {
            nm.notify(1, n);
        }

        // Send in service broadcast.
        Intent broadcastIntent = new Intent(ACTION_BLUETOOTH_SERVICE_CHANGED);
        broadcastIntent.putExtra(EXTRA_SERVING_STATE, true);
        sendBroadcast(broadcastIntent);

        // Register bluetooth receiver.
        filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
        //receiver = new PassivePairingReceiver();
        //registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d(intent != null ? intent.toString() : "null intent");
        LogUtil.d("startId = " + startId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        LogUtil.d();

        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }

        // Remove notification.
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(1);
        stopForeground(true);

        // Send out of service broadcast.
        Intent intent = new Intent(ACTION_BLUETOOTH_SERVICE_CHANGED);
        intent.putExtra(EXTRA_SERVING_STATE, false);
        sendBroadcast(intent);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        LogUtil.d();
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        hook = true;
        return null;
    }

    @Override
    public void onRebind(Intent intent) {
        LogUtil.d();
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        hook = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtil.d();
        receiver = new BluetoothReceiver();
        registerReceiver(receiver, filter);
        hook = false;
        return true;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        LogUtil.d();
        super.onTaskRemoved(rootIntent);
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            LogUtil.d("Received event: " + action.substring(action.lastIndexOf('.')+1));
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);
                    int currState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    String prevStateStr = ReflectUtil.getFieldName(BluetoothAdapter.class, prevState, "STATE_.*");
                    String currStateStr = ReflectUtil.getFieldName(BluetoothAdapter.class, currState, "STATE_.*");
                    LogUtil.d("Radio state: " + prevStateStr + " => " + currStateStr);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    break;

                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int deviceClass = device.getBluetoothClass().getDeviceClass();
                    LogUtil.d("Found device: " + device.toString() + ", class: " + String.format("%04X", deviceClass));
                    break;
                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    if (!hook) {
                        abortBroadcast();
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        LogUtil.d("Pairing with " + device.getName());
                        BluetoothUtil.setPairingConfirmation(device, true);
                        BluetoothUtil.setPin(device, "0000");
                    }
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
                    currState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                    prevStateStr = ReflectUtil.getFieldName(BluetoothDevice.class, prevState, "BOND_.*");
                    currStateStr = ReflectUtil.getFieldName(BluetoothDevice.class, currState, "BOND_.*");
                    LogUtil.d("Bond state: " + prevStateStr + " => " + currStateStr);
                    break;

                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                    prevState = intent.getIntExtra(BluetoothA2dp.EXTRA_PREVIOUS_STATE, BluetoothA2dp.STATE_DISCONNECTED);
                    currState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED);
                    prevStateStr = ReflectUtil.getFieldName(BluetoothProfile.class, prevState, "STATE_.*");
                    currStateStr = ReflectUtil.getFieldName(BluetoothProfile.class, currState, "STATE_.*");
                    LogUtil.d("Connection state: " + prevStateStr + " => " + currStateStr);
                    break;
                case BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED:
                    prevState = intent.getIntExtra(BluetoothA2dp.EXTRA_PREVIOUS_STATE, BluetoothA2dp.STATE_NOT_PLAYING);
                    currState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_NOT_PLAYING);
                    prevStateStr = ReflectUtil.getFieldName(BluetoothA2dp.class, prevState, "STATE_.*");
                    currStateStr = ReflectUtil.getFieldName(BluetoothA2dp.class, currState, "STATE_.*");
                    LogUtil.d("Audio state: " + prevStateStr + " => " + currStateStr);
                    break;
                default:
                    break;
            }
        }
    }
}
