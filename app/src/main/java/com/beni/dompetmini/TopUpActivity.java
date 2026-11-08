package com.beni.dompetmini;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

/** Layar pembelian generik: game, pulsa, dan voucher memakai alur yang sama. */
public class TopUpActivity extends BaseActivity {

    private PrefsManager prefs;
    private DbHelper db;
    private GameCatalog.Game game;
    private GameCatalog.Nominal selected;
    private View selectedView;

    private EditText etPlayerId;
    private TextView tvTotal, tvPayBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topup);

        prefs = new PrefsManager(this);
        db = new DbHelper(this);

        int index = getIntent().getIntExtra("game_index", 0);
        game = GameCatalog.GAMES.get(index);
        setupHeader(game.category.equals(GameCatalog.CAT_GAME) ? "Top-Up Game" : game.txTitle());

        Utils.bindGameIcon(this, findViewById(android.R.id.content), game);
        ((TextView) findViewById(R.id.tvGameName)).setText(game.name);
        ((TextView) findViewById(R.id.tvGameDesc)).setText(game.tagline());
        ((TextView) findViewById(R.id.tvInputLabel)).setText("1  Masukkan " + game.inputLabel);

        etPlayerId = findViewById(R.id.etPlayerId);
        etPlayerId.setInputType(game.inputType);
        etPlayerId.setHint(game.inputHint);
        // Data tujuan terakhir diingat lewat SharedPreferences
        etPlayerId.setText(prefs.getPlayerId(game.name));

        tvTotal = findViewById(R.id.tvTotal);
        tvPayBalance = findViewById(R.id.tvPayBalance);

        buildNominals();
        findViewById(R.id.btnBuy).setOnClickListener(v -> attemptPurchase());
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvPayBalance.setText("Saldo: " + Utils.rupiah(db.getBalance()));
    }

    private void buildNominals() {
        GridLayout grid = findViewById(R.id.gridNominal);
        for (GameCatalog.Nominal n : game.nominals) {
            View item = getLayoutInflater().inflate(R.layout.item_nominal, grid, false);
            ((TextView) item.findViewById(R.id.tvNominalLabel)).setText(n.label);
            ((TextView) item.findViewById(R.id.tvNominalPrice)).setText(Utils.rupiah(n.price));
            item.setLayoutParams(Utils.cell(this, 6));
            item.setOnClickListener(v -> {
                if (selectedView != null) selectedView.setSelected(false);
                v.setSelected(true);
                selectedView = v;
                selected = n;
                tvTotal.setText(Utils.rupiah(n.price));
            });
            grid.addView(item);
        }
    }

    private String validateTarget(String value) {
        switch (game.category) {
            case GameCatalog.CAT_PULSA:
                return Utils.isValidPhone(value) ? null : "Nomor HP tidak valid (contoh 081234567890)";
            case GameCatalog.CAT_VOUCHER:
                return Patterns.EMAIL_ADDRESS.matcher(value).matches() ? null : "Format email tidak valid";
            default:
                return value.length() >= 5 ? null : "ID minimal 5 digit";
        }
    }

    private void attemptPurchase() {
        final String target = etPlayerId.getText().toString().trim();
        String error = validateTarget(target);
        if (error != null) {
            etPlayerId.setError(error);
            return;
        }
        if (selected == null) {
            Toast.makeText(this, "Pilih nominal terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (db.getBalance() < selected.price) {
            new AlertDialog.Builder(this)
                    .setTitle("Saldo tidak cukup")
                    .setMessage("Saldo Dompet Mini Anda belum cukup untuk pembelian ini. "
                            + "Isi saldo dulu, ya.")
                    .setPositiveButton("Isi Saldo", (d, w) ->
                            startActivity(new Intent(this, IsiSaldoActivity.class)))
                    .setNegativeButton("Batal", null)
                    .show();
            return;
        }

        String summary = game.name + "\n" + selected.label
                + "\n" + game.inputLabel + ": " + target
                + "\nTotal: " + Utils.rupiah(selected.price);

        PinDialog.ask(this, db, summary, () ->
                Utils.showProcessing(this, "Memproses pembayaran…", () -> {
                    prefs.setPlayerId(game.name, target);
                    long id = db.addTransaction(game.category, game.txTitle(),
                            selected.label, target, -selected.price);

                    Intent intent = new Intent(this, ReceiptActivity.class);
                    intent.putExtra("tx_id", id);
                    startActivity(intent);
                    finish();
                }));
    }
}
