package com.company.common.security.crypto;

import org.bouncycastle.crypto.digests.SM3Digest;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class Sm3PasswordEncoder {

    public static final String PASSWORD_ALGO_PLAIN = "PLAIN";
    public static final String PASSWORD_ALGO_SM3 = "SM3";

    private final SecureRandom secureRandom = new SecureRandom();

    public String encode(String rawPassword, String salt) {
        if (rawPassword == null) {
            return null;
        }
        byte[] saltBytes = salt == null ? new byte[0] : salt.getBytes(StandardCharsets.UTF_8);
        byte[] rawBytes = rawPassword.getBytes(StandardCharsets.UTF_8);
        byte[] source = new byte[saltBytes.length + rawBytes.length];
        System.arraycopy(saltBytes, 0, source, 0, saltBytes.length);
        System.arraycopy(rawBytes, 0, source, saltBytes.length, rawBytes.length);

        SM3Digest digest = new SM3Digest();
        digest.update(source, 0, source.length);
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return toHex(result);
    }

    public String generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return toHex(salt);
    }

    public boolean matchesPlain(String rawPassword, String encodedPassword) {
        return rawPassword != null && rawPassword.equals(encodedPassword);
    }

    public boolean matchesSm3(String rawPassword, String salt, String encodedPassword) {
        return rawPassword != null
            && encodedPassword != null
            && encodedPassword.equalsIgnoreCase(encode(rawPassword, salt));
    }

    private String toHex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte item : value) {
            builder.append(String.format("%02x", item));
        }
        return builder.toString();
    }
}
