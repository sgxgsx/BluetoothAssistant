package xie.morrowind.tool.btassist;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Set;

import xie.morrowind.util.LogUtil;

public class MainActivity extends Activity {
    private final static String DEF_FILE = "bluetooth.txt";

    private String testName = null;
    private File resultFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.init(this);
        LogUtil.setEnforceOutput(true);
        LogUtil.d();

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            String errMsg = "手机不支持蓝牙功能或蓝牙出现故障，请检查。";
            Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        testName = getTestName();
        if (TextUtils.isEmpty(testName)) {
            String errMsg = "请通过adb shell来启动。";
            Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        resultFile = getResultFile();
        String clsName = testName.substring(0, 1).toUpperCase().concat(testName.substring(1));
        try {
            Class<?> cls = Class.forName(getPackageName() + "." + clsName + "Activity");
            if (Activity.class.isAssignableFrom(cls)) {
                Intent intent = new Intent();
                intent.setClass(this, cls);
                intent.putExtras(getIntent());
                startActivityForResult(intent, 0);
            } else {
                String errMsg = cls.getSimpleName() + "不是正确的Activity。";
                LogUtil.e(errMsg);
                Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
                writeResult(false, errMsg);
                finish();
            }

        } catch (ClassNotFoundException | ActivityNotFoundException e) {
            LogUtil.x(e);
            String errMsg = "测试指令\"" + testName + "\"暂不支持。";
            Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
            writeResult(false, errMsg);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String resCodeStr = (resultCode == Activity.RESULT_OK ? "RESULT_OK" : "RESULT_CANCELED");
        LogUtil.d("("+requestCode+", "+resCodeStr+")");
        if (requestCode == 0) {
            String reason = null;
            if (data != null) {
                reason = data.getStringExtra("reason");
            }
            writeResult(resultCode == Activity.RESULT_OK, reason);
            finish();
        }
    }

    private String getTestName() {
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    LogUtil.i("{" + key + ": " + extras.get(key) + "}");
                }
                return extras.getString("test");
            }
        }
        return null;
    }

    private File getResultFile() {
        String filepath = null;
        File file;
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                filepath = extras.getString("file");
            }
        }
        if (filepath == null) {
            filepath = DEF_FILE;
        }
        if (filepath.contains("/")) {
            file = new File(filepath);
        } else {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filepath);
        }
        if (file.exists()) {
            file.delete();
        }
        return file;
    }

    private void writeResult(boolean result) {
        writeResult(result, null);
    }

    private void writeResult(boolean result, String reason) {
        try {
            FileWriter fw = new FileWriter(resultFile);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(testName);
            bw.newLine();
            bw.write(result ? "1" : "0");
            if (!TextUtils.isEmpty(reason)) {
                bw.newLine();
                bw.write(reason);
            }
            bw.newLine();
            bw.close();
        } catch (Exception e) {
            LogUtil.x(e);
        }
    }
}
