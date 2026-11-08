package com.beni.dompetmini;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/** Kirim uang: ke sesama pengguna (nomor HP) atau ke rekening bank yang sudah ditautkan. */
public class TransferActivity extends BaseActivity {

    private static final long MIN_TRANSFER = 10000;
    private static final long BANK_FEE = 2500;
    private static final long[] QUICK = {10000, 50000, 100000, 250000};

    private PrefsManager prefs;
    private DbHelper db;
    private EditText etPhone, etAmount, etNote;
    private TextView tvBalance, tvFee, tvNoBank, tabWallet, tabBank;
    private View blockWallet, blockBank;
    private Spinner spBank;
    private List<DbHelper.Bank> banks = new ArrayList<>();
    private int mode = 0; // 0 = sesama dompet, 1 = rekening bank

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);
        setupHeader("Kirim Uang");

        prefs = new PrefsManager(this);
        db = new DbHelper(this);

        etPhone = findViewById(R.id.etPhone);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        tvBalance = findViewById(R.id.tvBalance);
        tvFee = findViewById(R.id.tvFee);
        tvNoBank = findViewById(R.id.tvNoBank);
        tabWallet = findViewById(R.id.tabWallet);
        tabBank = findViewById(R.id.tabBank);
        blockWallet = findViewById(R.id.blockWallet);
        blockBank = findViewById(R.id.blockBank);
        spBank = findViewById(R.id.spBank);

        // Penerima terakhir diingat lewat SharedPreferences
        final String last = prefs.getLastTransferNumber();
        TextView tvLast = findViewById(R.id.tvLastRecipient);
        if (!last.isEmpty()) {
            tvLast.setText("Penerima terakhir: " + last);
            tvLast.setVisibility(View.VISIBLE);
            tvLast.setOnClickListener(v -> etPhone.setText(last));
        }

        Utils.addAmountChips(this, (LinearLayout) findViewById(R.id.rowChips), etAmount, QUICK);

        tabWallet.setOnClickListener(v -> setMode(0));
        tabBank.setOnClickListener(v -> setMode(1));
        tvNoBank.setOnClickListener(v -> startActivity(new Intent(this, RekeningActivity.class)));
        findViewById(R.id.btnSend).setOnClickListener(v -> attemptTransfer());

        setMode(getIntent().getIntExtra("mode", 0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvBalance.setText("Saldo: " + Utils.rupiah(db.getBalance()));
        loadBanks();
    }

    private void loadBanks() {
        int previous = spBank.getSelectedItemPosition();
        banks = db.getBanks();

        List<String> labels = new ArrayList<>();
        for (DbHelper.Bank b : banks) {
            labels.add(b.bank + " • " + Utils.maskAccount(b.number) + " • " + b.holder);
        }
        spBank.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
        if (previous >= 0 && previous < labels.size()) spBank.setSelection(previous);

        boolean none = banks.isEmpty();
        spBank.setVisibility(none ? View.GONE : View.VISIBLE);
        tvNoBank.setVisibility(none ? View.VISIBLE : View.GONE);
    }

    private void setMode(int newMode) {
        mode = newMode;
        tabWallet.setSelected(mode == 0);
        tabBank.setSelected(mode == 1);
        tabWallet.setTextColor(ContextCompat.getColor(this,
                mode == 0 ? R.color.white : R.color.textSecondary));
        tabBank.setTextColor(ContextCompat.getColor(this,
                mode == 1 ? R.color.white : R.color.textSecondary));

        blockWallet.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
        blockBank.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
        tvFee.setText(mode == 0
                ? "Biaya admin Rp 0 • Transfer diproses instan"
                : "Biaya admin " + Utils.rupiah(BANK_FEE) + " • Diproses instan ke rekening tujuan");
    }

    private void attemptTransfer() {
        final String note = etNote.getText().toString().trim();

        long parsed = 0;
        try {
            parsed = Long.parseLong(etAmount.getText().toString().trim());
        } catch (NumberFormatException ignored) {
            // dibiarkan 0, ditangani di bawah
        }
        final long amount = parsed;

        if (mode == 0) {
            transferToWallet(amount, note);
        } else {
            transferToBank(amount, note);
        }
    }

    // ---------- Kirim ke sesama pengguna ----------
    private void transferToWallet(final long amount, final String note) {
        final String phone = etPhone.getText().toString().trim();

        if (!Utils.isValidPhone(phone)) {
            etPhone.setError("Nomor HP tidak valid (contoh 081234567890)");
            return;
        }
        DbHelper.User me = db.getUser();
        if (me != null && phone.equals(me.phone)) {
            etPhone.setError("Tidak bisa transfer ke nomor sendiri");
            return;
        }
        if (amount < MIN_TRANSFER) {
            etAmount.setError("Minimal transfer " + Utils.rupiah(MIN_TRANSFER));
            return;
        }
        if (amount > db.getBalance()) {
            showInsufficientDialog();
            return;
        }

        String summary = "Kirim " + Utils.rupiah(amount) + " ke " + phone
                + "\nBiaya admin: Rp 0";

        PinDialog.ask(this, db, summary, () ->
                Utils.showProcessing(this, "Mengirim uang…", () -> {
                    prefs.setLastTransferNumber(phone);
                    long id = db.addTransaction(DbHelper.CAT_TRANSFER, "Transfer ke " + phone,
                            note.isEmpty() ? "Transfer saldo" : note, phone, -amount);
                    openReceipt(id);
                }));
    }

    // ---------- Kirim ke rekening bank ----------
    private void transferToBank(final long amount, final String note) {
        if (banks.isEmpty()) {
            Toast.makeText(this, "Tambahkan rekening bank dulu", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, RekeningActivity.class));
            return;
        }
        int position = spBank.getSelectedItemPosition();
        if (position < 0 || position >= banks.size()) return;
        final DbHelper.Bank bank = banks.get(position);

        if (amount < MIN_TRANSFER) {
            etAmount.setError("Minimal transfer " + Utils.rupiah(MIN_TRANSFER));
            return;
        }
        final long total = amount + BANK_FEE;
        if (total > db.getBalance()) {
            showInsufficientDialog();
            return;
        }

        String summary = "Kirim " + Utils.rupiah(amount) + " ke " + bank.bank + " "
                + bank.number + " (a.n. " + bank.holder + ")"
                + "\nBiaya admin: " + Utils.rupiah(BANK_FEE)
                + "\nTotal: " + Utils.rupiah(total);

        PinDialog.ask(this, db, summary, () ->
                Utils.showProcessing(this, "Mengirim ke rekening…", () -> {
                    String detail = (note.isEmpty() ? "Transfer ke rekening" : note)
                            + " • a.n. " + bank.holder
                            + " • termasuk admin " + Utils.rupiah(BANK_FEE);
                    long id = db.addTransaction(DbHelper.CAT_BANK, "Transfer ke " + bank.bank,
                            detail, bank.number, -total);
                    openReceipt(id);
                }));
    }

    private void showInsufficientDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Saldo tidak cukup")
                .setMessage("Saldo Anda " + Utils.rupiah(db.getBalance())
                        + " belum cukup untuk transfer ini.")
                .setPositiveButton("Top Up", (d, w) ->
                        startActivity(new Intent(this, IsiSaldoActivity.class)))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void openReceipt(long id) {
        Intent intent = new Intent(this, ReceiptActivity.class);
        intent.putExtra("tx_id", id);
        startActivity(intent);
        finish();
    }
}
