package com.beni.dompetmini;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Pembungkus SharedPreferences. Semua akses pengaturan lewat kelas ini,
 * jadi nama key tidak tersebar di banyak Activity.
 * Hanya untuk data kecil (pengaturan & sesi). PIN disimpan sebagai hash di database.
 */
public class PrefsManager {

    private static final String PREF_NAME = "dompet_mini_prefs";

    private static final String KEY_USERNAME = "username";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_HIDE_BALANCE = "hide_balance";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_LAST_TRANSFER = "last_transfer_number";
    private static final String KEY_PLAYER_ID_PREFIX = "player_id_";

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ---------- Nama panggilan ----------
    public void setUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    // ---------- Tema gelap ----------
    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    // ---------- Ingat saya (lewati layar PIN saat aplikasi dibuka) ----------
    public void setRememberMe(boolean enabled) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, enabled).apply();
    }

    public boolean isRememberMe() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }

    // ---------- Status sesi masuk ----------
    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    // ---------- Sembunyikan saldo ----------
    public void setHideBalance(boolean enabled) {
        prefs.edit().putBoolean(KEY_HIDE_BALANCE, enabled).apply();
    }

    public boolean isHideBalance() {
        return prefs.getBoolean(KEY_HIDE_BALANCE, false);
    }

    // ---------- Penerima transfer terakhir ----------
    public void setLastTransferNumber(String number) {
        prefs.edit().putString(KEY_LAST_TRANSFER, number).apply();
    }

    public String getLastTransferNumber() {
        return prefs.getString(KEY_LAST_TRANSFER, "");
    }

    // ---------- ID pemain / nomor tujuan terakhir per produk ----------
    public void setPlayerId(String productName, String value) {
        prefs.edit().putString(KEY_PLAYER_ID_PREFIX + productName, value).apply();
    }

    public String getPlayerId(String productName) {
        return prefs.getString(KEY_PLAYER_ID_PREFIX + productName, "");
    }

    // ---------- Reset semua pengaturan ----------
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
