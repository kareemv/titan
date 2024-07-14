package com.github.kareemv.titan.wallet.exception;

public class BalanceUpdateException extends Exception {
  public BalanceUpdateException(String message) {
    super(message);
  }

  public BalanceUpdateException(String message, Throwable cause) {
    super(message, cause);
  }
}
