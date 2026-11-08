package com.beni.dompetmini;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private DbHelper db;
    private PrefsManager prefs;
    private EditText etName, etPhone, etPin, etPinConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = new DbHelper(this);
        prefs = new PrefsManager(this);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etPin = findViewById(R.id.etPin);
        etPinConfirm = findViewById(R.id.etPinConfirm);

        findViewById(R.id.btnRegister).setOnClickListener(v -> register());
    }

    private void register() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String pin = etPin.getText().toString();
        String confirm = etPinConfirm.getText().toString();

        if (name.length() < 3) {
            etName.setError("Nama minimal 3 karakter");
            return;
        }
        if (!Utils.isValidPhone(phone)) {
            etPhone.setError("Gunakan nomor HP Indonesia, contoh 081234567890");
            return;
        }
        if (pin.length() != 6) {
            etPin.setError("PIN harus 6 digit");
            return;
        }
        if (Utils.isWeakPin(pin)) {
            etPin.setError("PIN terlalu mudah ditebak");
            return;
        }
        if (!pin.equals(confirm)) {
            etPinConfirm.setError("PIN tidak sama");
            return;
        }

        db.createUser(name, phone, pin);

        // Sesi & preferensi awal disimpan di SharedPreferences
        prefs.setUsername(name.split("\\s+")[0]);
        prefs.setRememberMe(true);
        prefs.setLoggedIn(true);

        Toast.makeText(this, "Akun dibuat! Bonus Rp 100.000 🎉", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
