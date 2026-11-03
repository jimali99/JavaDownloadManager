const IDM_API_URL = "http://127.0.0.1:9614";

const statusBadge = document.getElementById("statusBadge");
const statusText = document.getElementById("statusText");
const activeCount = document.getElementById("activeCount");
const totalSpeed = document.getElementById("totalSpeed");
const urlInput = document.getElementById("urlInput");
const btnPaste = document.getElementById("btnPaste");
const btnDownload = document.getElementById("btnDownload");
const chkIntercept = document.getElementById("chkIntercept");
const chkAutoStart = document.getElementById("chkAutoStart");
const btnOpenIdm = document.getElementById("btnOpenIdm");
const btnRefresh = document.getElementById("btnRefresh");
const toastMessage = document.getElementById("toastMessage");
const videoCount = document.getElementById("videoCount");
const videoList = document.getElementById("videoList");

document.addEventListener("DOMContentLoaded", async () => {
    // Load stored settings
    chrome.storage.local.get({
        interceptDownloads: true,
        autoStart: true
    }, (items) => {
        chkIntercept.checked = items.interceptDownloads;
        chkAutoStart.checked = items.autoStart;
    });

    // Save settings when changed
    chkIntercept.addEventListener("change", () => {
        chrome.storage.local.set({ interceptDownloads: chkIntercept.checked });
        showToast(chkIntercept.checked ? "Chrome download interception enabled" : "Interception disabled");
    });

    chkAutoStart.addEventListener("change", () => {
        chrome.storage.local.set({ autoStart: chkAutoStart.checked });
    });

    // Check IDM status
    checkStatus();

    // Load detected videos on active tab
    loadDetectedVideos();

    // Auto-fill URL if current tab has a downloadable link
    try {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (tab && tab.url && (tab.url.startsWith("http://") || tab.url.startsWith("https://"))) {
            if (isLikelyDownloadUrl(tab.url)) {
                urlInput.value = tab.url;
            }
        }
    } catch (e) {}

    // Event listeners
    btnPaste.addEventListener("click", async () => {
        try {
            const text = await navigator.clipboard.readText();
            if (text) {
                urlInput.value = text.trim();
                urlInput.focus();
            }
        } catch (e) {
            showToast("Clipboard access denied. Please paste manually (Ctrl+V).");
        }
    });

    btnDownload.addEventListener("click", () => {
        startDownload();
    });

    urlInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
            startDownload();
        }
    });

    btnOpenIdm.addEventListener("click", async () => {
        try {
            await fetch(`${IDM_API_URL}/bring-to-front`);
            window.close();
        } catch (e) {
            showToast("IDM is not running. Please start IDM on your PC.");
        }
    });

    btnRefresh.addEventListener("click", () => {
        checkStatus();
        loadDetectedVideos();
    });
});

async function loadDetectedVideos() {
    try {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (!tab || !tab.id) return;

        chrome.runtime.sendMessage({ type: "GET_TAB_MEDIA", tabId: tab.id }, (response) => {
            const list = (response && response.media) ? response.media : [];
            videoCount.textContent = list.length;

            if (list.length === 0) {
                videoList.innerHTML = `<div class="no-videos">No active video streams detected yet.<br>Play a video on this page to capture it.</div>`;
                return;
            }

            videoList.innerHTML = "";
            list.forEach((media, i) => {
                const card = document.createElement("div");
                card.className = "video-card";

                let format = (media.mime || "").split("/")[1] || "MP4";
                if (media.url && media.url.includes(".m3u8")) format = "HLS";
                format = format.toUpperCase();

                let sizeStr = "";
                if (media.sizeBytes && media.sizeBytes > 0) {
                    sizeStr = formatBytes(media.sizeBytes);
                }

                card.innerHTML = `
                    <div class="video-card-info">
                        <span class="video-card-title" title="${escapeHtml(media.url)}">${escapeHtml(media.filename || 'Video ' + (i + 1))}</span>
                        <div class="video-card-meta">
                            <span class="badge-format">${escapeHtml(format)}</span>
                            ${sizeStr ? `<span>${sizeStr}</span>` : ""}
                        </div>
                    </div>
                    <button class="btn-video-dl">⬇ Download</button>
                `;

                const dlBtn = card.querySelector(".btn-video-dl");
                dlBtn.addEventListener("click", () => {
                    dlBtn.disabled = true;
                    dlBtn.textContent = "Sending...";
                    chrome.runtime.sendMessage({
                        type: "DOWNLOAD_MEDIA_ITEM",
                        url: media.url,
                        filename: media.filename
                    }, (res) => {
                        if (res && res.success) {
                            dlBtn.textContent = "✓ Sent!";
                            showToast("Video sent to IDM!");
                        } else {
                            dlBtn.textContent = "Offline";
                            showToast("IDM is offline. Start IDM on your PC.");
                        }
                    });
                });

                videoList.appendChild(card);
            });
        });
    } catch (e) {}
}

async function checkStatus() {
    setStatus("checking", "Checking...");
    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 1500);

        const res = await fetch(`${IDM_API_URL}/status`, {
            method: "GET",
            signal: controller.signal
        });
        clearTimeout(timeoutId);

        if (res.ok) {
            const data = await res.json();
            setStatus("online", "Connected");
            activeCount.textContent = data.activeTasks || 0;
            totalSpeed.textContent = data.totalSpeed || "0 KB/s";
        } else {
            setStatus("offline", "IDM Offline");
        }
    } catch (e) {
        setStatus("offline", "IDM Offline");
        activeCount.textContent = "0";
        totalSpeed.textContent = "0 KB/s";
    }
}

function setStatus(state, text) {
    statusBadge.className = `badge ${state}`;
    statusText.textContent = text;
}

async function startDownload() {
    const url = urlInput.value.trim();
    if (!url) {
        showToast("Please enter or paste a URL first.");
        urlInput.focus();
        return;
    }

    if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("ftp://")) {
        showToast("Invalid URL. Must begin with http://, https://, or ftp://");
        return;
    }

    btnDownload.disabled = true;
    btnDownload.innerHTML = "<span>⏳ Sending to IDM...</span>";

    try {
        const res = await fetch(`${IDM_API_URL}/add-download`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                url: url,
                autoStart: chkAutoStart.checked
            })
        });

        if (res.ok) {
            showToast("✓ Download sent to IDM!");
            urlInput.value = "";
            setTimeout(() => {
                window.close();
            }, 800);
        } else {
            showToast("Failed to add download. IDM returned an error.");
        }
    } catch (e) {
        showToast("Cannot connect to IDM. Make sure IDM is running on your PC.");
    } finally {
        btnDownload.disabled = false;
        btnDownload.innerHTML = "<span>⬇ Download with IDM</span>";
    }
}

function showToast(msg) {
    toastMessage.textContent = msg;
    toastMessage.classList.remove("hidden");
    setTimeout(() => {
        toastMessage.classList.add("hidden");
    }, 3000);
}

function isLikelyDownloadUrl(url) {
    const extensions = [
        ".zip", ".rar", ".7z", ".tar", ".gz", ".iso",
        ".exe", ".msi", ".dmg", ".apk", ".bin",
        ".mp4", ".mkv", ".avi", ".mov", ".webm",
        ".mp3", ".flac", ".wav",
        ".pdf", ".docx", ".xlsx"
    ];
    const cleanUrl = url.split("?")[0].toLowerCase();
    return extensions.some(ext => cleanUrl.endsWith(ext));
}

function formatBytes(bytes) {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + " MB";
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + " GB";
}

function escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
