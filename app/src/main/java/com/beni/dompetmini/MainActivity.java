package com.beni.dompetmini;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private PrefsManager prefs;
    private DbHelper db;
    private View secGames, secPulsa, secVoucher;
    private LinearLayout rowGames, listPulsa, listVoucher, recentContainer;
    private TextView tvGreeting, tvSub, tvBalance, tvEye, tvAvatar, tvNoResult, tvNoRecent;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new PrefsManager(this);
        db = new DbHelper(this);

        secGames = findViewById(R.id.secGames);
        secPulsa = findViewById(R.id.secPulsa);
        secVoucher = findViewById(R.id.secVoucher);
        rowGames = findViewById(R.id.rowGames);
        listPulsa = findViewById(R.id.listPulsa);
        listVoucher = findViewById(R.id.listVoucher);
        recentContainer = findViewById(R.id.recentContainer);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvSub = findViewById(R.id.tvSub);
        tvBalance = findViewById(R.id.tvBalance);
        tvEye = findViewById(R.id.tvEye);
        tvAvatar = findViewById(R.id.tvAvatar);
        tvNoResult = findViewById(R.id.tvNoResult);
        tvNoRecent = findViewById(R.id.tvNoRecent);
        etSearch = findViewById(R.id.etSearch);

        renderCatalog("");

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderCatalog(s.toString().trim().toLowerCase(Locale.ROOT));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Tombol sembunyikan/tampilkan saldo, disimpan di SharedPreferences
        tvEye.setOnClickListener(v -> {
            prefs.setHideBalance(!prefs.isHideBalance());
            refresh();
        });

        // Aksi utama di kartu dompet
        findViewById(R.id.actKirim).setOnClickListener(v ->
                startActivity(new Intent(this, TransferActivity.class)));
        findViewById(R.id.actMinta).setOnClickListener(v ->
                startActivity(new Intent(this, MintaActivity.class)));
        findViewById(R.id.actRekening).setOnClickListener(v ->
                startActivity(new Intent(this, RekeningActivity.class)));
        findViewById(R.id.actTopUp).setOnClickListener(v ->
                startActivity(new Intent(this, IsiSaldoActivity.class)));

        findViewById(R.id.tvHelp).setOnClickListener(v -> showHelp());
        tvAvatar.setOnClickListener(v -> openSettings());
        findViewById(R.id.tvSeeAll).setOnClickListener(v -> openRiwayat());
        findViewById(R.id.navHistory).setOnClickListener(v -> openRiwayat());
        findViewById(R.id.navSettings).setOnClickListener(v -> openSettings());
    }

    // Dibaca ulang setiap layar kembali tampil
    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        refreshRecent();
    }

    private void refresh() {
        String name = prefs.getUsername();
        if (name.isEmpty()) {
            DbHelper.User u = db.getUser();
            if (u != null) name = u.name.split("\\s+")[0];
        }
        tvGreeting.setText(name.isEmpty() ? "Halo, Kawan!" : "Halo, " + name + "!");
        tvAvatar.setText(name.isEmpty() ? "K" : name.substring(0, 1).toUpperCase());

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String sapa;
        if (hour < 11) {
            sapa = "Sudah sarapan? Isi saldo dulu, yuk.";
        } else if (hour < 15) {
            sapa = "Jangan lupa makan siang, ya.";
        } else if (hour < 18) {
            sapa = "Sore-sore gini enaknya push rank.";
        } else {
            sapa = "Malam begini, waktunya mabar!";
        }
        tvSub.setText(sapa);

        boolean hide = prefs.isHideBalance();
        tvBalance.setText(hide ? "Rp ••••••" : Utils.rupiah(db.getBalance()));
        tvEye.setText(hide ? "Tampilkan" : "Sembunyikan");
    }

    private void refreshRecent() {
        recentContainer.removeAllViews();
        List<DbHelper.Tx> list = db.getAll(3);
        tvNoRecent.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        for (DbHelper.Tx t : list) {
            View row = getLayoutInflater().inflate(R.layout.item_transaction, recentContainer, false);
            Utils.bindTxRow(this, row, t);
            final long id = t.id;
            row.setOnClickListener(v -> openReceipt(id));
            recentContainer.addView(row);
        }
    }

    private void renderCatalog(String query) {
        int total = fillCarousel(query);
        total += fillList(listPulsa, secPulsa, GameCatalog.CAT_PULSA, query);
        total += fillList(listVoucher, secVoucher, GameCatalog.CAT_VOUCHER, query);
        tvNoResult.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
    }

    /** Game ditampilkan sebagai kartu yang bisa digeser ke samping. */
    private int fillCarousel(String query) {
        rowGames.removeAllViews();
        int count = 0;
        for (GameCatalog.Game g : GameCatalog.byCategory(GameCatalog.CAT_GAME)) {
            if (!matches(g, query)) continue;
            count++;

            View card = getLayoutInflater().inflate(R.layout.item_game_card, rowGames, false);
            Utils.bindGameIcon(this, card, g, 16);
            ((TextView) card.findViewById(R.id.tvGameName)).setText(g.name);
            ((TextView) card.findViewById(R.id.tvGameCurrency)).setText(g.currency);
            card.setOnClickListener(v -> openProduct(g));
            rowGames.addView(card);
        }
        secGames.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        return count;
    }

    /** Pulsa dan voucher ditampilkan sebagai daftar baris. */
    private int fillList(LinearLayout list, View section, String category, String query) {
        list.removeAllViews();
        int count = 0;
        for (GameCatalog.Game g : GameCatalog.byCategory(category)) {
            if (!matches(g, query)) continue;
            count++;

            View row = getLayoutInflater().inflate(R.layout.item_product_row, list, false);
            Utils.bindGameIcon(this, row, g, 12);
            ((TextView) row.findViewById(R.id.tvGameName)).setText(g.name);
            ((TextView) row.findViewById(R.id.tvGameCurrency))
                    .setText("Mulai " + Utils.rupiah(g.minPrice()));
            row.setOnClickListener(v -> openProduct(g));
            list.addView(row);
        }
        section.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        return count;
    }

    private boolean matches(GameCatalog.Game g, String query) {
        return query.isEmpty() || g.name.toLowerCase(Locale.ROOT).contains(query);
    }

    private void openProduct(GameCatalog.Game g) {
        Intent intent = new Intent(this, TopUpActivity.class);
        intent.putExtra("game_index", GameCatalog.GAMES.indexOf(g));
        startActivity(intent);
    }

    private void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle("Butuh bantuan?")
                .setMessage("• Top-up langsung masuk setelah PIN kamu dikonfirmasi.\n"
                        + "• Saldo kurang? Ketuk Top Up di kartu dompet.\n"
                        + "• Semua bukti transaksi ada di menu Catatan, dan bisa dibagikan.\n"
                        + "• Lupa PIN? Pakai opsi reset di layar masuk.\n\n"
                        + "Kontak: support@dompetmini.example")
                .setPositiveButton("Oke, paham", null)
                .show();
    }

    private void openReceipt(long id) {
        Intent intent = new Intent(this, ReceiptActivity.class);
        intent.putExtra("tx_id", id);
        startActivity(intent);
    }

    private void openRiwayat() {
        startActivity(new Intent(this, RiwayatActivity.class));
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}
