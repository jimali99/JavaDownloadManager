package com.idm.util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class ClipboardMonitor implements Runnable {

    public interface ClipboardUrlListener {
        void onUrlDetected(String url);
    }

    private final List<ClipboardUrlListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;
    private String lastText = "";
    private Thread workerThread;

    public void addListener(ClipboardUrlListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ClipboardUrlListener listener) {
        listeners.remove(listener);
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        workerThread = new Thread(this, "ClipboardMonitorThread");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public synchronized void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                    String text = (String) clipboard.getData(DataFlavor.stringFlavor);
                    if (text != null) {
                        text = text.trim();
                        if (!text.equals(lastText)) {
                            lastText = text;
                            if (isDownloadableUrl(text)) {
                                for (ClipboardUrlListener l : listeners) {
                                    l.onUrlDetected(text);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Clipboard might be locked by another application temporarily
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private boolean isDownloadableUrl(String text) {
        if (text == null || text.length() < 10) return false;
        String lower = text.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://") && !lower.startsWith("ftp://")) {
            return false;
        }
        // Don't auto-catch pure domain homepages without paths unless asked
        if (lower.matches("^https?://[^/]+/?$")) {
            return false;
        }
        return true;
    }
}
