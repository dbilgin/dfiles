package com.dbilgin.dfiles.util

import android.os.Build
import android.os.Environment

object PermissionUtils {
    
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Handled by manifest permissions for older versions
        }
    }
}
