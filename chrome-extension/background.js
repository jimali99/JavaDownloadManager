const IDM_API_URL = "http://127.0.0.1:9614";
const interceptedItems = new Set();
const tabMedia = new Map(); // tabId -> Array of detected media objects

// Known media extensions and mime patterns
const MEDIA_EXTENSIONS = /\.(mp4|webm|mkv|m4v|mov|flv|avi|ts|m3u8|mpd|mp3|m4a|aac|ogg|wav)(\?|$)/i;
const MEDIA_MIMES = [
    "video/mp4", "video/webm", "video/ogg", "video/quicktime", "video/x-flv",
    "video/x-matroska", "video/mp2t", "application/x-mpegurl",
    "application/vnd.apple.mpegurl", "application/dash+xml",
    "audio/mpeg", "audio/mp4", "audio/ogg", "audio/aac", "audio/wav"
];

// Initialize on installation
chrome.runtime.onInstalled.addListener(() => {
    chrome.storage.local.get({
        interceptDownloads: false,
        autoStart: false,
        detectVideos: true
    }, (settings) => {
        // Enforce safe defaults: do not automatically intercept browser downloads without user consent
        chrome.storage.local.set({
            interceptDownloads: false,
            autoStart: false,
            detectVideos: true
        });
    });

    createContextMenus();
    updateExtensionBadge();
});

function createContextMenus() {
    chrome.contextMenus.removeAll(() => {
        chrome.contextMenus.create({
            id: "idm_download_link",
            title: "Download with JDM",
            contexts: ["link"]
        });

        chrome.contextMenus.create({
            id: "idm_download_media",
            title: "Download video / media with JDM",
            contexts: ["video", "audio", "image"]
        });

        chrome.contextMenus.create({
            id: "idm_download_selection",
            title: "Download selected link with JDM",
            contexts: ["selection"]
        });

        chrome.contextMenus.create({
            id: "idm_download_page",
            title: "Download current page with JDM",
            contexts: ["page"]
        });
    });
}

// Handle Context Menu clicks
chrome.contextMenus.onClicked.addListener((info, tab) => {
    let targetUrl = null;

    if (info.menuItemId === "idm_download_link" && info.linkUrl) {
        targetUrl = info.linkUrl;
    } else if (info.menuItemId === "idm_download_media" && info.srcUrl) {
        targetUrl = info.srcUrl;
    } else if (info.menuItemId === "idm_download_page" && info.pageUrl) {
        targetUrl = info.pageUrl;
    } else if (info.menuItemId === "idm_download_selection" && info.selectionText) {
        const text = info.selectionText.trim();
        if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("ftp://")) {
            targetUrl = text;
        } else {
            notifyUser("Invalid Link", "Selected text is not a valid download URL.");
            return;
        }
    }

    if (targetUrl) {
        chrome.storage.local.get({ autoStart: true }, (settings) => {
            sendDownloadToIdm(targetUrl, "", settings.autoStart);
        });
    }
});

// Automatic Chrome Download Interception
chrome.downloads.onCreated.addListener(async (downloadItem) => {
    const settings = await chrome.storage.local.get({ interceptDownloads: false, autoStart: false });
    if (!settings.interceptDownloads) {
        return;
    }

    // CRITICAL: Strictly ignore completed, interrupted, or past downloads from history
    if (downloadItem.state && downloadItem.state !== "in_progress") {
        return;
    }

    // Ignore downloads that were created more than 4 seconds ago (restored from browser history)
    if (downloadItem.startTime) {
        const elapsed = Date.now() - new Date(downloadItem.startTime).getTime();
        if (elapsed > 4000) {
            return;
        }
    }

    const url = downloadItem.finalUrl || downloadItem.url;
    if (!url || typeof url !== "string") {
        return;
    }

    const lower = url.toLowerCase();
    if (lower.startsWith("blob:") || lower.startsWith("data:") || lower.startsWith("chrome:") || lower.startsWith("chrome-extension:")) {
        return;
    }

    if (interceptedItems.has(downloadItem.id)) {
        return;
    }
    interceptedItems.add(downloadItem.id);

    const isOnline = await checkIdmStatus();
    if (!isOnline) {
        console.log("JDM is offline; allowing Chrome to download normally.");
        return;
    }

    try {
        await chrome.downloads.cancel(downloadItem.id);
        chrome.downloads.erase({ id: downloadItem.id });

        const filename = downloadItem.filename ? downloadItem.filename.replace(/^.*[\\\/]/, '') : "";
        const sent = await sendDownloadToIdm(url, filename, settings.autoStart);
        if (sent) {
            notifyUser("JDM Download Started", `Transferred "${filename || 'File'}" to JDM.`);
        }
    } catch (err) {
        console.error("Error intercepting download:", err);
    }
});

// -------------------------------------------------------------
// VIDEO & STREAM SNIFFER (Web Request Monitoring)
// -------------------------------------------------------------
chrome.webRequest.onHeadersReceived.addListener(
    (details) => {
        if (details.tabId < 0) return;

        let contentType = "";
        let contentLength = "";
        let contentDisposition = "";

        if (details.responseHeaders) {
            for (const h of details.responseHeaders) {
                const name = h.name.toLowerCase();
                if (name === "content-type") {
                    contentType = h.value.toLowerCase().split(";")[0].trim();
                } else if (name === "content-length") {
                    contentLength = h.value;
                } else if (name === "content-disposition") {
                    contentDisposition = h.value;
                }
            }
        }

        const isMediaMime = MEDIA_MIMES.some(m => contentType.includes(m));
        const isMediaExt = MEDIA_EXTENSIONS.test(details.url);

        if (isMediaMime || isMediaExt) {
            // Filter out tiny audio blips or tiny images disguised as video
            const lengthBytes = parseInt(contentLength, 10);
            if (!isNaN(lengthBytes) && lengthBytes > 0 && lengthBytes < 150000 && !details.url.includes(".m3u8")) {
                return; // Ignore sub-150KB files unless playlist
            }

            registerDetectedMedia(details.tabId, {
                url: details.url,
                mime: contentType || guessMimeFromUrl(details.url),
                sizeBytes: lengthBytes || -1,
                filename: extractFilename(details.url, contentDisposition),
                timestamp: Date.now()
            });
        }
    },
    { urls: ["<all_urls>"] },
    ["responseHeaders"]
);

function registerDetectedMedia(tabId, media) {
    if (!tabMedia.has(tabId)) {
        tabMedia.set(tabId, []);
    }
    const list = tabMedia.get(tabId);

    // Avoid duplicate URLs
    if (list.some(m => m.url === media.url)) {
        return;
    }

    list.unshift(media);
    if (list.length > 50) {
        list.pop();
    }

    // Update tab badge with video count
    chrome.action.setBadgeText({ tabId: tabId, text: String(list.length) });
    chrome.action.setBadgeBackgroundColor({ tabId: tabId, color: "#2563eb" }); // Blue badge

    // Notify content script in tab
    chrome.tabs.sendMessage(tabId, {
        type: "IDM_MEDIA_DETECTED",
        media: media,
        allMedia: list
    }).catch(() => {});
}

function guessMimeFromUrl(url) {
    const clean = url.split("?")[0].toLowerCase();
    if (clean.endsWith(".mp4") || clean.endsWith(".m4v")) return "video/mp4";
    if (clean.endsWith(".webm")) return "video/webm";
    if (clean.endsWith(".mkv")) return "video/x-matroska";
    if (clean.endsWith(".m3u8")) return "application/x-mpegurl";
    if (clean.endsWith(".ts")) return "video/mp2t";
    if (clean.endsWith(".mp3")) return "audio/mpeg";
    if (clean.endsWith(".flv")) return "video/x-flv";
    return "video/media";
}

function extractFilename(url, contentDisposition) {
    if (contentDisposition) {
        const match = contentDisposition.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i);
        if (match && match[1]) {
            return decodeURIComponent(match[1].trim());
        }
    }
    try {
        const parsed = new URL(url);
        const segments = parsed.pathname.split("/").filter(Boolean);
        if (segments.length > 0) {
            const last = decodeURIComponent(segments[segments.length - 1]);
            if (last.includes(".")) {
                return last;
            }
        }
    } catch (e) {}
    return "video_" + Date.now() + ".mp4";
}

// Clean up tab media when tab is closed or navigated
chrome.tabs.onRemoved.addListener((tabId) => {
    tabMedia.delete(tabId);
});

chrome.tabs.onUpdated.addListener((tabId, changeInfo) => {
    if (changeInfo.status === "loading" && changeInfo.url) {
        tabMedia.delete(tabId);
        chrome.action.setBadgeText({ tabId: tabId, text: "" });
    }
});

// Messages from Content Script or Popup
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type === "GET_TAB_MEDIA") {
        const tId = message.tabId || (sender.tab && sender.tab.id);
        const list = tabMedia.get(tId) || [];
        sendResponse({ media: list });
        return true;
    }

    if (message.type === "DOWNLOAD_MEDIA_ITEM") {
        chrome.storage.local.get({ autoStart: true }, (settings) => {
            sendDownloadToIdm(message.url, message.filename || "", settings.autoStart)
                .then(success => sendResponse({ success }))
                .catch(() => sendResponse({ success: false }));
        });
        return true;
    }
});

// Send Download to IDM API
async function sendDownloadToIdm(url, filename = "", autoStart = true) {
    try {
        const response = await fetch(`${IDM_API_URL}/add-download`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                url: url,
                fileName: filename,
                autoStart: autoStart
            })
        });

        if (response.ok) {
            updateExtensionBadge(true);
            return true;
        } else {
            notifyUser("JDM Error", "JDM rejected the download request.");
            return false;
        }
    } catch (err) {
        notifyUser("JDM Not Running", "Please start Java Download Manager on your PC first.");
        updateExtensionBadge(false);
        return false;
    }
}

// Check IDM Health Status
async function checkIdmStatus() {
    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 1200);

        const res = await fetch(`${IDM_API_URL}/status`, {
            method: "GET",
            signal: controller.signal
        });
        clearTimeout(timeoutId);
        const ok = res.ok;
        updateExtensionBadge(ok);
        return ok;
    } catch (e) {
        updateExtensionBadge(false);
        return false;
    }
}

function updateExtensionBadge(isOnline = null) {
    if (isOnline === true) {
        chrome.action.setBadgeBackgroundColor({ color: "#22c55e" });
        chrome.action.setBadgeText({ text: "ON" });
    } else if (isOnline === false) {
        chrome.action.setBadgeBackgroundColor({ color: "#ef4444" });
        chrome.action.setBadgeText({ text: "OFF" });
    } else {
        checkIdmStatus();
    }
}

function notifyUser(title, message) {
    chrome.notifications.create({
        type: "basic",
        iconUrl: "icons/icon128.png",
        title: title,
        message: message,
        priority: 1
    });
}

// Periodic status check every 30 seconds
setInterval(checkIdmStatus, 30000);
