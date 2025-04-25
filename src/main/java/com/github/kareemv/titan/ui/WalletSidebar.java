package com.github.kareemv.titan.ui;

import com.formdev.flatlaf.util.SystemInfo;
import com.github.kareemv.titan.Titan;
import com.github.kareemv.titan.util.IOUtils;
import com.github.kareemv.titan.wallet.Wallet;
import com.github.kareemv.titan.wallet.ethereum.EthereumWallet;
import com.github.kareemv.titan.wallet.solana.SolanaWallet;
import com.github.kareemv.titan.wallet.solana.encryption.SolanaWalletEncryptor;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.UIManager;
import javax.swing.border.*;
import org.sol4k.Keypair;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

public class WalletSidebar extends JPanel {
  private static final Color BORDER_COLOR = new Color(39, 39, 42);
  private final DefaultListModel<Wallet> ethereumWalletListModel = new DefaultListModel<>();
  private final DefaultListModel<Wallet> solanaWalletListModel = new DefaultListModel<>();
  private final JTabbedPane tabbedPane;
  private JList<Wallet> ethereumList;
  private JList<Wallet> solanaList;
  private JPopupMenu walletItemPopupMenu;

  public WalletSidebar() {
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    setPreferredSize(new Dimension(300, getPreferredSize().height));
    setBackground(Color.decode("#0A0A0A"));

    JPanel headerPanel = createHeaderPanel();
    add(headerPanel, BorderLayout.NORTH);

    UIManager.put("TabbedPane.tabAreaAlignment", "leading");
    tabbedPane = new JTabbedPane();
    tabbedPane.setTabPlacement(JTabbedPane.TOP);

    JPanel ethereumPanel = createWalletPanel(ethereumWalletListModel);
    tabbedPane.addTab(
        "Ethereum",
        loadIcon("/com/github/kareemv/titan/ui/icons/ethereum.png", 14, 18),
        ethereumPanel);

    JPanel solanaPanel = createWalletPanel(solanaWalletListModel);
    tabbedPane.addTab(
        "Solana", loadIcon("/com/github/kareemv/titan/ui/icons/solana.png", 18, 15), solanaPanel);

    add(tabbedPane, BorderLayout.CENTER);

    populateWalletLists();

    createWalletContextMenu();

    setupListSelectionListeners();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g.create();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    float scale = (float) g2d.getTransform().getScaleX();
    float borderWidth = 1f / scale;

    g2d.setColor(BORDER_COLOR);

    g2d.fillRect(getWidth() - (int) borderWidth, 0, (int) borderWidth, getHeight());

    g2d.dispose();
  }

  private JPanel createHeaderPanel() {
    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

    JLabel walletLabel = new JLabel("Wallets");
    walletLabel.setFont(new Font(walletLabel.getFont().getName(), Font.PLAIN, 28));
    headerPanel.add(walletLabel, BorderLayout.WEST);

    JButton addButton = createAddButton();
    headerPanel.add(addButton, BorderLayout.EAST);

    return headerPanel;
  }

  private JButton createAddButton() {
    JButton addButton = new JButton("+");
    addButton.setPreferredSize(new Dimension(36, 36));
    addButton.setToolTipText("Add Wallet");

    JPopupMenu popupMenu = createAddWalletPopupMenu();

    addButton.addActionListener(e -> popupMenu.show(addButton, 0, addButton.getHeight()));

    return addButton;
  }

  private JPopupMenu createAddWalletPopupMenu() {
    JPopupMenu popupMenu = new JPopupMenu();
    JMenuItem createNewWalletOption = new JMenuItem("Create New Wallet");
    JMenuItem importExistingWalletOption = new JMenuItem("Import Existing Wallet");
    popupMenu.add(createNewWalletOption);
    popupMenu.add(importExistingWalletOption);

    createNewWalletOption.addActionListener(e -> showCreateWalletPopup());
    importExistingWalletOption.addActionListener(e -> showImportWalletPopup());

    return popupMenu;
  }

  private JPanel createWalletPanel(DefaultListModel<Wallet> listModel) {
    JPanel panel = new JPanel(new BorderLayout());
    JList<Wallet> walletList = new JList<>(listModel);
    walletList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    walletList.setCellRenderer(new WalletRenderer());

    JScrollPane listScrollPane = new JScrollPane(walletList);
    listScrollPane.setBorder(null);
    listScrollPane.getVerticalScrollBar().setUnitIncrement(16);
    listScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    listScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, Integer.MAX_VALUE));

    panel.add(listScrollPane, BorderLayout.CENTER);

    return panel;
  }

  private void populateWalletLists() {
    Titan.INSTANCE.ethereumWallets.forEach(ethereumWalletListModel::addElement);
    Titan.INSTANCE.solanaWallets.forEach(solanaWalletListModel::addElement);
  }

  private void setupListSelectionListeners() {
    ethereumList =
        (JList<Wallet>)
            ((JScrollPane) ((JPanel) tabbedPane.getComponentAt(0)).getComponent(0))
                .getViewport()
                .getView();
    solanaList =
        (JList<Wallet>)
            ((JScrollPane) ((JPanel) tabbedPane.getComponentAt(1)).getComponent(0))
                .getViewport()
                .getView();

    WalletListMouseAdapter mouseAdapter = new WalletListMouseAdapter();
    ethereumList.addMouseListener(mouseAdapter);
    ethereumList.addMouseMotionListener(mouseAdapter);
    solanaList.addMouseListener(mouseAdapter);
    solanaList.addMouseMotionListener(mouseAdapter);

    ethereumList.addListSelectionListener(
        e -> {
          if (!e.getValueIsAdjusting()) {
            Wallet selectedWallet = ethereumList.getSelectedValue();
            if (selectedWallet != null) {
              solanaList.clearSelection();
              MainWindow.walletView.displayWalletDetails(selectedWallet);
            }
          }
        });

    solanaList.addListSelectionListener(
        e -> {
          if (!e.getValueIsAdjusting()) {
            Wallet selectedWallet = solanaList.getSelectedValue();
            if (selectedWallet != null) {
              ethereumList.clearSelection();
              MainWindow.walletView.displayWalletDetails(selectedWallet);
            }
          }
        });
  }

  public void selectFirstWallet() {
    if (!ethereumWalletListModel.isEmpty()) {
      ethereumList.setSelectedIndex(0);
      ethereumList.requestFocusInWindow();
    } else if (!solanaWalletListModel.isEmpty()) {
      solanaList.setSelectedIndex(0);
      solanaList.requestFocusInWindow();
    } else {
      MainWindow.walletView.displayNoWalletMessage();
    }
  }

  private void createWalletContextMenu() {
    walletItemPopupMenu = new JPopupMenu();
    JMenuItem copyAddressMenuItem = new JMenuItem("Copy Address");
    JMenuItem renameMenuItem = new JMenuItem("Rename Wallet");
    JMenuItem deleteMenuItem = new JMenuItem("Delete Wallet");
    JMenuItem exportPrivateKeyMenuItem = new JMenuItem("Export Private Key");

    walletItemPopupMenu.add(copyAddressMenuItem);
    walletItemPopupMenu.add(renameMenuItem);
    walletItemPopupMenu.add(deleteMenuItem);
    walletItemPopupMenu.addSeparator();
    walletItemPopupMenu.add(exportPrivateKeyMenuItem);

    copyAddressMenuItem.addActionListener(e -> handleCopyAddress());
    renameMenuItem.addActionListener(e -> handleRenameWallet());
    deleteMenuItem.addActionListener(e -> handleDeleteWallet());
    exportPrivateKeyMenuItem.addActionListener(e -> handleExportPrivateKey());
  }

  private void handleCopyAddress() {
    JList<Wallet> activeList = getActiveWalletList();
    if (activeList == null) return;
    Wallet selectedWallet = activeList.getSelectedValue();
    if (selectedWallet == null) return;

    String address = selectedWallet.getAddress();
    StringSelection selection = new StringSelection(address);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(selection, null);

    JOptionPane.showMessageDialog(
        null,
        "Address for wallet '" + selectedWallet.getName() + "' copied to clipboard.",
        "Address Copied",
        JOptionPane.INFORMATION_MESSAGE);
  }

  private void handleRenameWallet() {
    JList<Wallet> activeList = getActiveWalletList();
    if (activeList == null) return;
    Wallet selectedWallet = activeList.getSelectedValue();
    if (selectedWallet == null) return;

    String oldName = selectedWallet.getName();
    String newName =
        JOptionPane.showInputDialog(Titan.INSTANCE.mainWindow, "Enter new wallet name:", oldName);

    if (newName != null && !newName.trim().isEmpty() && !newName.equals(oldName)) {
      File oldFile;
      File newFile;
      File walletDir;

      if (selectedWallet instanceof EthereumWallet) {
        walletDir = IOUtils.ETH_WALLETS_DIRECTORY;
      } else if (selectedWallet instanceof SolanaWallet) {
        walletDir = IOUtils.SOL_WALLETS_DIRECTORY;
      } else {
        showErrorDialog("Unsupported wallet type for renaming.");
        return;
      }

      oldFile = new File(walletDir, oldName + ".json");
      newFile = new File(walletDir, newName + ".json");

      if (newFile.exists()) {
        showErrorDialog("A wallet with the name '" + newName + "' already exists.");
        return;
      }

      if (oldFile.exists()) {
        if (oldFile.renameTo(newFile)) {
          selectedWallet.setName(newName);
          activeList.repaint();
          MainWindow.walletView.displayWalletDetails(selectedWallet);
        } else {
          showErrorDialog("Failed to rename wallet file.");
        }
      } else {
        showErrorDialog("Original wallet file not found: " + oldFile.getName());
      }
    }
  }

  private void handleDeleteWallet() {
    JList<Wallet> activeList = getActiveWalletList();
    if (activeList == null) return;
    Wallet selectedWallet = activeList.getSelectedValue();
    if (selectedWallet == null) return;

    int response =
        JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to delete wallet '"
                + selectedWallet.getName()
                + "'?\n"
                + "This action cannot be undone.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

    if (response == JOptionPane.YES_OPTION) {
      File walletFile;
      File walletDir;
      boolean removedFromTitan = false;
      boolean removedFromModel = false;

      DefaultListModel<Wallet> model = (DefaultListModel<Wallet>) activeList.getModel();

      if (selectedWallet instanceof EthereumWallet) {
        walletDir = IOUtils.ETH_WALLETS_DIRECTORY;
        removedFromTitan = Titan.INSTANCE.ethereumWallets.remove(selectedWallet);
        removedFromModel = model.removeElement(selectedWallet);
      } else if (selectedWallet instanceof SolanaWallet) {
        walletDir = IOUtils.SOL_WALLETS_DIRECTORY;
        removedFromTitan = Titan.INSTANCE.solanaWallets.remove(selectedWallet);
        removedFromModel = model.removeElement(selectedWallet);
      } else {
        showErrorDialog("Unsupported wallet type for deletion.");
        return;
      }

      walletFile = new File(walletDir, selectedWallet.getName() + ".json");

      boolean fileDeleted = false;
      if (walletFile.exists()) {
        fileDeleted = walletFile.delete();
      }

      if (removedFromTitan && removedFromModel && fileDeleted) {
        selectFirstWallet();
      } else {
        StringBuilder errorMsg = new StringBuilder("Failed to fully delete wallet:\n");
        if (!removedFromTitan) errorMsg.append("- Could not remove from application list.\n");
        if (!removedFromModel) errorMsg.append("- Could not remove from sidebar list.\n");
        if (walletFile.exists() && !fileDeleted) errorMsg.append("- Could not delete wallet file.");
        showErrorDialog(errorMsg.toString());
      }
    }
  }

  private void handleExportPrivateKey() {
    JList<Wallet> activeList = getActiveWalletList();
    if (activeList == null) return;
    Wallet selectedWallet = activeList.getSelectedValue();
    if (selectedWallet == null) return;

    JPasswordField passwordField = new JPasswordField(20);
    JPanel passwordPanel = new JPanel(new BorderLayout(5, 5));
    passwordPanel.add(new JLabel("Enter password to continue:"), BorderLayout.NORTH);
    passwordPanel.add(passwordField, BorderLayout.CENTER);

    int option =
        JOptionPane.showConfirmDialog(
            null,
            passwordPanel,
            "Enter Password",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);

    if (option != JOptionPane.OK_OPTION) {
      return;
    }

    char[] enteredPasswordChars = passwordField.getPassword();
    String enteredPassword = new String(enteredPasswordChars);
    Arrays.fill(enteredPasswordChars, ' ');

    if (!enteredPassword.equals(Titan.INSTANCE.password)) {
      showErrorDialog("Incorrect password.");
      return;
    }

    int warningResult =
        JOptionPane.showConfirmDialog(
            null,
            "Warning: Exporting your private key exposes sensitive information.\n"
                + "Anyone with access to this key can control your funds.\n"
                + "Are you sure you want to proceed?",
            "Security Warning",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

    if (warningResult != JOptionPane.YES_OPTION) {
      return;
    }

    String privateKeyHex;
    try {
      privateKeyHex = decryptAndGetPrivateKeyHex(selectedWallet);
    } catch (Exception ex) {
      ex.printStackTrace();
      showErrorDialog("Failed to decrypt private key: " + ex.getMessage());
      return;
    }

    if (privateKeyHex == null) {
      showErrorDialog("Could not retrieve private key.");
      return;
    }

    showPrivateKeyDialog(selectedWallet, privateKeyHex);
  }

  private String decryptAndGetPrivateKeyHex(Wallet wallet) throws Exception {
    String password = Titan.INSTANCE.password;
    File walletFile;
    File walletDir;

    if (wallet instanceof EthereumWallet) {
      walletDir = IOUtils.ETH_WALLETS_DIRECTORY;
      walletFile = new File(walletDir, wallet.getName() + ".json");
      Credentials credentials = WalletUtils.loadCredentials(password, walletFile);
      BigInteger privateKeyInt = credentials.getEcKeyPair().getPrivateKey();
      return "0x" + privateKeyInt.toString(16);
    } else if (wallet instanceof SolanaWallet) {
      walletDir = IOUtils.SOL_WALLETS_DIRECTORY;
      walletFile = new File(walletDir, wallet.getName() + ".json");
      Keypair keypair = SolanaWalletEncryptor.decryptWalletFromFile(password, walletFile.getPath());
      byte[] secretKeyBytes = keypair.getSecret();
      StringBuilder hexString = new StringBuilder(2 * secretKeyBytes.length);
      for (byte b : secretKeyBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } else {
      throw new UnsupportedOperationException("Unsupported wallet type for key export.");
    }
  }

  private void showPrivateKeyDialog(Wallet wallet, String privateKeyHex) {
    JTextArea textArea = new JTextArea(privateKeyHex);
    textArea.setEditable(false);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setPreferredSize(new Dimension(400, 100));

    JButton copyButton = new JButton("Copy to Clipboard");
    copyButton.addActionListener(
        e -> {
          StringSelection selection = new StringSelection(privateKeyHex);
          Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
          clipboard.setContents(selection, null);
          JOptionPane.showMessageDialog(copyButton.getTopLevelAncestor(), "Private Key Copied!");
        });

    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bottomPanel.add(copyButton);

    String typeName = wallet.getWalletType().name().toLowerCase();
    typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);

    JLabel titleLabel =
        new JLabel("Private key for " + typeName + " wallet '" + wallet.getName() + "'");
    titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

    JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
    mainPanel.add(titleLabel, BorderLayout.NORTH);
    mainPanel.add(scrollPane, BorderLayout.CENTER);
    mainPanel.add(bottomPanel, BorderLayout.SOUTH);

    JOptionPane.showMessageDialog(
        null, mainPanel, "Exported Private Key", JOptionPane.PLAIN_MESSAGE);
  }

  private JList<Wallet> getActiveWalletList() {
    int selectedIndex = tabbedPane.getSelectedIndex();
    if (selectedIndex == 0) {
      return ethereumList;
    } else if (selectedIndex == 1) {
      return solanaList;
    } else {
      return null;
    }
  }

  private void showErrorDialog(String message) {
    JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
  }

  private void showCreateWalletPopup() {
    JTextField walletNameField = new JTextField();
    JComboBox chainComboBox = new JComboBox(new String[] {"Ethereum", "Solana"});
    JPanel panel = new JPanel(new GridLayout(0, 1));
    panel.add(new JLabel("Wallet Name"));
    panel.add(walletNameField);
    panel.add(new JLabel("Blockchain"));
    panel.add(chainComboBox);
    if (SystemInfo.isMacFullWindowContentSupported) {
      panel.putClientProperty("apple.awt.transparentTitleBar", true);
    }
    int result =
        JOptionPane.showOptionDialog(
            null,
            panel,
            "Create New Wallet",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            null);
    if (result == JOptionPane.OK_OPTION) {
      String walletName = walletNameField.getText();
      String blockchain = (String) chainComboBox.getSelectedItem();
      JOptionPane.showMessageDialog(
          Titan.INSTANCE.mainWindow, "Wallet '" + walletName + "' created.");
      try {
        Wallet newWallet;
        if (blockchain.equals("Ethereum")) {
          newWallet = EthereumWallet.createNew(walletName);
          Titan.INSTANCE.ethereumWallets.add((EthereumWallet) newWallet);
          ethereumWalletListModel.addElement(newWallet);
        } else if (blockchain.equals("Solana")) {
          newWallet = SolanaWallet.createNew(walletName);
          Titan.INSTANCE.solanaWallets.add((SolanaWallet) newWallet);
          solanaWalletListModel.addElement(newWallet);
        }
      } catch (Exception e) {
        JOptionPane.showMessageDialog(
            null, "Failed to create wallet: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void showImportWalletPopup() {
    JTextField walletNameField = new JTextField(20);
    JPasswordField privateKeyField = new JPasswordField(20);
    privateKeyField.putClientProperty("FlatLaf.style", "showRevealButton: true");
    JComboBox<String> chainComboBox = new JComboBox<>(new String[] {"Ethereum", "Solana"});
    JPanel panel = new JPanel(new GridLayout(0, 1));
    panel.add(new JLabel("Wallet Name:"));
    panel.add(walletNameField);
    panel.add(new JLabel("Private Key:"));
    panel.add(privateKeyField);
    panel.add(new JLabel("Blockchain:"));
    panel.add(chainComboBox);
    if (SystemInfo.isMacFullWindowContentSupported) {
      panel.putClientProperty("apple.awt.transparentTitleBar", true);
    }
    int result =
        JOptionPane.showOptionDialog(
            Titan.INSTANCE.mainWindow,
            panel,
            "Import Existing Wallet",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            null);
    if (result == JOptionPane.OK_OPTION) {
      String walletName = walletNameField.getText();
      String privateKey = privateKeyField.getText();
      String blockchain = (String) chainComboBox.getSelectedItem();
      try {
        Wallet importedWallet;
        if (blockchain.equals("Ethereum")) {
          importedWallet = EthereumWallet.createFromPrivateKey(walletName, privateKey);
          Titan.INSTANCE.ethereumWallets.add((EthereumWallet) importedWallet);
          ethereumWalletListModel.addElement(importedWallet);
        } else if (blockchain.equals("Solana")) {
          importedWallet = SolanaWallet.createFromPrivateKey(walletName, privateKey);
          Titan.INSTANCE.solanaWallets.add((SolanaWallet) importedWallet);
          solanaWalletListModel.addElement(importedWallet);
        }
      } catch (Exception e) {
        JOptionPane.showMessageDialog(
            null, "Failed to import wallet: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private Icon loadIcon(String path, int width, int height) {
    ImageIcon icon = new ImageIcon(getClass().getResource(path));
    if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
      Image img = icon.getImage();
      Image resizedImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
      return new ImageIcon(resizedImg);
    } else {
      System.err.println("Failed to load icon: " + path);
      return null;
    }
  }

  private class WalletRenderer extends JPanel implements ListCellRenderer<Wallet> {
    private final JLabel nameLabel = new JLabel();
    private final JLabel balanceLabel = new JLabel();

    WalletRenderer() {
      setLayout(new BorderLayout(10, 0));
      setBorder(new EmptyBorder(5, 10, 5, 5));

      add(nameLabel, BorderLayout.WEST);
      add(balanceLabel, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends Wallet> list,
        Wallet value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {
      String originalName = value.getName();
      String displayName;
      if (originalName.length() > 18) {
        displayName = originalName.substring(0, 15) + "...";
      } else {
        displayName = originalName;
      }
      nameLabel.setText(displayName);
      balanceLabel.setText(value.getDisplayBalance() + " " + value.getDisplayBalanceCurrency());

      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
        nameLabel.setForeground(list.getSelectionForeground());
        balanceLabel.setForeground(list.getSelectionForeground());
      } else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
        nameLabel.setForeground(list.getForeground());
        balanceLabel.setForeground(list.getForeground());
      }

      return this;
    }
  }

  private class WalletListMouseAdapter extends MouseAdapter implements MouseMotionListener {
    @Override
    public void mousePressed(MouseEvent e) {
      JList<Wallet> list = (JList<Wallet>) e.getSource();
      int row = list.locationToIndex(e.getPoint());

      if (row != -1 && list.getCellBounds(row, row).contains(e.getPoint())) {
        if (SwingUtilities.isRightMouseButton(e)) {
          list.setSelectedIndex(row);
          if (walletItemPopupMenu != null) {
            walletItemPopupMenu.show(list, e.getX(), e.getY());
          }
        }
      } else {
        list.clearSelection();
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
      JList<Wallet> list = (JList<Wallet>) e.getSource();
      int row = list.locationToIndex(e.getPoint());
      if (row != -1 && list.getCellBounds(row, row).contains(e.getPoint())) {
        list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      } else {
        list.setCursor(Cursor.getDefaultCursor());
      }
    }

    @Override
    public void mouseDragged(MouseEvent e) {}
  }
}
