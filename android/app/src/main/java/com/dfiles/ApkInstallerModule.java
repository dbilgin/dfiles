package com.dfiles;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.core.content.FileProvider;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import java.io.File;

public class ApkInstallerModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;

    public ApkInstallerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "ApkInstaller";
    }

    @ReactMethod
    public void installApk(String apkPath, Promise promise) {
        try {
            File apkFile = new File(apkPath);
            
            if (!apkFile.exists()) {
                promise.reject("FILE_NOT_FOUND", "APK file not found");
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // For Android 7.0 and above, use FileProvider
                String authority = reactContext.getPackageName() + ".fileprovider";
                apkUri = FileProvider.getUriForFile(reactContext, authority, apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                // For older versions, use file:// URI
                apkUri = Uri.fromFile(apkFile);
            }
            
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            
            reactContext.startActivity(intent);
            promise.resolve(true);
            
        } catch (Exception e) {
            promise.reject("INSTALL_ERROR", "Failed to install APK: " + e.getMessage());
        }
    }
} 