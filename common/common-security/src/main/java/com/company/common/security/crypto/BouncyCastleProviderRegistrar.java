package com.company.common.security.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public final class BouncyCastleProviderRegistrar {

    public static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;

    static {
        if (Security.getProvider(PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private BouncyCastleProviderRegistrar() {
    }

    public static void ensureRegistered() {
        // trigger static initialization
    }
}
