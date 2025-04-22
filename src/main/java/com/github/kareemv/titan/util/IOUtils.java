package com.github.kareemv.titan.util;

import com.github.kareemv.titan.Titan;
import com.github.kareemv.titan.wallet.ethereum.EthereumWallet;
import com.github.kareemv.titan.wallet.exception.IncorrectPasswordException;
import com.github.kareemv.titan.wallet.solana.SolanaWallet;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

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

  public static void loadWallets(String password) throws IncorrectPasswordException {
    ExecutorService executor = Executors.newCachedThreadPool();
    List<Future<EthereumWallet>> ethFutures = new ArrayList<>();
    List<Future<SolanaWallet>> solFutures = new ArrayList<>();

    File ethWalletDir = IOUtils.ETH_WALLETS_DIRECTORY;
    if (ethWalletDir.exists() && ethWalletDir.isDirectory()) {
      for (File file : Objects.requireNonNull(ethWalletDir.listFiles())) {
        if (file.isFile() && file.getName().endsWith(".json")) {
          final String fileName = file.getName();
          Callable<EthereumWallet> task = () -> EthereumWallet.loadFromFile(fileName, password);
          ethFutures.add(executor.submit(task));
        }
      }
    }

    File solWalletDir = IOUtils.SOL_WALLETS_DIRECTORY;
    if (solWalletDir.exists() && solWalletDir.isDirectory()) {
      for (File file : Objects.requireNonNull(solWalletDir.listFiles())) {
        if (file.isFile() && file.getName().endsWith(".json")) {
          final String fileName = file.getName();
          Callable<SolanaWallet> task = () -> SolanaWallet.loadFromFile(fileName, password);
          solFutures.add(executor.submit(task));
        }
      }
    }

    executor.shutdown();

    List<EthereumWallet> loadedEthWallets = new ArrayList<>();
    List<SolanaWallet> loadedSolWallets = new ArrayList<>();

    try {
      for (Future<EthereumWallet> future : ethFutures) {
        loadedEthWallets.add(future.get());
      }
      for (Future<SolanaWallet> future : solFutures) {
        loadedSolWallets.add(future.get());
      }

      Titan.INSTANCE.ethereumWallets.clear();
      Titan.INSTANCE.solanaWallets.clear();
      Titan.INSTANCE.ethereumWallets.addAll(loadedEthWallets);
      Titan.INSTANCE.solanaWallets.addAll(loadedSolWallets);

    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      throw new IncorrectPasswordException("Failed to load one or more wallets. Incorrect password?", cause);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IncorrectPasswordException("Wallet loading was interrupted.", e);
    } finally {
        if (!executor.isTerminated()) {
            executor.shutdownNow();
        }
    }
  }

  public static boolean hasExistingWallets() {
    boolean ethWalletsExist = false;
    if (ETH_WALLETS_DIRECTORY.exists() && ETH_WALLETS_DIRECTORY.isDirectory()) {
      for (File file : Objects.requireNonNull(ETH_WALLETS_DIRECTORY.listFiles())) {
        if (file.isFile() && file.getName().endsWith(".json")) {
          ethWalletsExist = true;
          break;
        }
      }
    }

    boolean solWalletsExist = false;
    if (SOL_WALLETS_DIRECTORY.exists() && SOL_WALLETS_DIRECTORY.isDirectory()) {
      for (File file : Objects.requireNonNull(SOL_WALLETS_DIRECTORY.listFiles())) {
        if (file.isFile() && file.getName().endsWith(".json")) {
          solWalletsExist = true;
          break;
        }
      }
    }

    return ethWalletsExist || solWalletsExist;
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
