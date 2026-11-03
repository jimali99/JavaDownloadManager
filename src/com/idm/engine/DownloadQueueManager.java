package com.idm.engine;

import com.idm.model.AppSettings;
import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;
import com.idm.util.StorageUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DownloadQueueManager {

    private final List<DownloadTask> tasks = new CopyOnWriteArrayList<>();
    private final Map<String, DownloadEngine> engines = new ConcurrentHashMap<>();
    private final AppSettings settings;
    private final ScheduledExecutorService queueChecker = Executors.newSingleThreadScheduledExecutor();

    public interface QueueListener {
        void onTasksChanged();
    }

    private final List<QueueListener> queueListeners = new CopyOnWriteArrayList<>();

    public DownloadQueueManager(AppSettings settings) {
        this.settings = settings;
        // Load tasks from storage
        List<DownloadTask> loaded = StorageUtils.loadTasks();
        this.tasks.addAll(loaded);

        for (DownloadTask t : tasks) {
            attachTaskListener(t);
        }

        // Periodic queue processor and persistence
        queueChecker.scheduleAtFixedRate(this::processQueue, 1, 1, TimeUnit.SECONDS);
    }

    public void addQueueListener(QueueListener listener) {
        if (!queueListeners.contains(listener)) {
            queueListeners.add(listener);
        }
    }

    public void removeQueueListener(QueueListener listener) {
        queueListeners.remove(listener);
    }

    private void notifyQueueChanged() {
        for (QueueListener l : queueListeners) {
            try {
                l.onTasksChanged();
            } catch (Exception ignored) {}
        }
    }

    private void attachTaskListener(DownloadTask task) {
        task.addListener(new DownloadTask.DownloadListener() {
            @Override
            public void onProgressUpdate(DownloadTask t) {}

            @Override
            public void onStatusChanged(DownloadTask t, DownloadStatus oldStatus, DownloadStatus newStatus) {
                if (newStatus == DownloadStatus.COMPLETED || newStatus == DownloadStatus.ERROR || newStatus == DownloadStatus.CANCELLED) {
                    engines.remove(t.getId());
                    processQueue();
                }
                saveTasks();
                notifyQueueChanged();
            }
        });
    }

    public synchronized void addTask(DownloadTask task, boolean startImmediately) {
        tasks.add(0, task);
        attachTaskListener(task);
        if (startImmediately) {
            task.setStatus(DownloadStatus.QUEUED);
        } else {
            task.setStatus(DownloadStatus.PAUSED);
        }
        saveTasks();
        notifyQueueChanged();
        processQueue();
    }

    public synchronized void resumeTask(DownloadTask task) {
        if (task.getStatus() == DownloadStatus.COMPLETED) {
            return;
        }
        task.setStatus(DownloadStatus.QUEUED);
        saveTasks();
        processQueue();
    }

    public synchronized void pauseTask(DownloadTask task) {
        DownloadEngine engine = engines.remove(task.getId());
        if (engine != null) {
            engine.pause();
        } else {
            task.setStatus(DownloadStatus.PAUSED);
        }
        saveTasks();
        processQueue();
    }

    public synchronized void restartTask(DownloadTask task) {
        pauseTask(task);
        StorageUtils.deleteTaskFiles(task);
        task.setSegments(new ArrayList<>());
        task.setStatus(DownloadStatus.QUEUED);
        saveTasks();
        processQueue();
    }

    public synchronized boolean removeTask(DownloadTask task, boolean deleteLocalFile) {
        pauseTask(task);
        tasks.removeIf(t -> t.getId().equals(task.getId()));
        StorageUtils.deleteTaskFiles(task);
        boolean fileDeleted = true;
        if (deleteLocalFile) {
            try {
                File out = task.getOutputFile();
                if (out.exists()) {
                    fileDeleted = out.delete();
                    if (!fileDeleted) {
                        System.gc();
                        fileDeleted = java.nio.file.Files.deleteIfExists(out.toPath());
                    }
                }
            } catch (Exception e) {
                fileDeleted = false;
                System.err.println("Could not delete file from disk: " + e.getMessage());
            }
        }
        saveTasks();
        notifyQueueChanged();
        processQueue();
        return fileDeleted;
    }

    public synchronized void pauseAll() {
        for (DownloadTask task : tasks) {
            DownloadEngine engine = engines.remove(task.getId());
            if (engine != null) {
                engine.pause();
            }
            if (task.getStatus().isActive() || task.getStatus() == DownloadStatus.QUEUED) {
                task.setStatus(DownloadStatus.PAUSED);
            }
        }
        saveTasks();
        notifyQueueChanged();
    }

    public synchronized void removeAllTasks(boolean deleteLocalFiles) {
        pauseAll();
        for (DownloadTask task : tasks) {
            StorageUtils.deleteTaskFiles(task);
            if (deleteLocalFiles) {
                try {
                    File out = task.getOutputFile();
                    if (out != null && out.exists()) {
                        out.delete();
                    }
                } catch (Exception ignored) {}
            }
        }
        tasks.clear();
        saveTasks();
        notifyQueueChanged();
    }

    public synchronized void resumeAll() {
        for (DownloadTask task : tasks) {
            if (task.getStatus() == DownloadStatus.PAUSED || task.getStatus() == DownloadStatus.ERROR) {
                task.setStatus(DownloadStatus.QUEUED);
            }
        }
        processQueue();
    }

    private synchronized void processQueue() {
        int maxActive = settings.getMaxConcurrentDownloads();
        int currentActive = 0;

        for (DownloadTask t : tasks) {
            if (t.getStatus().isActive()) {
                currentActive++;
            }
        }

        if (currentActive < maxActive) {
            for (DownloadTask t : tasks) {
                if (t.getStatus() == DownloadStatus.QUEUED) {
                    startEngineForTask(t);
                    currentActive++;
                    if (currentActive >= maxActive) {
                        break;
                    }
                }
            }
        }
    }

    private synchronized void startEngineForTask(DownloadTask task) {
        if (engines.containsKey(task.getId())) {
            return;
        }
        DownloadEngine engine = new DownloadEngine(task, settings);
        engines.put(task.getId(), engine);
        engine.start();
    }

    public List<DownloadTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void saveTasks() {
        StorageUtils.saveTasks(tasks);
    }

    public void shutdown() {
        queueChecker.shutdownNow();
        for (DownloadEngine eng : engines.values()) {
            eng.pause();
        }
        engines.clear();
        saveTasks();
    }
}
