package com.beni.dompetmini;

import android.util.Base64;

import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Menyimpan PIN dengan aman: PBKDF2 + salt acak. PIN asli tidak pernah disimpan. */
public final class PinHasher {

    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    private PinHasher() {}

    public static String newSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static String hash(String pin, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(),
                    Base64.decode(salt, Base64.NO_WRAP), ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return Base64.encodeToString(factory.generateSecret(spec).getEncoded(), Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("Gagal membuat hash PIN", e);
        }
    }
}
