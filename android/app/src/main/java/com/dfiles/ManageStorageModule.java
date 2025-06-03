package com.dfiles;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

public class ManageStorageModule extends ReactContextBaseJavaModule {

    public ManageStorageModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "ManageStorageModule";
    }

    @ReactMethod
    public void hasPermission(Promise promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ - Check if we have MANAGE_EXTERNAL_STORAGE permission
                boolean hasPermission = Environment.isExternalStorageManager();
                promise.resolve(hasPermission);
            } else {
                // Android 10 and below - always true (use regular storage permissions)
                promise.resolve(true);
            }
        } catch (Exception e) {
            promise.reject("ERROR", "Failed to check permission", e);
        }
    }

    @ReactMethod
    public void requestPermission(Promise promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ - Open the "All files access" settings page
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getReactApplicationContext().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getReactApplicationContext().startActivity(intent);
                promise.resolve(true);
            } else {
                // Android 10 and below - not needed
                promise.resolve(true);
            }
        } catch (Exception e) {
            promise.reject("ERROR", "Failed to request permission", e);
        }
    }
} 