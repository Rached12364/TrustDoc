package com.trustdoc.docs;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Empreinte SHA-256 : si un seul octet du fichier change, l'empreinte change complètement. */
public final class Hashing {

    private Hashing() {}

    public static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /** Comparaison en temps constant (pour ne rien révéler par le temps de réponse). */
    public static boolean same(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }
}
