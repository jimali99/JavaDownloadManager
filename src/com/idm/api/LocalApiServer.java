package com.idm.api;

import com.idm.engine.DownloadQueueManager;
import com.idm.engine.UrlProber;
import com.idm.model.AppSettings;
import com.idm.model.DownloadTask;
import com.idm.ui.AddDownloadDialog;
import com.idm.ui.DownloadDetailDialog;
import com.idm.ui.MainFrame;
import com.idm.util.FormatUtils;

import javax.swing.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalApiServer {

    public static final int PORT = 9614;

    private final MainFrame mainFrame;
    private final DownloadQueueManager queueManager;
    private final AppSettings settings;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;

    public LocalApiServer(MainFrame mainFrame, DownloadQueueManager queueManager, AppSettings settings) {
        this.mainFrame = mainFrame;
        this.queueManager = queueManager;
        this.settings = settings;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        threadPool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "LocalApiWorker");
            t.setDaemon(true);
            return t;
        });

        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.bind(new InetSocketAddress("127.0.0.1", PORT));
                System.out.println("Local API Server listening on http://127.0.0.1:" + PORT);

                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        client.setSoTimeout(5000);
                        threadPool.submit(() -> handleClient(client));
                    } catch (IOException e) {
                        if (!running) break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Could not bind Local API Server on port " + PORT + ": " + e.getMessage());
            }
        }, "LocalApiServerThread");

        serverThread.setDaemon(true);
        serverThread.start();
    }

    public synchronized void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception ignored) {}
            serverSocket = null;
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
            threadPool = null;
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            // Read raw bytes until end of HTTP headers (\r\n\r\n)
            ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
            int b;
            int matched = 0;
            byte[] separator = new byte[]{'\r', '\n', '\r', '\n'};

            while ((b = in.read()) != -1) {
                headerBuffer.write(b);
                if (b == separator[matched]) {
                    matched++;
                    if (matched == 4) break;
                } else if (b == '\r') {
                    matched = 1;
                } else {
                    matched = 0;
                }
            }

            if (matched < 4) return; // Incomplete HTTP header stream

            byte[] headerBytes = headerBuffer.toByteArray();
            String headerText = new String(headerBytes, 0, headerBytes.length - 4, StandardCharsets.UTF_8);
            String[] lines = headerText.split("\r\n");
            if (lines.length == 0 || lines[0].isEmpty()) return;

            String[] parts = lines[0].split(" ");
            if (parts.length < 2) return;

            String method = parts[0].toUpperCase();
            String fullPath = parts[1];

            // Read HTTP headers
            Map<String, String> headers = new HashMap<>();
            int contentLength = 0;
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                int colon = line.indexOf(':');
                if (colon > 0) {
                    String hKey = line.substring(0, colon).trim().toLowerCase();
                    String hVal = line.substring(colon + 1).trim();
                    headers.put(hKey, hVal);
                    if ("content-length".equals(hKey)) {
                        try {
                            contentLength = Integer.parseInt(hVal);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // Read exact body byte count if POST, avoiding UTF-8 character deadlock
            String body = "";
            if (contentLength > 0) {
                byte[] bodyBytes = in.readNBytes(contentLength);
                body = new String(bodyBytes, StandardCharsets.UTF_8);
            }

            // Separate path and query
            String path = fullPath;
            String query = "";
            int qIdx = fullPath.indexOf('?');
            if (qIdx != -1) {
                path = fullPath.substring(0, qIdx);
                query = fullPath.substring(qIdx + 1);
            }

            // Dispatch endpoint
            if ("OPTIONS".equals(method)) {
                sendResponse(out, 204, "No Content", "text/plain", "");
            } else if ("/status".equals(path)) {
                handleStatus(out);
            } else if ("/bring-to-front".equals(path)) {
                handleBringToFront(out);
            } else if ("/add-download".equals(path)) {
                handleAddDownload(method, body, query, out);
            } else {
                sendResponse(out, 404, "Not Found", "application/json", "{\"error\":\"Not Found\"}");
            }

        } catch (Exception e) {
            System.err.println("[LocalApiServer] Request processing error: " + e.getMessage());
        }
    }

    private void handleStatus(OutputStream out) throws IOException {
        int active = 0;
        double speed = 0.0;
        for (DownloadTask t : queueManager.getTasks()) {
            if (t.getStatus().isActive()) {
                active++;
                speed += t.getCurrentSpeed();
            }
        }

        String json = String.format(
            "{\"status\":\"online\",\"version\":\"1.0.0\",\"activeTasks\":%d,\"totalSpeed\":\"%s\",\"totalSpeedRaw\":%.0f}",
            active,
            FormatUtils.formatSpeed(speed),
            speed
        );
        sendResponse(out, 200, "OK", "application/json", json);
    }

    private void handleBringToFront(OutputStream out) throws IOException {
        SwingUtilities.invokeLater(() -> {
            mainFrame.setVisible(true);
            mainFrame.setExtendedState(JFrame.NORMAL);
            mainFrame.toFront();
            mainFrame.requestFocus();
        });
        sendResponse(out, 200, "OK", "application/json", "{\"success\":true}");
    }

    private void handleAddDownload(String method, String body, String query, OutputStream out) throws IOException {
        String url = null;
        String fileName = null;
        String cookiesData = null;
        boolean autoStart = false; // Safe default: prompt user dialog, never silently download

        if ("POST".equalsIgnoreCase(method)) {
            url = extractJsonField(body, "url");
            fileName = extractJsonField(body, "fileName");
            cookiesData = extractJsonString(body, "cookies");
            String autoStr = extractJsonField(body, "autoStart");
            if (autoStr != null) {
                autoStart = "true".equalsIgnoreCase(autoStr);
            }
        } else if ("GET".equalsIgnoreCase(method) && query != null && !query.isEmpty()) {
            Map<String, String> params = parseQueryParams(query);
            url = params.get("url");
            fileName = params.get("fileName");
            if (params.containsKey("autoStart")) {
                autoStart = "true".equalsIgnoreCase(params.get("autoStart"));
            }
        }

        if (url == null || url.trim().isEmpty()) {
            sendResponse(out, 400, "Bad Request", "application/json", "{\"success\":false,\"error\":\"Missing URL parameter\"}");
            return;
        }

        // Save authenticated session cookies if passed from browser
        if (cookiesData != null && !cookiesData.trim().isEmpty()) {
            try {
                File tempCookieFile = new File(System.getProperty("java.io.tmpdir"), "jdm_yt_cookies.txt");
                java.nio.file.Files.writeString(tempCookieFile.toPath(), cookiesData.trim(), java.nio.charset.StandardCharsets.UTF_8);

                String appData = System.getenv("APPDATA");
                if (appData != null) {
                    File jdmDir = new File(appData, "JavaDownloadManager");
                    if (!jdmDir.exists()) jdmDir.mkdirs();
                    File permCookieFile = new File(jdmDir, "youtube_cookies.txt");
                    java.nio.file.Files.writeString(permCookieFile.toPath(), cookiesData.trim(), java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {}
        }

        final String downloadUrl = url.trim();

        if (UrlProber.isYouTubeUrl(downloadUrl)) {
            final String cleanYtUrl = UrlProber.cleanYouTubeUrl(downloadUrl);
            SwingUtilities.invokeLater(() -> {
                mainFrame.setVisible(true);
                mainFrame.setExtendedState(JFrame.NORMAL);
                mainFrame.toFront();
                AddDownloadDialog.launchYouTubeDownloader(cleanYtUrl);
            });
            sendResponse(out, 200, "OK", "application/json", "{\"success\":true,\"message\":\"YouTube video sent to JDM YouTube Engine\"}");
            return;
        }

        final String suggestedFileName = (fileName != null && !fileName.trim().isEmpty()) ? fileName.trim() : null;
        final boolean finalAutoStart = autoStart;

        SwingUtilities.invokeLater(() -> {
            mainFrame.setVisible(true);
            mainFrame.setExtendedState(JFrame.NORMAL);
            mainFrame.toFront();

            if (!finalAutoStart) {
                AddDownloadDialog dialog = new AddDownloadDialog(mainFrame, downloadUrl, suggestedFileName, queueManager, settings);
                dialog.setVisible(true);
            } else {
                new Thread(() -> {
                    String name = suggestedFileName;
                    if (name == null || name.isEmpty()) {
                        String fromUrl = UrlProber.extractFileNameFromUrl(downloadUrl);
                        if (!fromUrl.startsWith("download_")) {
                            name = fromUrl;
                        } else {
                            try {
                                UrlProber.ProbeResult probe = UrlProber.probe(downloadUrl, settings.getUserAgent());
                                name = probe.getFileName();
                            } catch (Exception e) {
                                name = fromUrl;
                            }
                        }
                    }
                    name = FormatUtils.sanitizeFileName(name);

                    DownloadTask task = new DownloadTask(
                        downloadUrl,
                        name,
                        settings.getDefaultDownloadDir(),
                        settings.getDefaultConnections()
                    );

                    queueManager.addTask(task, true);

                    SwingUtilities.invokeLater(() -> {
                        DownloadDetailDialog dialog = new DownloadDetailDialog(mainFrame, task, queueManager);
                        dialog.setVisible(true);
                    });
                }, "ApiDownloadStarter").start();
            }
        });

        sendResponse(out, 200, "OK", "application/json", "{\"success\":true,\"message\":\"Download accepted by IDM\"}");
    }

    private void sendResponse(OutputStream out, int code, String statusText, String contentType, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 " + code + " " + statusText + "\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                        "Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With\r\n" +
                        "Content-Type: " + contentType + "; charset=UTF-8\r\n" +
                        "Content-Length: " + bodyBytes.length + "\r\n" +
                        "Connection: close\r\n\r\n";

        out.write(header.getBytes(StandardCharsets.UTF_8));
        if (bodyBytes.length > 0) {
            out.write(bodyBytes);
        }
        out.flush();
    }

    private static String extractJsonString(String json, String key) {
        if (json == null) return null;
        String needle = "\"" + key + "\"";
        int keyIdx = json.indexOf(needle);
        if (keyIdx == -1) return null;
        int colonIdx = json.indexOf(':', keyIdx + needle.length());
        if (colonIdx == -1) return null;
        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart == -1) return null;

        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = quoteStart + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                if (c == 'n') sb.append('\n');
                else if (c == 'r') sb.append('\r');
                else if (c == 't') sb.append('\t');
                else if (c == '"') sb.append('"');
                else if (c == '\\') sb.append('\\');
                else if (c == '/') sb.append('/');
                else sb.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String extractJsonField(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(?:\"([^\"]*)\"|([^,}\\]\\s]+))");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            if (matcher.group(1) != null) {
                return matcher.group(1);
            } else {
                return matcher.group(2);
            }
        }
        return null;
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                try {
                    String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name());
                    String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name());
                    params.put(k, v);
                } catch (Exception ignored) {}
            }
        }
        return params;
    }
}
