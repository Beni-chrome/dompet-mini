package com.beni.dompetmini;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Database SQLite untuk data yang banyak & terstruktur:
 * akun pengguna (PIN di-hash), saldo, riwayat transaksi, rekening bank, dan permintaan dana.
 * Saldo = jumlah semua nominal transaksi (masuk positif, keluar negatif).
 */
public class DbHelper extends SQLiteOpenHelper {

    public static final String CAT_TRANSFER = "TRANSFER";
    public static final String CAT_BANK = "BANK";
    public static final String CAT_REQUEST = "REQUEST";
    public static final String CAT_TOPUP = "TOPUP";
    public static final String CAT_BONUS = "BONUS";

    public static final String REQ_PENDING = "MENUNGGU";
    public static final String REQ_PAID = "LUNAS";
    public static final String REQ_CANCELLED = "DIBATALKAN";

    private static final String DB_NAME = "dompet_mini.db";
    private static final int DB_VERSION = 3;
    private static final String T_TX = "transaksi";
    private static final String T_USER = "pengguna";
    private static final String T_BANK = "rekening";
    private static final String T_REQ = "permintaan";

    public static class Tx {
        public long id;
        public String category;
        public String title;
        public String detail;
        public String target;
        public long amount;
        public long time;
    }

    public static class User {
        public String name;
        public String phone;
        public long created;
    }

    public static class Bank {
        public long id;
        public String bank;
        public String number;
        public String holder;
    }

    public static class Req {
        public long id;
        public long amount;
        public String note;
        public String status;
        public long created;
    }

    public DbHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_USER + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "phone TEXT NOT NULL, "
                + "salt TEXT NOT NULL, "
                + "pin_hash TEXT NOT NULL, "
                + "created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE " + T_TX + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "category TEXT NOT NULL, "
                + "title TEXT NOT NULL, "
                + "detail TEXT, "
                + "target TEXT, "
                + "amount INTEGER NOT NULL, "
                + "time INTEGER NOT NULL)");
        createExtraTables(db);
    }

    private void createExtraTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_BANK + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "bank TEXT NOT NULL, "
                + "number TEXT NOT NULL, "
                + "holder TEXT NOT NULL, "
                + "created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_REQ + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "amount INTEGER NOT NULL, "
                + "note TEXT, "
                + "status TEXT NOT NULL, "
                + "created INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS " + T_TX);
            db.execSQL("DROP TABLE IF EXISTS " + T_USER);
            onCreate(db);
            return;
        }
        // v2 -> v3: tambah tabel rekening & permintaan, data lama tetap aman
        if (oldVersion < 3) {
            createExtraTables(db);
        }
    }

    // ===================== AKUN =====================

    public boolean hasUser() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + T_USER, null);
        boolean has = c.moveToFirst() && c.getLong(0) > 0;
        c.close();
        return has;
    }

    public User getUser() {
        User u = null;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT name, phone, created FROM " + T_USER + " LIMIT 1", null);
        if (c.moveToFirst()) {
            u = new User();
            u.name = c.getString(0);
            u.phone = c.getString(1);
            u.created = c.getLong(2);
        }
        c.close();
        return u;
    }

    public void createUser(String name, String phone, String pin) {
        String salt = PinHasher.newSalt();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phone", phone);
        cv.put("salt", salt);
        cv.put("pin_hash", PinHasher.hash(pin, salt));
        cv.put("created", System.currentTimeMillis());
        getWritableDatabase().insert(T_USER, null, cv);

        addTransaction(CAT_BONUS, "Bonus pengguna baru", "Saldo awal pendaftaran", "", 100000);
    }

    public boolean verifyPin(String pin) {
        boolean ok = false;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT salt, pin_hash FROM " + T_USER + " LIMIT 1", null);
        if (c.moveToFirst()) {
            String computed = PinHasher.hash(pin, c.getString(0));
            ok = MessageDigest.isEqual(computed.getBytes(), c.getString(1).getBytes());
        }
        c.close();
        return ok;
    }

    public boolean changePin(String oldPin, String newPin) {
        if (!verifyPin(oldPin)) return false;
        String salt = PinHasher.newSalt();
        ContentValues cv = new ContentValues();
        cv.put("salt", salt);
        cv.put("pin_hash", PinHasher.hash(newPin, salt));
        getWritableDatabase().update(T_USER, cv, null, null);
        return true;
    }

    /** Menghapus seluruh database (dipakai untuk "Lupa PIN / reset akun"). */
    public static void wipe(Context context, DbHelper db) {
        db.close();
        context.deleteDatabase(DB_NAME);
    }

    // ===================== TRANSAKSI =====================

    public long addTransaction(String category, String title, String detail,
                               String target, long amount) {
        ContentValues cv = new ContentValues();
        cv.put("category", category);
        cv.put("title", title);
        cv.put("detail", detail);
        cv.put("target", target);
        cv.put("amount", amount);
        cv.put("time", System.currentTimeMillis());
        return getWritableDatabase().insert(T_TX, null, cv);
    }

    public long getBalance() {
        long total = 0;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount), 0) FROM " + T_TX, null);
        if (c.moveToFirst()) {
            total = c.getLong(0);
        }
        c.close();
        return total;
    }

    public List<Tx> getAll(int limit) {
        List<Tx> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, category, title, detail, target, amount, time FROM " + T_TX
                        + " ORDER BY time DESC, id DESC LIMIT " + limit, null);
        while (c.moveToNext()) {
            list.add(readTx(c));
        }
        c.close();
        return list;
    }

    public Tx getTx(long id) {
        Tx t = null;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, category, title, detail, target, amount, time FROM " + T_TX
                        + " WHERE id = ?", new String[]{String.valueOf(id)});
        if (c.moveToFirst()) {
            t = readTx(c);
        }
        c.close();
        return t;
    }

    private Tx readTx(Cursor c) {
        Tx t = new Tx();
        t.id = c.getLong(0);
        t.category = c.getString(1);
        t.title = c.getString(2);
        t.detail = c.getString(3);
        t.target = c.getString(4);
        t.amount = c.getLong(5);
        t.time = c.getLong(6);
        return t;
    }

    // ===================== REKENING BANK =====================

    public long addBank(String bank, String number, String holder) {
        ContentValues cv = new ContentValues();
        cv.put("bank", bank);
        cv.put("number", number);
        cv.put("holder", holder);
        cv.put("created", System.currentTimeMillis());
        return getWritableDatabase().insert(T_BANK, null, cv);
    }

    public boolean hasBank(String bank, String number) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + T_BANK + " WHERE bank = ? AND number = ?",
                new String[]{bank, number});
        boolean has = c.moveToFirst() && c.getLong(0) > 0;
        c.close();
        return has;
    }

    public List<Bank> getBanks() {
        List<Bank> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, bank, number, holder FROM " + T_BANK + " ORDER BY id ASC", null);
        while (c.moveToNext()) {
            Bank b = new Bank();
            b.id = c.getLong(0);
            b.bank = c.getString(1);
            b.number = c.getString(2);
            b.holder = c.getString(3);
            list.add(b);
        }
        c.close();
        return list;
    }

    public void deleteBank(long id) {
        getWritableDatabase().delete(T_BANK, "id = ?", new String[]{String.valueOf(id)});
    }

    // ===================== PERMINTAAN DANA =====================

    public long addRequest(long amount, String note) {
        ContentValues cv = new ContentValues();
        cv.put("amount", amount);
        cv.put("note", note == null ? "" : note);
        cv.put("status", REQ_PENDING);
        cv.put("created", System.currentTimeMillis());
        return getWritableDatabase().insert(T_REQ, null, cv);
    }

    public List<Req> getRequests(int limit) {
        List<Req> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, amount, note, status, created FROM " + T_REQ
                        + " ORDER BY created DESC, id DESC LIMIT " + limit, null);
        while (c.moveToNext()) {
            Req r = new Req();
            r.id = c.getLong(0);
            r.amount = c.getLong(1);
            r.note = c.getString(2) == null ? "" : c.getString(2);
            r.status = c.getString(3);
            r.created = c.getLong(4);
            list.add(r);
        }
        c.close();
        return list;
    }

    public void setRequestStatus(long id, String status) {
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        getWritableDatabase().update(T_REQ, cv, "id = ?", new String[]{String.valueOf(id)});
    }
}
