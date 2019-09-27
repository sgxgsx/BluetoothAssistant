package xie.morrowind.tool.btassist;

import android.view.accessibility.AccessibilityNodeInfo;

import xie.morrowind.util.LogUtil;
import xie.morrowind.util.android.AccessibilityUtilService;

public class BotService extends AccessibilityUtilService {

    @Override
    protected void onActivityChanged(String from, String to) {
        super.onActivityChanged(from, to);
        LogUtil.d(from + " -> " + to);
        if (to.equals("com.android.settings.bluetooth.BluetoothPairingDialog")) {
            printNodeTree(getRootInActiveWindow());
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) {
                return;
            }
            String id = "android:id/button1";
            AccessibilityNodeInfo pairButtonView = findFirstViewById(root, id);
            LogUtil.d("pariButtonView = " + pairButtonView);
            if(pairButtonView != null) {
                pairButtonView.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                pairButtonView.recycle();
            }
        }
    }

}
