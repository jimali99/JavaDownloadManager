package com.idm.model;

import java.io.Serializable;

public class DownloadSegment implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final long startByte;
    private final long endByte;
    private volatile long currentByte;
    private volatile boolean completed;
    private volatile double currentSpeed;
    private volatile String status;

    public DownloadSegment(int id, long startByte, long endByte) {
        this.id = id;
        this.startByte = startByte;
        this.endByte = endByte;
        this.currentByte = startByte;
        this.completed = false;
        this.currentSpeed = 0.0;
        this.status = "Pending";
    }

    public int getId() {
        return id;
    }

    public long getStartByte() {
        return startByte;
    }

    public long getEndByte() {
        return endByte;
    }

    public long getCurrentByte() {
        return currentByte;
    }

    public synchronized void setCurrentByte(long currentByte) {
        this.currentByte = currentByte;
        if (endByte > 0 && this.currentByte > endByte) {
            this.currentByte = endByte + 1;
            this.completed = true;
        }
    }

    public synchronized void addDownloadedBytes(long bytes) {
        this.currentByte += bytes;
        if (endByte > 0 && this.currentByte > endByte) {
            this.currentByte = endByte + 1;
            this.completed = true;
        }
    }

    public boolean isCompleted() {
        return completed || (endByte > 0 && currentByte > endByte);
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(double currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDownloadedBytes() {
        if (currentByte < startByte) return 0;
        long dl = currentByte - startByte;
        long total = getTotalBytes();
        if (total > 0 && dl > total) return total;
        return dl;
    }

    public long getTotalBytes() {
        if (endByte <= 0 || endByte < startByte) return -1;
        return (endByte - startByte) + 1;
    }

    public double getProgressPercentage() {
        long total = getTotalBytes();
        if (total <= 0) return 0.0;
        return Math.min(100.0, (getDownloadedBytes() * 100.0) / total);
    }
}
