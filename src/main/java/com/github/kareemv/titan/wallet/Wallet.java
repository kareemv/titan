package com.github.kareemv.titan.wallet;

import java.math.BigDecimal;

public interface Wallet {
    String getName();
    void setName(String name);
    String getAddress();
    WalletType getWalletType();
    BigDecimal getDisplayBalance();
}
