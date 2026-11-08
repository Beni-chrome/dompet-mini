package com.beni.dompetmini;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Utils {

    private Utils() {}

    // ---------- Format ----------
    public static String rupiah(long value) {
        String digits = String.format(Locale.US, "%,d", Math.abs(value)).replace(',', '.');
        return (value < 0 ? "-Rp " : "Rp ") + digits;
    }

    public static String signedRupiah(long value) {
        return value > 0 ? "+" + rupiah(value) : rupiah(value);
    }

    public static String formatTime(long millis) {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
                .format(new Date(millis));
    }

    public static String formatDate(long millis) {
        return new SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID"))
                .format(new Date(millis));
    }

    /** Nomor pesanan, mis. DM2026110600012 (tanggal + nomor urut transaksi). */
    public static String orderId(DbHelper.Tx t) {
        String date = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(t.time));
        return "DM" + date + String.format(Locale.US, "%05d", t.id);
    }

    // ---------- Validasi ----------
    public static boolean isValidPhone(String s) {
        return s.matches("^08\\d{8,11}$");
    }

    public static boolean isWeakPin(String pin) {
        return pin.matches("(\\d)\\1{5}")
                || "0123456789".contains(pin)
                || "9876543210".contains(pin);
    }

    // ---------- Tampilan ----------
    public static int dp(Context c, int value) {
        return (int) (value * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    /** Kotak berwarna dengan sudut membulat (dipakai untuk ikon cadangan). */
    public static GradientDrawable rounded(int color, int radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static int parse(String hex) {
        return Color.parseColor(hex);
    }

    /** Parameter sel GridLayout dengan lebar sama rata (weight 1). */
    public static GridLayout.LayoutParams cell(Context c, int marginDp) {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        int m = dp(c, marginDp);
        lp.setMargins(m, m, m, m);
        return lp;
    }

    /**
     * Menampilkan logo dari res/drawable (mis. logo_mlbb.png) jika file-nya ada.
     * Jika belum ada, otomatis memakai emoji berwarna sebagai cadangan.
     */
    public static void bindGameIcon(Context c, View root, GameCatalog.Game g) {
        bindGameIcon(c, root, g, 18);
    }

    public static void bindGameIcon(Context c, View root, GameCatalog.Game g, int radiusDp) {
        TextView tv = root.findViewById(R.id.tvGameIcon);
        ImageView iv = root.findViewById(R.id.ivGameLogo);
        int res = c.getResources().getIdentifier(g.logoName, "drawable", c.getPackageName());
        if (res != 0) {
            iv.setImageResource(res);
            iv.setBackground(rounded(Color.WHITE, radiusDp, c));
            iv.setClipToOutline(true);
            iv.setVisibility(View.VISIBLE);
            tv.setVisibility(View.GONE);
        } else {
            tv.setText(g.emoji);
            tv.setBackground(rounded(g.color, radiusDp, c));
            tv.setVisibility(View.VISIBLE);
            iv.setVisibility(View.GONE);
        }
    }

    public static String categoryIcon(String category) {
        if (category == null) return "🧾";
        switch (category) {
            case GameCatalog.CAT_GAME:
                return "🎮";
            case GameCatalog.CAT_PULSA:
                return "📱";
            case GameCatalog.CAT_VOUCHER:
                return "🎟️";
            case DbHelper.CAT_TRANSFER:
                return "💸";
            case DbHelper.CAT_BANK:
                return "🏦";
            case DbHelper.CAT_REQUEST:
                return "📥";
            case DbHelper.CAT_TOPUP:
                return "➕";
            case DbHelper.CAT_BONUS:
                return "🎁";
            default:
                return "🧾";
        }
    }

    /** Mengisi satu baris transaksi (dipakai di Beranda dan Riwayat). */
    public static void bindTxRow(Context c, View row, DbHelper.Tx t) {
        ((TextView) row.findViewById(R.id.tvTxIcon)).setText(categoryIcon(t.category));
        ((TextView) row.findViewById(R.id.tvTxTitle)).setText(t.title);

        String detail = (t.detail == null || t.detail.isEmpty()) ? "" : t.detail + "\n";
        ((TextView) row.findViewById(R.id.tvTxDetail)).setText(detail + formatTime(t.time));

        TextView tvAmount = row.findViewById(R.id.tvTxAmount);
        tvAmount.setText(signedRupiah(t.amount));
        tvAmount.setTextColor(ContextCompat.getColor(c,
                t.amount > 0 ? R.color.success : R.color.danger));
    }

    // ---------- Komponen bantu ----------
    /** Kolom PIN 6 digit yang disamarkan. */
    public static EditText pinField(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        return e;
    }

    /** Dialog "Memproses..." singkat supaya terasa seperti transaksi sungguhan. */
    public static void showProcessing(Activity a, String message, Runnable onDone) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        int p = dp(a, 24);
        box.setPadding(p, p, p, p);

        ProgressBar bar = new ProgressBar(a);
        TextView tv = new TextView(a);
        tv.setText(message);
        tv.setTextSize(15);
        tv.setPadding(dp(a, 16), 0, 0, 0);
        box.addView(bar);
        box.addView(tv);

        final AlertDialog dialog = new AlertDialog.Builder(a)
                .setView(box)
                .setCancelable(false)
                .create();
        dialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (a.isFinishing() || a.isDestroyed()) return;
            dialog.dismiss();
            onDone.run();
        }, 1400);
    }

    /** Menyamarkan nomor rekening, mis. 123••••7890. */
    public static String maskAccount(String number) {
        if (number == null || number.length() <= 6) return number == null ? "" : number;
        StringBuilder sb = new StringBuilder(number.substring(0, 3));
        for (int i = 0; i < number.length() - 7; i++) sb.append('•');
        sb.append(number.substring(number.length() - 4));
        return sb.toString();
    }

    /** Deretan chip nominal cepat (mis. 10rb, 50rb) yang mengisi kolom nominal saat diketuk. */
    public static void addAmountChips(Context c, LinearLayout row, EditText target, long[] values) {
        for (long q : values) {
            TextView chip = new TextView(c);
            chip.setText((q / 1000) + "rb");
            chip.setGravity(Gravity.CENTER);
            chip.setTextSize(13);
            chip.setTypeface(chip.getTypeface(), Typeface.BOLD);
            chip.setTextColor(ContextCompat.getColor(c, R.color.textPrimary));
            chip.setBackgroundResource(R.drawable.bg_nominal);
            chip.setPadding(0, dp(c, 10), 0, dp(c, 10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(c, 4), 0, dp(c, 4), 0);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> target.setText(String.valueOf(q)));
            row.addView(chip);
        }
    }
}
