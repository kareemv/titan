package com.github.kareemv.titan;

import com.formdev.flatlaf.util.SystemInfo;
import com.github.kareemv.titan.ui.MainWindow;
import com.github.kareemv.titan.ui.WelcomeDialog;
import com.github.kareemv.titan.ui.theme.GeistLaf;
import com.github.kareemv.titan.util.IOUtils;
import com.github.kareemv.titan.wallet.Wallet;
import com.github.kareemv.titan.wallet.ethereum.EthereumWallet;
import com.github.kareemv.titan.wallet.solana.SolanaWallet;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.awt.Window;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.sol4k.Connection;
import org.sol4k.RpcUrl;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

public enum Titan {
  INSTANCE;

  public final OkHttpClient okHttpClient = new OkHttpClient();
  private final ExecutorService executorService = Executors.newCachedThreadPool();
  public Web3j ethereumClient = Web3j.build(new HttpService("https://ethereum-rpc.publicnode.com"));
  public Connection solanaClient = new Connection(RpcUrl.MAINNNET);
  public String password; // set after successful decryption or creation
  public List<EthereumWallet> ethereumWallets = new ArrayList<>();
  public List<SolanaWallet> solanaWallets = new ArrayList<>();
  public MainWindow mainWindow;
  public BigDecimal ethUsdPrice = BigDecimal.ZERO;
  public BigDecimal solUsdPrice = BigDecimal.ZERO;
  private ScheduledExecutorService balanceUpdateScheduler;

  public void start() {
    if (SystemInfo.isMacOS) {
      System.setProperty("apple.awt.application.name", "Titan");
      System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
    }

    GeistLaf.setup();
    GeistLaf.setupFont();

    IOUtils.createUserDataDirectories();

    // create hidden frame to be owner of WelcomeDialog for Windows taskbar icon
    JFrame ownerFrame = new JFrame();
    ownerFrame.setUndecorated(true);
    ownerFrame.setSize(1, 1);
    ownerFrame.setType(Window.Type.NORMAL);
    ownerFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    ownerFrame.setVisible(true);
    ownerFrame.setLocation(-100, -100);

    WelcomeDialog welcomeDialog = new WelcomeDialog(ownerFrame);
    welcomeDialog.setVisible(true);

    ownerFrame.dispose();

    if (!welcomeDialog.isSuccessful()) {
      System.exit(0);
      return;
    }
    this.password = welcomeDialog.getPassword();
    if (this.password == null) {
      System.err.println("Password handling failed unexpectedly.");
      System.exit(1);
      return;
    }

    executorService.submit(this::updateEthPrice);
    executorService.submit(this::updateSolPrice);

    mainWindow = new MainWindow();

    startBalanceUpdater();
  }

  private void startBalanceUpdater() {
    if (balanceUpdateScheduler != null) {
      balanceUpdateScheduler.shutdown();
    }
    balanceUpdateScheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = Executors.defaultThreadFactory().newThread(r);
              t.setDaemon(true);
              return t;
            });

    Runnable refreshTask =
        () -> {
          List<Wallet> allWallets = new ArrayList<>();
          synchronized (ethereumWallets) {
            allWallets.addAll(new ArrayList<>(ethereumWallets));
          }
          synchronized (solanaWallets) {
            allWallets.addAll(new ArrayList<>(solanaWallets));
          }

          for (Wallet wallet : allWallets) {
            try {
              wallet.updateBalance();

              SwingUtilities.invokeLater(
                  () -> {
                    if (mainWindow != null && MainWindow.walletView != null) {
                      Wallet currentlyDisplayed = MainWindow.walletView.getCurrentWallet();
                      if (wallet.equals(currentlyDisplayed)) {
                        MainWindow.walletView.displayWalletDetails(wallet);
                      }
                    }
                  });
            } catch (Exception e) {
              System.err.println(
                  "Error updating balance for wallet " + wallet.getName() + ": " + e.getMessage());
            }
          }
        };

    balanceUpdateScheduler.scheduleAtFixedRate(refreshTask, 0, 5, TimeUnit.SECONDS);
  }

  public void updateEthPrice() {
    String apiUrl = "https://api.coinbase.com/v2/exchange-rates?currency=ETH";
    Request request = new Request.Builder().url(apiUrl).build();

    try (Response response = okHttpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) return;

      String responseBody = response.body().string();
      JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
      ethUsdPrice =
          jsonObject.getAsJsonObject("data").getAsJsonObject("rates").get("USD").getAsBigDecimal();
    } catch (IOException e) {
      System.err.println("Unable to fetch ETH price: " + e.getMessage());
    } catch (JsonSyntaxException e) {
      System.err.println("Invalid JSON response for ETH price");
    }
  }

  public void updateSolPrice() {
    String apiUrl = "https://api.coinbase.com/v2/exchange-rates?currency=SOL";
    Request request = new Request.Builder().url(apiUrl).build();

    try (Response response = okHttpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) return;

      String responseBody = response.body().string();
      JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
      solUsdPrice =
          jsonObject.getAsJsonObject("data").getAsJsonObject("rates").get("USD").getAsBigDecimal();
    } catch (IOException e) {
      System.err.println("Unable to fetch SOL price: " + e.getMessage());
    } catch (JsonSyntaxException e) {
      System.err.println("Invalid JSON response for SOL price");
    }
  }
}
