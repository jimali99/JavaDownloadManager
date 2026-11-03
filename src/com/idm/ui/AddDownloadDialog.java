package com.idm.ui;

import com.idm.engine.DownloadQueueManager;
import com.idm.engine.UrlProber;
import com.idm.model.AppSettings;
import com.idm.model.DownloadTask;
import com.idm.util.FormatUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;

public class AddDownloadDialog extends JDialog {

    private final DownloadQueueManager queueManager;
    private final AppSettings settings;

    private final JTextField txtUrl = new JTextField(35);
    private final JTextField txtFileName = new JTextField(25);
    private final JTextField txtSaveDir = new JTextField(25);
    private final JSpinner spConnections = new JSpinner(new SpinnerNumberModel(8, 1, 32, 1));
    private final JLabel lblFileSize = new JLabel("Probing URL...");
    private final JLabel lblResumeSupport = new JLabel("-");
    private final JButton btnDownloadNow = new JButton("Download Now");
    private final JButton btnDownloadLater = new JButton("Download Later");
    private final JButton btnBrowse = new JButton("Browse...");
    private final JButton btnProbe = new JButton("Probe");

    private volatile boolean probing = false;

    public AddDownloadDialog(Window owner, String initialUrl, DownloadQueueManager queueManager, AppSettings settings) {
        super(owner, "Enter New Address to Download", ModalityType.APPLICATION_MODAL);
        this.queueManager = queueManager;
        this.settings = settings;

        initUI();

        if (initialUrl != null && !initialUrl.trim().isEmpty()) {
            txtUrl.setText(initialUrl.trim());
            triggerProbe();
        }

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: URL
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(new JLabel("Address (URL):"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        JPanel urlPanel = new JPanel(new BorderLayout(5, 0));
        urlPanel.add(txtUrl, BorderLayout.CENTER);
        urlPanel.add(btnProbe, BorderLayout.EAST);
        form.add(urlPanel, gbc);

        // Row 1: File Name
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(new JLabel("Save As (Name):"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        form.add(txtFileName, gbc);

        // Row 2: Save To
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(new JLabel("Save In Folder:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        JPanel savePanel = new JPanel(new BorderLayout(5, 0));
        txtSaveDir.setText(settings.getDefaultDownloadDir());
        savePanel.add(txtSaveDir, BorderLayout.CENTER);
        savePanel.add(btnBrowse, BorderLayout.EAST);
        form.add(savePanel, gbc);

        // Row 3: Connections
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        form.add(new JLabel("Max Connections:"), gbc);

        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        JPanel connPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        spConnections.setValue(settings.getDefaultConnections());
        connPanel.add(spConnections);
        JLabel lblHint = new JLabel("  (1 - 32 threads for accelerated speeds)");
        lblHint.setForeground(Color.GRAY);
        connPanel.add(lblHint);
        form.add(connPanel, gbc);

        // Row 4: Info (Size & Resume)
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        form.add(new JLabel("File Size:"), gbc);

        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0;
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        infoPanel.add(lblFileSize);
        infoPanel.add(new JLabel("Resume Support:"));
        infoPanel.add(lblResumeSupport);
        form.add(infoPanel, gbc);

        add(form, BorderLayout.CENTER);

        // Bottom Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnCancel = new JButton("Cancel");

        btnDownloadNow.setFont(btnDownloadNow.getFont().deriveFont(Font.BOLD));

        buttonPanel.add(btnDownloadLater);
        buttonPanel.add(btnDownloadNow);
        buttonPanel.add(btnCancel);

        add(buttonPanel, BorderLayout.SOUTH);

        // Listeners
        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(txtSaveDir.getText());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                txtSaveDir.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        btnProbe.addActionListener(e -> triggerProbe());

        txtUrl.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (txtFileName.getText().trim().isEmpty() && !txtUrl.getText().trim().isEmpty()) {
                    triggerProbe();
                }
            }
        });

        btnDownloadNow.addActionListener(e -> startDownload(true));
        btnDownloadLater.addActionListener(e -> startDownload(false));
        btnCancel.addActionListener(e -> dispose());
    }

    private void triggerProbe() {
        final String url = txtUrl.getText().trim();
        if (url.isEmpty() || probing) return;

        probing = true;
        lblFileSize.setText("Connecting to server...");
        lblResumeSupport.setText("Checking...");
        btnProbe.setEnabled(false);

        new Thread(() -> {
            try {
                UrlProber.ProbeResult probe = UrlProber.probe(url, settings.getUserAgent());
                SwingUtilities.invokeLater(() -> {
                    if (txtFileName.getText().trim().isEmpty() || txtFileName.getText().startsWith("download_")) {
                        txtFileName.setText(probe.getFileName());
                    }
                    lblFileSize.setText(FormatUtils.formatBytes(probe.getContentLength()));
                    lblResumeSupport.setText(probe.isResumeSupported() ? "Yes (Accelerated)" : "No (Single thread)");
                    lblResumeSupport.setForeground(probe.isResumeSupported() ? new Color(34, 197, 94) : new Color(234, 179, 8));
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblFileSize.setText("Unknown size");
                    lblResumeSupport.setText("Unknown");
                    if (txtFileName.getText().trim().isEmpty()) {
                        txtFileName.setText("download_" + System.currentTimeMillis());
                    }
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    probing = false;
                    btnProbe.setEnabled(true);
                });
            }
        }, "UrlProbeWorker").start();
    }

    private void startDownload(boolean startNow) {
        String url = txtUrl.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid URL.", "Missing URL", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fileName = txtFileName.getText().trim();
        if (fileName.isEmpty()) {
            fileName = "download_" + System.currentTimeMillis();
        }
        fileName = FormatUtils.sanitizeFileName(fileName);

        String saveDir = txtSaveDir.getText().trim();
        if (saveDir.isEmpty()) {
            saveDir = settings.getDefaultDownloadDir();
        }

        int conns = (Integer) spConnections.getValue();

        DownloadTask task = new DownloadTask(url, fileName, saveDir, conns);
        queueManager.addTask(task, startNow);

        dispose();

        if (startNow) {
            // Automatically open Download Detail popup
            SwingUtilities.invokeLater(() -> {
                DownloadDetailDialog dialog = new DownloadDetailDialog(getOwner(), task, queueManager);
                dialog.setVisible(true);
            });
        }
    }
}
