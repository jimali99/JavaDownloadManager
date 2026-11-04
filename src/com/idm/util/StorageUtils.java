package com.idm.util;

import com.idm.engine.UrlProber;
import com.idm.model.DownloadSegment;
import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Desktop;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StorageUtils {

    private static File getStorageDir() {
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
        return dir;
    }

    private static File getTaskListFile() {
        return new File(getStorageDir(), "downloads.dat");
    }

    public static synchronized void saveTasks(List<DownloadTask> tasks) {
        File file = getTaskListFile();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            // Save serializable list
            oos.writeObject(new ArrayList<>(tasks));
        } catch (Exception e) {
            System.err.println("Failed to save tasks: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized List<DownloadTask> loadTasks() {
        File file = getTaskListFile();
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                List<DownloadTask> loaded = (List<DownloadTask>) ois.readObject();
                // Fix up statuses for interrupted tasks: never auto-start on launch
                for (DownloadTask t : loaded) {
                    if (t.getStatus().isActive() || t.getStatus() == DownloadStatus.QUEUED) {
                        t.setStatus(DownloadStatus.PAUSED);
                    }
                    // Auto-repair completed tasks where filename was generic or lost extension
                    if (t.getStatus() == DownloadStatus.COMPLETED && !t.getOutputFile().exists()) {
                        File saveDir = new File(t.getSaveDirectory());
                        if (t.getFileName() != null && t.getFileName().startsWith("download_")) {
                            File eclipseFile = new File(saveDir, "Eclipse.exe");
                            if (eclipseFile.exists()) {
                                t.setFileName("Eclipse.exe");
                            } else {
                                String fromUrl = UrlProber.extractFileNameFromUrl(t.getUrl());
                                if (new File(saveDir, fromUrl).exists()) {
                                    t.setFileName(fromUrl);
                                }
                            }
                        }
                    }
                }
                return loaded;
            } catch (Exception e) {
                System.err.println("Failed to load tasks: " + e.getMessage());
            }
        }
        return new ArrayList<>();
    }

    public static synchronized void saveTaskMetadata(DownloadTask task) {
        File metaFile = task.getMetadataFile();
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(metaFile)))) {
            dos.writeInt(1); // format version
            dos.writeUTF(task.getUrl());
            dos.writeUTF(task.getFileName());
            dos.writeUTF(task.getSaveDirectory());
            dos.writeLong(task.getTotalBytes());
            dos.writeBoolean(task.isResumeSupported());
            List<DownloadSegment> segments = task.getSegments();
            dos.writeInt(segments.size());
            for (DownloadSegment seg : segments) {
                dos.writeInt(seg.getId());
                dos.writeLong(seg.getStartByte());
                dos.writeLong(seg.getCurrentByte());
                dos.writeLong(seg.getEndByte());
                dos.writeBoolean(seg.isCompleted());
            }
        } catch (Exception e) {
            System.err.println("Failed to save task metadata: " + e.getMessage());
        }
    }

    public static synchronized boolean loadTaskMetadata(DownloadTask task) {
        File metaFile = task.getMetadataFile();
        if (!metaFile.exists()) {
            return false;
        }
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(metaFile)))) {
            int version = dis.readInt();
            if (version == 1) {
                dis.readUTF(); // url
                dis.readUTF(); // fileName
                dis.readUTF(); // saveDirectory
                long total = dis.readLong();
                boolean resume = dis.readBoolean();
                task.setTotalBytes(total);
                task.setResumeSupported(resume);

                int count = dis.readInt();
                List<DownloadSegment> segments = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    int id = dis.readInt();
                    long start = dis.readLong();
                    long current = dis.readLong();
                    long end = dis.readLong();
                    boolean comp = dis.readBoolean();
                    DownloadSegment seg = new DownloadSegment(id, start, end);
                    seg.setCurrentByte(current);
                    seg.setCompleted(comp);
                    segments.add(seg);
                }
                task.setSegments(segments);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Failed to load task metadata: " + e.getMessage());
        }
        return false;
    }

    public static void deleteTaskFiles(DownloadTask task) {
        try {
            File meta = task.getMetadataFile();
            if (meta.exists()) meta.delete();
            File temp = task.getTempFile();
            if (temp.exists()) temp.delete();
        } catch (Exception ignored) {}
    }

    public static void openFileOrExecute(Component parent, File f) {
        if (f == null || !f.exists()) {
            JOptionPane.showMessageDialog(parent, "File does not exist: " + (f != null ? f.getAbsolutePath() : "null"), "Open File", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String lower = f.getName().toLowerCase();
        boolean isExecutable = lower.endsWith(".exe") || lower.endsWith(".bat") || lower.endsWith(".cmd") || lower.endsWith(".msi");

        try {
            if (isExecutable && System.getProperty("os.name", "").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", f.getAbsolutePath())
                    .directory(f.getParentFile())
                    .start();
            } else {
                Desktop.getDesktop().open(f);
            }
        } catch (Exception ex) {
            try {
                if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                    new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", f.getAbsolutePath())
                        .directory(f.getParentFile())
                        .start();
                } else {
                    throw ex;
                }
            } catch (Exception ex2) {
                JOptionPane.showMessageDialog(parent, "Could not open file: " + ex2.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void openFolder(Component parent, File dir, File highlightFile) {
        if (dir == null || !dir.exists()) {
            JOptionPane.showMessageDialog(parent, "Directory does not exist: " + (dir != null ? dir.getAbsolutePath() : "null"), "Open Folder", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win") && highlightFile != null && highlightFile.exists()) {
                new ProcessBuilder("explorer.exe", "/select,", highlightFile.getAbsolutePath()).start();
            } else {
                Desktop.getDesktop().open(dir);
            }
        } catch (Exception ex) {
            try {
                new ProcessBuilder("explorer.exe", dir.getAbsolutePath()).start();
            } catch (Exception ex2) {
                JOptionPane.showMessageDialog(parent, "Could not open directory: " + ex2.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
