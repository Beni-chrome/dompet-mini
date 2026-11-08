package com.beni.dompetmini;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ReceiptActivity extends BaseActivity {

    private LinearLayout rows;
    private DbHelper.Tx tx;
    private final StringBuilder shareText = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);
        setupHeader("Bukti Transaksi");

        DbHelper db = new DbHelper(this);
        tx = db.getTx(getIntent().getLongExtra("tx_id", -1));
        if (tx == null) {
            finish();
            return;
        }

        rows = findViewById(R.id.rowsContainer);
        ((TextView) findViewById(R.id.tvAmount)).setText(Utils.signedRupiah(tx.amount));
        ((TextView) findViewById(R.id.tvStatusIcon)).setText(Utils.categoryIcon(tx.category));

        shareText.append("Bukti Transaksi Dompet Mini\n");
        addRow("No. Pesanan", Utils.orderId(tx));
        addRow("Waktu", Utils.formatTime(tx.time));
        addRow("Layanan", tx.title);
        if (tx.detail != null && !tx.detail.isEmpty()) addRow("Rincian", tx.detail);
        if (tx.target != null && !tx.target.isEmpty()) addRow("Tujuan", tx.target);
        if (tx.amount < 0) addRow("Metode", "Saldo Dompet Mini");
        addRow("Total", Utils.rupiah(Math.abs(tx.amount)));
        addRow("Status", "Berhasil");

        findViewById(R.id.btnShare).setOnClickListener(v -> share());
        findViewById(R.id.btnDone).setOnClickListener(v -> finish());
    }

    private void addRow(String label, String value) {
        View row = getLayoutInflater().inflate(R.layout.item_detail_row, rows, false);
        ((TextView) row.findViewById(R.id.tvRowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.tvRowValue)).setText(value);
        rows.addView(row);
        shareText.append(label).append(": ").append(value).append("\n");
    }

    private void share() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(intent, "Bagikan bukti transaksi"));
    }
}
