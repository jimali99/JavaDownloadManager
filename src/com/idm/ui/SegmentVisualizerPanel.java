package com.idm.ui;

import com.idm.model.DownloadSegment;
import com.idm.model.DownloadTask;
import com.idm.util.FormatUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SegmentVisualizerPanel extends JPanel {

    private final DownloadTask task;
    private final JTable connectionTable;
    private final DefaultTableModel tableModel;
    private final JPanel barPanel;

    // Segment color palette (pleasant distinct colors for IDM multi-threads)
    private static final Color[] SEGMENT_COLORS = {
        new Color(59, 130, 246),  // Blue
        new Color(16, 185, 129),  // Emerald
        new Color(245, 158, 11),  // Amber
        new Color(168, 85, 247),  // Purple
        new Color(236, 72, 153),  // Pink
        new Color(6, 182, 212),   // Cyan
        new Color(132, 204, 22),  // Lime
        new Color(249, 115, 22),  // Orange
    };

    public SegmentVisualizerPanel(DownloadTask task) {
        this.task = task;
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);

        // Top: Visual chunk progress bar
        barPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawSegmentBar((Graphics2D) g, getWidth(), getHeight());
            }
        };
        barPanel.setPreferredSize(new Dimension(500, 38));
        barPanel.setBorder(BorderFactory.createLineBorder(new Color(75, 85, 99), 1, true));
        barPanel.setBackground(new Color(30, 35, 45));

        JPanel topContainer = new JPanel(new BorderLayout(5, 5));
        topContainer.setOpaque(false);
        JLabel lblTitle = new JLabel("Connection Segments Acceleration:");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 12f));
        topContainer.add(lblTitle, BorderLayout.NORTH);
        topContainer.add(barPanel, BorderLayout.CENTER);

        // Bottom: Connection detail table
        String[] columnNames = {"Conn #", "Range", "Downloaded", "Progress", "Speed", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        connectionTable = new JTable(tableModel);
        connectionTable.setRowHeight(24);
        connectionTable.getTableHeader().setReorderingAllowed(false);
        connectionTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        connectionTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        connectionTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        connectionTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        connectionTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        connectionTable.getColumnModel().getColumn(5).setPreferredWidth(90);

        JScrollPane scrollPane = new JScrollPane(connectionTable);
        scrollPane.setPreferredSize(new Dimension(500, 140));

        add(topContainer, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        updateVisuals();
    }

    private void drawSegmentBar(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        List<DownloadSegment> segments = task.getSegments();
        long total = task.getTotalBytes();

        if (segments == null || segments.isEmpty() || total <= 0) {
            // Indeterminate or single unsegmented state
            double pct = task.getProgressPercentage();
            int fillW = (int) Math.round((pct / 100.0) * width);
            g2.setColor(new Color(59, 130, 246));
            g2.fillRect(0, 0, fillW, height);
            return;
        }

        // Draw each segment proportionally
        for (int i = 0; i < segments.size(); i++) {
            DownloadSegment seg = segments.get(i);
            long segTotal = seg.getTotalBytes();
            if (segTotal <= 0) continue;

            int segStartX = (int) ((seg.getStartByte() * (double) width) / total);
            int segEndX = (int) (((seg.getEndByte() + 1) * (double) width) / total);
            int segWidth = Math.max(1, segEndX - segStartX);

            long downloadedInSeg = seg.getDownloadedBytes();
            int dlWidth = (int) ((downloadedInSeg * (double) segWidth) / segTotal);
            dlWidth = Math.min(segWidth, Math.max(0, dlWidth));

            Color baseColor = SEGMENT_COLORS[i % SEGMENT_COLORS.length];
            // Background of segment (unfilled part)
            g2.setColor(new Color(baseColor.getRed() / 4, baseColor.getGreen() / 4, baseColor.getBlue() / 4));
            g2.fillRect(segStartX, 0, segWidth, height);

            // Filled downloaded part
            g2.setColor(baseColor);
            g2.fillRect(segStartX, 0, dlWidth, height);

            // Segment boundary divider
            g2.setColor(new Color(20, 20, 20));
            g2.drawLine(segEndX, 0, segEndX, height);
        }
    }

    public void updateVisuals() {
        barPanel.repaint();

        List<DownloadSegment> segments = task.getSegments();
        if (segments == null) return;

        // Ensure table rows match segment count
        while (tableModel.getRowCount() < segments.size()) {
            tableModel.addRow(new Object[]{"", "", "", "", "", ""});
        }
        while (tableModel.getRowCount() > segments.size()) {
            tableModel.removeRow(tableModel.getRowCount() - 1);
        }

        for (int i = 0; i < segments.size(); i++) {
            DownloadSegment seg = segments.get(i);
            String idStr = "Conn " + seg.getId();
            String rangeStr = FormatUtils.formatBytes(seg.getStartByte()) + " - " +
                              (seg.getEndByte() > 0 ? FormatUtils.formatBytes(seg.getEndByte()) : "End");
            String dlStr = FormatUtils.formatBytes(seg.getDownloadedBytes());
            String pctStr = String.format("%.1f%%", seg.getProgressPercentage());
            String speedStr = FormatUtils.formatSpeed(seg.getCurrentSpeed());
            String statusStr = seg.getStatus() != null ? seg.getStatus() : "Idle";

            tableModel.setValueAt(idStr, i, 0);
            tableModel.setValueAt(rangeStr, i, 1);
            tableModel.setValueAt(dlStr, i, 2);
            tableModel.setValueAt(pctStr, i, 3);
            tableModel.setValueAt(speedStr, i, 4);
            tableModel.setValueAt(statusStr, i, 5);
        }
    }
}
