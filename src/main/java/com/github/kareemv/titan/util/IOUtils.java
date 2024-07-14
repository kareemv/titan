package com.github.kareemv.titan.util;

import com.github.kareemv.titan.Titan;
import com.github.kareemv.titan.wallet.ethereum.EthereumWallet;
import com.github.kareemv.titan.wallet.exception.WalletException;
import com.github.kareemv.titan.wallet.solana.SolanaWallet;
import java.io.File;

public class IOUtils {
  public static final File USER_DATA_DIRECTORY =
      new File(System.getProperty("user.home") + File.separator + ".titan");
  public static final File WALLETS_DIRECTORY =
      new File(USER_DATA_DIRECTORY + File.separator + "wallets");
  public static final File ETH_WALLETS_DIRECTORY =
      new File(WALLETS_DIRECTORY + File.separator + "ethereum");
  public static final File SOL_WALLETS_DIRECTORY =
      new File(WALLETS_DIRECTORY + File.separator + "solana");

  private IOUtils() {
    throw new IllegalStateException("Utility class cannot be instantiated");
  }

  public static void loadWallets() {
    File ethWalletDir = IOUtils.ETH_WALLETS_DIRECTORY;
    if (ethWalletDir.exists() && ethWalletDir.isDirectory()) {
      for (File file : ethWalletDir.listFiles()) {
        if (file.isFile() && file.getName().endsWith(".json")) {
          try {
            EthereumWallet wallet = EthereumWallet.loadFromFile(file.getName());
            Titan.INSTANCE.ethereumWallets.add(wallet);
          } catch (WalletException e) {
            e.printStackTrace();
          }
        }
      }
    }

    // Load Solana wallets
    File solWalletDir = IOUtils.SOL_WALLETS_DIRECTORY;
    if (solWalletDir.exists() && solWalletDir.isDirectory()) {
      for (File file : solWalletDir.listFiles()) {
        if (file.isFile() && file.getName().endsWith(".json")) {
          try {
            SolanaWallet wallet = SolanaWallet.loadFromFile(file.getName());
            Titan.INSTANCE.solanaWallets.add(wallet);
          } catch (WalletException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  public static void createUserDataDirectories() {
    if (!USER_DATA_DIRECTORY.exists()) {
      USER_DATA_DIRECTORY.mkdir();
    }
    if (!WALLETS_DIRECTORY.exists()) {
      WALLETS_DIRECTORY.mkdir();
    }
    if (!ETH_WALLETS_DIRECTORY.exists()) {
      ETH_WALLETS_DIRECTORY.mkdir();
    }
    if (!SOL_WALLETS_DIRECTORY.exists()) {
      SOL_WALLETS_DIRECTORY.mkdir();
    }
  }

  public static void clearUserData() throws Exception {
    File[] files = USER_DATA_DIRECTORY.listFiles();
    for (File file : files) {
      if (!file.delete()) {
        throw new Exception("Failed to delete file: " + file.getAbsolutePath());
      }
    }
  }
}
