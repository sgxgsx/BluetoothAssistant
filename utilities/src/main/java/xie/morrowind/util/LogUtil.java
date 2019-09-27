/*
 * Copyright (C) 2013 <MorrowindXie@gmail.com>.
 * This module is cloned from https://github.com/morrowind/Android-LogUtil
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations 
 * under the License.
 */

package xie.morrowind.util;

import android.Manifest;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresPermission;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

/**
 * Wrapper and extension of {@link android.util.Log} APIs for sending log output.<br/>
 *
 * <b>Features:</b>
 * <ul>
 * <li/>Insert invoking method name and code line number at front of user log messages;
 * <li/>Set default log tag as package name of current class, you can change it by {@link #setTag(String)};
 * <li/>An extended API to directly print object instance content;
 * <li/>Support cache main logs and dump it to external storage if necessary;
 * <li/>Wrap {@link Throwable#printStackTrace()} into {@link #x(Exception)}.
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
 * @author morrowindxie
 * @version 2.0
 */
public final class LogUtil {
    public static final boolean IS_DEBUGGABLE = !"user".equals(Build.TYPE);
    private static String TAG = "morrowind";
    private static boolean cache = false;
    private static final String FAULT_FILE = "error.log";
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Set default log tag.
     * @param tag The log tag used to filter.
     */
    public static void setTag(String tag) {
        TAG = tag;
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
    
    /**
     * Send an empty {@link Log#VERBOSE} level message with default {@link #TAG}.
     * @return The number of output characters.
     */
    public static int v() {
        String tag = getTag();
        String trace = getStackTrace(null);
        return android.util.Log.v(tag, trace);
    }
    
    /**
     * Send a {@link Log#VERBOSE} level message.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     */
    public static int v(String msg) {
        String tag = getTag();
        String trace = getStackTrace(msg);
        return android.util.Log.v(tag, trace);
    }

    /**
     * Send a {@link Log#VERBOSE} level message.
     * @param arg An object of argument to print.
     * @return The number of output characters.
     */
    public static int v(Object arg) {
        String tag = getTag();
        String trace = getStackTrace("["+arg);
        return android.util.Log.v(tag, trace);
    }
    
    /**
     * Send an empty {@link Log#VERBOSE} level message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @return The number of output characters.
     * @deprecated Use {@link #setTag(String)} then {@link #v()} instead.
     */
    public static int vt(String tag) {
        String trace = getStackTrace(null);
        return android.util.Log.v(tag, trace); 
    }
    
    /**
     * Send a {@link Log#VERBOSE} level message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     * @deprecated Use {@link #setTag(String)} then {@link #v(String)} instead.
     */
    public static int vt(String tag, String msg) {
        String trace = getStackTrace(msg);
        return android.util.Log.v(tag, trace); 
    }
    
    /**
     * Send an empty {@link Log#DEBUG} level message.
     * @return The number of output characters.
     */
    public static int d() {
        String tag = getTag();
        String trace = getStackTrace(null);
        if(cache) {
            offerToList(TAG, trace);
        }
        if(IS_DEBUGGABLE) {
            return android.util.Log.d(tag, trace);
        } else { // Change debug log to info log in USER version.
            return android.util.Log.i(tag, trace);
        }
    }
    
    /**
     * Send a {@link Log#DEBUG} level message.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     */
    public static int d(String msg) {
        String tag = getTag();
        String trace = getStackTrace(msg);
        if(cache) {
            offerToList(tag, trace);
        }
        if(IS_DEBUGGABLE) {
            return android.util.Log.d(tag, trace);
        } else {
            return android.util.Log.i(tag, trace);
        }
    }

    /**
     * Send a {@link Log#DEBUG} level message.
     * @param arg An object of argument to print.
     * @return The number of output characters.
     */
    public static int d(Object arg) {
        String tag = getTag();
        String trace = getStackTrace("["+arg);
        if(cache) {
            offerToList(tag, trace);
        }
        if(IS_DEBUGGABLE) {
            return android.util.Log.d(tag, trace);
        } else {
            return android.util.Log.i(tag, trace);
        }
    }
    
    /**
     * Send an empty {@link Log#DEBUG} level message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @return The number of output characters.
     * @deprecated Use {@link #setTag(String)} then {@link #d()} instead.
     */
    public static int dt(String tag) {
        String trace = getStackTrace(null);
        return android.util.Log.d(tag, trace); 
    }
    
    /**
     * Send a {@link Log#DEBUG} level message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     * @deprecated Use {@link #setTag(String)} then {@link #d(String)} instead.
     */
    public static int dt(String tag, String msg) {
        String trace = getStackTrace(msg);
        return android.util.Log.d(tag, trace); 
    }
    
    /**
     * Send an empty {@link Log#INFO} level message.
     * @return The number of output characters.
     */
    public static int i() {
        String tag = getTag();
        String trace = getStackTrace(null);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.i(tag, trace);
    }
    
    /**
     * Send an {@link Log#INFO} level message.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     */
    public static int i(String msg) {
        String tag = getTag();
        String trace = getStackTrace(msg);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.i(tag, trace);
    }

    /**
     * Send an {@link Log#INFO} level message.
     * @param arg An object of argument to print.
     * @return The number of output characters.
     */
    public static int i(Object arg) {
        String tag = getTag();
        String trace = getStackTrace("["+arg);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.i(tag, trace);
    }
    
    /**
     * Send an empty {@link Log#INFO} level message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @return The number of output characters.
     * @deprecated Use {@link #setTag(String)} then {@link #i()} instead.
     */
    public static int it(String tag) {
        String trace = getStackTrace(null);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.i(tag, trace); 
    }
    
    /**
     * Send an {@link Log#INFO} level message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     * @deprecated Use {@link #setTag(String)} then {@link #i(String)} instead.
     */
    public static int it(String tag, String msg) {
        String trace = getStackTrace(msg);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.v(tag, trace); 
    }
    
    /**
     * Send an empty {@link Log#WARN} level message.
     * @return The number of output characters.
     */
    public static int w() {
        String tag = getTag();
        String trace = getStackTrace(null);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.w(tag, trace);
    }
    
    /**
     * Send a {@link Log#WARN} level message.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     */
    public static int w(String msg) {
        String tag = getTag();
        String trace = getStackTrace(msg);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.w(tag, trace);
    }

    /**
     * Send a {@link Log#WARN} level message.
     * @param arg An object of argument to print.
     * @return The number of output characters.
     */
    public static int w(Object arg) {
        String tag = getTag();
        String trace = getStackTrace("["+arg);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.w(tag, trace);
    }
    
    /**
     * Send an empty {@link Log#WARN} level message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @return The number of output characters.
     * @deprecated Use {@link #setTag(String)} then {@link #w()} instead.
     */
    public static int wt(String tag) {
        String trace = getStackTrace(null);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.w(tag, trace); 
    }
    
    /**
     * Send a {@link Log#WARN} level message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     * @deprecated Use {@link #setTag(String)} then {@link #w(String)} instead.
     */
    public static int wt(String tag, String msg) {
        String trace = getStackTrace(msg);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.w(tag, trace); 
    }
    
    /**
     * Send an empty {@link Log#ERROR} level message.
     * @return The number of output characters.
     */
    public static int e() {
        String tag = getTag();
        String trace = getStackTrace(null);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.e(tag, trace);
    }
    
    /**
     * Send an {@link Log#ERROR} level message.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     */
    public static int e(String msg) {
        String tag = getTag();
        String trace = getStackTrace(msg);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.e(tag, trace);
    }

    /**
     * Send an {@link Log#ERROR} level message.
     * @param arg An object of argument to print.
     * @return The number of output characters.
     */
    public static int e(Object arg) {
        String tag = getTag();
        String trace = getStackTrace("["+arg);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.e(tag, trace);
    }
    
    /**
     * Send an empty {@link Log#ERROR} level message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @return The number of output characters.
     * @deprecated Use {@link #setTag(String)} then {@link #e()} instead.
     */
    public static int et(String tag) {
        String trace = getStackTrace(null);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.e(tag, trace); 
    }
    
    /**
     * Send an {@link Log#ERROR} level message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     * @deprecated Use {@link #setTag(String)} then {@link #e(String)} instead.
     */
    public static int et(String tag, String msg) {
        String trace = getStackTrace(msg);
        if(cache) {
            offerToList(tag, trace);
        }
        return android.util.Log.e(tag, trace); 
    }
    
    /**
     * Store an empty {@link Log#ASSERT} level message into permanent storage.
     * @return The number of stored characters.
     */
    @RequiresPermission (Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static int f() {
        String tag = getTag();
        return writeToFile(tag, getStackTrace(null));
    }
    
    /**
     * Store an {@link Log#ASSERT} level message into permanent storage.
     * @param msg The message you would like logged.
     * @return The number of stored characters.
     */
    @RequiresPermission (Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static int f(String msg) {
        String tag = getTag();
        return writeToFile(tag, getStackTrace(msg));
    }

    /**
     * Store an {@link Log#ASSERT} level message into permanent storage.
     * @param arg An object of argument to log.
     * @return The number of output characters.
     */
    public static int f(Object arg) {
        String tag = getTag();
        String trace = getStackTrace("["+arg);
        return writeToFile(tag, trace);
    }
    
    /**
     * Store an empty {@link Log#ASSERT} level message into permanent storage.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @return The number of output characters.
     * @deprecated Use {@link #setTag(String)} then {@link #f()} instead.
     */
    @RequiresPermission (Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static int ft(String tag) {
        return writeToFile(tag, getStackTrace(null));
    }
    
    /**
     * Store an {@link Log#ASSERT} level message into permanent storage.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @return The number of output characters.
     * @deprecated Use {@link #setTag(String)} then {@link #f(String)} instead.
     */
    @RequiresPermission (Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static int ft(String tag, String msg) {
        return writeToFile(tag, getStackTrace(msg));
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
            int index = className.lastIndexOf('.');
            if(index > 0) {
                sb.append(className.substring(index+1));
            } else {
                sb.append(className);
            }
            sb.append('.');
            String methodName = e.getMethodName();
            if(TextUtils.isEmpty(methodName)) {
                sb.append("{null:");
                sb.append(e.getLineNumber());
                sb.append("}");
            } else if(methodName.endsWith(">")) { // Constructor.
                sb.append(methodName);
                sb.deleteCharAt(sb.length()-1);
                sb.append(":");
                sb.append(e.getLineNumber());
                sb.append(">");
            } else {
                sb.append(methodName);
                sb.append("(");
                sb.append(e.getLineNumber());
                sb.append(")");
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
    
    private static void offerToList(String tag, String msg) {
        LogInfo log = new LogInfo();
        log.ts = System.currentTimeMillis();
        log.tag = tag;
        log.msg = msg;
        synchronized (logQueue) {
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
        synchronized (logQueue) {
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
    @RequiresPermission (Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static void dump(File file) throws IOException {
        if(!file.exists()) {
            file.createNewFile();            
        } else if(file.isDirectory()) {
            throw new IOException("Destination dump file is a directory.");
        }
        try(FileWriter writer = new FileWriter(file, true)) {
            StringBuilder sb = new StringBuilder(1024);
            SimpleDateFormat formatter = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US);
            synchronized (logQueue) {
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
    @RequiresPermission (Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static void dump(String path) throws IOException {
        dump(new File(path));
    }
    
    /**
     *  Print an exception log with stack trace elements.
     * @param e The exception.
     */
    public static void x(Exception e) {
        String tag = getTag();
        String trace = getStackTrace(e.toString());
        if(cache) {
            offerToList(tag, trace);
        }
        android.util.Log.e(tag, trace);
        StackTraceElement[] traceElements = e.getStackTrace();
        String msg;
        for(StackTraceElement element : traceElements) {
            msg = "\t"+element.toString();
            if(cache) {
                offerToList(tag, msg);
            }
            android.util.Log.e(tag, msg);
        }
    }
}
