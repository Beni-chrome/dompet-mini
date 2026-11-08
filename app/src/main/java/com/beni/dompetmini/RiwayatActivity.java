package com.beni.dompetmini;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

public class RiwayatActivity extends BaseActivity {

    private DbHelper db;
    private LinearLayout container;
    private TextView tvEmpty;
    private TextView[] tabs;
    private int filter = 0; // 0 = semua, 1 = pemasukan, 2 = pengeluaran

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat);
        setupHeader("Riwayat Transaksi");

        db = new DbHelper(this);
        container = findViewById(R.id.listContainer);
        tvEmpty = findViewById(R.id.tvEmpty);

        tabs = new TextView[]{
                findViewById(R.id.tabAll), findViewById(R.id.tabIn), findViewById(R.id.tabOut)};
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> {
                filter = index;
                render();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        for (int i = 0; i < tabs.length; i++) {
            boolean active = i == filter;
            tabs[i].setSelected(active);
            tabs[i].setTextColor(ContextCompat.getColor(this,
                    active ? R.color.white : R.color.textSecondary));
        }

        container.removeAllViews();
        List<DbHelper.Tx> list = db.getAll(100);
        int shown = 0;

        for (DbHelper.Tx t : list) {
            if (filter == 1 && t.amount <= 0) continue;
            if (filter == 2 && t.amount >= 0) continue;
            shown++;

            View row = getLayoutInflater().inflate(R.layout.item_transaction, container, false);
            Utils.bindTxRow(this, row, t);
            final long id = t.id;
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReceiptActivity.class);
                intent.putExtra("tx_id", id);
                startActivity(intent);
            });
            container.addView(row);
        }
        tvEmpty.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
    }
}
