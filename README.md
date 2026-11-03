<p align="center">
  <img src="assets/logo-banner.png" alt="JDM - Java Download Manager" width="850">
</p>

<h1 align="center">JDM — Java Download Manager (Accelerated Pro)</h1>

<p align="center">
  <strong>High-Performance Multi-Threaded File Download Accelerator for Windows</strong><br>
  <em>Accelerate download speeds up to 5x-10x with dynamic connection segmentation, zero-copy assembly, and Chrome browser integration.</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-26%20%7C%2017%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Platform-Windows-0078D6?style=for-the-badge&logo=windows&logoColor=white" alt="Windows">
  <img src="https://img.shields.io/badge/Chrome_Extension-Manifest_V3-4285F4?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Chrome Extension">
  <img src="https://img.shields.io/badge/UI-FlatLaf%20Modern%20Dark-10b981?style=for-the-badge" alt="FlatLaf">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License">
</p>

---

## ⚡ Why Does It Download So Fast?

Traditional web browsers download files using a **single TCP connection**, which is severely bottlenecked by:
1. TCP window scaling limits and latency over long distances.
2. Server-side per-connection bandwidth throttling.
3. Network congestion on individual streams.

### JDM Dynamic File Segmentation:
1. **Parallel Segment Acceleration (1 to 32 Connections):**
   - Probes the server with HTTP Range requests (`Accept-Ranges: bytes` & HTTP 206 Partial Content).
   - Divides the file into $N$ equal chunks (e.g. 8, 16, or 32 parallel connections).
   - Concurrently downloads each chunk through separate simultaneous HTTP streams, multiplying throughput to max out your internet bandwidth.

2. **Zero-Copy Instant Assembly (`java.nio.channels.FileChannel`):**
   - Unlike naive downloaders that download `.part1`, `.part2` files and spend several minutes merging them at the end, this application pre-allocates the file and uses thread-safe positioned writes (`FileChannel.write(buffer, offset)`).
   - The file is assembled **in real-time as bytes arrive**. The moment the last byte finishes downloading, the file is ready instantly!

3. **Rock-Solid Pause & Resume:**
   - Tracks exact downloaded offsets for every active chunk in a `.idm_meta` state file.
   - You can pause downloads, close the app, shut down your PC, and resume right where you left off.

---

## 🚀 Key Features Matching JDM

- **JDM Visual Segment Acceleration Bar:**
  - Iconic real-time visualization showing live progress and colors for every connection chunk.
  - Per-thread inspection table showing Thread #, Range, Downloaded Bytes, %, Speed, and Status.
- **Modern Polished Interface:**
  - Official custom JDM logo, window icons, and system tray branding.
  - Modern Windows 11 rounded dark theme powered by FlatLaf.
  - Interactive table with embedded progress capsules, transfer speed, and ETA countdown.
  - Category sidebar: Compressed (.zip, .rar, .iso), Documents (.pdf, .doc), Videos (.mp4, .mkv), Music (.mp3), Programs (.exe, .msi), and Custom.
- **Smart Queue & Scheduling:**
  - Limit max concurrent active downloads (e.g., 3 active) while others wait in queue.
  - Auto-start next task upon completion.
  - Pause All / Resume All buttons.
- **Clipboard URL Sniffer:**
  - Background monitor that automatically catches copied download links and prompts you to download immediately.
- **Completion Chimes & System Tray:**
  - Synthesized pleasant completion chime.
  - Minimizes cleanly to Windows System Tray.
- **Customizable Speed Limiter & User-Agent:**
  - Set global speed cap (e.g. 500 KB/s or unlimited).
  - Uses modern browser User-Agent so CDNs and Cloudflare don't throttle or block automated requests.

---

## 🖥️ How to Run & Use

### Option 1: One-Click Run (Easiest)
Simply double-click **`run.bat`** in the project folder, or run:
```cmd
run.bat
```
*(Uses `javaw.exe` so the application opens cleanly without an ugly command prompt console window).*

You can also directly double-click **`IDM-Java.jar`**.

### Option 2: Create Desktop Shortcut
Right-click `create_desktop_shortcut.ps1` and select **"Run with PowerShell"**, or execute in PowerShell:
```powershell
powershell -ExecutionPolicy Bypass -File .\create_desktop_shortcut.ps1
```
A shortcut named **"Java Download Manager (JDM Pro)"** will appear on your Windows Desktop.

### Option 3: Command Line (CLI) Download
You can pass any URL directly to download it:
```cmd
run.bat https://example.com/file.zip
```

---

## 🌐 Chrome Extension (JDM Integration Module)

A dedicated Chrome Extension (Manifest V3) is included in `chrome-extension/`. It intercepts browser downloads and adds right-click context menus so you can download files with JDM anytime with a single click.

### Features of the Extension:
* **🎬 Automatic Video & Media Stream Sniffer:**
  * Displays IDM's iconic floating **"⬇ Download this video"** button directly on video players across the web.
  * Real-time network sniffer detects streaming media (`.mp4`, `.webm`, `.mkv`, `.m4v`, `.flv`, `.ts`, HLS `.m3u8` playlists).
  * Extension badge shows the count of detected videos on the active tab (e.g. `1`, `2`).
  * Popup includes a dedicated **"Videos on this Page"** card with one-click download buttons for each detected stream.
* **Automatic Download Interception:** When you click any download link in Chrome, the extension automatically catches it, pauses Chrome's slow single-thread download, and redirects it to JDM for multi-thread acceleration.
* **Right-Click Context Menus:**
  * Right-click any link ➔ **"Download with JDM"**
  * Right-click any video, audio, or image ➔ **"Download video / media with JDM"**
  * Right-click selected text with a URL ➔ **"Download selected link with JDM"**
* **Popup Control Center:**
  * Live status indicator: 🟢 Connected to JDM / 🔴 JDM Offline
  * Real-time active download counter and aggregate download speed
  * Quick URL download box (with clipboard paste support)
  * One-click toggle switch to enable/disable automatic interception
  * "Open JDM Window" button

### 30-Second Installation in Chrome:
1. Double-click `install_chrome_extension.bat` (or open `chrome://extensions` in Google Chrome).
2. Toggle on **Developer mode** in the top-right corner of the Extensions page.
3. Click the **"Load unpacked"** button in the top-left corner.
4. Select the `chrome-extension` folder inside this repository:
   ```text
   JavaDownloadManager\chrome-extension
   ```
5. Done! The extension is active and will immediately route downloads to JDM whenever JDM is running.

---

## 🛠️ How to Rebuild from Source

If you ever modify the Java source files in `src/`, simply run:
```cmd
build.bat
```
It will recompile with `javac` and generate an updated self-contained `IDM-Java.jar`.

---

## 📁 Project Structure

```
JavaDownloadManager/
├── IDM-Java.jar                 # Standalone executable FAT-JAR (bundled with FlatLaf & icons)
├── assets/                      # High-res logos and banners for repository
│   ├── logo-banner.png          # Hero presentation banner (1024x558)
│   └── logo-square.png          # Square focused brand emblem
├── resources/                   # Application icons (16px to 256px)
├── chrome-extension/            # Chrome Extension (Manifest V3)
│   ├── manifest.json
│   ├── background.js            # Download interceptor & media sniffer
│   ├── content.js               # Floating video download button
│   ├── popup.html               # Extension UI & video downloader
│   └── icons/                   # Chrome extension icons
├── run.bat                      # Silent launcher script
├── build.bat                    # One-click compiler & packager
├── push_to_github.bat           # 1-Click sync to GitHub repository
├── create_desktop_shortcut.ps1  # Windows desktop shortcut creator
├── flatlaf-3.5.4.jar            # Modern UI theme library
└── src/
    └── com/idm/
        ├── Main.java                        # High-DPI entry point
        ├── model/                           # Task models and state management
        ├── engine/                          # Multi-thread FileChannel accelerated engine
        ├── ui/                              # Modern FlatLaf dark interface and icons
        │   ├── AppIcons.java                # Vector and resource icon loader
        │   ├── MainFrame.java               # Main JDM window with toolbar & categories
        │   ├── DownloadDetailDialog.java    # Real-time inspection window
        │   ├── SegmentVisualizerPanel.java  # Visual connection chunk bar
        │   └── SettingsDialog.java          # Preferences dialog
        └── util/                            # Sound, clipboard, and format helpers
```
