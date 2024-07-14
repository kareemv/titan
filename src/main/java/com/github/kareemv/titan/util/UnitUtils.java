package com.github.kareemv.titan.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public class UnitUtils {
  private static final BigDecimal LAMPORTS_PER_SOL = BigDecimal.valueOf(1_000_000_000L);

  private UnitUtils() {
    throw new IllegalStateException("Utility class cannot be instantiated");
  }

  public static long convertSolToLamports(BigDecimal solAmount) {
    return solAmount.multiply(LAMPORTS_PER_SOL).longValue();
  }

  public static BigDecimal convertLamportsToSol(BigInteger lamportsAmount) {
    return new BigDecimal(lamportsAmount).divide(LAMPORTS_PER_SOL);
  }
}
