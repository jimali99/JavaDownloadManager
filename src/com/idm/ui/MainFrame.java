package com.idm.ui;

import com.idm.engine.DownloadQueueManager;
import com.idm.model.AppSettings;
import com.idm.model.DownloadCategory;
import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;
import com.idm.util.ClipboardMonitor;
import com.idm.util.FormatUtils;
import com.idm.util.StorageUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class MainFrame extends JFrame {

    private final AppSettings settings;
    private final DownloadQueueManager queueManager;
    private final ClipboardMonitor clipboardMonitor;

    private DownloadTableModel tableModel;
    private JTable downloadTable;
    private DefaultListModel<CategoryItem> categoryListModel;
    private JList<CategoryItem> categoryList;

    private final JLabel lblTotalSpeed = new JLabel("Transfer Rate: 0 KB/s");
    private final JLabel lblActiveCount = new JLabel("Active: 0");
    private final JLabel lblClipboardStatus = new JLabel("Clipboard: ON");

    private Timer uiRefreshTimer;
    private TrayIcon trayIcon;

    static class CategoryItem {
        final DownloadCategory category;
        int count;

        CategoryItem(DownloadCategory category) {
            this.category = category;
            this.count = 0;
        }

        @Override
        public String toString() {
            return category.getDisplayName() + " (" + count + ")";
        }
    }

    public MainFrame(AppSettings settings, DownloadQueueManager queueManager, ClipboardMonitor clipboardMonitor) {
        super("JDM — Java Download Manager (Accelerated Pro)");
        this.settings = settings;
        this.queueManager = queueManager;
        this.clipboardMonitor = clipboardMonitor;

        java.util.List<Image> appIcons = AppIcons.getAppIconImages();
        if (!appIcons.isEmpty()) {
            setIconImages(appIcons);
        }

        initUI();
        initSystemTray();
        initClipboardListener();
        startUiTicker();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (SystemTray.isSupported() && trayIcon != null && settings.isMinimizeToTrayOnClose()) {
                    setVisible(false);
                } else {
                    exitApp();
                }
            }
        });

        setSize(1100, 650);
        setMinimumSize(new Dimension(850, 480));
        setLocationRelativeTo(null);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 1. Menu Bar
        setJMenuBar(createMenuBar());

        // 2. Toolbar
        add(createToolBar(), BorderLayout.NORTH);

        // 3. Main Center SplitPane (Categories on Left, Table on Right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(220);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        // Left Category List
        categoryListModel = new DefaultListModel<>();
        for (DownloadCategory cat : DownloadCategory.values()) {
            categoryListModel.addElement(new CategoryItem(cat));
        }

        categoryList = new JList<>(categoryListModel);
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.setSelectedIndex(0);
        categoryList.setFixedCellHeight(36);
        categoryList.setCellRenderer(new CategoryListCellRenderer());
        categoryList.setBorder(new EmptyBorder(5, 5, 5, 5));
        categoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                CategoryItem item = categoryList.getSelectedValue();
                if (item != null) {
                    tableModel.setCategory(item.category);
                }
            }
        });

        JPanel leftPanel = new JPanel(new BorderLayout());

        // JDM Brand header in sidebar
        JPanel brandHeader = new JPanel(new BorderLayout(10, 0));
        brandHeader.setBorder(new EmptyBorder(12, 14, 12, 14));
        brandHeader.setBackground(UIManager.getColor("Panel.background"));

        Image logoImg = AppIcons.getAppLogoImage();
        if (logoImg != null) {
            ImageIcon logoIcon = new ImageIcon(logoImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH));
            brandHeader.add(new JLabel(logoIcon), BorderLayout.WEST);
        }

        JPanel brandTextPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        brandTextPanel.setOpaque(false);
        JLabel brandTitle = new JLabel("JDM Pro");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel brandSub = new JLabel("Java Download Manager");
        brandSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        brandSub.setForeground(UIManager.getColor("Label.disabledForeground"));
        brandTextPanel.add(brandTitle);
        brandTextPanel.add(brandSub);
        brandHeader.add(brandTextPanel, BorderLayout.CENTER);

        JPanel sidebarTop = new JPanel(new BorderLayout());
        sidebarTop.add(brandHeader, BorderLayout.NORTH);
        sidebarTop.add(new JSeparator(), BorderLayout.CENTER);

        JLabel lblCatTitle = new JLabel(" Categories");
        lblCatTitle.setFont(lblCatTitle.getFont().deriveFont(Font.BOLD, 11f));
        lblCatTitle.setBorder(new EmptyBorder(8, 12, 6, 12));
        sidebarTop.add(lblCatTitle, BorderLayout.SOUTH);

        leftPanel.add(sidebarTop, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(categoryList), BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);

        // Right Table
        tableModel = new DownloadTableModel(queueManager);
        downloadTable = new JTable(tableModel);
        downloadTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        downloadTable.setDefaultRenderer(DownloadTask.class, new ProgressCellRenderer());
        downloadTable.getColumnModel().getColumn(0).setCellRenderer(new TaskCellRenderers.FileNameRenderer(tableModel));
        downloadTable.getColumnModel().getColumn(2).setCellRenderer(new TaskCellRenderers.StatusRenderer(tableModel));
        downloadTable.getTableHeader().setReorderingAllowed(false);
        downloadTable.setFillsViewportHeight(true);

        // Column widths
        downloadTable.getColumnModel().getColumn(0).setPreferredWidth(260); // Name
        downloadTable.getColumnModel().getColumn(1).setPreferredWidth(85);  // Size
        downloadTable.getColumnModel().getColumn(2).setPreferredWidth(95);  // Status
        downloadTable.getColumnModel().getColumn(3).setPreferredWidth(130); // Progress
        downloadTable.getColumnModel().getColumn(4).setPreferredWidth(95);  // Speed
        downloadTable.getColumnModel().getColumn(5).setPreferredWidth(85);  // ETA
        downloadTable.getColumnModel().getColumn(6).setPreferredWidth(85);  // Connections
        downloadTable.getColumnModel().getColumn(7).setPreferredWidth(125); // Date

        // Double click to open Details / Visualizer dialog
        downloadTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedTaskDetails();
                }
            }
        });

        // Context Menu
        downloadTable.setComponentPopupMenu(createContextMenu());

        // Delete key shortcut
        downloadTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    promptDeleteSelected();
                }
            }
        });

        // Listen for queue modifications
        queueManager.addQueueListener(() -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.refreshFilteredList();
                tableModel.fireTableDataChanged();
            });
        });

        JScrollPane tableScroll = new JScrollPane(downloadTable);
        splitPane.setRightComponent(tableScroll);

        add(splitPane, BorderLayout.CENTER);

        // 4. Bottom Status Bar
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();

        // Tasks Menu
        JMenu mTasks = new JMenu("Tasks");
        JMenuItem miAdd = new JMenuItem("Add new download...", AppIcons.getAddIcon(16));
        miAdd.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        miAdd.addActionListener(e -> showAddDialog(null));

        JMenuItem miYouTube = new JMenuItem("Download YouTube Video...", AppIcons.getCategoryIcon(DownloadCategory.VIDEOS, 16));
        miYouTube.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        miYouTube.addActionListener(e -> {
            String url = JOptionPane.showInputDialog(
                this,
                "Enter YouTube video or Shorts link:",
                "Download YouTube Video",
                JOptionPane.QUESTION_MESSAGE
            );
            if (url != null && !url.trim().isEmpty()) {
                AddDownloadDialog.launchYouTubeDownloader(url.trim());
            }
        });

        JMenuItem miResume = new JMenuItem("Resume Download", AppIcons.getResumeIcon(16));
        miResume.addActionListener(e -> resumeSelected());

        JMenuItem miPause = new JMenuItem("Pause Download", AppIcons.getPauseIcon(16));
        miPause.addActionListener(e -> pauseSelected());

        JMenuItem miPauseAll = new JMenuItem("Pause All Active", AppIcons.getStopIcon(16));
        miPauseAll.addActionListener(e -> queueManager.pauseAll());

        JMenuItem miResumeAll = new JMenuItem("Resume All", AppIcons.getResumeIcon(16));
        miResumeAll.addActionListener(e -> queueManager.resumeAll());

        JMenuItem miExit = new JMenuItem("Exit");
        miExit.addActionListener(e -> exitApp());

        JMenuItem miDelete = new JMenuItem("Delete Download...", AppIcons.getDeleteIcon(16));
        miDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        miDelete.addActionListener(e -> promptDeleteSelected());

        JMenuItem miClearAll = new JMenuItem("Clear All Downloads...", AppIcons.getDeleteIcon(16));
        miClearAll.addActionListener(e -> promptClearAllDownloads());

        mTasks.add(miAdd);
        mTasks.add(miYouTube);
        mTasks.addSeparator();
        mTasks.add(miResume);
        mTasks.add(miPause);
        mTasks.add(miDelete);
        mTasks.add(miClearAll);
        mTasks.addSeparator();
        mTasks.add(miResumeAll);
        mTasks.add(miPauseAll);
        mTasks.addSeparator();
        mTasks.add(miExit);

        // Options Menu
        JMenu mOptions = new JMenu("Options");
        JMenuItem miSettings = new JMenuItem("Configuration & Settings...", AppIcons.getSettingsIcon(16));
        miSettings.addActionListener(e -> showSettingsDialog());
        mOptions.add(miSettings);

        // Help Menu
        JMenu mHelp = new JMenu("Help");
        JMenuItem miAbout = new JMenuItem("About JDM Pro...", AppIcons.getLogoIcon(16, 16));
        miAbout.addActionListener(e -> showAboutDialog());
        mHelp.add(miAbout);

        mb.add(mTasks);
        mb.add(mOptions);
        mb.add(mHelp);

        return mb;
    }

    private JToolBar createToolBar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(new EmptyBorder(8, 10, 8, 10));

        JButton btnAdd = createToolButton("Add URL", AppIcons.getAddIcon(18), "Add a new download from URL");
        btnAdd.addActionListener(e -> showAddDialog(null));

        JButton btnResume = createToolButton("Resume", AppIcons.getResumeIcon(18), "Resume selected download");
        btnResume.addActionListener(e -> resumeSelected());

        JButton btnPause = createToolButton("Pause", AppIcons.getPauseIcon(18), "Pause selected download");
        btnPause.addActionListener(e -> pauseSelected());

        JButton btnStop = createToolButton("Stop All", AppIcons.getStopIcon(18), "Pause all active downloads");
        btnStop.addActionListener(e -> queueManager.pauseAll());

        JButton btnDelete = createToolButton("Delete", AppIcons.getDeleteIcon(18), "Delete selected download (from list or disk)");
        btnDelete.addActionListener(e -> promptDeleteSelected());

        JButton btnOpenFile = createToolButton("Open File", AppIcons.getOpenFileIcon(18), "Open downloaded file");
        btnOpenFile.addActionListener(e -> openSelectedFile());

        JButton btnOpenFolder = createToolButton("Open Folder", AppIcons.getOpenFolderIcon(18), "Open download directory");
        btnOpenFolder.addActionListener(e -> openSelectedFolder());

        JButton btnSettings = createToolButton("Options", AppIcons.getSettingsIcon(18), "Open settings");
        btnSettings.addActionListener(e -> showSettingsDialog());

        tb.add(btnAdd);
        tb.addSeparator(new Dimension(8, 24));
        tb.add(btnResume);
        tb.add(btnPause);
        tb.add(btnStop);
        tb.addSeparator(new Dimension(8, 24));
        tb.add(btnDelete);
        tb.addSeparator(new Dimension(8, 24));
        tb.add(btnOpenFile);
        tb.add(btnOpenFolder);
        tb.add(Box.createHorizontalGlue());
        tb.add(btnSettings);

        return tb;
    }

    private JButton createToolButton(String text, Icon icon, String tooltip) {
        JButton btn = new JButton(text, icon);
        btn.setToolTipText(tooltip);
        btn.setFocusable(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setIconTextGap(8);
        btn.setMargin(new Insets(6, 12, 6, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem miOpen = new JMenuItem("Open File", AppIcons.getOpenFileIcon(16));
        miOpen.addActionListener(e -> openSelectedFile());

        JMenuItem miOpenFolder = new JMenuItem("Open Containing Folder", AppIcons.getOpenFolderIcon(16));
        miOpenFolder.addActionListener(e -> openSelectedFolder());

        JMenuItem miDetails = new JMenuItem("Download Status / Segment Acceleration...", AppIcons.getCategoryIcon(DownloadCategory.DOWNLOADING, 16));
        miDetails.addActionListener(e -> openSelectedTaskDetails());

        JMenuItem miResume = new JMenuItem("Resume", AppIcons.getResumeIcon(16));
        miResume.addActionListener(e -> resumeSelected());

        JMenuItem miPause = new JMenuItem("Pause", AppIcons.getPauseIcon(16));
        miPause.addActionListener(e -> pauseSelected());

        JMenuItem miRestart = new JMenuItem("Restart from Beginning", AppIcons.getRestartIcon(16));
        miRestart.addActionListener(e -> restartSelected());

        JMenuItem miCopyUrl = new JMenuItem("Copy URL to Clipboard", AppIcons.getOpenFileIcon(16));
        miCopyUrl.addActionListener(e -> {
            DownloadTask t = getSelectedTask();
            if (t != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection(t.getUrl()), null
                );
            }
        });

        JMenuItem miDelete = new JMenuItem("Delete Download...", AppIcons.getDeleteIcon(16));
        miDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        miDelete.addActionListener(e -> promptDeleteSelected());

        JMenuItem miDeleteFile = new JMenuItem("Permanently Delete File from Disk", AppIcons.getDeleteIcon(16));
        miDeleteFile.addActionListener(e -> deleteSelectedTasks(true));

        JMenuItem miRemoveOnly = new JMenuItem("Remove from List Only", AppIcons.getStopIcon(16));
        miRemoveOnly.addActionListener(e -> deleteSelectedTasks(false));

        menu.add(miOpen);
        menu.add(miOpenFolder);
        menu.add(miDetails);
        menu.addSeparator();
        menu.add(miResume);
        menu.add(miPause);
        menu.add(miRestart);
        menu.addSeparator();
        menu.add(miCopyUrl);
        menu.addSeparator();
        menu.add(miDelete);
        menu.add(miDeleteFile);
        menu.add(miRemoveOnly);

        return menu;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout(15, 0));
        status.setBorder(new EmptyBorder(6, 12, 6, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        lblTotalSpeed.setFont(lblTotalSpeed.getFont().deriveFont(Font.BOLD));
        left.add(lblTotalSpeed);
        left.add(lblActiveCount);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        lblClipboardStatus.setForeground(settings.isMonitorClipboard() ? new Color(34, 197, 94) : Color.GRAY);
        right.add(lblClipboardStatus);

        status.add(left, BorderLayout.WEST);
        status.add(right, BorderLayout.EAST);

        return status;
    }

    private void initSystemTray() {
        if (!SystemTray.isSupported()) return;

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image img = AppIcons.getAppLogoImage();
            if (img == null) img = getDefaultIconImage();

            PopupMenu popup = new PopupMenu();
            MenuItem miOpen = new MenuItem("Open JDM Pro");
            miOpen.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
                toFront();
            });

            MenuItem miAdd = new MenuItem("Add URL...");
            miAdd.addActionListener(e -> showAddDialog(null));

            MenuItem miExit = new MenuItem("Exit");
            miExit.addActionListener(e -> exitApp());

            popup.add(miOpen);
            popup.add(miAdd);
            popup.addSeparator();
            popup.add(miExit);

            trayIcon = new TrayIcon(img, "JDM — Java Download Manager", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
                toFront();
            });

            tray.add(trayIcon);

        } catch (Exception ignored) {}
    }

    private void showAboutDialog() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBorder(new EmptyBorder(6, 6, 6, 6));

        Image banner = AppIcons.getBannerImage();
        if (banner != null) {
            Image scaledBanner = banner.getScaledInstance(460, 250, Image.SCALE_SMOOTH);
            JLabel bannerLabel = new JLabel(new ImageIcon(scaledBanner));
            bannerLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(bannerLabel, BorderLayout.NORTH);
        }

        JLabel infoLabel = new JLabel(
            "<html><div style='text-align: center; font-family: Segoe UI;'>" +
            "<h2 style='margin: 4px 0; color: #38bdf8;'>JDM — Java Download Manager</h2>" +
            "<p style='color: #94a3b8; font-size: 11px;'>Version 1.1.0 • High-Performance Java Accelerated Downloader</p>" +
            "<p style='margin-top: 8px;'>Featuring multi-segment parallel HTTP byte-range connections, zero-copy FileChannel writes, dynamic pause/resume, and Chrome Extension integration.</p>" +
            "</div></html>"
        );
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(infoLabel, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, panel, "About JDM Pro", JOptionPane.PLAIN_MESSAGE);
    }

    private Image getDefaultIconImage() {
        // Create a crisp 32x32 colored icon programmatically
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(37, 99, 235));
        g2.fillRoundRect(2, 2, 28, 28, 8, 8);
        g2.setColor(Color.WHITE);
        // Draw download arrow
        g2.fillRect(13, 6, 6, 12);
        int[] xPts = {8, 16, 24};
        int[] yPts = {17, 24, 17};
        g2.fillPolygon(xPts, yPts, 3);
        g2.fillRect(8, 26, 16, 2);
        g2.dispose();
        return img;
    }

    private void initClipboardListener() {
        clipboardMonitor.addListener(url -> {
            if (!settings.isMonitorClipboard()) return;

            SwingUtilities.invokeLater(() -> {
                // If window is minimized or not active, we can ask user
                int res = JOptionPane.showConfirmDialog(
                    this,
                    "Download link detected in clipboard:\n\n" + url + "\n\nWould you like to start downloading?",
                    "IDM - Download Link Detected",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                if (res == JOptionPane.YES_OPTION) {
                    setVisible(true);
                    toFront();
                    showAddDialog(url);
                }
            });
        });

        if (settings.isMonitorClipboard()) {
            clipboardMonitor.start();
        }
    }

    private int lastTaskCount = -1;

    private void startUiTicker() {
        uiRefreshTimer = new Timer(300, e -> {
            List<DownloadTask> tasks = queueManager.getTasks();

            // Only refresh structural table data when count changes!
            if (tasks.size() != lastTaskCount) {
                lastTaskCount = tasks.size();
                DownloadTask selected = getSelectedTask();
                tableModel.refreshFilteredList();
                tableModel.fireTableDataChanged();
                if (selected != null) {
                    for (int r = 0; r < tableModel.getRowCount(); r++) {
                        DownloadTask t = tableModel.getTaskAt(r);
                        if (t != null && t.getId().equals(selected.getId())) {
                            downloadTable.setRowSelectionInterval(r, r);
                            break;
                        }
                    }
                }
            } else {
                // Just repaint table to smoothly update speed, progress and ETA without resetting selection!
                downloadTable.repaint();
            }

            double totalSpeed = 0;
            int active = 0;

            // Counts for category badges
            int countAll = tasks.size();
            int countDownloading = 0;
            int countCompleted = 0;
            int countPaused = 0;
            int countFailed = 0;
            int countComp = 0, countDocs = 0, countVid = 0, countMus = 0, countProg = 0, countOther = 0;

            for (DownloadTask t : tasks) {
                if (t.getStatus().isActive()) {
                    totalSpeed += t.getCurrentSpeed();
                    active++;
                    countDownloading++;
                } else if (t.getStatus() == DownloadStatus.COMPLETED) {
                    countCompleted++;
                } else if (t.getStatus() == DownloadStatus.PAUSED) {
                    countPaused++;
                } else if (t.getStatus() == DownloadStatus.ERROR || t.getStatus() == DownloadStatus.CANCELLED) {
                    countFailed++;
                } else if (t.getStatus() == DownloadStatus.QUEUED) {
                    countDownloading++;
                }

                switch (t.getCategory()) {
                    case COMPRESSED: countComp++; break;
                    case DOCUMENTS: countDocs++; break;
                    case VIDEOS: countVid++; break;
                    case MUSIC: countMus++; break;
                    case PROGRAMS: countProg++; break;
                    default: countOther++; break;
                }
            }

            lblTotalSpeed.setText("Transfer Rate: " + FormatUtils.formatSpeed(totalSpeed));
            lblActiveCount.setText("Active: " + active + " / " + countAll);
            lblClipboardStatus.setText("Clipboard: " + (settings.isMonitorClipboard() ? "ON" : "OFF"));
            lblClipboardStatus.setForeground(settings.isMonitorClipboard() ? new Color(34, 197, 94) : Color.GRAY);

            // Update category badge numbers
            for (int i = 0; i < categoryListModel.size(); i++) {
                CategoryItem item = categoryListModel.get(i);
                int c = 0;
                switch (item.category) {
                    case ALL: c = countAll; break;
                    case DOWNLOADING: c = countDownloading; break;
                    case COMPLETED: c = countCompleted; break;
                    case PAUSED: c = countPaused; break;
                    case FAILED: c = countFailed; break;
                    case COMPRESSED: c = countComp; break;
                    case DOCUMENTS: c = countDocs; break;
                    case VIDEOS: c = countVid; break;
                    case MUSIC: c = countMus; break;
                    case PROGRAMS: c = countProg; break;
                    case OTHER: c = countOther; break;
                }
                if (item.count != c) {
                    item.count = c;
                    categoryListModel.set(i, item);
                }
            }
        });
        uiRefreshTimer.start();
    }

    private DownloadTask getSelectedTask() {
        int row = downloadTable.getSelectedRow();
        if (row != -1) {
            return tableModel.getTaskAt(row);
        }
        return null;
    }

    private void showAddDialog(String initialUrl) {
        String url = initialUrl;
        if (url == null) {
            // Check if clipboard contains a URL
            try {
                String clip = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                if (clip != null && (clip.trim().startsWith("http://") || clip.trim().startsWith("https://"))) {
                    url = clip.trim();
                }
            } catch (Exception ignored) {}
        }
        AddDownloadDialog dialog = new AddDownloadDialog(this, url, queueManager, settings);
        dialog.setVisible(true);
    }

    private void openSelectedTaskDetails() {
        DownloadTask task = getSelectedTask();
        if (task != null) {
            DownloadDetailDialog dialog = new DownloadDetailDialog(this, task, queueManager);
            dialog.setVisible(true);
        }
    }

    private void resumeSelected() {
        int[] rows = downloadTable.getSelectedRows();
        for (int r : rows) {
            DownloadTask task = tableModel.getTaskAt(r);
            if (task != null) {
                queueManager.resumeTask(task);
            }
        }
    }

    private void pauseSelected() {
        int[] rows = downloadTable.getSelectedRows();
        for (int r : rows) {
            DownloadTask task = tableModel.getTaskAt(r);
            if (task != null) {
                queueManager.pauseTask(task);
            }
        }
    }

    private void restartSelected() {
        DownloadTask task = getSelectedTask();
        if (task != null) {
            int res = JOptionPane.showConfirmDialog(
                this,
                "Do you want to re-download '" + task.getFileName() + "' from the beginning?",
                "Restart Download",
                JOptionPane.YES_NO_OPTION
            );
            if (res == JOptionPane.YES_OPTION) {
                queueManager.restartTask(task);
            }
        }
    }

    private void promptDeleteSelected() {
        int[] rows = downloadTable.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select one or more downloads to delete.", "No Download Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        java.util.List<DownloadTask> toDelete = new java.util.ArrayList<>();
        boolean hasExistingLocalFiles = false;
        StringBuilder names = new StringBuilder();

        for (int r : rows) {
            DownloadTask t = tableModel.getTaskAt(r);
            if (t != null) {
                toDelete.add(t);
                if (t.getOutputFile().exists()) {
                    hasExistingLocalFiles = true;
                }
                if (names.length() > 0) names.append("\n");
                names.append("• ").append(t.getFileName());
            }
        }

        if (toDelete.isEmpty()) return;

        boolean deleteFromDisk = false;

        if (hasExistingLocalFiles) {
            Object[] options = {"Delete from Disk & List", "Remove from List Only", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                this,
                "Do you want to permanently delete the downloaded file(s) from your hard drive as well?\n\n" +
                names.toString() + "\n\n" +
                "• 'Delete from Disk & List' will permanently delete the file(s) from your PC.\n" +
                "• 'Remove from List Only' will keep the file(s) on your disk but remove them from IDM.",
                "Delete Download",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );

            if (choice == 0) {
                deleteFromDisk = true;
            } else if (choice == 1) {
                deleteFromDisk = false;
            } else {
                return; // Cancelled
            }
        } else {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Remove selected download(s) from the list?\n\n" + names.toString(),
                "Remove from List",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        deleteTasksInternal(toDelete, deleteFromDisk);
    }

    private void deleteSelectedTasks(boolean deleteFileFromDisk) {
        int[] rows = downloadTable.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select one or more downloads to delete.", "No Download Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        java.util.List<DownloadTask> toDelete = new java.util.ArrayList<>();
        StringBuilder names = new StringBuilder();

        for (int r : rows) {
            DownloadTask t = tableModel.getTaskAt(r);
            if (t != null) {
                toDelete.add(t);
                if (names.length() > 0) names.append("\n");
                names.append("• ").append(t.getFileName());
            }
        }

        String msg = deleteFileFromDisk ?
            "Are you sure you want to permanently delete selected file(s) from your hard drive?\n\n" + names :
            "Remove selected download(s) from the download list?\n\n" + names;

        int res = JOptionPane.showConfirmDialog(this, msg, "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            deleteTasksInternal(toDelete, deleteFileFromDisk);
        }
    }

    private void deleteTasksInternal(java.util.List<DownloadTask> tasks, boolean deleteFromDisk) {
        boolean anyFileFailed = false;
        for (DownloadTask t : tasks) {
            boolean ok = queueManager.removeTask(t, deleteFromDisk);
            if (deleteFromDisk && !ok) {
                anyFileFailed = true;
            }
        }

        lastTaskCount = queueManager.getTasks().size();
        tableModel.refreshFilteredList();
        tableModel.fireTableDataChanged();
        downloadTable.clearSelection();

        if (anyFileFailed) {
            JOptionPane.showMessageDialog(
                this,
                "One or more files could not be deleted from disk. They might be currently open in another program.",
                "File Deletion Notice",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void promptClearAllDownloads() {
        int count = queueManager.getTasks().size();
        if (count == 0) {
            JOptionPane.showMessageDialog(this, "The download list is already empty.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Object[] options = {"Remove from list only", "Delete files from disk too", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Are you sure you want to clear all " + count + " downloads from JDM?",
            "Clear All Downloads",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[0]
        );

        if (choice == 0) {
            queueManager.removeAllTasks(false);
            tableModel.refreshFilteredList();
            tableModel.fireTableDataChanged();
            downloadTable.clearSelection();
        } else if (choice == 1) {
            queueManager.removeAllTasks(true);
            tableModel.refreshFilteredList();
            tableModel.fireTableDataChanged();
            downloadTable.clearSelection();
        }
    }

    private void openSelectedFile() {
        DownloadTask task = getSelectedTask();
        if (task != null) {
            StorageUtils.openFileOrExecute(this, task.getOutputFile());
        }
    }

    private void openSelectedFolder() {
        DownloadTask task = getSelectedTask();
        if (task != null) {
            StorageUtils.openFolder(this, new File(task.getSaveDirectory()), task.getOutputFile());
        }
    }

    private void showSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(this, settings);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            if (settings.isMonitorClipboard()) {
                clipboardMonitor.start();
            } else {
                clipboardMonitor.stop();
            }
            lblClipboardStatus.setText("Clipboard: " + (settings.isMonitorClipboard() ? "ON" : "OFF"));
            lblClipboardStatus.setForeground(settings.isMonitorClipboard() ? new Color(34, 197, 94) : Color.GRAY);
        }
    }

    private void exitApp() {
        int active = 0;
        for (DownloadTask t : queueManager.getTasks()) {
            if (t.getStatus().isActive()) active++;
        }

        if (active > 0) {
            int res = JOptionPane.showConfirmDialog(
                this,
                "There are " + active + " active download(s) in progress.\nAre you sure you want to pause them and exit?",
                "Active Downloads in Progress",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (res != JOptionPane.YES_OPTION) {
                return;
            }
        }

        if (uiRefreshTimer != null) uiRefreshTimer.stop();
        clipboardMonitor.stop();
        queueManager.shutdown();
        settings.save();
        System.exit(0);
    }
}
