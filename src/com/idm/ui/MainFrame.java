package com.idm.ui;

import com.idm.engine.DownloadQueueManager;
import com.idm.model.AppSettings;
import com.idm.model.DownloadCategory;
import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;
import com.idm.util.ClipboardMonitor;
import com.idm.util.FormatUtils;

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

    private static class CategoryItem {
        final DownloadCategory category;
        int count;

        CategoryItem(DownloadCategory category) {
            this.category = category;
            this.count = 0;
        }

        @Override
        public String toString() {
            return category.getIcon() + "  " + category.getDisplayName() + " (" + count + ")";
        }
    }

    public MainFrame(AppSettings settings, DownloadQueueManager queueManager, ClipboardMonitor clipboardMonitor) {
        super("Internet Download Manager (IDM Pro - Java Accelerated)");
        this.settings = settings;
        this.queueManager = queueManager;
        this.clipboardMonitor = clipboardMonitor;

        initUI();
        initSystemTray();
        initClipboardListener();
        startUiTicker();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApp();
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
        categoryList.setFixedCellHeight(32);
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
        JLabel lblCatTitle = new JLabel(" Categories");
        lblCatTitle.setFont(lblCatTitle.getFont().deriveFont(Font.BOLD, 12f));
        lblCatTitle.setBorder(new EmptyBorder(8, 10, 8, 10));
        leftPanel.add(lblCatTitle, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(categoryList), BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);

        // Right Table
        tableModel = new DownloadTableModel(queueManager);
        downloadTable = new JTable(tableModel);
        downloadTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        downloadTable.setDefaultRenderer(DownloadTask.class, new ProgressCellRenderer());
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
        JMenuItem miAdd = new JMenuItem("Add new download...");
        miAdd.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        miAdd.addActionListener(e -> showAddDialog(null));

        JMenuItem miResume = new JMenuItem("Resume Download");
        miResume.addActionListener(e -> resumeSelected());

        JMenuItem miPause = new JMenuItem("Pause Download");
        miPause.addActionListener(e -> pauseSelected());

        JMenuItem miPauseAll = new JMenuItem("Pause All Active");
        miPauseAll.addActionListener(e -> queueManager.pauseAll());

        JMenuItem miResumeAll = new JMenuItem("Resume All");
        miResumeAll.addActionListener(e -> queueManager.resumeAll());

        JMenuItem miExit = new JMenuItem("Exit");
        miExit.addActionListener(e -> exitApp());

        JMenuItem miDelete = new JMenuItem("Delete Download...");
        miDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        miDelete.addActionListener(e -> promptDeleteSelected());

        mTasks.add(miAdd);
        mTasks.addSeparator();
        mTasks.add(miResume);
        mTasks.add(miPause);
        mTasks.add(miDelete);
        mTasks.addSeparator();
        mTasks.add(miResumeAll);
        mTasks.add(miPauseAll);
        mTasks.addSeparator();
        mTasks.add(miExit);

        // Options Menu
        JMenu mOptions = new JMenu("Options");
        JMenuItem miSettings = new JMenuItem("Configuration & Settings...");
        miSettings.addActionListener(e -> showSettingsDialog());
        mOptions.add(miSettings);

        // Help Menu
        JMenu mHelp = new JMenu("Help");
        JMenuItem miAbout = new JMenuItem("About IDM Accelerated...");
        miAbout.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                this,
                "<html><h2>Internet Download Manager (IDM Pro)</h2>" +
                "<p>High-Performance Multi-Connection Download Accelerator built in Java 26.</p>" +
                "<p><b>Features:</b></p>" +
                "<ul>" +
                "<li>Dynamic Multi-Segment parallel downloading (up to 32 connections)</li>" +
                "<li>Full Pause & Resume capability on supporting servers</li>" +
                "<li>Zero-assembly zero-copy direct FileChannel positioning</li>" +
                "<li>Real-time segment visualization</li>" +
                "<li>Clipboard link detection and sound alerts</li>" +
                "</ul>" +
                "</html>",
                "About IDM Java",
                JOptionPane.INFORMATION_MESSAGE
            );
        });
        mHelp.add(miAbout);

        mb.add(mTasks);
        mb.add(mOptions);
        mb.add(mHelp);

        return mb;
    }

    private JToolBar createToolBar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(new EmptyBorder(6, 8, 6, 8));

        JButton btnAdd = createToolButton("➕ Add URL", "Add a new download from URL");
        btnAdd.addActionListener(e -> showAddDialog(null));

        JButton btnResume = createToolButton("▶ Resume", "Resume selected download");
        btnResume.addActionListener(e -> resumeSelected());

        JButton btnPause = createToolButton("⏸ Pause", "Pause selected download");
        btnPause.addActionListener(e -> pauseSelected());

        JButton btnStop = createToolButton("⏹ Stop All", "Pause all active downloads");
        btnStop.addActionListener(e -> queueManager.pauseAll());

        JButton btnDelete = createToolButton("🗑 Delete", "Delete selected download (from list or disk)");
        btnDelete.addActionListener(e -> promptDeleteSelected());

        JButton btnOpenFile = createToolButton("📂 Open File", "Open downloaded file");
        btnOpenFile.addActionListener(e -> openSelectedFile());

        JButton btnOpenFolder = createToolButton("📁 Open Folder", "Open download directory");
        btnOpenFolder.addActionListener(e -> openSelectedFolder());

        JButton btnSettings = createToolButton("⚙ Options", "Open settings");
        btnSettings.addActionListener(e -> showSettingsDialog());

        tb.add(btnAdd);
        tb.addSeparator(new Dimension(8, 20));
        tb.add(btnResume);
        tb.add(btnPause);
        tb.add(btnStop);
        tb.addSeparator(new Dimension(8, 20));
        tb.add(btnDelete);
        tb.addSeparator(new Dimension(8, 20));
        tb.add(btnOpenFile);
        tb.add(btnOpenFolder);
        tb.add(Box.createHorizontalGlue());
        tb.add(btnSettings);

        return tb;
    }

    private JButton createToolButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.setFocusable(false);
        btn.setFont(btn.getFont().deriveFont(12f));
        btn.setMargin(new Insets(5, 10, 5, 10));
        return btn;
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem miOpen = new JMenuItem("Open File");
        miOpen.addActionListener(e -> openSelectedFile());

        JMenuItem miOpenFolder = new JMenuItem("Open Containing Folder");
        miOpenFolder.addActionListener(e -> openSelectedFolder());

        JMenuItem miDetails = new JMenuItem("Download Status / Segment Acceleration...");
        miDetails.addActionListener(e -> openSelectedTaskDetails());

        JMenuItem miResume = new JMenuItem("Resume");
        miResume.addActionListener(e -> resumeSelected());

        JMenuItem miPause = new JMenuItem("Pause");
        miPause.addActionListener(e -> pauseSelected());

        JMenuItem miRestart = new JMenuItem("Restart from Beginning");
        miRestart.addActionListener(e -> restartSelected());

        JMenuItem miCopyUrl = new JMenuItem("Copy URL to Clipboard");
        miCopyUrl.addActionListener(e -> {
            DownloadTask t = getSelectedTask();
            if (t != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection(t.getUrl()), null
                );
            }
        });

        JMenuItem miDelete = new JMenuItem("Delete Download...");
        miDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        miDelete.addActionListener(e -> promptDeleteSelected());

        JMenuItem miDeleteFile = new JMenuItem("Permanently Delete File from Disk");
        miDeleteFile.addActionListener(e -> deleteSelectedTasks(true));

        JMenuItem miRemoveOnly = new JMenuItem("Remove from List Only");
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
            Image img = getDefaultIconImage();
            try {
                java.net.URL iconUrl = getClass().getResource("/icon.png");
                if (iconUrl != null) {
                    img = new ImageIcon(iconUrl).getImage();
                }
            } catch (Exception ignored) {}

            PopupMenu popup = new PopupMenu();
            MenuItem miOpen = new MenuItem("Open IDM");
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

            trayIcon = new TrayIcon(img, "Internet Download Manager", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
                toFront();
            });

            tray.add(trayIcon);

        } catch (Exception ignored) {}
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

    private void openSelectedFile() {
        DownloadTask task = getSelectedTask();
        if (task != null) {
            File f = task.getOutputFile();
            if (f.exists()) {
                try {
                    Desktop.getDesktop().open(f);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Could not open file: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(this, "File does not exist or has not finished downloading.");
            }
        }
    }

    private void openSelectedFolder() {
        DownloadTask task = getSelectedTask();
        if (task != null) {
            File dir = new File(task.getSaveDirectory());
            if (dir.exists()) {
                try {
                    Desktop.getDesktop().open(dir);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Could not open directory: " + ex.getMessage());
                }
            }
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
