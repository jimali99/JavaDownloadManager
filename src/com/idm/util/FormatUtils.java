package com.idm.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FormatUtils {
    private static final DecimalFormat DF = new DecimalFormat("#,##0.0");
    private static final DecimalFormat DF2 = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static String formatBytes(long bytes) {
        if (bytes < 0) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return DF.format(bytes / 1024.0) + " KB";
        if (bytes < 1024 * 1024 * 1024) return DF2.format(bytes / (1024.0 * 1024.0)) + " MB";
        return DF2.format(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
    }

    public static String formatSpeed(double bytesPerSec) {
        if (bytesPerSec <= 0) return "0 KB/s";
        if (bytesPerSec < 1024) return DF.format(bytesPerSec) + " B/s";
        if (bytesPerSec < 1024 * 1024) return DF.format(bytesPerSec / 1024.0) + " KB/s";
        return DF2.format(bytesPerSec / (1024.0 * 1024.0)) + " MB/s";
    }

    public static String formatTime(long seconds) {
        if (seconds < 0 || seconds > 86400 * 365) return "--:--:--";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) {
            return String.format("%02d:%02d:%02d", h, m, s);
        } else {
            return String.format("%02d:%02d", m, s);
        }
    }

    public static String formatDate(long millis) {
        return DATE_FORMAT.format(new Date(millis));
    }

    public static String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "download_" + System.currentTimeMillis();
        }
        String clean = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (clean.length() > 200) {
            int extIdx = clean.lastIndexOf('.');
            if (extIdx != -1 && clean.length() - extIdx <= 10) {
                String ext = clean.substring(extIdx);
                clean = clean.substring(0, 190) + ext;
            } else {
                clean = clean.substring(0, 200);
            }
        }
        return clean.isEmpty() ? "download" : clean;
    }
}
