package com.idm.engine;

import com.idm.model.DownloadSegment;
import com.idm.model.DownloadTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class SegmentWorker implements Runnable {

    private final DownloadTask task;
    private final DownloadSegment segment;
    private final FileChannel fileChannel;
    private final String userAgent;
    private final int speedLimitKBps;
    private volatile boolean cancelled = false;
    private volatile boolean paused = false;
    private HttpURLConnection connection;

    public SegmentWorker(DownloadTask task, DownloadSegment segment, FileChannel fileChannel, String userAgent, int speedLimitKBps) {
        this.task = task;
        this.segment = segment;
        this.fileChannel = fileChannel;
        this.userAgent = userAgent;
        this.speedLimitKBps = speedLimitKBps;
    }

    public void pause() {
        this.paused = true;
        closeConnection();
    }

    public void cancel() {
        this.cancelled = true;
        closeConnection();
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void run() {
        if (segment.isCompleted()) {
            segment.setStatus("Completed");
            return;
        }

        segment.setStatus("Connecting...");
        InputStream in = null;
        try {
            long current = segment.getCurrentByte();
            long end = segment.getEndByte();

            if (end > 0 && current > end) {
                segment.setCompleted(true);
                segment.setStatus("Completed");
                return;
            }

            URL url = new URL(task.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", userAgent != null ? userAgent : "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Connection", "keep-alive");

            if (task.isResumeSupported() || end > 0 || current > 0) {
                if (end > 0) {
                    connection.setRequestProperty("Range", "bytes=" + current + "-" + end);
                } else if (current > 0) {
                    connection.setRequestProperty("Range", "bytes=" + current + "-");
                }
            }

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                throw new RuntimeException("Server returned HTTP " + code + " for segment " + segment.getId());
            }
            if (code == HttpURLConnection.HTTP_OK && current > 0) {
                throw new RuntimeException("Server ignored Range request for segment " + segment.getId() + " and returned full file (HTTP 200).");
            }

            in = connection.getInputStream();
            segment.setStatus("Downloading");

            // 64KB buffer for high throughput
            byte[] buffer = new byte[65536];
            long lastSpeedCheckTime = System.currentTimeMillis();
            long bytesSinceSpeedCheck = 0;

            long writePosition = current;

            while (!cancelled && !paused) {
                long bytesToRead = (end > 0) ? Math.min(buffer.length, (end - writePosition) + 1) : buffer.length;
                if (end > 0 && bytesToRead <= 0) {
                    // Reached end of segment
                    segment.setCompleted(true);
                    break;
                }

                int read = in.read(buffer, 0, (int) bytesToRead);
                if (read == -1) {
                    // End of stream
                    segment.setCompleted(true);
                    break;
                }

                // Concurrent write at absolute position to pre-allocated file
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, read);
                while (byteBuffer.hasRemaining()) {
                    int written = fileChannel.write(byteBuffer, writePosition);
                    writePosition += written;
                }

                segment.setCurrentByte(writePosition);
                bytesSinceSpeedCheck += read;

                // Speed calculation for this segment
                long now = System.currentTimeMillis();
                long elapsed = now - lastSpeedCheckTime;
                if (elapsed >= 500) {
                    double segSpeed = (bytesSinceSpeedCheck * 1000.0) / elapsed;
                    segment.setCurrentSpeed(segSpeed);
                    lastSpeedCheckTime = now;
                    bytesSinceSpeedCheck = 0;
                }

                // Speed limiter if enabled
                if (speedLimitKBps > 0) {
                    long targetSpeedBytesPerSec = (speedLimitKBps * 1024L) / task.getNumConnections();
                    if (targetSpeedBytesPerSec > 0) {
                        double expectedMs = (read * 1000.0) / targetSpeedBytesPerSec;
                        if (expectedMs > 1) {
                            try {
                                Thread.sleep((long) expectedMs);
                            } catch (InterruptedException ignored) {
                                break;
                            }
                        }
                    }
                }
            }

            if (segment.isCompleted()) {
                segment.setStatus("Completed");
                segment.setCurrentSpeed(0.0);
            } else if (paused) {
                segment.setStatus("Paused");
                segment.setCurrentSpeed(0.0);
            } else if (cancelled) {
                segment.setStatus("Cancelled");
                segment.setCurrentSpeed(0.0);
            }

        } catch (Exception e) {
            if (!paused && !cancelled) {
                segment.setStatus("Error: " + e.getMessage());
                segment.setCurrentSpeed(0.0);
                throw new RuntimeException("Worker " + segment.getId() + " failed: " + e.getMessage(), e);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {}
            }
            closeConnection();
        }
    }
}
