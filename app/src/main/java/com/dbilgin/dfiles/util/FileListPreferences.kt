package com.dbilgin.dfiles.util

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "file_list_prefs"
private const val KEY_SORT_TYPE = "sort_type"
private const val KEY_SORT_ORDER = "sort_order"
private const val KEY_SHOW_HIDDEN = "show_hidden"
private const val KEY_VIEW_MODE = "view_mode"

class FileListPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSortType(): String = prefs.getString(KEY_SORT_TYPE, null) ?: "NAME"
    fun setSortType(value: String) = prefs.edit().putString(KEY_SORT_TYPE, value).apply()

    fun getSortOrder(): String = prefs.getString(KEY_SORT_ORDER, null) ?: "ASCENDING"
    fun setSortOrder(value: String) = prefs.edit().putString(KEY_SORT_ORDER, value).apply()

    fun getShowHidden(): Boolean = prefs.getBoolean(KEY_SHOW_HIDDEN, false)
    fun setShowHidden(value: Boolean) = prefs.edit().putBoolean(KEY_SHOW_HIDDEN, value).apply()

    fun getViewMode(): String = prefs.getString(KEY_VIEW_MODE, null) ?: "LIST"
    fun setViewMode(value: String) = prefs.edit().putString(KEY_VIEW_MODE, value).apply()
}
