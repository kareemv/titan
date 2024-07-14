package com.github.kareemv.titan.wallet;

import com.github.kareemv.titan.wallet.exception.BalanceUpdateException;
import java.math.BigDecimal;

public interface Wallet {
  String getName();

  void setName(String name);

  String getAddress();

  WalletType getWalletType();

  BigDecimal getDisplayBalance();

  String getDisplayBalanceCurrency();

  void updateBalance() throws BalanceUpdateException;
}
