package com.idm.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class AppSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    private String defaultDownloadDir;
    private int defaultConnections;
    private int maxConcurrentDownloads;
    private boolean monitorClipboard;
    private boolean playSoundOnComplete;
    private boolean darkMode;
    private int speedLimitKBps;
    private String userAgent;
    private boolean minimizeToTrayOnClose;

    public AppSettings() {
        String home = System.getProperty("user.home", ".");
        File dl = new File(home, "Downloads");
        if (!dl.exists()) {
            dl.mkdirs();
        }
        this.defaultDownloadDir = dl.getAbsolutePath();
        this.defaultConnections = 8;
        this.maxConcurrentDownloads = 3;
        this.monitorClipboard = true;
        this.playSoundOnComplete = true;
        this.darkMode = true;
        this.speedLimitKBps = 0; // unlimited
        this.userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
        this.minimizeToTrayOnClose = true;
    }

    public boolean isMinimizeToTrayOnClose() {
        return minimizeToTrayOnClose;
    }

    public void setMinimizeToTrayOnClose(boolean minimizeToTrayOnClose) {
        this.minimizeToTrayOnClose = minimizeToTrayOnClose;
    }

    public String getDefaultDownloadDir() {
        return defaultDownloadDir;
    }

    public void setDefaultDownloadDir(String defaultDownloadDir) {
        this.defaultDownloadDir = defaultDownloadDir;
    }

    public int getDefaultConnections() {
        return defaultConnections;
    }

    public void setDefaultConnections(int defaultConnections) {
        this.defaultConnections = Math.max(1, Math.min(32, defaultConnections));
    }

    public int getMaxConcurrentDownloads() {
        return maxConcurrentDownloads;
    }

    public void setMaxConcurrentDownloads(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = Math.max(1, Math.min(10, maxConcurrentDownloads));
    }

    public boolean isMonitorClipboard() {
        return monitorClipboard;
    }

    public void setMonitorClipboard(boolean monitorClipboard) {
        this.monitorClipboard = monitorClipboard;
    }

    public boolean isPlaySoundOnComplete() {
        return playSoundOnComplete;
    }

    public void setPlaySoundOnComplete(boolean playSoundOnComplete) {
        this.playSoundOnComplete = playSoundOnComplete;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public int getSpeedLimitKBps() {
        return speedLimitKBps;
    }

    public void setSpeedLimitKBps(int speedLimitKBps) {
        this.speedLimitKBps = Math.max(0, speedLimitKBps);
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    private static File getConfigFile() {
        String appData = System.getenv("APPDATA");
        File dir;
        if (appData != null && !appData.isEmpty()) {
            dir = new File(appData, "JavaDownloadManager");
        } else {
            dir = new File(System.getProperty("user.home"), ".jdm");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "settings.dat");
    }

    public static AppSettings load() {
        File file = getConfigFile();
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                return (AppSettings) ois.readObject();
            } catch (Exception ignored) {}
        }
        return new AppSettings();
    }

    public void save() {
        File file = getConfigFile();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
        } catch (Exception ignored) {}
    }
}
