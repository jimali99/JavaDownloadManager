package com.idm.ui;

import com.idm.model.AppSettings;

import javax.swing.*;
import java.awt.*;

public class ThemeManager {

    public static void applyTheme(AppSettings settings) {
        try {
            // Attempt to load FlatLaf
            if (settings.isDarkMode()) {
                try {
                    Class<?> darkClass = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
                    UIManager.setLookAndFeel((LookAndFeel) darkClass.getDeclaredConstructor().newInstance());
                } catch (Exception e) {
                    tryModernSystemTheme();
                }
            } else {
                try {
                    Class<?> lightClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
                    UIManager.setLookAndFeel((LookAndFeel) lightClass.getDeclaredConstructor().newInstance());
                } catch (Exception e) {
                    tryModernSystemTheme();
                }
            }

            // Global UI Tweaks
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 6);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("Table.rowHeight", 32);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 1));

            // Set system font
            Font font = new Font("Segoe UI", Font.PLAIN, 13);
            setUIFont(new javax.swing.plaf.FontUIResource(font));

        } catch (Exception e) {
            tryModernSystemTheme();
        }
    }

    private static void tryModernSystemTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    private static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }
}
