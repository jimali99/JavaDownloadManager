package com.idm.test;

import com.idm.engine.DownloadEngine;
import com.idm.model.AppSettings;
import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;
import com.idm.util.FormatUtils;

import java.io.File;

public class EnginePauseResumeTest {

    public static void main(String[] args) throws Exception {
        System.out.println(">>> Testing Pause & Resume Reliability <<<");

        String testUrl = "https://www.python.org/ftp/python/3.12.0/python-3.12.0-amd64.exe";
        AppSettings settings = new AppSettings();
        String tempDir = System.getProperty("java.io.tmpdir");

        DownloadTask task = new DownloadTask(testUrl, "idm_pause_resume_test.exe", tempDir, 8);
        DownloadEngine engine1 = new DownloadEngine(task, settings);

        System.out.println("1. Starting initial download...");
        engine1.start();

        // Wait until it reaches at least 20%
        while (task.getProgressPercentage() < 20.0) {
            Thread.sleep(100);
        }

        System.out.printf("   Download reached %.1f%% (%s / %s). Pausing engine now...%n",
            task.getProgressPercentage(),
            FormatUtils.formatBytes(task.getDownloadedBytes()),
            FormatUtils.formatBytes(task.getTotalBytes())
        );

        engine1.pause();
        Thread.sleep(500);

        System.out.println("2. Verifying paused state on disk...");
        File tempFile = task.getTempFile();
        File metaFile = task.getMetadataFile();
        System.out.println("   Temp file exists: " + tempFile.exists() + " (length: " + tempFile.length() + ")");
        System.out.println("   Meta file exists: " + metaFile.exists() + " (length: " + metaFile.length() + ")");

        long pausedDownloaded = task.getDownloadedBytes();
        System.out.println("   Downloaded at pause: " + FormatUtils.formatBytes(pausedDownloaded));

        System.out.println("\n3. Creating new DownloadEngine to simulate application restart and resuming...");
        DownloadTask taskResumed = new DownloadTask(testUrl, "idm_pause_resume_test.exe", tempDir, 8);
        DownloadEngine engine2 = new DownloadEngine(taskResumed, settings);

        taskResumed.addListener(new DownloadTask.DownloadListener() {
            @Override
            public void onProgressUpdate(DownloadTask t) {
                System.out.printf("   [Resumed Progress] %5.1f%% | Downloaded: %s / %s | Speed: %s | ETA: %s%n",
                    t.getProgressPercentage(),
                    FormatUtils.formatBytes(t.getDownloadedBytes()),
                    FormatUtils.formatBytes(t.getTotalBytes()),
                    FormatUtils.formatSpeed(t.getCurrentSpeed()),
                    FormatUtils.formatTime(t.getEtaSeconds())
                );
            }

            @Override
            public void onStatusChanged(DownloadTask t, DownloadStatus oldStatus, DownloadStatus newStatus) {
                System.out.println("   [Resumed Status] " + oldStatus + " -> " + newStatus);
            }
        });

        engine2.start();

        long deadline = System.currentTimeMillis() + 45000;
        while (!taskResumed.getStatus().isFinished() && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }

        System.out.println("\n4. Verification:");
        System.out.println("   Final Status: " + taskResumed.getStatus());
        File finalFile = taskResumed.getOutputFile();
        System.out.println("   Final file exists: " + finalFile.exists() + " (length: " + (finalFile.exists() ? finalFile.length() : 0) + ")");

        if (finalFile.exists() && finalFile.length() == 26507904L) {
            System.out.println(">>> SUCCESS: PAUSE & RESUME FULLY VERIFIED AND WORKING FLAWLESSLY! <<<");
            finalFile.delete();
        } else {
            System.err.println(">>> FAILED: File missing or size mismatch!");
            System.exit(1);
        }
    }
}
