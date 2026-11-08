package com.beni.dompetmini;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Dijalankan sekali saat aplikasi dibuka, sehingga tema tersimpan
 * langsung diterapkan sebelum Activity pertama tampil.
 * Daftarkan di AndroidManifest: android:name=".App"
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PrefsManager prefs = new PrefsManager(this);
        AppCompatDelegate.setDefaultNightMode(
                prefs.isDarkMode()
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
