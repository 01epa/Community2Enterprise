package com.intellij.ultimate;

import java.math.BigInteger;

public final class UltimateVerifier {
    public volatile BigInteger result = null;

    public UltimateVerifier() {
    }

    public static UltimateVerifier getInstance() {
        return new UltimateVerifier();
    }

    public String verify(BigInteger key) {
        result = key;
        return "";
    }

    public BigInteger checkResult(String verificationResult, BigInteger exp, BigInteger mod) throws Exception {
        if (result == null) {
            throw new RuntimeException("No verify() function called before");
        }
        return result;
    }
}
