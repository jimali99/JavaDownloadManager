package com.idm.engine;

import com.idm.model.AppSettings;
import com.idm.model.DownloadSegment;
import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;
import com.idm.util.SoundUtils;
import com.idm.util.StorageUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class DownloadEngine {

    private final DownloadTask task;
    private final AppSettings settings;
    private final List<SegmentWorker> workers = new CopyOnWriteArrayList<>();
    private final SpeedCalculator speedCalculator = new SpeedCalculator();
    private ExecutorService workerPool;
    private ScheduledExecutorService tickerPool;
    private RandomAccessFile raf;
    private FileChannel fileChannel;
    private volatile boolean isRunning = false;

    public DownloadEngine(DownloadTask task, AppSettings settings) {
        this.task = task;
        this.settings = settings;
    }

    public synchronized void start() {
        if (isRunning || task.getStatus() == DownloadStatus.COMPLETED) {
            return;
        }

        isRunning = true;
        task.setStatus(DownloadStatus.CONNECTING);
        task.setErrorMessage(null);

        new Thread(() -> {
            try {
                initAndRun();
            } catch (Exception e) {
                if (isRunning && task.getStatus() != DownloadStatus.PAUSED && task.getStatus() != DownloadStatus.CANCELLED) {
                    task.setErrorMessage(e.getMessage());
                    task.setStatus(DownloadStatus.ERROR);
                }
                cleanup();
            }
        }, "EngineInitThread-" + task.getFileName()).start();
    }

    private void initAndRun() throws Exception {
        // Step 1: Probe URL if totalBytes not known or metadata not present
        boolean loadedMeta = StorageUtils.loadTaskMetadata(task);

        if (!loadedMeta || task.getTotalBytes() <= 0) {
            UrlProber.ProbeResult probe = UrlProber.probe(task.getUrl(), settings.getUserAgent());
            task.setUrl(probe.getFinalUrl());
            if (task.getTotalBytes() <= 0) {
                task.setTotalBytes(probe.getContentLength());
            }
            task.setResumeSupported(probe.isResumeSupported());
        }

        // Prepare destination files
        File saveDir = new File(task.getSaveDirectory());
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }

        File tempFile = task.getTempFile();
        raf = new RandomAccessFile(tempFile, "rw");
        if (task.getTotalBytes() > 0 && raf.length() < task.getTotalBytes()) {
            // Pre-allocate file size for zero fragmentation
            raf.setLength(task.getTotalBytes());
        }
        fileChannel = raf.getChannel();

        // Step 2: Initialize segments if not already set up
        if (task.getSegments().isEmpty()) {
            List<DownloadSegment> segments = new ArrayList<>();
            long total = task.getTotalBytes();
            int connections = task.getNumConnections();

            if (task.isResumeSupported() && total > 0 && connections > 1) {
                long chunkSize = total / connections;
                for (int i = 0; i < connections; i++) {
                    long start = i * chunkSize;
                    long end = (i == connections - 1) ? (total - 1) : (start + chunkSize - 1);
                    segments.add(new DownloadSegment(i + 1, start, end));
                }
            } else {
                // Single connection
                task.setNumConnections(1);
                segments.add(new DownloadSegment(1, 0, total > 0 ? (total - 1) : -1));
            }
            task.setSegments(segments);
            StorageUtils.saveTaskMetadata(task);
        }

        // Step 3: Launch workers
        task.setStatus(DownloadStatus.DOWNLOADING);
        workerPool = Executors.newFixedThreadPool(task.getSegments().size());
        workers.clear();

        List<Future<?>> workerFutures = new ArrayList<>();
        for (DownloadSegment segment : task.getSegments()) {
            SegmentWorker worker = new SegmentWorker(
                task,
                segment,
                fileChannel,
                settings.getUserAgent(),
                settings.getSpeedLimitKBps()
            );
            workers.add(worker);
            workerFutures.add(workerPool.submit(worker));
        }

        // Step 4: Progress Ticker
        speedCalculator.reset();
        tickerPool = Executors.newSingleThreadScheduledExecutor();
        tickerPool.scheduleAtFixedRate(() -> {
            try {
                if (!isRunning) return;

                long downloaded = task.getDownloadedBytes();
                speedCalculator.recordSample(downloaded);
                double speed = speedCalculator.getCurrentSpeed();
                task.setCurrentSpeed(speed);

                long total = task.getTotalBytes();
                if (total > 0 && speed > 0) {
                    long remaining = total - downloaded;
                    task.setEtaSeconds(Math.max(0, (long) (remaining / speed)));
                } else {
                    task.setEtaSeconds(-1);
                }

                task.fireProgressUpdate();
                StorageUtils.saveTaskMetadata(task);

            } catch (Exception ignored) {}
        }, 200, 250, TimeUnit.MILLISECONDS);

        // Step 5: Wait for all workers to finish
        boolean allSuccess = true;
        for (Future<?> f : workerFutures) {
            try {
                f.get();
            } catch (Exception e) {
                allSuccess = false;
                if (isRunning && task.getStatus() != DownloadStatus.PAUSED && task.getStatus() != DownloadStatus.CANCELLED) {
                    task.setErrorMessage(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                }
            }
        }

        if (allSuccess && isRunning && task.getStatus() == DownloadStatus.DOWNLOADING) {
            // Verify all segments completed
            boolean allCompleted = true;
            for (DownloadSegment seg : task.getSegments()) {
                if (!seg.isCompleted()) {
                    allCompleted = false;
                    break;
                }
            }

            if (allCompleted) {
                finalizeDownload();
            }
        }
    }

    private void finalizeDownload() {
        cleanup();
        File tempFile = task.getTempFile();
        File outputFile = task.getOutputFile();

        // If output file already exists, create unique name
        if (outputFile.exists()) {
            String name = task.getFileName();
            String base = name;
            String ext = "";
            int dot = name.lastIndexOf('.');
            if (dot != -1) {
                base = name.substring(0, dot);
                ext = name.substring(dot);
            }
            int counter = 1;
            while (outputFile.exists()) {
                outputFile = new File(task.getSaveDirectory(), base + "_" + counter + ext);
                counter++;
            }
            task.setFileName(outputFile.getName());
        }

        // Rename temp to output
        boolean renamed = false;
        for (int retry = 0; retry < 5; retry++) {
            if (tempFile.renameTo(outputFile)) {
                renamed = true;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }

        if (renamed) {
            StorageUtils.deleteTaskFiles(task);
            task.setStatus(DownloadStatus.COMPLETED);
            task.fireProgressUpdate();
            if (settings.isPlaySoundOnComplete()) {
                SoundUtils.playCompleteSound();
            }
        } else {
            task.setErrorMessage("Failed to rename temporary download file to final output.");
            task.setStatus(DownloadStatus.ERROR);
        }
    }

    public synchronized void pause() {
        if (!isRunning) return;
        isRunning = false;
        task.setStatus(DownloadStatus.PAUSED);
        for (SegmentWorker worker : workers) {
            worker.pause();
        }
        cleanup();
        StorageUtils.saveTaskMetadata(task);
        task.fireProgressUpdate();
    }

    public synchronized void cancel() {
        isRunning = false;
        task.setStatus(DownloadStatus.CANCELLED);
        for (SegmentWorker worker : workers) {
            worker.cancel();
        }
        cleanup();
        StorageUtils.deleteTaskFiles(task);
        task.fireProgressUpdate();
    }

    private synchronized void cleanup() {
        isRunning = false;
        if (tickerPool != null) {
            tickerPool.shutdownNow();
            tickerPool = null;
        }
        if (workerPool != null) {
            workerPool.shutdownNow();
            workerPool = null;
        }
        if (fileChannel != null) {
            try {
                fileChannel.close();
            } catch (Exception ignored) {}
            fileChannel = null;
        }
        if (raf != null) {
            try {
                raf.close();
            } catch (Exception ignored) {}
            raf = null;
        }
    }
}
