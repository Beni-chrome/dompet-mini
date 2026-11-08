package com.beni.dompetmini;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

/** Mengelola rekening bank yang ditautkan (dipakai untuk transfer ke bank). */
public class RekeningActivity extends BaseActivity {

    private static final String[] BANKS = {
            "BCA", "BRI", "BNI", "Mandiri", "BSI", "CIMB Niaga",
            "Permata", "BTN", "Jago", "SeaBank"};
    private static final int MAX_ACCOUNTS = 5;

    private DbHelper db;
    private Spinner spBankName;
    private EditText etNumber, etHolder;
    private LinearLayout container;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekening);
        setupHeader("Rekening Bank");

        db = new DbHelper(this);
        spBankName = findViewById(R.id.spBankName);
        etNumber = findViewById(R.id.etNumber);
        etHolder = findViewById(R.id.etHolder);
        container = findViewById(R.id.listContainer);
        tvEmpty = findViewById(R.id.tvEmpty);

        spBankName.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, BANKS));

        DbHelper.User me = db.getUser();
        if (me != null) etHolder.setText(me.name);

        findViewById(R.id.btnSaveBank).setOnClickListener(v -> saveBank());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void saveBank() {
        String bank = BANKS[spBankName.getSelectedItemPosition()];
        String number = etNumber.getText().toString().trim();
        String holder = etHolder.getText().toString().trim();

        if (!number.matches("^\\d{8,16}$")) {
            etNumber.setError("Nomor rekening 8–16 digit angka");
            return;
        }
        if (holder.length() < 3) {
            etHolder.setError("Isi nama pemilik rekening");
            return;
        }
        if (db.getBanks().size() >= MAX_ACCOUNTS) {
            Toast.makeText(this, "Maksimal " + MAX_ACCOUNTS + " rekening", Toast.LENGTH_SHORT).show();
            return;
        }
        if (db.hasBank(bank, number)) {
            etNumber.setError("Rekening ini sudah tertaut");
            return;
        }

        db.addBank(bank, number, holder);
        etNumber.setText("");
        Toast.makeText(this, "Rekening " + bank + " ditautkan", Toast.LENGTH_SHORT).show();
        render();
    }

    private void render() {
        container.removeAllViews();
        List<DbHelper.Bank> list = db.getBanks();
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

        for (DbHelper.Bank b : list) {
            View row = getLayoutInflater().inflate(R.layout.item_bank, container, false);
            ((TextView) row.findViewById(R.id.tvBankName))
                    .setText(b.bank + " • " + Utils.maskAccount(b.number));
            ((TextView) row.findViewById(R.id.tvBankHolder)).setText("a.n. " + b.holder);
            row.findViewById(R.id.tvBankDelete).setOnClickListener(v -> confirmDelete(b));
            container.addView(row);
        }
    }

    private void confirmDelete(final DbHelper.Bank b) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus rekening?")
                .setMessage(b.bank + " " + Utils.maskAccount(b.number) + "\na.n. " + b.holder)
                .setPositiveButton("Hapus", (d, w) -> {
                    db.deleteBank(b.id);
                    render();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
