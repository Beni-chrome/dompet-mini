package com.beni.dompetmini;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.util.List;

/** Minta uang: buat permintaan dana, bagikan ke teman, dan pantau statusnya. */
public class MintaActivity extends BaseActivity {

    private static final long MIN_REQUEST = 1000;
    private static final long[] QUICK = {10000, 50000, 100000, 250000};

    private DbHelper db;
    private EditText etAmount, etNote;
    private LinearLayout container;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minta);
        setupHeader("Minta Uang");

        db = new DbHelper(this);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        container = findViewById(R.id.listContainer);
        tvEmpty = findViewById(R.id.tvEmpty);

        Utils.addAmountChips(this, (LinearLayout) findViewById(R.id.rowChips), etAmount, QUICK);
        findViewById(R.id.btnCreate).setOnClickListener(v -> createRequest());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void createRequest() {
        long amount = 0;
        try {
            amount = Long.parseLong(etAmount.getText().toString().trim());
        } catch (NumberFormatException ignored) {
            // ditangani di bawah
        }
        if (amount < MIN_REQUEST) {
            etAmount.setError("Minimal " + Utils.rupiah(MIN_REQUEST));
            return;
        }

        String note = etNote.getText().toString().trim();
        long id = db.addRequest(amount, note);
        etAmount.setText("");
        etNote.setText("");
        Toast.makeText(this, "Permintaan dibuat", Toast.LENGTH_SHORT).show();
        render();

        // Langsung tawarkan untuk dibagikan
        for (DbHelper.Req r : db.getRequests(1)) {
            if (r.id == id) share(r);
        }
    }

    private void render() {
        container.removeAllViews();
        List<DbHelper.Req> list = db.getRequests(50);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

        for (DbHelper.Req r : list) {
            View row = getLayoutInflater().inflate(R.layout.item_transaction, container, false);
            ((TextView) row.findViewById(R.id.tvTxIcon)).setText("📥");
            ((TextView) row.findViewById(R.id.tvTxTitle)).setText("Minta " + Utils.rupiah(r.amount));
            ((TextView) row.findViewById(R.id.tvTxDetail)).setText(
                    (r.note.isEmpty() ? "Tanpa catatan" : r.note) + "\n" + Utils.formatTime(r.created));

            TextView tvStatus = row.findViewById(R.id.tvTxAmount);
            tvStatus.setText(statusLabel(r.status));
            tvStatus.setTextColor(ContextCompat.getColor(this, statusColor(r.status)));

            row.setOnClickListener(v -> showActions(r));
            container.addView(row);
        }
    }

    private String statusLabel(String status) {
        if (DbHelper.REQ_PAID.equals(status)) return "Lunas";
        if (DbHelper.REQ_CANCELLED.equals(status)) return "Dibatalkan";
        return "Menunggu";
    }

    private int statusColor(String status) {
        if (DbHelper.REQ_PAID.equals(status)) return R.color.success;
        if (DbHelper.REQ_CANCELLED.equals(status)) return R.color.textSecondary;
        return R.color.brandStart;
    }

    private void showActions(final DbHelper.Req r) {
        final boolean pending = DbHelper.REQ_PENDING.equals(r.status);
        final String[] options = pending
                ? new String[]{"Bagikan permintaan", "Simulasikan dibayar", "Batalkan permintaan"}
                : new String[]{"Bagikan permintaan"};

        new AlertDialog.Builder(this)
                .setTitle("Minta " + Utils.rupiah(r.amount))
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        share(r);
                    } else if (which == 1) {
                        markPaid(r);
                    } else {
                        db.setRequestStatus(r.id, DbHelper.REQ_CANCELLED);
                        render();
                    }
                })
                .show();
    }

    private void share(DbHelper.Req r) {
        DbHelper.User me = db.getUser();
        StringBuilder sb = new StringBuilder();
        sb.append("Halo! ").append(me != null ? me.name : "Saya")
                .append(" meminta ").append(Utils.rupiah(r.amount))
                .append(" lewat Dompet Mini.\n");
        if (!r.note.isEmpty()) sb.append("Untuk: ").append(r.note).append("\n");
        if (me != null) sb.append("Kirim ke nomor: ").append(me.phone).append("\n");
        sb.append("Terima kasih!");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(intent, "Bagikan permintaan"));
    }

    /** Simulasi: pihak lain membayar permintaan ini, saldo bertambah. */
    private void markPaid(DbHelper.Req r) {
        db.setRequestStatus(r.id, DbHelper.REQ_PAID);
        long txId = db.addTransaction(DbHelper.CAT_REQUEST, "Permintaan dibayar",
                r.note.isEmpty() ? "Pembayaran permintaan dana" : r.note, "", r.amount);

        Intent intent = new Intent(this, ReceiptActivity.class);
        intent.putExtra("tx_id", txId);
        startActivity(intent);
    }
}
