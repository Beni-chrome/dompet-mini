package com.beni.dompetmini;

import android.text.InputType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Katalog produk simulasi: game, pulsa, dan voucher (harga hanya contoh). */
public final class GameCatalog {

    public static final String CAT_GAME = "GAME";
    public static final String CAT_PULSA = "PULSA";
    public static final String CAT_VOUCHER = "VOUCHER";

    public static class Nominal {
        public final String label;
        public final long price;

        Nominal(String label, long price) {
            this.label = label;
            this.price = price;
        }
    }

    public static class Game {
        public final String name;
        public final String logoName; // nama file di res/drawable (tanpa ekstensi)
        public final String emoji;
        public final String currency;
        public final int color;
        public final String category;
        public final String inputLabel;
        public final String inputHint;
        public final int inputType;
        public final List<Nominal> nominals;

        Game(String name, String logoName, String emoji, String currency, String colorHex,
             String category, String inputLabel, String inputHint, int inputType,
             List<Nominal> nominals) {
            this.name = name;
            this.logoName = logoName;
            this.emoji = emoji;
            this.currency = currency;
            this.color = Utils.parse(colorHex);
            this.category = category;
            this.inputLabel = inputLabel;
            this.inputHint = inputHint;
            this.inputType = inputType;
            this.nominals = nominals;
        }

        public long minPrice() {
            long min = Long.MAX_VALUE;
            for (Nominal n : nominals) min = Math.min(min, n.price);
            return min == Long.MAX_VALUE ? 0 : min;
        }

        public String tagline() {
            switch (category) {
                case CAT_PULSA:
                    return "Pulsa & paket data • Proses instan";
                case CAT_VOUCHER:
                    return "Voucher digital • Dikirim ke email";
                default:
                    return "Top-up " + currency + " • Proses instan";
            }
        }

        public String txTitle() {
            switch (category) {
                case CAT_PULSA:
                    return "Pulsa " + name;
                case CAT_VOUCHER:
                    return "Voucher " + name;
                default:
                    return "Top-Up " + name;
            }
        }
    }

    /** Semua produk (game, pulsa, voucher) dalam satu daftar. */
    public static final List<Game> GAMES = new ArrayList<>();

    private static final int TYPE_ID = InputType.TYPE_CLASS_NUMBER;
    private static final int TYPE_PHONE = InputType.TYPE_CLASS_PHONE;
    private static final int TYPE_EMAIL =
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;

    static {
        // ---------- GAME ----------
        GAMES.add(new Game("Mobile Legends", "logo_mlbb", "⚔️", "Diamonds", "#2F6BFF",
                CAT_GAME, "ID Pemain", "Contoh: 123456789", TYPE_ID, Arrays.asList(
                n("86 Diamonds", 22000), n("172 Diamonds", 44000), n("257 Diamonds", 66000),
                n("344 Diamonds", 88000), n("514 Diamonds", 132000), n("706 Diamonds", 176000))));

        GAMES.add(new Game("Free Fire", "logo_freefire", "🔥", "Diamonds", "#FF6D00",
                CAT_GAME, "ID Pemain", "Contoh: 123456789", TYPE_ID, Arrays.asList(
                n("70 Diamonds", 10000), n("140 Diamonds", 20000), n("355 Diamonds", 50000),
                n("720 Diamonds", 100000), n("1450 Diamonds", 200000), n("2180 Diamonds", 300000))));

        GAMES.add(new Game("PUBG Mobile", "logo_pubg", "🎯", "UC", "#F9A825",
                CAT_GAME, "ID Pemain", "Contoh: 5123456789", TYPE_ID, Arrays.asList(
                n("60 UC", 15000), n("325 UC", 75000), n("660 UC", 150000),
                n("1800 UC", 400000), n("3850 UC", 800000), n("8100 UC", 1600000))));

        GAMES.add(new Game("Genshin Impact", "logo_genshin", "✨", "Genesis Crystals", "#7E57C2",
                CAT_GAME, "UID", "Contoh: 800123456", TYPE_ID, Arrays.asList(
                n("60 Genesis Crystals", 16000), n("330 Genesis Crystals", 79000),
                n("1090 Genesis Crystals", 249000), n("2240 Genesis Crystals", 479000),
                n("3880 Genesis Crystals", 799000), n("8080 Genesis Crystals", 1599000))));

        GAMES.add(new Game("Valorant", "logo_valorant", "🗡️", "VP", "#E5484D",
                CAT_GAME, "Riot ID / ID Akun", "Contoh: 12345678", TYPE_ID, Arrays.asList(
                n("475 VP", 50000), n("1000 VP", 100000), n("2050 VP", 200000),
                n("3650 VP", 350000), n("5350 VP", 500000), n("11000 VP", 1000000))));

        GAMES.add(new Game("Roblox", "logo_roblox", "🧱", "Robux", "#00B35A",
                CAT_GAME, "ID Pengguna", "Contoh: 1234567890", TYPE_ID, Arrays.asList(
                n("80 Robux", 15000), n("400 Robux", 75000), n("800 Robux", 150000),
                n("1700 Robux", 300000), n("4500 Robux", 750000), n("10000 Robux", 1600000))));

        // ---------- PULSA ----------
        GAMES.add(new Game("Telkomsel", "logo_telkomsel", "📶", "Pulsa", "#E5484D",
                CAT_PULSA, "Nomor HP", "Contoh: 081234567890", TYPE_PHONE, pulsa()));
        GAMES.add(new Game("Indosat", "logo_indosat", "📡", "Pulsa", "#F9A825",
                CAT_PULSA, "Nomor HP", "Contoh: 081234567890", TYPE_PHONE, pulsa()));
        GAMES.add(new Game("XL Axiata", "logo_xl", "📲", "Pulsa", "#2F6BFF",
                CAT_PULSA, "Nomor HP", "Contoh: 081234567890", TYPE_PHONE, pulsa()));

        // ---------- VOUCHER ----------
        GAMES.add(new Game("Google Play", "logo_googleplay", "🛒", "Voucher", "#00B35A",
                CAT_VOUCHER, "Email Penerima", "nama@email.com", TYPE_EMAIL, Arrays.asList(
                n("Voucher Rp 20.000", 21500), n("Voucher Rp 50.000", 52500),
                n("Voucher Rp 100.000", 102500), n("Voucher Rp 150.000", 152500),
                n("Voucher Rp 300.000", 302500), n("Voucher Rp 500.000", 502500))));

        GAMES.add(new Game("Steam Wallet", "logo_steam", "🎲", "Voucher", "#37474F",
                CAT_VOUCHER, "Email Penerima", "nama@email.com", TYPE_EMAIL, Arrays.asList(
                n("Steam Rp 12.000", 13500), n("Steam Rp 45.000", 46500),
                n("Steam Rp 60.000", 61500), n("Steam Rp 90.000", 91500),
                n("Steam Rp 120.000", 121500), n("Steam Rp 250.000", 251500))));

        GAMES.add(new Game("Spotify", "logo_spotify", "🎧", "Voucher", "#1DB954",
                CAT_VOUCHER, "Email Penerima", "nama@email.com", TYPE_EMAIL, Arrays.asList(
                n("Premium 1 Bulan", 54990), n("Premium 3 Bulan", 159000),
                n("Premium 6 Bulan", 299000), n("Premium 12 Bulan", 549000))));
    }

    private static List<Nominal> pulsa() {
        return Arrays.asList(
                n("Pulsa 10.000", 11500), n("Pulsa 20.000", 21500), n("Pulsa 50.000", 51500),
                n("Pulsa 100.000", 101500), n("Pulsa 200.000", 201500), n("Pulsa 500.000", 501500));
    }

    private static Nominal n(String label, long price) {
        return new Nominal(label, price);
    }

    public static List<Game> byCategory(String category) {
        List<Game> list = new ArrayList<>();
        for (Game g : GAMES) {
            if (g.category.equals(category)) list.add(g);
        }
        return list;
    }

    private GameCatalog() {}
}
