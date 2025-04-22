package com.github.kareemv.titan;

import com.formdev.flatlaf.util.SystemInfo;
import com.github.kareemv.titan.ui.MainWindow;
import com.github.kareemv.titan.ui.theme.GeistLaf;
import com.github.kareemv.titan.util.IOUtils;
import com.github.kareemv.titan.wallet.ethereum.EthereumWallet;
import com.github.kareemv.titan.wallet.exception.IncorrectPasswordException;
import com.github.kareemv.titan.wallet.solana.SolanaWallet;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.sol4k.Connection;
import org.sol4k.RpcUrl;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

public enum Titan {
  INSTANCE;

  public Web3j ethereumClient = Web3j.build(new HttpService("https://ethereum-rpc.publicnode.com"));
  public Connection solanaClient = new Connection(RpcUrl.MAINNNET);
  public final OkHttpClient okHttpClient = new OkHttpClient();
  private final ExecutorService executorService = Executors.newCachedThreadPool();
  public String password; // set after successful decryption or creation
  public List<EthereumWallet> ethereumWallets = new ArrayList<>();
  public List<SolanaWallet> solanaWallets = new ArrayList<>();
  public MainWindow mainWindow;
  public BigDecimal ethUsdPrice = BigDecimal.ZERO;
  public BigDecimal solUsdPrice = BigDecimal.ZERO;

  // start the application
  public void start() {
    if (SystemInfo.isMacOS) {
      System.setProperty("apple.awt.application.name", "Titan");
      System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
    }

    GeistLaf.setup();
    GeistLaf.setupFont();

    IOUtils.createUserDataDirectories();

    if (!handlePasswordAndLoadWallets()) {
      System.exit(0);
      return;
    }

    executorService.submit(this::updateEthPrice);
    executorService.submit(this::updateSolPrice);

    mainWindow = new MainWindow();
  }

  private boolean handlePasswordAndLoadWallets() {
    if (IOUtils.hasExistingWallets()) {
      return promptForExistingPassword();
    } else {
      return promptForNewPassword();
    }
  }

  private boolean promptForExistingPassword() {
    JPasswordField passwordField = new JPasswordField(20);
    JPanel panel = new JPanel(new BorderLayout(5, 5));
    panel.add(new JLabel("Enter password to decrypt wallets:"), BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);

    while (true) {
      passwordField.setText("");
      int option =
          JOptionPane.showConfirmDialog(
              null, panel, "Titan", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

      if (option == JOptionPane.OK_OPTION) {
        char[] passwordChars = passwordField.getPassword();
        String enteredPassword = new String(passwordChars);
        Arrays.fill(passwordChars, ' ');

        if (enteredPassword.isEmpty()) {
          JOptionPane.showMessageDialog(
              null, "Password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
          continue;
        }

        try {
          IOUtils.loadWallets(enteredPassword);
          this.password = enteredPassword;
          return true;
        } catch (IncorrectPasswordException e) {
          JOptionPane.showMessageDialog(
              null, "Incorrect password. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
          JOptionPane.showMessageDialog(
              null,
              "An error occurred while loading wallets: " + e.getMessage(),
              "Error",
              JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
          return false;
        }
      } else {
        return false;
      }
    }
  }

  private boolean promptForNewPassword() {
    JPasswordField passwordField = new JPasswordField(20);
    JPasswordField confirmPasswordField = new JPasswordField(20);
    JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
    panel.add(new JLabel("Create a new password for your wallets:"));
    panel.add(passwordField);
    panel.add(new JLabel("Confirm password:"));
    panel.add(confirmPasswordField);

    while (true) {
      passwordField.setText("");
      confirmPasswordField.setText("");
      int option =
          JOptionPane.showConfirmDialog(
              null, panel, "Titan", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

      if (option == JOptionPane.OK_OPTION) {
        char[] passwordChars = passwordField.getPassword();
        char[] confirmPasswordChars = confirmPasswordField.getPassword();
        String newPassword = new String(passwordChars);
        String confirmPassword = new String(confirmPasswordChars);
        Arrays.fill(passwordChars, ' ');
        Arrays.fill(confirmPasswordChars, ' ');

        if (newPassword.isEmpty()) {
          JOptionPane.showMessageDialog(
              null, "Password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
          continue;
        }

        if (!newPassword.equals(confirmPassword)) {
          JOptionPane.showMessageDialog(
              null,
              "Passwords do not match. Please try again.",
              "Error",
              JOptionPane.ERROR_MESSAGE);
          continue;
        }

        this.password = newPassword;
        return true;
      } else {
        return false;
      }
    }
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
