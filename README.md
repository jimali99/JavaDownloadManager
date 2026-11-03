# Internet Download Manager (IDM Pro - Java Accelerated Edition)

A high-performance, multi-threaded file download manager written in pure Java (Java 26 / 11+) designed specifically for Windows PCs to accelerate download speeds up to 5x-10x, exactly like the iconic **Internet Download Manager (IDM)**.

---

## ⚡ Why Does It Download So Fast?

Traditional web browsers download files using a **single TCP connection**, which is severely bottlenecked by:
1. TCP window scaling limits and latency over long distances.
2. Server-side per-connection bandwidth throttling.
3. Network congestion on individual streams.

### IDM Dynamic File Segmentation:
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

## 🚀 Key Features Matching IDM

- **IDM Visual Segment Acceleration Bar:**
  - Iconic real-time visualization showing live progress and colors for every connection chunk.
  - Per-thread inspection table showing Thread #, Range, Downloaded Bytes, %, Speed, and Status.
- **Modern Polished Interface:**
  - Modern Windows 11 rounded dark theme powered by FlatLaf.
  - Interactive table with embedded progress bars, transfer speed, and ETA countdown.
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
  - Uses modern browser User-Agent so CDNs and cloudflare don't throttle or block automated requests.

---

## 🖥️ How to Run & Use

### Option 1: One-Click Run (Easiest)
Simply double-click **`run.bat`** in this folder:
```
C:\Users\jim\.gemini\antigravity\scratch\JavaDownloadManager\run.bat
```
*(Uses `javaw.exe` so the application opens cleanly without an ugly command prompt console window).*

You can also directly double-click **`IDM-Java.jar`**.

### Option 2: Create Desktop Shortcut
Right-click `create_desktop_shortcut.ps1` and select **"Run with PowerShell"**, or execute in PowerShell:
```powershell
powershell -ExecutionPolicy Bypass -File "C:\Users\jim\.gemini\antigravity\scratch\JavaDownloadManager\create_desktop_shortcut.ps1"
```
A shortcut named **"Internet Download Manager (Java)"** will appear on your Windows Desktop.

### Option 3: Command Line (CLI) Download
You can pass any URL directly to download it:
```cmd
run.bat https://example.com/file.zip
```

---

## 🌐 Chrome Extension (IDM Integration Module)

A dedicated Chrome Extension (Manifest V3) is included in `chrome-extension/`. It intercepts browser downloads and adds right-click context menus so you can download files with IDM anytime with a single click.

### Features of the Extension:
* **🎬 Automatic Video & Media Stream Sniffer:**
  * Displays IDM's iconic floating **"⬇ Download this video"** button directly on video players across the web.
  * Real-time network sniffer detects streaming media (`.mp4`, `.webm`, `.mkv`, `.m4v`, `.flv`, `.ts`, HLS `.m3u8` playlists).
  * Extension badge shows the count of detected videos on the active tab (e.g. `1`, `2`).
  * Popup includes a dedicated **"Videos on this Page"** card with one-click download buttons for each detected stream.
* **Automatic Download Interception:** When you click any download link in Chrome, the extension automatically catches it, pauses Chrome's slow single-thread download, and redirects it to IDM for multi-thread acceleration.
* **Right-Click Context Menus:**
  * Right-click any link ➔ **"Download with IDM"**
  * Right-click any video, audio, or image ➔ **"Download video / media with IDM"**
  * Right-click selected text with a URL ➔ **"Download selected link with IDM"**
* **Popup Control Center:**
  * Live status indicator: 🟢 Connected to IDM / 🔴 IDM Offline
  * Real-time active download counter and aggregate download speed
  * Quick URL download box (with clipboard paste support)
  * One-click toggle switch to enable/disable automatic interception
  * "Open IDM Window" button

### 30-Second Installation in Chrome:
1. Double-click `install_chrome_extension.bat` (or open `chrome://extensions` in Google Chrome).
2. Toggle on **Developer mode** in the top-right corner of the Extensions page.
3. Click the **"Load unpacked"** button in the top-left corner.
4. Select the folder:
   ```
   C:\Users\jim\.gemini\antigravity\scratch\JavaDownloadManager\chrome-extension
   ```
5. Done! The extension is active and will immediately route downloads to IDM whenever IDM is running.

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
├── IDM-Java.jar                 # Standalone executable FAT-JAR (bundled with FlatLaf)
├── run.bat                      # Silent launcher script
├── build.bat                    # One-click compiler & packager
├── create_desktop_shortcut.ps1  # Windows desktop shortcut creator
├── flatlaf-3.5.4.jar            # Modern UI theme library
├── bin/                         # Compiled bytecodes
└── src/
    └── com/idm/
        ├── Main.java                        # High-DPI entry point
        ├── model/
        │   ├── DownloadTask.java            # Task state, listeners, file metadata
        │   ├── DownloadSegment.java         # Connection chunk boundaries & status
        │   ├── DownloadStatus.java          # State enum (Downloading, Paused, Completed)
        │   ├── DownloadCategory.java        # Extension-based category organizer
        │   └── AppSettings.java             # User preferences & persistence
        ├── engine/
        │   ├── DownloadEngine.java          # Multi-thread coordinator
        │   ├── SegmentWorker.java           # FileChannel concurrent stream worker
        │   ├── DownloadQueueManager.java    # Queue concurrency scheduler
        │   ├── SpeedCalculator.java         # Rolling EMA speed & ETA calculation
        │   └── UrlProber.java               # HTTP redirect & Accept-Ranges probing
        ├── ui/
        │   ├── MainFrame.java               # Main IDM window with toolbar & categories
        │   ├── DownloadDetailDialog.java    # Real-time inspection window
        │   ├── SegmentVisualizerPanel.java  # Visual connection chunk bar
        │   ├── AddDownloadDialog.java       # Add URL dialog with server probe
        │   ├── SettingsDialog.java          # Preferences dialog
        │   ├── DownloadTableModel.java      # JTable model
        │   ├── ProgressCellRenderer.java    # Table cell progress bar renderer
        │   └── ThemeManager.java            # FlatLaf dark theme styling
        └── util/
            ├── FormatUtils.java             # Byte/speed/time formatting
            ├── SoundUtils.java              # Pure Java audio chime generator
            ├── StorageUtils.java            # Task history and resume metadata I/O
            └── ClipboardMonitor.java        # Clipboard link listener
```
