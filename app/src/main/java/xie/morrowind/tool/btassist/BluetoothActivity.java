package xie.morrowind.tool.btassist;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import xie.morrowind.util.LogUtil;
import xie.morrowind.util.ReflectUtil;

public abstract class BluetoothActivity extends Activity {
    public static String testDeviceName = "test-bt";

    void onBluetoothOpen(boolean success) {
        LogUtil.d("success = " + success);
    }

    void onBluetoothClose(boolean succ) {
        LogUtil.d("succ = " + succ);
    }

    void onBluetoothBonded(BluetoothDevice device, boolean succ) {
        LogUtil.d("Bond " + device.getName() + " " + (succ ? "succ" : "fail"));
    }

    void onBluetoothUnbind(BluetoothDevice device) {
        LogUtil.d("Unbond with " + device.getName());
    }

    void onBluetoothConnected(BluetoothDevice device, boolean succ) {
        LogUtil.d("Connect " + device.getName() + " " + (succ ? "succ" : "fail"));
    }

    void onBluetoothDisconnected(BluetoothDevice device, boolean succ) {
        LogUtil.d("Disconnect " + device.getName() + " " + (succ ? "succ" : "fail"));
    }

    void onBluetoothDeviceFound(BluetoothDevice device) {
        LogUtil.d("Found device: " + device.getName());
    }

    final void finishTest(boolean result) {
        finishTest(result, null);
    }
    final void finishTest(boolean result, String reason) {
        LogUtil.d(result + ", " + reason);
        Intent data = new Intent();
        if(!TextUtils.isEmpty(reason)) {
            data.putExtra("reason", reason);
        }
        int res = (result ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
        BluetoothActivity.this.setResult(res, data);
        finish();
    }

    String deviceName = null;
    final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BaseAdapter listAdapter = null;
    private final List<BluetoothDevice> deviceList = new ArrayList<>();
    private BroadcastReceiver receiver = null;
    private BluetoothDevice targetDevice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        if (data != null) {
            deviceName = getIntent().getExtras().getString("device");
        } else {
            deviceName = "";
        }
        LogUtil.d("target device: " + deviceName);

        setContentView(R.layout.activity_main);
        ListView listView = findViewById(R.id.device_list);
        listAdapter = new DeviceListAdapter(this);
        listView.setAdapter(listAdapter);

        receiver = new BluetoothReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        LogUtil.d();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        super.onDestroy();
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
                    if (prevState == BluetoothAdapter.STATE_TURNING_ON && currState == BluetoothAdapter.STATE_ON) {
                        onBluetoothOpen(true);
                    } else if (prevState == BluetoothAdapter.STATE_TURNING_ON && currState == BluetoothAdapter.STATE_OFF) {
                        onBluetoothOpen(false);
                    } else if (prevState == BluetoothAdapter.STATE_TURNING_OFF && currState == BluetoothAdapter.STATE_OFF) {
                        onBluetoothClose(true);
                    } else if (prevState == BluetoothAdapter.STATE_TURNING_OFF && currState == BluetoothAdapter.STATE_ON) {
                        onBluetoothClose(false);
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    deviceList.clear();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    if (targetDevice == null) {
                        finishTest(false);
                    }
                    break;

                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int deviceClass = device.getBluetoothClass().getDeviceClass();
                    LogUtil.d("Found device: " + device.getName() + ", class: " + String.format("%04X", deviceClass));
                    deviceList.add(0, device);
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                    if (deviceName.equals(device.getName())) {
                        LogUtil.d("Match target device!");
                        targetDevice = device;
                        bluetoothAdapter.cancelDiscovery();
                        onBluetoothDeviceFound(device);
                    }
                    /*
                    if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET
                            || deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
                            || deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
                            || deviceClass == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER) {
                    }*/
                    break;
                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    abortBroadcast();
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    LogUtil.d("Pairing with " + device.getName());
                    boolean ret = BluetoothUtil.setPairingConfirmation(device, true);
                    LogUtil.d("setPairingConfirmation = " + ret);
                    if (ret) {
                        ret = BluetoothUtil.setPin(device, "0000");
                        LogUtil.d("setPin = " + ret);
                    }
                    if (ret) {
                        LogUtil.d("Waiting for bond state changing...");
                    } else {
                        finishTest(false, "Permission denied.");
                    }
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
                    currState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                    prevStateStr = ReflectUtil.getFieldName(BluetoothDevice.class, prevState, "BOND_.*");
                    currStateStr = ReflectUtil.getFieldName(BluetoothDevice.class, currState, "BOND_.*");
                    LogUtil.d("Bond state: " + prevStateStr + " => " + currStateStr);
                    if (prevState == BluetoothDevice.BOND_BONDING && currState == BluetoothDevice.BOND_BONDED) {
                        onBluetoothBonded(device, true);
                    } else if (prevState == BluetoothDevice.BOND_BONDING && currState == BluetoothDevice.BOND_NONE) {
                        onBluetoothBonded(device, false);
                    } else if (prevState == BluetoothDevice.BOND_BONDED && currState == BluetoothDevice.BOND_NONE) {
                        onBluetoothUnbind(device);
                    }
                    break;

                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    prevState = intent.getIntExtra(BluetoothA2dp.EXTRA_PREVIOUS_STATE, BluetoothA2dp.STATE_DISCONNECTED);
                    currState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED);
                    prevStateStr = ReflectUtil.getFieldName(BluetoothProfile.class, prevState, "STATE_.*");
                    currStateStr = ReflectUtil.getFieldName(BluetoothProfile.class, currState, "STATE_.*");
                    LogUtil.d("Connection state: " + prevStateStr + " => " + currStateStr);
                    if (prevState == BluetoothProfile.STATE_CONNECTING && currState == BluetoothProfile.STATE_CONNECTED) {
                        onBluetoothConnected(device, true);
                    } else if (prevState == BluetoothProfile.STATE_CONNECTING && currState == BluetoothProfile.STATE_DISCONNECTED) {
                        onBluetoothConnected(device, false);
                    } else if (prevState == BluetoothProfile.STATE_DISCONNECTING && currState == BluetoothProfile.STATE_DISCONNECTED) {
                        onBluetoothDisconnected(device, true);
                    } else if (prevState == BluetoothProfile.STATE_DISCONNECTING && currState == BluetoothProfile.STATE_CONNECTED) {
                        onBluetoothDisconnected(device, false);
                    }
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

    private class DeviceListAdapter extends BaseAdapter {
        final Context context;

        DeviceListAdapter(Context context) {
            super();
            this.context = context;
        }

        @Override
        public int getCount() {
            return deviceList.size();
        }

        @Override
        public BluetoothDevice getItem(int position) {
            return deviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return deviceList.get(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.device_item, parent, false);
            }
            BluetoothDevice device = getItem(position);
            TextView nameView = convertView.findViewById(R.id.name_view);
            nameView.setText(device.getName());
            if (!TextUtils.isEmpty(device.getName()) && device.getName().contains(testDeviceName)) {
                nameView.setTextColor(Color.YELLOW);
            } else {
                nameView.setTextColor(getColor(android.R.color.primary_text_dark));
            }
            TextView addressView = convertView.findViewById(R.id.address_view);
            addressView.setText(device.getAddress());
            return convertView;
        }
    }
}
