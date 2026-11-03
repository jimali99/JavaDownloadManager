package com.idm.model;

import java.util.Locale;

public enum DownloadCategory {
    ALL("All Downloads", "📥"),
    DOWNLOADING("Downloading", "⚡"),
    COMPLETED("Completed", "✅"),
    PAUSED("Paused", "⏸"),
    FAILED("Failed", "❌"),
    COMPRESSED("Compressed", "📦"),
    DOCUMENTS("Documents", "📄"),
    VIDEOS("Videos", "🎬"),
    MUSIC("Music", "🎵"),
    PROGRAMS("Programs", "💻"),
    OTHER("Other", "📁");

    private final String displayName;
    private final String icon;

    DownloadCategory(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public static DownloadCategory fromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return OTHER;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        int dotIdx = lower.lastIndexOf('.');
        if (dotIdx == -1) {
            return OTHER;
        }
        String ext = lower.substring(dotIdx + 1);

        switch (ext) {
            case "zip": case "rar": case "7z": case "tar": case "gz":
            case "bz2": case "xz": case "iso": case "tgz": case "cab":
                return COMPRESSED;

            case "pdf": case "doc": case "docx": case "txt": case "rtf":
            case "xls": case "xlsx": case "ppt": case "pptx": case "epub":
            case "odt": case "csv":
                return DOCUMENTS;

            case "mp4": case "mkv": case "avi": case "mov": case "wmv":
            case "flv": case "webm": case "m4v": case "3gp": case "ts":
                return VIDEOS;

            case "mp3": case "wav": case "flac": case "aac": case "ogg":
            case "m4a": case "wma": case "opus":
                return MUSIC;

            case "exe": case "msi": case "jar": case "apk": case "dmg":
            case "deb": case "rpm": case "appimage": case "bin":
                return PROGRAMS;

            default:
                return OTHER;
        }
    }
}
