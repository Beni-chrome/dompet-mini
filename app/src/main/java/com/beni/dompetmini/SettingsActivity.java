package com.beni.dompetmini;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends BaseActivity {

    private PrefsManager prefs;
    private DbHelper db;
    private EditText etUsername;
    private SwitchCompat swDarkMode, swRememberMe, swHideBalance;
    private Button btnSave, btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupHeader("Pengaturan");

        prefs = new PrefsManager(this);
        db = new DbHelper(this);

        etUsername = findViewById(R.id.etUsername);
        swDarkMode = findViewById(R.id.swDarkMode);
        swRememberMe = findViewById(R.id.swRememberMe);
        swHideBalance = findViewById(R.id.swHideBalance);
        btnSave = findViewById(R.id.btnSave);
        btnReset = findViewById(R.id.btnReset);

        bindProfile();

        // Tema langsung berubah saat switch diketuk (setOnClickListener
        // hanya aktif karena ketukan pengguna, bukan saat setChecked dari kode).
        swDarkMode.setOnClickListener(v -> {
            boolean dark = swDarkMode.isChecked();
            prefs.setDarkMode(dark);
            AppCompatDelegate.setDefaultNightMode(
                    dark ? AppCompatDelegate.MODE_NIGHT_YES
                         : AppCompatDelegate.MODE_NIGHT_NO);
        });

        btnSave.setOnClickListener(v -> saveSettings());

        btnReset.setOnClickListener(v -> {
            prefs.clearAll();
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            loadSettings();
            Toast.makeText(this, "Pengaturan direset", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnChangePin).setOnClickListener(v -> showChangePinDialog());
        findViewById(R.id.btnLogout).setOnClickListener(v -> confirmLogout());
    }

    // Pengaturan dibaca ulang setiap layar kembali tampil.
    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
    }

    private void bindProfile() {
        DbHelper.User user = db.getUser();
        if (user == null) return;
        ((TextView) findViewById(R.id.tvProfileAvatar))
                .setText(user.name.substring(0, 1).toUpperCase());
        ((TextView) findViewById(R.id.tvProfileName)).setText(user.name);
        ((TextView) findViewById(R.id.tvProfilePhone)).setText(user.phone);
        ((TextView) findViewById(R.id.tvProfileSince))
                .setText("Member sejak " + Utils.formatDate(user.created));
    }

    private void loadSettings() {
        etUsername.setText(prefs.getUsername());
        swDarkMode.setChecked(prefs.isDarkMode());
        swRememberMe.setChecked(prefs.isRememberMe());
        swHideBalance.setChecked(prefs.isHideBalance());
    }

    private void saveSettings() {
        String username = etUsername.getText().toString().trim();

        if (username.isEmpty()) {
            etUsername.setError("Nama panggilan tidak boleh kosong");
            return;
        }

        prefs.setUsername(username);
        prefs.setRememberMe(swRememberMe.isChecked());
        prefs.setHideBalance(swHideBalance.isChecked());
        // dark mode sudah tersimpan saat switch diketuk

        Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show();
    }

    private void showChangePinDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Utils.dp(this, 24);
        box.setPadding(p, Utils.dp(this, 8), p, 0);

        final EditText etOld = Utils.pinField(this, "PIN lama");
        final EditText etNew = Utils.pinField(this, "PIN baru (6 digit)");
        final EditText etConfirm = Utils.pinField(this, "Ulangi PIN baru");
        box.addView(etOld);
        box.addView(etNew);
        box.addView(etConfirm);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Ganti PIN")
                .setView(box)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(x ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String oldPin = etOld.getText().toString();
                    String newPin = etNew.getText().toString();
                    String confirm = etConfirm.getText().toString();

                    if (oldPin.length() != 6) {
                        etOld.setError("PIN lama 6 digit");
                        return;
                    }
                    if (newPin.length() != 6) {
                        etNew.setError("PIN baru harus 6 digit");
                        return;
                    }
                    if (Utils.isWeakPin(newPin)) {
                        etNew.setError("PIN terlalu mudah ditebak");
                        return;
                    }
                    if (!newPin.equals(confirm)) {
                        etConfirm.setError("PIN tidak sama");
                        return;
                    }
                    if (!db.changePin(oldPin, newPin)) {
                        etOld.setError("PIN lama salah");
                        return;
                    }
                    Toast.makeText(this, "PIN berhasil diganti", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Keluar dari akun?")
                .setMessage("Anda perlu memasukkan PIN lagi untuk masuk.")
                .setPositiveButton("Keluar", (d, w) -> {
                    prefs.setLoggedIn(false);
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
