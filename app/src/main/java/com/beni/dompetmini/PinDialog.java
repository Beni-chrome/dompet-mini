package com.beni.dompetmini;

import android.content.DialogInterface;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/** Dialog konfirmasi PIN sebelum pembayaran / transfer. */
public final class PinDialog {

    public interface OnVerified {
        void onVerified();
    }

    private PinDialog() {}

    public static void ask(AppCompatActivity activity, DbHelper db, String message,
                           OnVerified callback) {
        final EditText et = Utils.pinField(activity, "••••••");
        et.setGravity(Gravity.CENTER);
        et.setTextSize(24);

        FrameLayout box = new FrameLayout(activity);
        int p = Utils.dp(activity, 24);
        box.setPadding(p, Utils.dp(activity, 8), p, 0);
        box.addView(et);

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Masukkan PIN")
                .setMessage(message)
                .setView(box)
                .setPositiveButton("Konfirmasi", null)
                .setNegativeButton("Batal", null)
                .create();

        // Tombol dioverride supaya dialog tetap terbuka jika PIN salah.
        dialog.setOnShowListener(x ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String pin = et.getText().toString();
                    if (pin.length() != 6) {
                        et.setError("PIN harus 6 digit");
                        return;
                    }
                    if (db.verifyPin(pin)) {
                        dialog.dismiss();
                        callback.onVerified();
                    } else {
                        et.setText("");
                        et.setError("PIN salah");
                    }
                }));
        dialog.show();
    }
}
