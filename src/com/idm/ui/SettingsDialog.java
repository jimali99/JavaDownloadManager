package com.idm.ui;

import com.idm.model.AppSettings;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SettingsDialog extends JDialog {

    private final AppSettings settings;
    private final JTextField txtDownloadDir = new JTextField(25);
    private final JSpinner spConnections = new JSpinner(new SpinnerNumberModel(8, 1, 32, 1));
    private final JSpinner spMaxConcurrent = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
    private final JSpinner spSpeedLimit = new JSpinner(new SpinnerNumberModel(0, 0, 1000000, 100));
    private final JCheckBox chkClipboard = new JCheckBox("Monitor system clipboard for downloadable URLs");
    private final JCheckBox chkSound = new JCheckBox("Play notification sound when download completes");
    private final JCheckBox chkDarkMode = new JCheckBox("Dark Theme");
    private final JTextField txtUserAgent = new JTextField(30);

    private boolean saved = false;

    public SettingsDialog(Window owner, AppSettings settings) {
        super(owner, "Download Manager Options & Configuration", ModalityType.APPLICATION_MODAL);
        this.settings = settings;

        initUI();
        loadValues();

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));

        JTabbedPane tabs = new JTabbedPane();

        // General tab
        JPanel generalPanel = new JPanel(new GridBagLayout());
        generalPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        generalPanel.add(new JLabel("Default Download Folder:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel dirPanel = new JPanel(new BorderLayout(5, 0));
        dirPanel.add(txtDownloadDir, BorderLayout.CENTER);
        JButton btnBrowse = new JButton("Browse...");
        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(txtDownloadDir.getText());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtDownloadDir.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        dirPanel.add(btnBrowse, BorderLayout.EAST);
        generalPanel.add(dirPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        generalPanel.add(new JLabel("Default Parallel Connections:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel conPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        conPanel.add(spConnections);
        conPanel.add(new JLabel("  (Default: 8, Max: 32)"));
        generalPanel.add(conPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        generalPanel.add(new JLabel("Max Simultaneous Downloads:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel maxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        maxPanel.add(spMaxConcurrent);
        maxPanel.add(new JLabel("  (Queue manager limit)"));
        generalPanel.add(maxPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        generalPanel.add(new JLabel("Global Speed Limit:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        speedPanel.add(spSpeedLimit);
        speedPanel.add(new JLabel(" KB/s (0 = Unlimited)"));
        generalPanel.add(speedPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        generalPanel.add(chkClipboard, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        generalPanel.add(chkSound, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        generalPanel.add(chkDarkMode, gbc);

        tabs.addTab("General & Downloads", generalPanel);

        // Network tab
        JPanel netPanel = new JPanel(new GridBagLayout());
        netPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbcNet = new GridBagConstraints();
        gbcNet.insets = new Insets(6, 6, 6, 6);
        gbcNet.fill = GridBagConstraints.HORIZONTAL;

        gbcNet.gridx = 0; gbcNet.gridy = 0;
        netPanel.add(new JLabel("Browser User-Agent Header:"), gbcNet);
        gbcNet.gridx = 0; gbcNet.gridy = 1;
        netPanel.add(txtUserAgent, gbcNet);

        JLabel lblAgentHint = new JLabel("<html><i>Using a standard Chrome/Firefox User-Agent prevents web servers and CDNs from throttling or rejecting automated download requests.</i></html>");
        lblAgentHint.setForeground(Color.GRAY);
        gbcNet.gridx = 0; gbcNet.gridy = 2;
        netPanel.add(lblAgentHint, gbcNet);

        tabs.addTab("Network & Connection", netPanel);

        add(tabs, BorderLayout.CENTER);

        // Bottom buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnSave = new JButton("Save & Apply");
        JButton btnCancel = new JButton("Cancel");

        btnSave.addActionListener(e -> {
            saveValues();
            saved = true;
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadValues() {
        txtDownloadDir.setText(settings.getDefaultDownloadDir());
        spConnections.setValue(settings.getDefaultConnections());
        spMaxConcurrent.setValue(settings.getMaxConcurrentDownloads());
        spSpeedLimit.setValue(settings.getSpeedLimitKBps());
        chkClipboard.setSelected(settings.isMonitorClipboard());
        chkSound.setSelected(settings.isPlaySoundOnComplete());
        chkDarkMode.setSelected(settings.isDarkMode());
        txtUserAgent.setText(settings.getUserAgent());
    }

    private void saveValues() {
        settings.setDefaultDownloadDir(txtDownloadDir.getText().trim());
        settings.setDefaultConnections((Integer) spConnections.getValue());
        settings.setMaxConcurrentDownloads((Integer) spMaxConcurrent.getValue());
        settings.setSpeedLimitKBps((Integer) spSpeedLimit.getValue());
        settings.setMonitorClipboard(chkClipboard.isSelected());
        settings.setPlaySoundOnComplete(chkSound.isSelected());
        settings.setDarkMode(chkDarkMode.isSelected());
        settings.setUserAgent(txtUserAgent.getText().trim());
        settings.save();
    }

    public boolean isSaved() {
        return saved;
    }
}
