package com.github.kareemv.titan.ui;

import com.formdev.flatlaf.util.SystemInfo;
import java.awt.*;
import javax.swing.*;

public class MainWindow extends JFrame {

  public static final WalletSidebar walletSidebar = new WalletSidebar();
  public static final WalletView walletView = new WalletView();
  private static final TitleBarBorderPanel titleBarPanel = new TitleBarBorderPanel();

  public MainWindow() {
    super("Titan");

    if (SystemInfo.isMacFullWindowContentSupported) {
      getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
    }

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setPreferredSize(new Dimension(1100, 700));
    getContentPane().setPreferredSize(new Dimension(1100, 700));
    getContentPane().setMinimumSize(new Dimension(900, 500));
    setMinimumSize(new Dimension(900, 500));

    add(walletSidebar, BorderLayout.WEST);
    add(walletView, BorderLayout.CENTER);

    titleBarPanel.setPreferredSize(new Dimension(getWidth(), 1));
    add(titleBarPanel, BorderLayout.NORTH);

    pack();
    setLocationRelativeTo(null);
    setVisible(true);
    walletSidebar.requestFocusInWindow();
    walletSidebar.selectFirstWallet();
  }

  private static class TitleBarBorderPanel extends JPanel {
    private static final Color BORDER_COLOR = new Color(39, 39, 42);

    public TitleBarBorderPanel() {
      setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      float scale = (float) g2d.getTransform().getScaleX();
      float borderWidth = 1f / scale;

      g2d.setColor(BORDER_COLOR);
      g2d.fillRect(0, getHeight() - (int) borderWidth, getWidth(), (int) borderWidth);

      g2d.dispose();
    }
  }
}
