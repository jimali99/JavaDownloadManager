package com.idm.ui;

import com.idm.engine.DownloadQueueManager;
import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;
import com.idm.util.FormatUtils;
import com.idm.util.StorageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class DownloadDetailDialog extends JDialog implements DownloadTask.DownloadListener {

    private final DownloadTask task;
    private final DownloadQueueManager queueManager;

    private final JLabel lblFileName = new JLabel();
    private final JLabel lblStatus = new JLabel();
    private final JLabel lblFileSize = new JLabel();
    private final JLabel lblDownloaded = new JLabel();
    private final JLabel lblTransferRate = new JLabel();
    private final JLabel lblTimeLeft = new JLabel();
    private final JLabel lblResume = new JLabel();
    private final JLabel lblSaveTo = new JLabel();
    private final JLabel lblUrl = new JLabel();

    private final JProgressBar masterProgressBar = new JProgressBar(0, 100);
    private final SegmentVisualizerPanel visualizerPanel;

    private final JButton btnPauseResume = new JButton("Pause");
    private final JButton btnCancel = new JButton("Cancel");
    private final JButton btnOpenFile = new JButton("Open File");
    private final JButton btnOpenFolder = new JButton("Open Folder");

    public DownloadDetailDialog(Window owner, DownloadTask task, DownloadQueueManager queueManager) {
        super(owner, "Download Status - " + task.getFileName(), ModalityType.MODELESS);
        this.task = task;
        this.queueManager = queueManager;
        this.visualizerPanel = new SegmentVisualizerPanel(task);

        java.util.List<Image> appIcons = AppIcons.getAppIconImages();
        if (!appIcons.isEmpty()) {
            setIconImages(appIcons);
        }

        initUI();
        task.addListener(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                task.removeListener(DownloadDetailDialog.this);
            }
        });

        updateFields();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout(10, 5));
        lblFileName.setFont(lblFileName.getFont().deriveFont(Font.BOLD, 15f));
        lblFileName.setIcon(AppIcons.getCategoryIcon(task.getCategory(), 22));
        lblFileName.setIconTextGap(10);
        headerPanel.add(lblFileName, BorderLayout.NORTH);

        // Details grid
        JPanel grid = new JPanel(new GridLayout(4, 4, 10, 6));
        grid.add(createBoldLabel("Status:"));
        grid.add(lblStatus);
        grid.add(createBoldLabel("File Size:"));
        grid.add(lblFileSize);

        grid.add(createBoldLabel("Downloaded:"));
        grid.add(lblDownloaded);
        grid.add(createBoldLabel("Transfer Rate:"));
        grid.add(lblTransferRate);

        grid.add(createBoldLabel("Time Left:"));
        grid.add(lblTimeLeft);
        grid.add(createBoldLabel("Resume Capable:"));
        grid.add(lblResume);

        grid.add(createBoldLabel("Save To:"));
        lblSaveTo.setToolTipText(task.getSaveDirectory());
        grid.add(lblSaveTo);
        grid.add(createBoldLabel("Connections:"));
        JLabel lblConns = new JLabel(task.getNumConnections() + " active threads");
        grid.add(lblConns);

        headerPanel.add(grid, BorderLayout.CENTER);

        // Progress section
        masterProgressBar.setStringPainted(true);
        masterProgressBar.setPreferredSize(new Dimension(550, 24));
        headerPanel.add(masterProgressBar, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // Center: Segment visualizer
        add(visualizerPanel, BorderLayout.CENTER);

        // Bottom: Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        btnOpenFile.setIcon(AppIcons.getOpenFileIcon(16));
        btnOpenFolder.setIcon(AppIcons.getOpenFolderIcon(16));
        btnCancel.setIcon(AppIcons.getStopIcon(16));

        btnPauseResume.addActionListener(e -> {
            if (task.getStatus().isActive()) {
                queueManager.pauseTask(task);
            } else {
                queueManager.resumeTask(task);
            }
            updateFields();
        });

        btnCancel.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel this download?",
                "Cancel Download",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                queueManager.pauseTask(task);
                dispose();
            }
        });

        btnOpenFile.addActionListener(e -> StorageUtils.openFileOrExecute(this, task.getOutputFile()));

        btnOpenFolder.addActionListener(e -> StorageUtils.openFolder(this, new File(task.getSaveDirectory()), task.getOutputFile()));

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());

        buttonPanel.add(btnOpenFile);
        buttonPanel.add(btnOpenFolder);
        buttonPanel.add(btnPauseResume);
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnClose);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JLabel createBoldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private void updateFields() {
        SwingUtilities.invokeLater(() -> {
            lblFileName.setText(task.getFileName());
            setTitle("Download Status - " + task.getFileName());

            DownloadStatus status = task.getStatus();
            lblStatus.setText(status.getDisplayName());
            if (status == DownloadStatus.COMPLETED) {
                lblStatus.setForeground(new Color(34, 197, 94));
                btnPauseResume.setEnabled(false);
                btnPauseResume.setIcon(AppIcons.getResumeIcon(16));
                btnOpenFile.setEnabled(true);
            } else if (status.isActive()) {
                lblStatus.setForeground(new Color(59, 130, 246));
                btnPauseResume.setText("Pause");
                btnPauseResume.setIcon(AppIcons.getPauseIcon(16));
                btnPauseResume.setEnabled(true);
                btnOpenFile.setEnabled(false);
            } else if (status == DownloadStatus.PAUSED) {
                lblStatus.setForeground(new Color(234, 179, 8));
                btnPauseResume.setText("Resume");
                btnPauseResume.setIcon(AppIcons.getResumeIcon(16));
                btnPauseResume.setEnabled(true);
                btnOpenFile.setEnabled(false);
            } else if (status == DownloadStatus.ERROR) {
                lblStatus.setForeground(new Color(239, 68, 68));
                lblStatus.setText("Error: " + (task.getErrorMessage() != null ? task.getErrorMessage() : "Failed"));
                btnPauseResume.setText("Resume");
                btnPauseResume.setIcon(AppIcons.getResumeIcon(16));
                btnOpenFile.setEnabled(false);
            }

            lblFileSize.setText(FormatUtils.formatBytes(task.getTotalBytes()));
            lblDownloaded.setText(FormatUtils.formatBytes(task.getDownloadedBytes()));
            lblTransferRate.setText(FormatUtils.formatSpeed(task.getCurrentSpeed()));
            lblTimeLeft.setText(FormatUtils.formatTime(task.getEtaSeconds()));
            lblResume.setText(task.isResumeSupported() ? "Yes" : "No");
            lblSaveTo.setText(task.getSaveDirectory());

            int pct = (int) Math.round(task.getProgressPercentage());
            masterProgressBar.setValue(pct);
            masterProgressBar.setString(pct + "% (" + FormatUtils.formatBytes(task.getDownloadedBytes()) + " / " + FormatUtils.formatBytes(task.getTotalBytes()) + ")");

            visualizerPanel.updateVisuals();
        });
    }

    @Override
    public void onProgressUpdate(DownloadTask task) {
        updateFields();
    }

    @Override
    public void onStatusChanged(DownloadTask task, DownloadStatus oldStatus, DownloadStatus newStatus) {
        updateFields();
    }
}
