package com.github.kareemv.titan.ui;

import com.github.kareemv.titan.util.IOUtils;
import com.github.kareemv.titan.wallet.exception.IncorrectPasswordException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import javax.swing.*;

public class WelcomeDialog extends JDialog implements ActionListener {

  private final boolean isExistingUser;
  private JPasswordField passwordField;
  private JPasswordField confirmPasswordField;
  private JLabel instructionLabel;
  private JLabel confirmPasswordLabel;
  private JButton actionButton;
  private String enteredPassword;
  private boolean successful = false;

  public WelcomeDialog(Frame owner) {
    super(owner, "Titan", true);
    this.isExistingUser = IOUtils.hasExistingWallets();

    initComponents();
    layoutComponents();
    setupWindowProperties();
  }

  private void initComponents() {
    passwordField = new JPasswordField(20);
    confirmPasswordField = new JPasswordField(20);
    instructionLabel = new JLabel();
    confirmPasswordLabel = new JLabel("Confirm Password:");
    actionButton = new JButton();

    if (isExistingUser) {
      instructionLabel.setText("Enter password to unlock wallets:");
      actionButton.setText("Unlock");
      confirmPasswordField.setVisible(false);
      confirmPasswordLabel.setVisible(false);
    } else {
      instructionLabel.setText("Create a new password for your wallets:");
      actionButton.setText("Create");
    }

    actionButton.putClientProperty("JButton.buttonType", "primary");
    Color originalBgColor = Color.decode("#FAFAFA");
    Color hoverBgColor = Color.decode("#F3F3F3");
    actionButton.setBackground(originalBgColor);
    actionButton.setForeground(Color.BLACK);
    actionButton.setBorderPainted(false);
    actionButton.setFocusPainted(false);
    actionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    actionButton.addActionListener(this);
    actionButton.setRolloverEnabled(false);
    actionButton.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseEntered(MouseEvent e) {
            actionButton.setBackground(hoverBgColor);
          }

          @Override
          public void mouseExited(MouseEvent e) {
            actionButton.setBackground(originalBgColor);
          }
        });
    passwordField.addActionListener(this);
    confirmPasswordField.addActionListener(this);
  }

  private void layoutComponents() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(10, 5, 10, 5);
    gbc.gridx = 0;
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    JLabel titleLabel = new JLabel("Titan Wallet");
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 28f));
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    gbc.gridy = 0;
    gbc.insets = new Insets(10, 5, 5, 5);
    panel.add(titleLabel, gbc);

    JLabel subHeadingLabel = new JLabel("Welcome.");
    subHeadingLabel.setFont(subHeadingLabel.getFont().deriveFont(18f));
    subHeadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
    gbc.gridy = 1;
    gbc.insets = new Insets(0, 5, 20, 5);
    panel.add(subHeadingLabel, gbc);

    gbc.insets = new Insets(5, 5, 5, 5);

    instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
    gbc.gridy = 2;
    panel.add(instructionLabel, gbc);

    gbc.gridy = 3;
    panel.add(passwordField, gbc);

    int nextRow = 4;
    if (!isExistingUser) {
      confirmPasswordLabel.setHorizontalAlignment(SwingConstants.CENTER);
      gbc.gridy = nextRow++;
      panel.add(confirmPasswordLabel, gbc);

      gbc.gridy = nextRow++;
      panel.add(confirmPasswordField, gbc);
    }

    gbc.gridy = nextRow;
    gbc.insets = new Insets(20, 5, 5, 5);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.CENTER;
    panel.add(actionButton, gbc);

    add(panel);
  }

  private void setupWindowProperties() {
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    setPreferredSize(new Dimension(500, 350));
    pack();
    setLocationRelativeTo(null);
    setResizable(false);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == passwordField && isExistingUser) {
      handleAction();
    } else if (e.getSource() == confirmPasswordField && !isExistingUser) {
      handleAction();
    } else if (e.getSource() == actionButton) {
      handleAction();
    }
  }

  private void handleAction() {
    char[] passwordChars = passwordField.getPassword();
    enteredPassword = new String(passwordChars);
    Arrays.fill(passwordChars, ' ');

    if (enteredPassword.isEmpty()) {
      JOptionPane.showMessageDialog(
          null, "Password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
      passwordField.requestFocusInWindow();
      return;
    }

    if (isExistingUser) {
      try {
        IOUtils.loadWallets(enteredPassword);
        successful = true;
        dispose();
      } catch (IncorrectPasswordException ex) {
        JOptionPane.showMessageDialog(
            null, "Incorrect password. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
        passwordField.setText("");
        passwordField.requestFocusInWindow();
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(
            null,
            "An error occurred loading wallets: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
        successful = false;
        dispose();
      }
    } else {
      char[] confirmPasswordChars = confirmPasswordField.getPassword();
      String confirmPassword = new String(confirmPasswordChars);
      Arrays.fill(confirmPasswordChars, ' ');

      if (!enteredPassword.equals(confirmPassword)) {
        JOptionPane.showMessageDialog(
            null, "Passwords do not match. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
        passwordField.setText("");
        confirmPasswordField.setText("");
        passwordField.requestFocusInWindow();
      } else {
        successful = true;
        dispose();
      }
    }
  }

  public String getPassword() {
    return successful ? enteredPassword : null;
  }

  public boolean isSuccessful() {
    return successful;
  }
}
