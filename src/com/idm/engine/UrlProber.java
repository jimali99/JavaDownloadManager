package com.idm.engine;

import com.idm.util.FormatUtils;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlProber {

    public static class ProbeResult {
        private final String finalUrl;
        private final String fileName;
        private final long contentLength;
        private final boolean resumeSupported;
        private final String contentType;

        public ProbeResult(String finalUrl, String fileName, long contentLength, boolean resumeSupported, String contentType) {
            this.finalUrl = finalUrl;
            this.fileName = fileName;
            this.contentLength = contentLength;
            this.resumeSupported = resumeSupported;
            this.contentType = contentType;
        }

        public String getFinalUrl() {
            return finalUrl;
        }

        public String getFileName() {
            return fileName;
        }

        public long getContentLength() {
            return contentLength;
        }

        public boolean isResumeSupported() {
            return resumeSupported;
        }

        public String getContentType() {
            return contentType;
        }
    }

    public static boolean isYouTubeUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase().trim();
        return lower.contains("youtube.com/watch") ||
               lower.contains("youtu.be/") ||
               lower.contains("youtube.com/shorts/") ||
               lower.contains("youtube.com/embed/") ||
               lower.contains("youtube.com/live/");
    }

    public static String cleanYouTubeUrl(String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        try {
            // Match watch?v=VIDEO_ID (11 alphanumeric / dash / underscore chars)
            Pattern pWatch = Pattern.compile("https?://(?:www\\.)?youtube\\.com/watch\\?[^\"'\\s]*v=([a-zA-Z0-9_-]{11})");
            Matcher mWatch = pWatch.matcher(trimmed);
            if (mWatch.find()) {
                return "https://www.youtube.com/watch?v=" + mWatch.group(1);
            }
            // Match youtu.be/VIDEO_ID
            Pattern pShort = Pattern.compile("https?://youtu\\.be/([a-zA-Z0-9_-]{11})");
            Matcher mShort = pShort.matcher(trimmed);
            if (mShort.find()) {
                return "https://www.youtube.com/watch?v=" + mShort.group(1);
            }
            // Match shorts/VIDEO_ID
            Pattern pShorts = Pattern.compile("https?://(?:www\\.)?youtube\\.com/shorts/([a-zA-Z0-9_-]{11})");
            Matcher mShorts = pShorts.matcher(trimmed);
            if (mShorts.find()) {
                return "https://www.youtube.com/watch?v=" + mShorts.group(1);
            }
        } catch (Exception ignored) {}
        return trimmed;
    }

    public static ProbeResult probe(String targetUrl, String userAgent) throws IOException {
        String currentUrl = targetUrl;
        HttpURLConnection conn = null;
        int redirects = 0;
        final int MAX_REDIRECTS = 10;

        while (redirects < MAX_REDIRECTS) {
            URL url = new URL(currentUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", userAgent != null ? userAgent : "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Connection", "keep-alive");
            // Test byte range support by requesting byte 0-0
            conn.setRequestProperty("Range", "bytes=0-0");

            int responseCode = conn.getResponseCode();

            // Handle redirect codes (301, 302, 303, 307, 308)
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 307 || responseCode == 308) {

                String location = conn.getHeaderField("Location");
                if (location == null || location.isEmpty()) {
                    break;
                }
                URL base = new URL(currentUrl);
                currentUrl = new URL(base, location).toExternalForm();
                conn.disconnect();
                redirects++;
                continue;
            }

            // Normal response (200 OK or 206 Partial Content)
            boolean resumeSupported = false;
            long contentLength = -1;

            if (responseCode == HttpURLConnection.HTTP_PARTIAL) { // 206
                resumeSupported = true;
                // Parse Content-Range header: bytes 0-0/1234567
                String contentRange = conn.getHeaderField("Content-Range");
                if (contentRange != null) {
                    Pattern p = Pattern.compile("/(\\d+)$");
                    Matcher m = p.matcher(contentRange);
                    if (m.find()) {
                        try {
                            contentLength = Long.parseLong(m.group(1));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } else if (responseCode == HttpURLConnection.HTTP_OK) { // 200
                String acceptRanges = conn.getHeaderField("Accept-Ranges");
                if ("bytes".equalsIgnoreCase(acceptRanges)) {
                    resumeSupported = true;
                }
                contentLength = conn.getContentLengthLong();
            }

            if (contentLength <= 0) {
                contentLength = conn.getContentLengthLong();
            }

            String contentType = conn.getContentType();
            String contentDisposition = conn.getHeaderField("Content-Disposition");
            String fileName = extractFileName(currentUrl, contentDisposition, contentType);

            conn.disconnect();
            return new ProbeResult(currentUrl, fileName, contentLength, resumeSupported, contentType);
        }

        throw new IOException("Too many redirects: " + targetUrl);
    }

    public static String extractFileNameFromUrl(String urlStr) {
        if (urlStr == null || urlStr.trim().isEmpty()) {
            return "download_" + System.currentTimeMillis();
        }
        String cleanUrl = urlStr.trim();
        // Remove query parameters and fragment
        int qIdx = cleanUrl.indexOf('?');
        if (qIdx != -1) {
            cleanUrl = cleanUrl.substring(0, qIdx);
        }
        int hashIdx = cleanUrl.indexOf('#');
        if (hashIdx != -1) {
            cleanUrl = cleanUrl.substring(0, hashIdx);
        }

        String fileName = null;
        int lastSlash = Math.max(cleanUrl.lastIndexOf('/'), cleanUrl.lastIndexOf('\\'));
        if (lastSlash != -1 && lastSlash < cleanUrl.length() - 1) {
            try {
                fileName = URLDecoder.decode(cleanUrl.substring(lastSlash + 1), StandardCharsets.UTF_8.name());
            } catch (Exception ignored) {
                fileName = cleanUrl.substring(lastSlash + 1);
            }
        }

        if (fileName != null && !fileName.trim().isEmpty() && !fileName.trim().equals("/")) {
            return FormatUtils.sanitizeFileName(fileName.trim());
        }
        return "download_" + System.currentTimeMillis();
    }

    private static String extractFileName(String urlStr, String contentDisposition, String contentType) {
        String fileName = null;

        // 1. Try parsing Content-Disposition
        if (contentDisposition != null) {
            // Check for UTF-8 encoded filename: filename*=UTF-8''...
            Pattern utf8Pattern = Pattern.compile("filename\\*\\s*=\\s*UTF-8''([^;]+)", Pattern.CASE_INSENSITIVE);
            Matcher utf8Matcher = utf8Pattern.matcher(contentDisposition);
            if (utf8Matcher.find()) {
                try {
                    fileName = URLDecoder.decode(utf8Matcher.group(1).trim(), StandardCharsets.UTF_8.name());
                } catch (Exception ignored) {}
            }

            if (fileName == null) {
                Pattern standardPattern = Pattern.compile("filename\\s*=\\s*\"?([^;\"]+)\"?", Pattern.CASE_INSENSITIVE);
                Matcher standardMatcher = standardPattern.matcher(contentDisposition);
                if (standardMatcher.find()) {
                    fileName = standardMatcher.group(1).trim();
                }
            }
        }

        // 2. Try extracting from URL path
        if (fileName == null || fileName.isEmpty()) {
            String fromUrl = extractFileNameFromUrl(urlStr);
            if (!fromUrl.startsWith("download_")) {
                fileName = fromUrl;
            }
        }

        // 3. Fallback to default name if still empty
        if (fileName == null || fileName.isEmpty() || fileName.equals("/")) {
            fileName = "download_" + System.currentTimeMillis();
        }

        // If filename does not have an extension, try guessing from contentType or URL
        if (!fileName.contains(".") && contentType != null) {
            String ext = guessExtension(contentType, urlStr);
            if (ext != null) {
                fileName += ext;
            }
        }

        return FormatUtils.sanitizeFileName(fileName);
    }

    public static String guessExtension(String contentType, String fallbackUrl) {
        if (contentType == null) return null;
        String cleanType = contentType.split(";")[0].trim().toLowerCase();
        switch (cleanType) {
            case "application/x-msdownload":
            case "application/x-msdos-program":
            case "application/x-exe":
            case "application/exe":
            case "application/vnd.microsoft.portable-executable":
                return ".exe";
            case "application/x-msi":
                return ".msi";
            case "application/java-archive":
                return ".jar";
            case "application/zip":
            case "application/x-zip-compressed":
                return ".zip";
            case "application/x-rar-compressed":
            case "application/vnd.rar":
                return ".rar";
            case "application/x-7z-compressed":
                return ".7z";
            case "application/pdf":
                return ".pdf";
            case "application/msword":
                return ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return ".docx";
            case "application/x-iso9660-image":
                return ".iso";
            case "application/vnd.android.package-archive":
                return ".apk";
            case "application/gzip":
            case "application/x-gzip":
                return ".gz";
            case "application/x-tar":
            case "application/tar":
                return ".tar";
            case "video/mp4":
                return ".mp4";
            case "video/x-matroska":
                return ".mkv";
            case "video/webm":
                return ".webm";
            case "audio/mpeg":
                return ".mp3";
            case "audio/wav":
            case "audio/x-wav":
                return ".wav";
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "text/html":
                return ".html";
            case "text/plain":
                return ".txt";
            case "application/octet-stream":
                if (fallbackUrl != null) {
                    String lowerUrl = fallbackUrl.toLowerCase();
                    if (lowerUrl.contains(".exe")) return ".exe";
                    if (lowerUrl.contains(".msi")) return ".msi";
                    if (lowerUrl.contains(".zip")) return ".zip";
                    if (lowerUrl.contains(".jar")) return ".jar";
                    if (lowerUrl.contains(".iso")) return ".iso";
                    if (lowerUrl.contains(".dmg")) return ".dmg";
                }
                return ".bin";
            default:
                return null;
        }
    }
}
