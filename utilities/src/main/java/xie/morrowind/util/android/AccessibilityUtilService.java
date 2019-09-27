package xie.morrowind.util.android;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import xie.morrowind.util.LogUtil;

public class AccessibilityUtilService extends AccessibilityService {
    protected Context context;
    protected SharedPreferences prefs = null;
    protected String topPackageName = "";
    protected String topActivityName = "";

    /**
     * Called when app changed.
     * @param from The last app package name.
     * @param to The current app package name.
     */
    protected void onAppChanged(String from, String to) {
    } 
    
    protected void onActivityChanged(String from, String to) {
        
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        this.context = this;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if(event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String eventPackageName = event.getPackageName().toString();
            String eventActivityName = event.getClassName().toString();
            if(eventPackageName.equals(topPackageName)) {
                // App internal window changed.
                if(eventActivityName.equals(topActivityName)) {
                    // Same class name ?
                } else {
                    onActivityChanged(topActivityName, eventActivityName);
                    topActivityName = eventActivityName;
                }
                
            } else if(eventPackageName.equals("com.android.systemui")) {
                // Ignore SystemUI (status bar pull down?).
            } else {
                if(getPackageName().equals(eventPackageName)) {
                    // Ignore own package.
                } else {
                    onAppChanged(topPackageName, eventPackageName);
                    topPackageName = eventPackageName;
                    onActivityChanged(topActivityName, eventActivityName);
                    topActivityName = eventActivityName;
                }
            }
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    @Override
    public void onInterrupt() {
        LogUtil.w();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void printNodeTree(AccessibilityNodeInfo root, int[] levels) {
        if(root == null) {
            LogUtil.d("Root is @null");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<levels.length; i++) {
            sb.append(levels[i]);
            sb.append(' ');
        }
        sb.append(root.getClassName());
        sb.append(" [");
        sb.append(root.getViewIdResourceName());
        sb.append('(');
        sb.append(root.getText());
        sb.append(')');
        LogUtil.d(sb.toString());
        int[] newLevels = Arrays.copyOf(levels, levels.length+1);
        for(int i=0; i<root.getChildCount(); i++) {
            newLevels[levels.length] = i;
            AccessibilityNodeInfo child = root.getChild(i); 
            if(child != null) {
                printNodeTree(child, newLevels);
                child.recycle();
            }
        }
    }
    public final void printNodeTree(AccessibilityNodeInfo root) {
        printNodeTree(root, new int[0]);
    }

    public final boolean extendsOf(AccessibilityNodeInfo info, Class<?> parentCls) {
        try {
            String clsName = info.getClassName().toString();
            Context remoteContext = createPackageContext(info.getPackageName().toString(), Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
            Class<?> cls = Class.forName(clsName, false, remoteContext.getClassLoader());
            return parentCls.isAssignableFrom(cls);
            
        } catch (Exception e) {
            LogUtil.x(e);
            return false;
        }
    }
    
    public final boolean extendsOf(AccessibilityNodeInfo info, String parentClsName) {
        try {
            String clsName = info.getClassName().toString();
            Context remoteContext = createPackageContext(info.getPackageName().toString(), Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
            Class<?> cls = Class.forName(clsName, false, remoteContext.getClassLoader());
            Class<?> parentCls = Class.forName(parentClsName, false, remoteContext.getClassLoader());
            return parentCls.isAssignableFrom(cls);
            
        } catch (Exception e) {
            LogUtil.x(e);
            return false;
        }
    }
    
    /**
     * Set an editor's text.
     * @param editor The editor to be set text. This method will check if editor is subclass of EditText.
     * @param text The text to be set.
     * @param append Whether append text or replace the old content.
     */
    public final void setEditText(AccessibilityNodeInfo editor, String text, boolean append) {
        if(TextUtils.isEmpty(text)) {
            return;
        }
        try {
            String clsName = editor.getClassName().toString();
            Context remoteContext = createPackageContext(editor.getPackageName().toString(), Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
            Class<?> cls = Class.forName(clsName, false, remoteContext.getClassLoader());
            if(EditText.class.isAssignableFrom(cls)) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getPackageName(), text);
                clipboard.setPrimaryClip(clip);
                Bundle args = new Bundle();
                CharSequence finalText;
                if(append) {
                    CharSequence oldText = editor.getText();
                    if(!TextUtils.isEmpty(oldText)) {
                        finalText = oldText + text;
                    } else {
                        finalText = text;
                    }
                } else {
                    finalText = text;
                }
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, finalText);                    
                editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            } else {
                LogUtil.w("Not an EditText.");
            }
        } catch (ClassNotFoundException e) {
            LogUtil.x(e);
        } catch (NameNotFoundException e) {
            LogUtil.x(e);
        }
    }
    
    /**
     * Set an editor's text by focusing and pasting text in it.
     * @param editor The editor to be set text. This method will check if editor is subclass of EditText.
     * @param text The text to be set.
     */
    public final void setEditText(AccessibilityNodeInfo editor, String text) {
        setEditText(editor, text, false);
    }

    private List<AccessibilityNodeInfo> recursiveFindViewByClass(AccessibilityNodeInfo parent, String clsName) {
        List<AccessibilityNodeInfo> list = new ArrayList<AccessibilityNodeInfo>();
        for(int i=0; i<parent.getChildCount(); i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if(child != null) {
                if(child.getChildCount() > 0) {
                    List<AccessibilityNodeInfo> childList = recursiveFindViewByClass(child, clsName);
                    list.addAll(childList);
                }
                if(clsName.equals(child.getClassName())) {
                    LogUtil.d("Found: id="+child.getViewIdResourceName());
                    list.add(child);
                } else {
                    child.recycle();
                }
            }
        }
        return list;
    }
    
    /**
     * Do not forget to recycle list elements after use.
     */
    public final List<AccessibilityNodeInfo> findViewByClass(AccessibilityNodeInfo root, String clsName) {
        List<AccessibilityNodeInfo> list = new ArrayList<AccessibilityNodeInfo>();
        if(root != null) {
            if(clsName.equals(root.getClassName())) {
                LogUtil.d(root.getViewIdResourceName());
                list.add(root);
            }
            list.addAll(recursiveFindViewByClass(root, clsName));
        }
        return list;
    }
    
    public final List<AccessibilityNodeInfo> findViewByClass(AccessibilityNodeInfo root, Class<?> cls) {
        return findViewByClass(root, cls.getName());
    }
    
    private AccessibilityNodeInfo recursiveFindFirstViewByClass(AccessibilityNodeInfo root, String clsName) {
        for(int i=0; i<root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if(child != null) {
                if(clsName.equals(child.getClassName())) {
                    LogUtil.d("Found: id="+child.getViewIdResourceName());
                    return child;
                }
                AccessibilityNodeInfo target = recursiveFindFirstViewByClass(child, clsName);
                child.recycle();
                if(target != null) {
                    return target;
                }
            }
        }
        return null;
    }
    public final AccessibilityNodeInfo findFirstViewByClass(AccessibilityNodeInfo root, String clsName) {
        if(root != null) {
            if(clsName.equals(root.getClassName())) {
                LogUtil.d("Found: id="+root.getViewIdResourceName());
                return root;
            } else {
                return recursiveFindFirstViewByClass(root, clsName);
            }
        }
        return null;
    }
    
    public final AccessibilityNodeInfo findFirstViewByClass(AccessibilityNodeInfo root, Class<?> cls) {
        return findFirstViewByClass(root, cls.getName());
    }
    
    /**
     * Find view by Id, if view is not unique, will return first found, or return null if not found.
     */
    public final AccessibilityNodeInfo findFirstViewById(AccessibilityNodeInfo root, String viewId) {
        LogUtil.v(viewId);
        AccessibilityNodeInfo target = null;
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId(viewId);
        if(list.size() != 1) {
            LogUtil.w("Not unique ("+list.size()+"), No more idea but to return the first node if has.");
        }
        if(list.size() > 0) {
            target = list.get(0);
            for(int i=1; i<list.size(); i++) {
                AccessibilityNodeInfo info = list.get(i);
                info.recycle();
            }
        }
        return target;
    }
    
    private boolean textMatchs(CharSequence text, String regex) {
        if(TextUtils.isEmpty(regex)) {
            return TextUtils.isEmpty(text);
        } else if(TextUtils.isEmpty(text)) {
            return false;
        } else {
            return Pattern.matches(regex, text);
        }
    }
    
    private AccessibilityNodeInfo findView(AccessibilityNodeInfo info, String clsName, String regex, int level) {
        CharSequence infoClsName = info.getClassName();
        CharSequence infoText = info.getText();
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<level; i++) {
            sb.append('.');
        }
        sb.append(infoClsName);
        sb.append('(');
        sb.append(infoText);
        sb.append(')');
        LogUtil.v(sb);
        // 1. check if this node info is just we finding.
        if(infoClsName.equals(clsName) && textMatchs(infoText, regex)) {
            return info;
        } else {
            // 2. then check all this node info's children to find by recusive.
            level++;
            for (int i=0; i<info.getChildCount(); i++) {
                AccessibilityNodeInfo child = info.getChild(i);
                AccessibilityNodeInfo target = findView(child, clsName, regex, level);
                child.recycle();
                if(target != null) {
                    return target;
                }                
            }
            return null;
        }
    }
    
    /**
     * Find view by view's class and view's text regular expression.
     * @param info The root node of tree to be find view in.
     * @param clsName Class name of view.
     * @param regex Regular expression of view's text.
     * @return The view's node info, <code>null</code> if not find.
     */
    public final AccessibilityNodeInfo findView(AccessibilityNodeInfo info, String clsName, String regex) {
        return findView(info, clsName, regex, 0);
    }
    
    /**
     * Find view by view's class and view's text regular expression.
     * @param info The root node of tree to be find view in.
     * @param cls Class of view.
     * @param regex Regular expression of view's text.
     * @return The view's node info, <code>null</code> if not find.
     */
    public final AccessibilityNodeInfo findView(AccessibilityNodeInfo info, Class<?> cls, String regex) {
        return findView(info, cls.getName(), regex);
    }
    
    /** Needs  "android.uid.system".
     */
    public final static void enableService(Context context, Class<?> service) {
        ComponentName cn = new ComponentName(context, service);
        ContentResolver cr = context.getContentResolver();
        Secure.putString(cr, Secure.ENABLED_ACCESSIBILITY_SERVICES, cn.flattenToString());
        Secure.putInt(cr, Secure.ACCESSIBILITY_ENABLED, 1);
    }
    
    /**
     * Check if accessibility service is enabled.
     */
    public final static boolean isEnabled(Context context, Class<?> service) {
        ComponentName cn = new ComponentName(context, service);
        String serviceId = cn.flattenToShortString();
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> accessibilityServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        //LogUtil.v(accessibilityServices.size());
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().equals(serviceId)) {
                LogUtil.v(info.getId());
                return true;
            }
        }
        return false;
    }

    /**
     * Goto accessibility service enable/disable page.
     */
    public final static void gotoAccessibility(Context context) {
        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    
    public final boolean getPrefs(String key, boolean defVal) {
        return prefs.getBoolean(key, defVal);
    }
    
    public final void setPrefs(String key, boolean val) {
        prefs.edit().putBoolean(key, val).apply();
    }
    
    public final int getPrefs(String key, int defVal) {
        return prefs.getInt(key, defVal);
    }
    
    public final void setPrefs(String key, int val) {
        prefs.edit().putInt(key, val).apply();
    }
    
    public final long getPrefs(String key, long defVal) {
        return prefs.getLong(key, defVal);
    }
    
    public final void setPrefs(String key, long val) {
        prefs.edit().putLong(key, val).apply();
    }
    
    public final String getPrefs(String key, String defVal) {
        return prefs.getString(key, defVal);
    }
    
    public final void setPrefs(String key, String val) {
        prefs.edit().putString(key, val).apply();
    }
    
    public final float getPrefs(String key, float defVal) {
        return prefs.getFloat(key, defVal);
    }
    
    public final void setPrefs(String key, float val) {
        prefs.edit().putFloat(key, val).apply();
    }

}
