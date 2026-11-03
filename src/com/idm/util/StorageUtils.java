package com.idm.util;

import com.idm.model.DownloadSegment;
import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;

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
}
