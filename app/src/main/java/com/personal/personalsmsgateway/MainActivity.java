package com.personal.personalsmsgateway;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;
    private static final String TAG = "MainActivity";
    
    private EditText etApiUrl, etSecretKey;
    private Button btnSave, btnViewLogs, btnSettings;
    private SharedPreferences prefs;
    private ExecutorService executor;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity created on " + CompatibilityHelper.getAndroidVersion() + " (" + CompatibilityHelper.getDeviceModel() + ")");

        // Initialize executor
        executor = Executors.newSingleThreadExecutor();

        // Initialize views
        etApiUrl = findViewById(R.id.etApiUrl);
        etSecretKey = findViewById(R.id.etSecretKey);
        btnSave = findViewById(R.id.btnSave);
        btnViewLogs = findViewById(R.id.btnViewLogs);
        btnSettings = findViewById(R.id.btnSettings);
        progressBar = findViewById(R.id.progressBar);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("GatewayPrefs", MODE_PRIVATE);

        // Load saved configuration
        loadConfiguration();

        // Request permissions if needed
        if (CompatibilityHelper.isAndroid6OrHigher()) {
            checkSmsPermission();
        }

        // Save button click listener
        btnSave.setOnClickListener(v -> saveConfiguration());
        
        // View logs button click listener
        if (btnViewLogs != null) {
            btnViewLogs.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SmsLogsActivity.class);
                startActivity(intent);
            });
        }

        // Settings button click listener
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showSettingsMenu());
        }
    }

    private void loadConfiguration() {
        String apiUrl = prefs.getString("api_url", "");
        String secretKey = prefs.getString("secret_key", "");

        etApiUrl.setText(apiUrl);
        etSecretKey.setText(secretKey);
    }

    private void saveConfiguration() {
        String apiUrl = etApiUrl.getText().toString().trim();
        String secretKey = etSecretKey.getText().toString().trim();

        // Validation
        if (apiUrl.isEmpty()) {
            Toast.makeText(this, "Please enter API URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidUrl(apiUrl)) {
            Toast.makeText(this, "Please enter valid URL (http:// or https://)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (secretKey.isEmpty() || secretKey.length() < 6) {
            Toast.makeText(this, "Secret Key must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("api_url", apiUrl);
        editor.putString("secret_key", secretKey);
        editor.apply();

        Toast.makeText(this, "Configuration saved successfully!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Configuration saved for " + CompatibilityHelper.getAndroidVersion());
    }

    private boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) 
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, PermissionManager.getSmsPermissions(), 
                    SMS_PERMISSION_CODE);
        }
    }

    private void showSettingsMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("App Settings")
                .setItems(new String[]{
                        "📊 View App Size",
                        "🗑️ Clear Cache",
                        "📝 Clear Database",
                        "⚙️ Clear All Data",
                        "🔄 Reset App",
                        "📦 Uninstall App"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showAppSize();
                            break;
                        case 1:
                            clearCache();
                            break;
                        case 2:
                            clearDatabase();
                            break;
                        case 3:
                            clearAllData();
                            break;
                        case 4:
                            resetApp();
                            break;
                        case 5:
                            uninstallApp();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAppSize() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            long cacheSize = MemoryManager.getCacheSize(this);
            long dataSize = MemoryManager.getDataSize(this);
            long totalSize = cacheSize + dataSize;

            runOnUiThread(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("App Storage Size")
                        .setMessage(
                                "Cache: " + MemoryManager.formatBytes(cacheSize) + "\n" +
                                "Data: " + MemoryManager.formatBytes(dataSize) + "\n" +
                                "Total: " + MemoryManager.formatBytes(totalSize)
                        )
                        .setPositiveButton("OK", null)
                        .show();
            });
        });
    }

    private void clearCache() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Cache")
                .setMessage("Are you sure? This will clear temporary files.")
                .setPositiveButton("Yes", (dialog, which) -> performClearCache())
                .setNegativeButton("No", null)
                .show();
    }

    private void performClearCache() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            boolean success = MemoryManager.clearCache(this);
            runOnUiThread(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                if (success) {
                    Toast.makeText(MainActivity.this, "Cache cleared successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to clear cache", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void clearDatabase() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear SMS Database")
                .setMessage("Are you sure? All SMS logs will be deleted.")
                .setPositiveButton("Yes", (dialog, which) -> performClearDatabase())
                .setNegativeButton("No", null)
                .show();
    }

    private void performClearDatabase() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            boolean success = MemoryManager.clearDatabase(this);
            runOnUiThread(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                if (success) {
                    Toast.makeText(MainActivity.this, "Database cleared successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to clear database", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void clearAllData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear All App Data")
                .setMessage("This will delete:\n• All settings\n• All SMS logs\n• All cache\n\nAre you sure?")
                .setPositiveButton("Yes", (dialog, which) -> performClearAllData())
                .setNegativeButton("No", null)
                .show();
    }

    private void performClearAllData() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            boolean success = MemoryManager.clearAllAppData(this);
            runOnUiThread(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                if (success) {
                    Toast.makeText(MainActivity.this, "All data cleared!", Toast.LENGTH_SHORT).show();
                    etApiUrl.setText("");
                    etSecretKey.setText("");
                } else {
                    Toast.makeText(MainActivity.this, "Failed to clear all data", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void resetApp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset App")
                .setMessage("This will reset the app to factory settings.")
                .setPositiveButton("Yes", (dialog, which) -> performResetApp())
                .setNegativeButton("No", null)
                .show();
    }

    private void performResetApp() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            // Clear all data
            MemoryManager.clearAllAppData(this);
            
            // Restart activity
            runOnUiThread(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(MainActivity.this, "App reset successfully!", Toast.LENGTH_SHORT).show();
                
                // Recreate activity
                recreate();
            });
        });
    }

    private void uninstallApp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Uninstall App")
                .setMessage("Do you want to uninstall Personal SMS Gateway?\n\nThis will:\n• Delete all app data\n• Remove the app from your device")
                .setPositiveButton("Yes, Uninstall", (dialog, which) -> performUninstall())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performUninstall() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            // Clear all data first
            MemoryManager.clearAllAppData(this);
            
            runOnUiThread(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                
                // Launch uninstall intent
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:com.personal.personalsmsgateway"));
                startActivity(intent);
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == SMS_PERMISSION_CODE) {
            boolean allPermissionsGranted = true;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            
            if (allPermissionsGranted) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Permissions granted on " + CompatibilityHelper.getAndroidVersion());
            } else {
                Toast.makeText(this, "Some permissions were denied!", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Some permissions denied");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        
        // Clean up references
        etApiUrl = null;
        etSecretKey = null;
        btnSave = null;
        btnViewLogs = null;
        btnSettings = null;
        prefs = null;
    }
}
