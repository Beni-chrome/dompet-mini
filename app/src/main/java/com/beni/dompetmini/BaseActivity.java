package com.beni.dompetmini;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Activity dasar: mengatur header (judul + tombol kembali) yang dipakai semua layar. */
public abstract class BaseActivity extends AppCompatActivity {

    protected void setupHeader(String title) {
        TextView tvTitle = findViewById(R.id.tvHeaderTitle);
        tvTitle.setText(title);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
