package xie.morrowind.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

/**
 * Wrapper and extension of {@link android.util.Log} APIs for sending log output.<br/>
 *
 * <b>Features:</b>
 * <ul>
 * <li/>Insert invoking method name and code line number at front of user log messages;
 * <li/>Set default log tag as package name of current class, you can change it by {@link #setTag(CharSequence)};
 * <li/>An extended API to directly print object instance content;
 * <li/>Support cache main logs and dump it to external storage if necessary;
 * <li/>Wrap {@link Throwable#printStackTrace()} into {@link #x(Throwable)}.
 * </ul>
 * For example:<br/>
 * <pre><code>
 *  1| package com.example;
 *  2|
 *  3| import android.app.Activity;
 *  4| import android.os.Bundle;
 *  5|
 *  6| import xie.morrowind.util.LogUtil;
 *  7|
 *  8| public class MainActivity extends Activity {
 *  9|
 * 10|     @Override
 * 11|     protected void onCreate(Bundle savedInstanceState) {
 * 12|         super.onCreate(savedInstanceState);
 * 13|         LogUtil.d("Hello!");
 * 14|     }
 * 15| }
 * </code></pre>
 * Will output<br/>
 * 1554192176.437 9747-9747/com.example D/com.example: MainActivity.onCreate(13)   Hello!<br/>
 * Which "onCreate(13)" means you print the log at source file line 13.<br/>
 * <br/>
 * The order in terms of verbosity, from least to most is <code>FAULT, ERROR, WARN, INFO, DEBUG, VERBOSE</code>.<br/>
 * <code>VERBOSE</code> should never be compiled into an application except during development.<br/>
 * <code>DEBUG</code> logs are compiled in but stripped at runtime.<br/>
 * <code>FAULT, ERROR, WARNING and INFO</code> logs are always kept.<br/>
 *
 * @version 2.0
 * @author morrowindxie
 */
public final class LogUtil {
    public static final boolean IS_DEBUGGABLE = !"user".equals(Build.TYPE);
    private static boolean ENFORCE_OUTPUT = IS_DEBUGGABLE;
    private static String TAG = null;
    private static boolean cache = false;
    private static final String FAULT_FILE = "error.log";
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static void init(@NonNull Context context) {
        // 设置默认TAG为应用程序的名称
        CharSequence label = context.getPackageName();
        try {
            int flags = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                flags = PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS;
            }
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), flags);
            if (pi.applicationInfo != null) {
                label = pi.applicationInfo.loadLabel(context.getPackageManager());
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        setTag(label);

        // 先尝试从app包名.BuildConfig读取DEBUG字段，作为是否强制输出log的依据
        try {
            String packageName = context.getPackageName();
            Class<?> cls = Class.forName(packageName + ".BuildConfig");
            Field field = cls.getDeclaredField("DEBUG");
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                if (field.getType().equals(boolean.class)) {
                    boolean debuggable = field.getBoolean(null);
                    setEnforceOutput(debuggable);
                    return;
                }
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        // 再尝试根据AndroidManifest中Application节点定义的debuggable元素来判断
        ApplicationInfo ai = context.getApplicationInfo();
        setEnforceOutput((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
    }

    /**
     * Set default log tag.
     * @param tag The log tag used to filter.
     */
    public static void setTag(CharSequence tag) {
        TAG = tag.toString();
    }

    /**
     * Get log tag.
     */
    private static String getTag() {
        String tag = TAG;
        if(TextUtils.isEmpty(tag)) {
            tag = getPackageName();
            if(TextUtils.isEmpty(tag)) {
                tag = LogUtil.class.getSimpleName();
            }
        }
        if (tag != null && tag.length() > 23) {
            tag = tag.substring(0, 23);
        }
        return tag;
    }

    /**
     * Enable/disable log cache for later dump.<br/>
     * All log messages will be cached to an array list.<br/>
     * Maximal number of messages is defined by {@link #MAX_CACHE}.
     * @param enable <code>true</code> if want to enable cache mechanism else <code>false</code>.
     */
    public static void setCache(boolean enable) {
        cache = enable;
    }

    public static void setEnforceOutput(boolean enforceOutput) {
        ENFORCE_OUTPUT = enforceOutput;
    }

    /**
     * Send an empty {@link Log#VERBOSE} level message with default {@link #TAG}.
     * @return The number of output characters.
     */
    public static int v() {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.VERBOSE)) {
            String trace = getStackTrace(null);
            if (cache) {
                offerToList(TAG, trace);
            }
            return Log.v(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#VERBOSE} level message.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     */
    public static int v(String msg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.VERBOSE)) {
            String trace = getStackTrace(msg);
            if (cache) {
                offerToList(TAG, trace);
            }
            return Log.v(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#VERBOSE} level message.
     * @param value The boolean value you would like logged.
     * @return The number of output characters.
     */
    public static int v(boolean value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.VERBOSE)) {
            String trace = getStackTrace(Boolean.toString(value));
            if (cache) {
                offerToList(TAG, trace);
            }
            return Log.v(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#VERBOSE} level message.
     * @param value The integer value you would like logged.
     * @return The number of output characters.
     */
    public static int v(int value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.VERBOSE)) {
            String trace = getStackTrace(value < 0x10000 ? Integer.toString(value) : "0x"+Integer.toHexString(value));
            if (cache) {
                offerToList(TAG, trace);
            }
            return Log.v(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#VERBOSE} level message.
     * @param value The long value you would like logged.
     * @return The number of output characters.
     */
    public static int v(long value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.VERBOSE)) {
            String trace = getStackTrace(value < 0x10000 ? Long.toString(value) : "0x"+Long.toHexString(value));
            if (cache) {
                offerToList(TAG, trace);
            }
            return Log.v(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#VERBOSE} level message.
     * @param value The float value you would like logged.
     * @return The number of output characters.
     */
    public static int v(float value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.VERBOSE)) {
            String trace = getStackTrace(Float.toString(value));
            if (cache) {
                offerToList(TAG, trace);
            }
            return Log.v(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#VERBOSE} level message.
     * @param value The double value you would like logged.
     * @return The number of output characters.
     */
    public static int v(double value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.VERBOSE)) {
            String trace = getStackTrace(Double.toString(value));
            if (cache) {
                offerToList(TAG, trace);
            }
            return Log.v(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#VERBOSE} level message.
     * @param arg An object of argument to print.
     * @return The number of output characters.
     */
    public static int v(Object arg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.VERBOSE)) {
            String trace = getStackTrace("[" + arg);
            if (cache) {
                offerToList(TAG, trace);
            }
            return Log.v(tag, trace);
        }
        return 0;
    }

    /**
     * Send an empty {@link Log#DEBUG} level message.
     * @return The number of output characters.
     */
    public static int d() {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.DEBUG)) {
            String trace = getStackTrace(null);
            if (cache) {
                offerToList(TAG, trace);
            }
            return Log.d(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#DEBUG} level message.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     */
    public static int d(String msg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.DEBUG)) {
            String trace = getStackTrace(msg);
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.d(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#DEBUG} level message.
     * @param value The boolean value you would like logged.
     * @return The number of output characters.
     */
    public static int d(boolean value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.DEBUG)) {
            String trace = getStackTrace(Boolean.toString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.d(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#DEBUG} level message.
     * @param value The integer value you would like logged.
     * @return The number of output characters.
     */
    public static int d(int value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.DEBUG)) {
            String trace = getStackTrace(value < 0x10000 ? Integer.toString(value) : "0x"+Integer.toHexString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.d(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#DEBUG} level message.
     * @param value The long value you would like logged.
     * @return The number of output characters.
     */
    public static int d(long value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.DEBUG)) {
            String trace = getStackTrace(value < 0x10000 ? Long.toString(value) : "0x"+Long.toHexString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.d(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#DEBUG} level message.
     * @param value The float value you would like logged.
     * @return The number of output characters.
     */
    public static int d(float value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.DEBUG)) {
            String trace = getStackTrace(Float.toString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.d(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#DEBUG} level message.
     * @param value The double value you would like logged.
     * @return The number of output characters.
     */
    public static int d(double value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.DEBUG)) {
            String trace = getStackTrace(Double.toString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.d(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#DEBUG} level message.
     * @param arg An object of argument to print.
     * @return The number of output characters.
     */
    public static int d(Object arg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.DEBUG)) {
            String trace = getStackTrace("[" + arg);
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.d(tag, trace);
        }
        return 0;
    }

    /**
     * Send an empty {@link Log#INFO} level message.
     * @return The number of output characters.
     */
    public static int i() {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.INFO)) {
            String trace = getStackTrace(null);
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.i(tag, trace);
        }
        return 0;
    }

    /**
     * Send an {@link Log#INFO} level message.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     */
    public static int i(String msg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.INFO)) {
            String trace = getStackTrace(msg);
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.i(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#INFO} level message.
     * @param value The boolean value you would like logged.
     * @return The number of output characters.
     */
    public static int i(boolean value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.INFO)) {
            String trace = getStackTrace(Boolean.toString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.i(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#INFO} level message.
     * @param value The integer value you would like logged.
     * @return The number of output characters.
     */
    public static int i(int value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.INFO)) {
            String trace = getStackTrace(value < 0x10000 ? Integer.toString(value) : "0x"+Integer.toHexString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.i(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#INFO} level message.
     * @param value The long value you would like logged.
     * @return The number of output characters.
     */
    public static int i(long value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.INFO)) {
            String trace = getStackTrace(value < 0x10000 ? Long.toString(value) : "0x"+Long.toHexString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.i(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#INFO} level message.
     * @param value The float value you would like logged.
     * @return The number of output characters.
     */
    public static int i(float value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.INFO)) {
            String trace = getStackTrace(Float.toString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.i(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#INFO} level message.
     * @param value The double value you would like logged.
     * @return The number of output characters.
     */
    public static int i(double value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.INFO)) {
            String trace = getStackTrace(Double.toString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.i(tag, trace);
        }
        return 0;
    }

    /**
     * Send an {@link Log#INFO} level message.
     * @param arg An object of argument to print.
     * @return The number of output characters.
     */
    public static int i(Object arg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.INFO)) {
            String trace = getStackTrace("[" + arg);
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.i(tag, trace);
        }
        return 0;
    }

    /**
     * Send an empty {@link Log#WARN} level message.
     * @return The number of output characters.
     */
    public static int w() {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.WARN)) {
            String trace = getStackTrace(null);
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.w(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#WARN} level message.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     */
    public static int w(String msg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.WARN)) {
            String trace = getStackTrace(msg);
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.w(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#WARN} level message.
     * @param value The boolean value you would like logged.
     * @return The number of output characters.
     */
    public static int w(boolean value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.WARN)) {
            String trace = getStackTrace(Boolean.toString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.w(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#WARN} level message.
     * @param value The integer value you would like logged.
     * @return The number of output characters.
     */
    public static int w(int value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.WARN)) {
            String trace = getStackTrace(value < 0x10000 ? Integer.toString(value) : "0x"+Integer.toHexString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.w(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#WARN} level message.
     * @param value The long value you would like logged.
     * @return The number of output characters.
     */
    public static int w(long value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.WARN)) {
            String trace = getStackTrace(value < 0x10000 ? Long.toString(value) : "0x"+Long.toHexString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.w(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#WARN} level message.
     * @param value The float value you would like logged.
     * @return The number of output characters.
     */
    public static int w(float value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.WARN)) {
            String trace = getStackTrace(Float.toString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.w(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#WARN} level message.
     * @param value The double value you would like logged.
     * @return The number of output characters.
     */
    public static int w(double value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.WARN)) {
            String trace = getStackTrace(Double.toString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.w(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#WARN} level message.
     * @param arg An object of argument to print.
     * @return The number of output characters.
     */
    public static int w(Object arg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.WARN)) {
            String trace = getStackTrace("[" + arg);
            if (cache) {
                offerToList(tag, trace);
            }
            return android.util.Log.w(tag, trace);
        }
        return 0;
    }

    /**
     * Send an empty {@link Log#ERROR} level message.
     * @return The number of output characters.
     */
    public static int e() {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ERROR)) {
            String trace = getStackTrace(null);
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.e(tag, trace);
        }
        return 0;
    }

    /**
     * Send an {@link Log#ERROR} level message.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     */
    public static int e(String msg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ERROR)) {
            String trace = getStackTrace(msg);
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.e(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#ERROR} level message.
     * @param value The boolean value you would like logged.
     * @return The number of output characters.
     */
    public static int e(boolean value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ERROR)) {
            String trace = getStackTrace(Boolean.toString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.e(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#ERROR} level message.
     * @param value The integer value you would like logged.
     * @return The number of output characters.
     */
    public static int e(int value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ERROR)) {
            String trace = getStackTrace(value < 0x10000 ? Integer.toString(value) : "0x"+Integer.toHexString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.e(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#ERROR} level message.
     * @param value The long value you would like logged.
     * @return The number of output characters.
     */
    public static int e(long value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ERROR)) {
            String trace = getStackTrace(value < 0x10000 ? Long.toString(value) : "0x"+Long.toHexString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.e(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#ERROR} level message.
     * @param value The float value you would like logged.
     * @return The number of output characters.
     */
    public static int e(float value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ERROR)) {
            String trace = getStackTrace(Float.toString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.e(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#ERROR} level message.
     * @param value The double value you would like logged.
     * @return The number of output characters.
     */
    public static int e(double value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ERROR)) {
            String trace = getStackTrace(Double.toString(value));
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.e(tag, trace);
        }
        return 0;
    }

    /**
     * Send an {@link Log#ERROR} level message.
     * @param arg An object of argument to print.
     * @return The number of output characters.
     */
    public static int e(Object arg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ERROR)) {
            String trace = getStackTrace("[" + arg);
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.e(tag, trace);
        }
        return 0;
    }

    /**
     * Store an empty {@link Log#ASSERT} level message into permanent storage.
     * @return The number of stored characters.
     */
    @RequiresPermission (Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static int f() {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ASSERT)) {
            return writeToFile(tag, getStackTrace(null));
        }
        return 0;
    }

    /**
     * Store an {@link Log#ASSERT} level message into permanent storage.
     * @param msg The message you would like logged.
     * @return The number of stored characters.
     */
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static int f(String msg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ASSERT)) {
            return writeToFile(tag, getStackTrace(msg));
        }
        return 0;
    }

    /**
     * Send a {@link Log#ASSERT} level message.
     * @param value The boolean value you would like logged.
     * @return The number of output characters.
     */
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static int f(boolean value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ASSERT)) {
            return writeToFile(tag, getStackTrace(Boolean.toString(value)));
        }
        return 0;
    }

    /**
     * Send a {@link Log#ASSERT} level message.
     * @param value The integer value you would like logged.
     * @return The number of output characters.
     */
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static int f(int value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ASSERT)) {
            return writeToFile(tag, getStackTrace(value < 0x10000 ? Integer.toString(value) : "0x"+Integer.toHexString(value)));
        }
        return 0;
    }

    /**
     * Send a {@link Log#ASSERT} level message.
     * @param value The long value you would like logged.
     * @return The number of output characters.
     */
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static int f(long value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ASSERT)) {
            return writeToFile(tag, getStackTrace(value < 0x10000 ? Long.toString(value) : "0x"+Long.toHexString(value)));
        }
        return 0;
    }

    /**
     * Send a {@link Log#ASSERT} level message.
     * @param value The float value you would like logged.
     * @return The number of output characters.
     */
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static int f(float value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ASSERT)) {
            return writeToFile(tag, getStackTrace(Float.toString(value)));
        }
        return 0;
    }

    /**
     * Send a {@link Log#ASSERT} level message.
     * @param value The double value you would like logged.
     * @return The number of output characters.
     */
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static int f(double value) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ASSERT)) {
            return writeToFile(tag, getStackTrace(Double.toString(value)));
        }
        return 0;
    }

    /**
     * Store an {@link Log#ASSERT} level message into permanent storage.
     * @param arg An object of argument to log.
     * @return The number of output characters.
     */
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static int f(Object arg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ASSERT)) {
            String trace = getStackTrace("[" + arg);
            return writeToFile(tag, trace);
        }
        return 0;
    }

    /**
     * Clear {@link Log#ASSERT} level messages in permanent storage.
     */
    @RequiresPermission (Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static void fc() {
        File dir = Environment.getExternalStorageDirectory();
        File file = new File(dir, FAULT_FILE);
        if(file.exists() && file.isFile()) {
            file.delete();
        }
    }

    private static long logTime = 0;

    /**
     * Set start point of time log.
     */
    public static void ts() {
        logTime = System.currentTimeMillis();
    }

    /**
     * Send an empty {@link Log#DEBUG} level message with time consumed.
     * @return The number of output characters.
     */
    public static int t() {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.DEBUG)) {
            long now = System.currentTimeMillis();
            String trace = getStackTrace(null) + "\t[dt: " + (now - logTime) + "]";
            logTime = now;
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.d(tag, trace);
        }
        return 0;
    }

    /**
     * Send a {@link Log#DEBUG} level message with time consumed.
     * @param msg The message you would like logged together.
     * @return The number of output characters.
     */
    public static int t(String msg) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.DEBUG)) {
            long now = System.currentTimeMillis();
            String trace = getStackTrace(msg) + "\t[dt: " + (now - logTime) + "]";
            logTime = now;
            if (cache) {
                offerToList(tag, trace);
            }
            return Log.d(tag, trace);
        }
        return 0;
    }

    private synchronized static String getPackageName() {
        String className;
        try {
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            StackTraceElement e;
            /*
             * Explain why the 5th element.
             *  VMStack.getThreadStackTrace() ... (0)
             *  Thread.getStackTrace() .......... (1)
             *  LogUtil.getPackageName() ........ (2)
             *  LogUtil.getTag() ................ (3)
             *  LogUtil.?() ..................... (4)
             *  Where we print log .............. (5)
             *  xxx ............................. (6)
             *  ??? ............................. (n)
             */
            if(elements.length > 5) {
                e = elements[5];
            } else {
                return String.format(Locale.getDefault(), "{StackTraceError: Depth(%d) less than 6}", elements.length);
            }
            className = e.getClassName();
            Package pkg = Class.forName(className).getPackage();
            if (pkg != null) {
                return pkg.getName();
            } else {
                return null;
            }

        } catch (SecurityException e) {
            return "{StackTraceError: SecurityException}";
        } catch (ClassNotFoundException e) {
            return "{StackTraceError: ClassNotFound}";
        }
    }

    private synchronized static String getStackTrace(String msg) {
        try {
            StringBuilder sb = new StringBuilder();
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            StackTraceElement e;
            String className;
            /*
             * Explain why the 4th element.
             *  VMStack.getThreadStackTrace() ... (0)
             *  Thread.getStackTrace() .......... (1)
             *  LogUtil.getStackTrace() ......... (2)
             *  LogUtil.?() ..................... (3)
             *  Where we print log .............. (4)
             *  xxx ............................. (5)
             *  ??? ............................. (n)
             */
            if(elements.length > 4) {
                e = elements[4];
            } else {
                return String.format(Locale.getDefault(), "{StackTraceError: Depth(%d) less than 5}", elements.length);
            }
            className = e.getClassName();
            sb.append(String.format(Locale.getDefault(), "[%d]", e.getLineNumber()));
            int index = className.lastIndexOf('.');
            if(index > 0) {
                sb.append(className.substring(index+1));
            } else {
                sb.append(className);
            }
            sb.append('.');
            String methodName = e.getMethodName();
            if(TextUtils.isEmpty(methodName)) {
                sb.append("{null}");
            } else if(methodName.endsWith(">")) { // Constructor.
                sb.append(methodName);
            } else {
                sb.append(methodName);
            }
            sb.append("    ");
            if(!TextUtils.isEmpty(msg)) {
                sb.append(msg);
            }
            return sb.toString();

        } catch (SecurityException e) {
            return "{StackTraceError: SecurityException}";
        }
    }

    private static String getTimestamp(long millis) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Date date = new Date(millis);
        return formatter.format(date).concat(String.format(Locale.US, ".%1$03d", millis%1000));
    }

    private synchronized static int writeToFile(String tag, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(getTimestamp(System.currentTimeMillis()));
        sb.append("\t");
        sb.append(tag);
        sb.append("\t");
        sb.append(msg);
        sb.append("\n");

        File dir = Environment.getExternalStorageDirectory();
        File file = new File(dir, FAULT_FILE);
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
        } else if(file.isDirectory()) {
            return 0;
        }

        try(FileWriter writer = new FileWriter(file, true)) {
            writer.write(sb.toString());
            writer.close();
            return sb.length();

        } catch (IOException e) {
            return 0;
        }
    }

    private static class LogInfo {
        private long ts;
        private String tag;
        private String msg;
    }

    private static final int MAX_CACHE = 102400;
    private static final Queue<LogInfo> logQueue = new LinkedList<>();
    private static final Object locker = new Object();

    public static int getCacheLogAmount() {
        return logQueue.size();
    }

    private static void offerToList(String tag, String msg) {
        LogInfo log = new LogInfo();
        log.ts = System.currentTimeMillis();
        log.tag = tag;
        log.msg = msg;
        synchronized (locker) {
            if(logQueue.size() >= MAX_CACHE) {
                logQueue.poll();
            }
            logQueue.offer(log);
        }
    }

    /**
     * Dump all cached log messages to specified output stream and then clear log queue.
     */
    public static void dump(OutputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder(1024);
        SimpleDateFormat formatter = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US);
        synchronized (locker) {
            for (LogInfo log : logQueue) {
                sb.setLength(0);
                sb.append(formatter.format(new Date(log.ts)));
                sb.append(String.format(Locale.US, ".%1$03d", log.ts%1000));
                sb.append("\t");
                sb.append(log.tag);
                sb.append("\t");
                sb.append(log.msg);
                sb.append("\n");
                stream.write(sb.toString().getBytes());
            }
            logQueue.clear();
        }
    }

    /**
     * Dump all cached log messages to specified file and then clear log queue.
     */
    public static void dump(File file) throws IOException {
        if(!file.exists()) {
            file.createNewFile();
        } else if(file.isDirectory()) {
            throw new IOException("Destination dump file is a directory.");
        }
        try(FileWriter writer = new FileWriter(file, true)) {
            StringBuilder sb = new StringBuilder(1024);
            SimpleDateFormat formatter = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US);
            synchronized (locker) {
                for (LogInfo log : logQueue) {
                    sb.setLength(0);
                    sb.append(formatter.format(new Date(log.ts)));
                    sb.append(String.format(Locale.US, ".%1$03d", log.ts%1000));
                    sb.append("\t");
                    sb.append(log.tag);
                    sb.append("\t");
                    sb.append(log.msg);
                    sb.append("\n");
                    writer.write(sb.toString());
                }
                logQueue.clear();
            }
        }
    }

    /**
     * Dump all cached log messages to specified file path and then clear log queue.
     */
    public static void dump(String path) throws IOException {
        dump(new File(path));
    }

    /**
     *  Print an exception log with stack trace elements.
     * @param e The exception.
     */
    public static void x(Throwable e) {
        String tag = getTag();
        if(ENFORCE_OUTPUT || Log.isLoggable(tag, Log.ERROR)) {
            String trace = getStackTrace(e.toString());
            if (cache) {
                offerToList(tag, trace);
            }
            Log.e(tag, trace);
            StackTraceElement[] traceElements = e.getStackTrace();
            String msg;
            for (StackTraceElement element : traceElements) {
                msg = "\t" + element.toString();
                if (cache) {
                    offerToList(tag, msg);
                }
                Log.e(tag, msg);
            }
        }
    }

    private static class StackTraceException extends RuntimeException {
        public StackTraceException() {
            super();
        }
        public StackTraceException(String message) {
            super(message);
        }
        public StackTraceException(String message, Throwable cause) {
            super(message, cause);
        }
        public StackTraceException(Throwable cause) {
            super(cause);
        }
    }
    /**
     * Print stack trace elements of current thread.
     */
    public static void s(String msg) {
        x(new StackTraceException(msg));
    }

    public static void s() {
        x(new StackTraceException("To trace stack invocation."));
    }
}
