package com.github.kareemv.titan.wallet.exception;

public class WalletException extends Exception {
  public WalletException(String message) {
    super(message);
  }

  public WalletException(String message, Throwable cause) {
    super(message, cause);
  }
}
