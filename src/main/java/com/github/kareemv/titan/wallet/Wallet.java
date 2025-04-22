package com.github.kareemv.titan.wallet;

import com.github.kareemv.titan.wallet.exception.BalanceUpdateException;
import com.github.kareemv.titan.wallet.exception.TransactionException;
import java.math.BigDecimal;

public interface Wallet {
  String getName();

  void setName(String name);

  String getAddress();

  WalletType getWalletType();

  BigDecimal getDisplayBalance();

  String getDisplayBalanceCurrency();

  BigDecimal getDisplayBalanceUSD();

  void updateBalance() throws BalanceUpdateException;

  String sendFundsTo(String recipientAddress, BigDecimal amount) throws TransactionException;
}
