package com.personal.personalsmsgateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Memory and cache management utility
 * Helps optimize device performance
 */
public class MemoryManager {

    private static final String TAG = "MemoryManager";

    /**
     * Clear all app cache
     */
    public static boolean clearCache(Context context) {
        try {
            // Get cache directory
            java.io.File cacheDir = context.getCacheDir();
            deleteDir(cacheDir);
            Log.d(TAG, "Cache cleared successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clear all app data (files, preferences)
     */
    public static boolean clearAllAppData(Context context) {
        try {
            // Clear SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("GatewayPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            Log.d(TAG, "SharedPreferences cleared");

            // Clear app files directory
            java.io.File filesDir = context.getFilesDir();
            deleteDir(filesDir);
            Log.d(TAG, "App files cleared");

            // Clear cache
            java.io.File cacheDir = context.getCacheDir();
            deleteDir(cacheDir);
            Log.d(TAG, "Cache cleared");

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error clearing app data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clear app database
     */
    public static boolean clearDatabase(Context context) {
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            boolean success = dbHelper.clearAllSms();
            dbHelper.close();
            Log.d(TAG, "Database cleared");
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error clearing database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get app cache size
     */
    public static long getCacheSize(Context context) {
        try {
            java.io.File cacheDir = context.getCacheDir();
            return getDirSize(cacheDir);
        } catch (Exception e) {
            Log.e(TAG, "Error getting cache size: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get app data size
     */
    public static long getDataSize(Context context) {
        try {
            java.io.File filesDir = context.getFilesDir();
            return getDirSize(filesDir);
        } catch (Exception e) {
            Log.e(TAG, "Error getting data size: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Format bytes to human readable format
     */
    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * Delete directory recursively
     */
    private static void deleteDir(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            java.io.File[] children = dir.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    if (child.isDirectory()) {
                        deleteDir(child);
                    } else {
                        child.delete();
                    }
                }
            }
        }
    }

    /**
     * Get directory size
     */
    private static long getDirSize(java.io.File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            java.io.File[] children = dir.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    if (child.isDirectory()) {
                        size += getDirSize(child);
                    } else {
                        size += child.length();
                    }
                }
            }
        }
        return size;
    }
}
