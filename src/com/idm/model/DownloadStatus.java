package com.idm.model;

public enum DownloadStatus {
    QUEUED("Queued"),
    CONNECTING("Connecting..."),
    DOWNLOADING("Downloading"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    ERROR("Error"),
    CANCELLED("Cancelled");

    private final String displayName;

    DownloadStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == CONNECTING || this == DOWNLOADING;
    }

    public boolean isFinished() {
        return this == COMPLETED || this == ERROR || this == CANCELLED;
    }
}
