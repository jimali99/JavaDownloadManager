package com.idm.ui;

import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ProgressCellRenderer extends JProgressBar implements TableCellRenderer {

    public ProgressCellRenderer() {
        super(0, 100);
        setStringPainted(true);
        setBorderPainted(false);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof DownloadTask) {
            DownloadTask task = (DownloadTask) value;
            int progress = (int) Math.round(task.getProgressPercentage());
            setValue(progress);

            DownloadStatus status = task.getStatus();
            setString(progress + "%");

            if (status == DownloadStatus.COMPLETED) {
                setForeground(new Color(34, 197, 94)); // Green
            } else if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.CONNECTING) {
                setForeground(new Color(59, 130, 246)); // Blue
            } else if (status == DownloadStatus.PAUSED) {
                setForeground(new Color(234, 179, 8)); // Yellow/Amber
            } else if (status == DownloadStatus.ERROR) {
                setForeground(new Color(239, 68, 68)); // Red
                setString("Error");
            } else if (status == DownloadStatus.QUEUED) {
                setForeground(new Color(148, 163, 184)); // Slate
                setString("Queued");
            } else {
                setForeground(new Color(156, 163, 175));
            }

            setBackground(isSelected ? table.getSelectionBackground().darker() : new Color(50, 50, 50));
        }
        return this;
    }
}
