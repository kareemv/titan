package com.github.kareemv.titan.wallet.solana.encryption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.sol4k.Keypair;

public class SolanaWalletEncryptor {

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int ITERATION_COUNT = 65536;
  private static final int KEY_LENGTH = 256;
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 16;

  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private SolanaWalletEncryptor() {
    throw new IllegalStateException("Utility class cannot be instantiated");
  }

  public static void encryptWalletToFile(Keypair wallet, String password, String fileName)
      throws Exception {
    byte[] walletBytes = wallet.getSecret();

    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);

    byte[] iv = new byte[GCM_IV_LENGTH];
    random.nextBytes(iv);

    SecretKey key = generateKey(password, salt);

    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
    cipher.init(Cipher.ENCRYPT_MODE, key, spec);
    byte[] encryptedWallet = cipher.doFinal(walletBytes);

    String encodedSalt = Base64.getEncoder().encodeToString(salt);
    String encodedIv = Base64.getEncoder().encodeToString(iv);
    String encodedWallet = Base64.getEncoder().encodeToString(encryptedWallet);

    EncryptedWalletData data = new EncryptedWalletData(encodedSalt, encodedIv, encodedWallet);

    try (FileWriter writer = new FileWriter(fileName)) {
      gson.toJson(data, writer);
    }
  }

  private static SecretKey generateKey(String password, byte[] salt) throws Exception {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
    SecretKey tmp = factory.generateSecret(spec);
    return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
  }

  public static Keypair decryptWalletFromFile(String password, String filePath) throws Exception {
    EncryptedWalletData data;
    try (FileReader reader = new FileReader(filePath)) {
      data = gson.fromJson(reader, EncryptedWalletData.class);
    }

    byte[] salt = Base64.getDecoder().decode(data.salt);
    byte[] iv = Base64.getDecoder().decode(data.iv);
    byte[] encryptedWallet = Base64.getDecoder().decode(data.encryptedWallet);

    SecretKey key = generateKey(password, salt);

    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
    cipher.init(Cipher.DECRYPT_MODE, key, spec);
    byte[] decryptedWallet = cipher.doFinal(encryptedWallet);

    return Keypair.fromSecretKey(decryptedWallet);
  }

  private static class EncryptedWalletData {
    String salt;
    String iv;
    String encryptedWallet;

    EncryptedWalletData(String salt, String iv, String encryptedWallet) {
      this.salt = salt;
      this.iv = iv;
      this.encryptedWallet = encryptedWallet;
    }
  }
}
