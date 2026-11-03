// IDM Floating Video Button & Sniffer Script
(() => {
    let pageMedia = [];

    // Fetch already detected media from background worker
    chrome.runtime.sendMessage({ type: "GET_TAB_MEDIA" }, (response) => {
        if (response && response.media) {
            pageMedia = response.media;
            scanAndAttachButtons();
        }
    });

    // Listen for live media detections while watching video
    chrome.runtime.onMessage.addListener((msg) => {
        if (msg.type === "IDM_MEDIA_DETECTED") {
            pageMedia = msg.allMedia || [];
            scanAndAttachButtons();
        }
    });

    // Attach to video elements on page
    function scanAndAttachButtons() {
        const isYouTube = window.location.hostname.includes("youtube.com") || window.location.hostname.includes("youtu.be");

        const videos = document.querySelectorAll("video");
        videos.forEach((video) => {
            if (video.dataset.idmAttached) return;

            let parent = video.parentElement;
            if (isYouTube) {
                const playerContainer = video.closest(".html5-video-player") || video.closest("#player") || video.parentElement;
                if (playerContainer) parent = playerContainer;
            }
            if (!parent) return;

            // Avoid attaching inside tiny icons or invisible videos
            const rect = video.getBoundingClientRect();
            if (rect.width > 0 && rect.width < 120 && rect.height < 90) return;

            attachIdmButton(video, parent, isYouTube);
        });
    }

    function attachIdmButton(video, container, isYouTube = false) {
        video.dataset.idmAttached = "true";

        const btn = document.createElement("div");
        btn.className = "idm-video-download-btn";
        if (isYouTube) {
            btn.classList.add("idm-youtube-btn");
        }
        btn.innerHTML = `
            <span class="idm-btn-icon">⬇</span>
            <span>${isYouTube ? "Download YouTube Video" : "Download this video"}</span>
        `;

        const dropdown = document.createElement("div");
        dropdown.className = "idm-video-dropdown";
        dropdown.innerHTML = `<div class="idm-dropdown-header">Available Video Streams</div>`;
        btn.appendChild(dropdown);

        // Click handler
        btn.addEventListener("click", (e) => {
            e.stopPropagation();
            e.preventDefault();

            if (isYouTube) {
                const videoTitle = (document.title || "YouTube Video").replace(" - YouTube", "").trim();
                const proceed = confirm(`Download this YouTube video in Full HD with audio?\n\n"${videoTitle}"\n\nFile will be saved to your Downloads folder.`);
                if (!proceed) return;

                const label = btn.querySelector("span:last-child");
                label.textContent = "⏳ Sending to JDM...";
                triggerIdmDownload(window.location.href, videoTitle + ".mp4", btn);
                return;
            }

            const sources = collectVideoSources(video);
            if (sources.length === 0) {
                // If direct sources not found yet, use tab media
                if (pageMedia.length > 0) {
                    showDropdown(dropdown, pageMedia);
                } else {
                    btn.querySelector("span:last-child").textContent = "Detecting streams...";
                    setTimeout(() => {
                        btn.querySelector("span:last-child").textContent = "Download this video";
                    }, 2000);
                }
            } else if (sources.length === 1 && pageMedia.length <= 1) {
                // Direct download single source
                triggerIdmDownload(sources[0].url, sources[0].filename, btn);
            } else {
                // Show choice
                const combined = [...sources];
                pageMedia.forEach(pm => {
                    if (!combined.some(c => c.url === pm.url)) {
                        combined.push(pm);
                    }
                });
                showDropdown(dropdown, combined);
            }
        });

        // Close dropdown when clicking outside
        document.addEventListener("click", () => {
            dropdown.classList.remove("show");
        });

        // Position overlay relative to container or video
        const computedStyle = window.getComputedStyle(container);
        if (computedStyle.position === "static") {
            container.style.position = "relative";
        }

        container.appendChild(btn);
    }

    function collectVideoSources(video) {
        const sources = [];

        // 1. Direct src
        if (video.src && isValidHttpUrl(video.src)) {
            sources.push({
                url: video.src,
                filename: extractFilenameFromUrl(video.src),
                mime: "video/mp4"
            });
        }

        // 2. <source> child tags
        const childSources = video.querySelectorAll("source");
        childSources.forEach(s => {
            if (s.src && isValidHttpUrl(s.src)) {
                sources.push({
                    url: s.src,
                    filename: extractFilenameFromUrl(s.src),
                    mime: s.type || "video/mp4"
                });
            }
        });

        // 3. CurrentSrc
        if (video.currentSrc && isValidHttpUrl(video.currentSrc) && !sources.some(s => s.url === video.currentSrc)) {
            sources.push({
                url: video.currentSrc,
                filename: extractFilenameFromUrl(video.currentSrc),
                mime: "video/mp4"
            });
        }

        return sources;
    }

    function showDropdown(dropdown, items) {
        dropdown.innerHTML = `<div class="idm-dropdown-header">Select Video Quality / Format</div>`;

        items.forEach((item, index) => {
            const row = document.createElement("div");
            row.className = "idm-dropdown-item";

            let label = item.filename || `Video Stream ${index + 1}`;
            let ext = (item.mime || "").split("/")[1] || "MP4";
            if (item.url && item.url.includes(".m3u8")) ext = "HLS (.m3u8)";

            row.innerHTML = `
                <span class="item-title" title="${item.url}">${escapeHtml(label)}</span>
                <span class="item-badge">${escapeHtml(ext.toUpperCase())}</span>
            `;

            row.addEventListener("click", (e) => {
                e.stopPropagation();
                dropdown.classList.remove("show");
                triggerIdmDownload(item.url, item.filename, dropdown.parentElement);
            });

            dropdown.appendChild(row);
        });

        dropdown.classList.add("show");
    }

    function triggerIdmDownload(url, filename, btn) {
        chrome.runtime.sendMessage({
            type: "DOWNLOAD_MEDIA_ITEM",
            url: url,
            filename: filename || ""
        }, (res) => {
            const label = btn.querySelector("span:last-child");
            if (res && res.success) {
                const old = label.textContent;
                label.textContent = "✓ Starting in JDM...";
                setTimeout(() => { label.textContent = old; }, 3500);
            } else {
                label.textContent = "JDM Offline";
                alert("Java Download Manager is not currently running!\n\nPlease open Java Download Manager on your PC (run.bat or IDM-Java.jar), then click Download again.");
                setTimeout(() => { label.textContent = "Download this video"; }, 2500);
            }
        });
    }

    function isValidHttpUrl(url) {
        return url && (url.startsWith("http://") || url.startsWith("https://"));
    }

    function extractFilenameFromUrl(url) {
        try {
            const clean = url.split("?")[0];
            const parts = clean.split("/").filter(Boolean);
            const name = parts[parts.length - 1];
            if (name && name.includes(".")) {
                return decodeURIComponent(name);
            }
        } catch (e) {}
        return "video_" + Date.now() + ".mp4";
    }

    function escapeHtml(str) {
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    // Observer for dynamically loaded videos (YouTube, SPAs, Twitter, etc.)
    const observer = new MutationObserver(() => {
        scanAndAttachButtons();
    });

    observer.observe(document.body || document.documentElement, {
        childList: true,
        subtree: true
    });

    // Listen for YouTube single-page navigation
    window.addEventListener("yt-navigate-finish", () => {
        setTimeout(scanAndAttachButtons, 1000);
    });

    // Initial scan
    scanAndAttachButtons();
})();
