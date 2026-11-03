package com.idm;

import com.idm.api.LocalApiServer;
import com.idm.engine.DownloadQueueManager;
import com.idm.model.AppSettings;
import com.idm.ui.AddDownloadDialog;
import com.idm.ui.MainFrame;
import com.idm.ui.ThemeManager;
import com.idm.util.ClipboardMonitor;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        // High DPI scaling and rendering properties for modern Windows
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("sun.java2d.dpiaware", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Load configuration
        AppSettings settings = AppSettings.load();

        // Apply theme
        ThemeManager.applyTheme(settings);

        SwingUtilities.invokeLater(() -> {
            try {
                DownloadQueueManager queueManager = new DownloadQueueManager(settings);
                ClipboardMonitor clipboardMonitor = new ClipboardMonitor();

                MainFrame mainFrame = new MainFrame(settings, queueManager, clipboardMonitor);
                mainFrame.setVisible(true);

                // Start local API server for Chrome extension integration
                LocalApiServer apiServer = new LocalApiServer(mainFrame, queueManager, settings);
                apiServer.start();

                // If URL was passed as command line argument
                if (args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
                    String url = args[0].trim();
                    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("ftp://")) {
                        AddDownloadDialog addDialog = new AddDownloadDialog(mainFrame, url, queueManager, settings);
                        addDialog.setVisible(true);
                    }
                }

                // Shutdown hook to ensure downloads are cleanly flushed
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    apiServer.stop();
                    queueManager.shutdown();
                    settings.save();
                }, "AppShutdownHook"));

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error launching application: " + e.getMessage(), "Fatal Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
