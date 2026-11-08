package com.beni.dompetmini;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class IsiSaldoActivity extends BaseActivity {

    private static final long[] AMOUNTS = {20000, 50000, 100000, 200000, 500000, 1000000};

    private DbHelper db;
    private long selectedAmount = 0;
    private View selectedView;
    private TextView tvTotal, tvCurrentBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_isisaldo);
        setupHeader("Isi Saldo");

        db = new DbHelper(this);
        tvTotal = findViewById(R.id.tvTotal);
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);

        GridLayout grid = findViewById(R.id.gridAmount);
        for (long amount : AMOUNTS) {
            View item = getLayoutInflater().inflate(R.layout.item_nominal, grid, false);
            ((TextView) item.findViewById(R.id.tvNominalLabel)).setText(Utils.rupiah(amount));
            item.findViewById(R.id.tvNominalPrice).setVisibility(View.GONE);
            item.setLayoutParams(Utils.cell(this, 6));
            item.setOnClickListener(v -> {
                if (selectedView != null) selectedView.setSelected(false);
                v.setSelected(true);
                selectedView = v;
                selectedAmount = amount;
                tvTotal.setText(Utils.rupiah(amount));
            });
            grid.addView(item);
        }

        findViewById(R.id.btnTopUpSaldo).setOnClickListener(v -> topUp());
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvCurrentBalance.setText("Saldo saat ini: " + Utils.rupiah(db.getBalance()));
    }

    private void topUp() {
        if (selectedAmount <= 0) {
            Toast.makeText(this, "Pilih nominal isi saldo", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioGroup group = findViewById(R.id.rgMethod);
        RadioButton checked = findViewById(group.getCheckedRadioButtonId());
        final String method = checked.getText().toString();

        Utils.showProcessing(this, "Memproses isi saldo…", () -> {
            long id = db.addTransaction(DbHelper.CAT_TOPUP, "Isi Saldo", method, "", selectedAmount);
            Intent intent = new Intent(this, ReceiptActivity.class);
            intent.putExtra("tx_id", id);
            startActivity(intent);
            finish();
        });
    }
}
