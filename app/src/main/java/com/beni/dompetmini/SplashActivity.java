package com.beni.dompetmini;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

/** Layar pembuka: menentukan tujuan berikutnya (daftar, masuk, atau langsung beranda). */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final PrefsManager prefs = new PrefsManager(this);
        final DbHelper db = new DbHelper(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Class<?> next;
            if (!db.hasUser()) {
                next = RegisterActivity.class;
            } else if (prefs.isRememberMe() && prefs.isLoggedIn()) {
                next = MainActivity.class;
            } else {
                next = LoginActivity.class;
            }
            startActivity(new Intent(this, next));
            finish();
        }, 1300);
    }
}
