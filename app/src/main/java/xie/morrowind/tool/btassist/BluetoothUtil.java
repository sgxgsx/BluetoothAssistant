package xie.morrowind.tool.btassist;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import xie.morrowind.util.LogUtil;

final class BluetoothUtil {

    public static boolean isBonded(String name) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (pairedDevices != null && pairedDevices.size() > 0) {
            for(BluetoothDevice dev : pairedDevices) {
                if (name.equals(dev.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean createBond(BluetoothDevice device) {
        return callBooleanMethod(device.getClass(), "createBond", device);
    }

    public static boolean removeBond(BluetoothDevice device) {
        return callBooleanMethod(device.getClass(), "removeBond", device);
    }

    public static boolean removeBond(String name) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (pairedDevices != null && pairedDevices.size() > 0) {
            for(BluetoothDevice dev : pairedDevices) {
                if (name.equals(dev.getName())) {
                    return removeBond(dev);
                }
            }
        }
        return true;
    }

    public static boolean setPin(BluetoothDevice device, String code) {
        try {
            Method method = device.getClass().getDeclaredMethod("setPin", byte[].class);
            method.setAccessible(true);
            return (boolean) method.invoke(device, new Object[] { code.getBytes() } );
        } catch (Exception e) {
            LogUtil.x(e);
            return false;
        }
    }

    public static boolean cancelPairingUserInput(BluetoothDevice device) {
        return callBooleanMethod(device.getClass(), "cancelPairingUserInput", device);
    }

    public static boolean cancelBondProcess(BluetoothDevice device) {
        return callBooleanMethod(device.getClass(), "cancelBondProcess", device);
    }

    public static boolean setPairingConfirmation(BluetoothDevice device, boolean isConfirm) {
        try {
            Method method = device.getClass().getDeclaredMethod("setPairingConfirmation", boolean.class);
            method.setAccessible(true);
            method.invoke(device, isConfirm);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.x(e);
            return false;
        }
    }

    public static boolean connect(BluetoothDevice device) {
        UUID uuid = UUID.fromString("14c5449a-6267-4c7e-bd10-63dd79740e5d");
        try {
            BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            LogUtil.d(socket);
            socket.connect();
            return true;
        } catch (IOException e) {
            LogUtil.x(e);
            return false;
        }
    }

    private static boolean callBooleanMethod(Class<?> clz, String methodName, BluetoothDevice device) {
        try {
            Method method = clz.getMethod(methodName);
            method.setAccessible(true);
            return (boolean) method.invoke(device);
        } catch (Exception e) {
            LogUtil.x(e);
            return false;
        }
    }
}
