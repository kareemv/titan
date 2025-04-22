package com.github.kareemv.titan.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.*;
import java.io.InputStream;
import java.util.Enumeration;
import javax.swing.*;

public class GeistLaf extends FlatDarkLaf {
  public static final String NAME = "GeistLaf";

  public static boolean setup() {
    return setup(new GeistLaf());
  }

  public static void setupFont() {
    String fontPath = "/com/github/kareemv/titan/ui/theme/Geist-Medium.ttf";
    Font customFont = null;
    InputStream is = GeistLaf.class.getResourceAsStream(fontPath);

    if (is != null) {
      try {
        customFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(14f);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(customFont);
      } catch (Exception e) {
        customFont = UIManager.getFont("Label.font").deriveFont(14f);
      }
    } else {
      customFont = UIManager.getFont("Label.font").deriveFont(14f);
    }

    javax.swing.plaf.FontUIResource f = new javax.swing.plaf.FontUIResource(customFont);

    Enumeration<Object> keys = UIManager.getDefaults().keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object value = UIManager.get(key);
      if (value instanceof javax.swing.plaf.FontUIResource) {
        UIManager.put(key, f);
      }
    }
  }

  @Override
  public String getName() {
    return NAME;
  }
}
