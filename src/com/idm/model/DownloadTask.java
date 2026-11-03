package com.idm.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class DownloadTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String url;
    private String fileName;
    private String saveDirectory;
    private long totalBytes;
    private int numConnections;
    private boolean resumeSupported;
    private volatile DownloadStatus status;
    private DownloadCategory category;
    private final List<DownloadSegment> segments;
    private long dateAdded;
    private String errorMessage;

    // Transient fields for runtime tracking (not serialized)
    private transient volatile double currentSpeed; // bytes/sec
    private transient volatile long etaSeconds;
    private transient List<DownloadListener> listeners = new CopyOnWriteArrayList<>();

    public interface DownloadListener {
        void onProgressUpdate(DownloadTask task);
        void onStatusChanged(DownloadTask task, DownloadStatus oldStatus, DownloadStatus newStatus);
    }

    public DownloadTask(String url, String fileName, String saveDirectory, int numConnections) {
        this.id = UUID.randomUUID().toString();
        this.url = url;
        this.fileName = fileName;
        this.saveDirectory = saveDirectory;
        this.numConnections = Math.max(1, Math.min(32, numConnections));
        this.totalBytes = -1;
        this.resumeSupported = false;
        this.status = DownloadStatus.QUEUED;
        this.category = DownloadCategory.fromFileName(fileName);
        this.segments = new ArrayList<>();
        this.dateAdded = System.currentTimeMillis();
        this.currentSpeed = 0.0;
        this.etaSeconds = -1;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        this.category = DownloadCategory.fromFileName(fileName);
    }

    public String getSaveDirectory() {
        return saveDirectory;
    }

    public void setSaveDirectory(String saveDirectory) {
        this.saveDirectory = saveDirectory;
    }

    public File getOutputFile() {
        return new File(saveDirectory, fileName);
    }

    public File getTempFile() {
        return new File(saveDirectory, fileName + ".idm_temp");
    }

    public File getMetadataFile() {
        return new File(saveDirectory, fileName + ".idm_meta");
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public int getNumConnections() {
        return numConnections;
    }

    public void setNumConnections(int numConnections) {
        this.numConnections = Math.max(1, Math.min(32, numConnections));
    }

    public boolean isResumeSupported() {
        return resumeSupported;
    }

    public void setResumeSupported(boolean resumeSupported) {
        this.resumeSupported = resumeSupported;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(DownloadStatus newStatus) {
        DownloadStatus oldStatus = this.status;
        this.status = newStatus;
        if (newStatus == DownloadStatus.COMPLETED) {
            this.currentSpeed = 0;
            this.etaSeconds = 0;
        } else if (newStatus == DownloadStatus.PAUSED || newStatus == DownloadStatus.ERROR || newStatus == DownloadStatus.CANCELLED) {
            this.currentSpeed = 0;
        }
        if (listeners != null) {
            for (DownloadListener listener : listeners) {
                try {
                    listener.onStatusChanged(this, oldStatus, newStatus);
                } catch (Exception ignored) {}
            }
        }
    }

    public DownloadCategory getCategory() {
        return category;
    }

    public void setCategory(DownloadCategory category) {
        this.category = category;
    }

    public synchronized List<DownloadSegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public synchronized void setSegments(List<DownloadSegment> newSegments) {
        this.segments.clear();
        if (newSegments != null) {
            this.segments.addAll(newSegments);
        }
    }

    public synchronized long getDownloadedBytes() {
        if (segments.isEmpty()) {
            return 0;
        }
        long sum = 0;
        for (DownloadSegment seg : segments) {
            sum += seg.getDownloadedBytes();
        }
        if (totalBytes > 0 && sum > totalBytes) {
            return totalBytes;
        }
        return sum;
    }

    public double getProgressPercentage() {
        if (totalBytes <= 0) {
            return (status == DownloadStatus.COMPLETED) ? 100.0 : 0.0;
        }
        return Math.min(100.0, (getDownloadedBytes() * 100.0) / totalBytes);
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(double currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public long getEtaSeconds() {
        return etaSeconds;
    }

    public void setEtaSeconds(long etaSeconds) {
        this.etaSeconds = etaSeconds;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public void addListener(DownloadListener listener) {
        if (listeners == null) {
            listeners = new CopyOnWriteArrayList<>();
        }
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(DownloadListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public void fireProgressUpdate() {
        if (listeners == null) return;
        for (DownloadListener listener : listeners) {
            try {
                listener.onProgressUpdate(this);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadTask that = (DownloadTask) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
