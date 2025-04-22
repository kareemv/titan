package com.github.kareemv.titan.ui;

import com.github.kareemv.titan.wallet.Wallet;
import com.github.kareemv.titan.wallet.WalletType;
import com.github.kareemv.titan.wallet.exception.TransactionException;
import java.awt.*;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.math.BigDecimal;
import javax.swing.*;
import javax.swing.table.*;

public class WalletView extends JPanel {
  private JLabel walletTitleLabel;
  private JLabel totalBalanceLabel;
  private JLabel usdValueLabel;
  private JLabel blockchainLabel;
  private JLabel balanceHeaderLabel;
  private Wallet currentWallet;

  public WalletView() {
    setLayout(new BorderLayout());
    setBackground(Color.decode("#000000"));

    JPanel topPanel = createTopPanel();
    add(topPanel, BorderLayout.NORTH);

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.setBackground(Color.decode("#000000"));
    centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    JPanel walletDetailsPanel = createWalletDetailsPanel();
    centerPanel.add(walletDetailsPanel, BorderLayout.NORTH);

    add(centerPanel, BorderLayout.CENTER);
  }

  public void displayNoWalletMessage() {
    removeAll();
    JLabel noWalletMessage = new JLabel("Add a wallet to get started", SwingConstants.CENTER);
    noWalletMessage.setFont(new Font(noWalletMessage.getFont().getName(), Font.PLAIN, 24));
    noWalletMessage.setForeground(Color.GRAY);
    setLayout(new BorderLayout());
    add(noWalletMessage, BorderLayout.CENTER);
    revalidate();
    repaint();
  }

  public void displayWalletDetails(Wallet wallet) {
    this.currentWallet = wallet;
    String typeName = wallet.getWalletType().name().toLowerCase();
    typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);

    blockchainLabel.setText(typeName);
    balanceHeaderLabel.setText(
        wallet.getDisplayBalance() + " " + wallet.getDisplayBalanceCurrency());

    walletTitleLabel.setText(wallet.getName());
    totalBalanceLabel.setText(
        wallet.getDisplayBalance().toPlainString() + " " + wallet.getDisplayBalanceCurrency());
    usdValueLabel.setText(String.format("$%.2f", wallet.getDisplayBalanceUSD()));
  }

  private JPanel createTopPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(Color.decode("#000000"));
    panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

    walletTitleLabel = new JLabel("Ethereum Wallet");
    walletTitleLabel.setFont(new Font(walletTitleLabel.getFont().getName(), Font.PLAIN, 28));
    panel.add(walletTitleLabel, BorderLayout.WEST);

    JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonsPanel.setBackground(Color.decode("#000000"));

    JButton receiveButton = new JButton("Receive");
    receiveButton.addActionListener(e -> copyWalletAddressToClipboard());
    JButton sendButton = new JButton("Send");
    sendButton.addActionListener(e -> showSendFundsDialog());
    JButton viewExplorerButton = new JButton("View in Explorer");
    viewExplorerButton.addActionListener(e -> openInBlockExplorer());

    buttonsPanel.add(viewExplorerButton);
    buttonsPanel.add(receiveButton);
    buttonsPanel.add(sendButton);

    panel.add(buttonsPanel, BorderLayout.EAST);

    return panel;
  }

  private JPanel createWalletDetailsPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(Color.decode("#000000"));
    panel.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(45, 45, 45)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)));

    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setBackground(Color.decode("#000000"));
    blockchainLabel = new JLabel();
    blockchainLabel.setFont(new Font(blockchainLabel.getFont().getName(), Font.PLAIN, 18));
    headerPanel.add(blockchainLabel, BorderLayout.WEST);

    balanceHeaderLabel = new JLabel();
    balanceHeaderLabel.setFont(new Font(balanceHeaderLabel.getFont().getName(), Font.PLAIN, 18));
    headerPanel.add(balanceHeaderLabel, BorderLayout.EAST);

    panel.add(headerPanel);
    panel.add(Box.createVerticalStrut(20));

    JPanel balancePanel = new JPanel(new GridLayout(2, 2, 10, 5));
    balancePanel.setBackground(Color.decode("#000000"));

    balancePanel.add(createLabel("Balance", Color.GRAY));
    balancePanel.add(createLabel("USD Value", Color.GRAY));

    totalBalanceLabel = createLabel("0.5 ETH", Color.WHITE);
    usdValueLabel = createLabel("$800", Color.WHITE);
    balancePanel.add(totalBalanceLabel);
    balancePanel.add(usdValueLabel);

    panel.add(balancePanel);
    panel.add(Box.createVerticalStrut(20));

    JPanel actionPanel = new JPanel(new GridLayout(1, 2, 10, 0));
    actionPanel.setBackground(Color.decode("#000000"));
    JButton receiveButton = new JButton("Receive");
    receiveButton.addActionListener(e -> copyWalletAddressToClipboard());
    JButton sendButton = new JButton("Send");
    sendButton.addActionListener(e -> showSendFundsDialog());
    actionPanel.add(receiveButton);
    actionPanel.add(sendButton);

    panel.add(actionPanel);

    return panel;
  }

  private JLabel createLabel(String text, Color color) {
    JLabel label = new JLabel(text);
    label.setBackground(Color.decode("#000000"));
    return label;
  }

  private void copyWalletAddressToClipboard() {
    if (currentWallet != null) {
      String address = currentWallet.getAddress();
      StringSelection selection = new StringSelection(address);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
      JOptionPane.showMessageDialog(
          null,
          "Address for wallet '" + currentWallet.getName() + "' copied to clipboard.",
          "Address Copied",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  private void showSendFundsDialog() {
    if (currentWallet == null) {
      JOptionPane.showMessageDialog(this, "No wallet selected", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    String currencyName = getCurrencyName(currentWallet.getWalletType());

    JPanel panel = new JPanel(new GridLayout(0, 1));

    JTextField recipientField = new JTextField();
    JTextField amountField = new JTextField();

    panel.add(new JLabel("Recipient Address:"));
    panel.add(recipientField);
    panel.add(Box.createVerticalStrut(15));
    panel.add(new JLabel("Amount (" + currencyName + "):"));
    panel.add(amountField);

    int result =
        JOptionPane.showOptionDialog(
            null,
            panel,
            "Send from " + currentWallet.getName(),
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            new Object[] {"Next", "Cancel"},
            "Next");

    if (result == 0) {
      String recipientAddress = recipientField.getText().trim();
      String amountText = amountField.getText().trim();

      if (recipientAddress.isEmpty() || amountText.isEmpty()) {
        JOptionPane.showMessageDialog(
            this, "Please fill in all fields", "Input Error", JOptionPane.ERROR_MESSAGE);
        return;
      }

      BigDecimal amount;
      try {
        amount = new BigDecimal(amountText);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
          throw new NumberFormatException("Amount must be positive");
        }
      } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(
            this, "Please enter a valid amount", "Input Error", JOptionPane.ERROR_MESSAGE);
        return;
      }

      showSendConfirmationDialog(recipientAddress, amount, currencyName);
    }
  }

  private void showSendConfirmationDialog(
      String recipientAddress, BigDecimal amount, String currencyName) {
    JPanel panel = new JPanel(new BorderLayout());

    JTextArea messageArea = new JTextArea();
    messageArea.setEditable(false);
    messageArea.setBackground(panel.getBackground());
    messageArea.setText(
        "You are about to send:\n\n"
            + amount
            + " "
            + currencyName
            + "\n\n"
            + "To address:\n"
            + recipientAddress
            + "\n\n"
            + "Please confirm this transaction.");

    panel.add(messageArea, BorderLayout.CENTER);

    int result =
        JOptionPane.showOptionDialog(
            this,
            panel,
            "Confirm Transaction",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            new Object[] {"Send", "Cancel"},
            "Cancel");

    if (result == 0) {
      try {
        String txHash = currentWallet.sendFundsTo(recipientAddress, amount);
        System.out.println(txHash);

        JPanel successPanel = new JPanel(new BorderLayout(0, 10));

        JLabel successLabel = new JLabel("Transaction sent successfully!");
        successLabel.setFont(new Font(successLabel.getFont().getName(), Font.BOLD, 14));
        successPanel.add(successLabel, BorderLayout.NORTH);

        JPanel hashPanel = new JPanel(new BorderLayout(5, 0));
        JLabel hashLabel = new JLabel("Transaction hash: ");
        JTextField hashField = new JTextField(txHash);
        hashField.setEditable(false);

        hashPanel.add(hashLabel, BorderLayout.WEST);
        hashPanel.add(hashField, BorderLayout.CENTER);

        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(
            e -> {
              StringSelection selection = new StringSelection(txHash);
              Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
              copyButton.setText("Copied!");

              Timer timer = new Timer(1500, event -> copyButton.setText("Copy to Clipboard"));
              timer.setRepeats(false);
              timer.start();
            });

        hashPanel.add(copyButton, BorderLayout.EAST);
        successPanel.add(hashPanel, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(
            this, successPanel, "Transaction Sent", JOptionPane.INFORMATION_MESSAGE);
      } catch (TransactionException e) {
        JOptionPane.showMessageDialog(
            this,
            "Failed to send transaction: " + e.getMessage(),
            "Transaction Error",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private String getCurrencyName(WalletType walletType) {
    switch (walletType) {
      case ETHEREUM:
        return "ETH";
      case SOLANA:
        return "SOL";
      default:
        return "UNKNOWN";
    }
  }

  private void openInBlockExplorer() {
    String explorerUrl = null;
    if (currentWallet.getWalletType() == WalletType.ETHEREUM) {
      explorerUrl = "https://etherscan.io/address/" + currentWallet.getAddress();
    } else if (currentWallet.getWalletType() == WalletType.SOLANA) {
      explorerUrl = "https://solscan.io/account/" + currentWallet.getAddress();
    }

    try {
      Desktop.getDesktop().browse(new java.net.URI(explorerUrl));
    } catch (Exception e) {
      JOptionPane.showMessageDialog(
          this, "Failed to open browser: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }
}
