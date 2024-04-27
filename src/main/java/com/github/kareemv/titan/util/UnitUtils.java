package com.github.kareemv.titan.util;

import java.math.BigDecimal;

public class UnitUtils {
    private static final BigDecimal LAMPORTS_PER_SOL = BigDecimal.valueOf(1_000_000_000L);

    public static long convertSolToLamports(BigDecimal solAmount) {
        return solAmount.multiply(LAMPORTS_PER_SOL).longValue();
    }

    public static BigDecimal convertLamportsToSol(long lamportsAmount) {
        return BigDecimal.valueOf(lamportsAmount).divide(LAMPORTS_PER_SOL);
    }
}