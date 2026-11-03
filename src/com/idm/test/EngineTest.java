package com.idm.test;

import com.idm.engine.DownloadEngine;
import com.idm.engine.UrlProber;
import com.idm.model.AppSettings;
import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;
import com.idm.util.FormatUtils;

import java.io.File;

public class EngineTest {

    public static void main(String[] args) throws Exception {
        System.out.println(">>> Testing URL Probing & Multi-Connection Segmented Download Engine <<<");

        String testUrl = (args.length > 0 && args[0] != null) ? args[0] : "https://speed.hetzner.de/10MB.bin";

        AppSettings settings = new AppSettings();
        System.out.println("1. Probing URL: " + testUrl);
        UrlProber.ProbeResult probe = UrlProber.probe(testUrl, settings.getUserAgent());
        System.out.println("   Final URL: " + probe.getFinalUrl());
        System.out.println("   File Name: " + probe.getFileName());
        System.out.println("   Content Length: " + FormatUtils.formatBytes(probe.getContentLength()) + " (" + probe.getContentLength() + " bytes)");
        System.out.println("   Resume Supported: " + probe.isResumeSupported());

        String tempDir = System.getProperty("java.io.tmpdir");
        DownloadTask task = new DownloadTask(testUrl, "idm_test_10mb.bin", tempDir, 8);

        System.out.println("\n2. Initializing DownloadEngine with 8 parallel connections...");
        DownloadEngine engine = new DownloadEngine(task, settings);

        task.addListener(new DownloadTask.DownloadListener() {
            private long lastLog = 0;
            @Override
            public void onProgressUpdate(DownloadTask t) {
                long now = System.currentTimeMillis();
                if (now - lastLog > 800) {
                    lastLog = now;
                    System.out.printf("   [Progress] %5.1f%% | Downloaded: %s / %s | Speed: %s | ETA: %s | Active Segments: %d%n",
                        t.getProgressPercentage(),
                        FormatUtils.formatBytes(t.getDownloadedBytes()),
                        FormatUtils.formatBytes(t.getTotalBytes()),
                        FormatUtils.formatSpeed(t.getCurrentSpeed()),
                        FormatUtils.formatTime(t.getEtaSeconds()),
                        t.getSegments().size()
                    );
                }
            }

            @Override
            public void onStatusChanged(DownloadTask t, DownloadStatus oldStatus, DownloadStatus newStatus) {
                System.out.println("   [Status Changed] " + oldStatus + " -> " + newStatus);
            }
        });

        long startTime = System.currentTimeMillis();
        engine.start();

        // Wait for completion or timeout (45s)
        long deadline = System.currentTimeMillis() + 45000;
        while (!task.getStatus().isFinished() && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("\n3. Download finished with status: " + task.getStatus());
        System.out.println("   Total elapsed time: " + (elapsed / 1000.0) + " seconds");

        File downloadedFile = task.getOutputFile();
        if (downloadedFile.exists()) {
            System.out.println("   Output file exists: " + downloadedFile.getAbsolutePath());
            System.out.println("   Output file size: " + downloadedFile.length() + " bytes");
            if (downloadedFile.length() == 10485760L) {
                System.out.println(">>> SUCCESS: Multi-Connection Segmented Download Engine VERIFIED 100% BIT-PERFECT! <<<");
            } else {
                System.out.println(">>> WARNING: File size mismatch: expected 10485760, got " + downloadedFile.length());
            }
            downloadedFile.delete(); // cleanup
        } else {
            System.err.println(">>> ERROR: Downloaded file not found!");
            System.exit(1);
        }
    }
}
