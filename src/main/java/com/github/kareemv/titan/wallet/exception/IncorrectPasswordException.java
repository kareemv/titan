package com.github.kareemv.titan.wallet.exception;

public class IncorrectPasswordException extends Exception {
  public IncorrectPasswordException(String message) {
    super(message);
  }

  public IncorrectPasswordException(String message, Throwable cause) {
    super(message, cause);
  }
} 