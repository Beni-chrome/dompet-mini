package com.beni.dompetmini;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_SECONDS = 30;

    private DbHelper db;
    private PrefsManager prefs;
    private EditText etPin;
    private CheckBox cbRemember;
    private Button btnLogin;
    private int attempts = 0;
    private CountDownTimer lockTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new DbHelper(this);
        prefs = new PrefsManager(this);

        etPin = findViewById(R.id.etPin);
        cbRemember = findViewById(R.id.cbRemember);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvWelcome = findViewById(R.id.tvWelcome);

        DbHelper.User user = db.getUser();
        tvWelcome.setText(user == null ? "Halo lagi!" : "Halo lagi, " + user.name.split("\\s+")[0] + "!");
        cbRemember.setChecked(prefs.isRememberMe());

        btnLogin.setOnClickListener(v -> login());
        findViewById(R.id.tvForgot).setOnClickListener(v -> confirmReset());
    }

    private void login() {
        String pin = etPin.getText().toString();
        if (pin.length() != 6) {
            etPin.setError("PIN harus 6 digit");
            return;
        }

        if (db.verifyPin(pin)) {
            prefs.setLoggedIn(true);
            prefs.setRememberMe(cbRemember.isChecked());
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            attempts++;
            etPin.setText("");
            if (attempts >= MAX_ATTEMPTS) {
                lock();
            } else {
                etPin.setError("PIN salah. Sisa percobaan: " + (MAX_ATTEMPTS - attempts));
            }
        }
    }

    private void lock() {
        btnLogin.setEnabled(false);
        etPin.setEnabled(false);
        lockTimer = new CountDownTimer(LOCK_SECONDS * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnLogin.setText("Coba lagi dalam " + (millisUntilFinished / 1000 + 1) + " dtk");
            }

            @Override
            public void onFinish() {
                attempts = 0;
                btnLogin.setEnabled(true);
                etPin.setEnabled(true);
                btnLogin.setText("Masuk");
            }
        }.start();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Lupa PIN?")
                .setMessage("Demi keamanan, PIN tidak bisa dilihat. Anda dapat mereset akun, "
                        + "tetapi seluruh saldo dan riwayat di perangkat ini akan dihapus.")
                .setPositiveButton("Hapus & Daftar Ulang", (d, w) -> {
                    DbHelper.wipe(this, db);
                    prefs.clearAll();
                    Intent intent = new Intent(this, RegisterActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lockTimer != null) lockTimer.cancel();
    }
}
