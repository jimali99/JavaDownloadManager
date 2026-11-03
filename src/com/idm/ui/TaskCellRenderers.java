package com.idm.ui;

import com.idm.model.DownloadCategory;
import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class TaskCellRenderers {

    public static class FileNameRenderer extends DefaultTableCellRenderer {
        private final DownloadTableModel model;

        public FileNameRenderer(DownloadTableModel model) {
            this.model = model;
            setBorder(new EmptyBorder(0, 8, 0, 8));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            DownloadTask task = model.getTaskAt(row);
            if (task != null) {
                DownloadCategory cat = task.getCategory();
                setIcon(AppIcons.getCategoryIcon(cat, 18));
            } else {
                setIcon(AppIcons.getCategoryIcon(DownloadCategory.OTHER, 18));
            }

            setFont(getFont().deriveFont(12.5f));
            setIconTextGap(8);
            return this;
        }
    }

    public static class StatusRenderer extends DefaultTableCellRenderer {
        private final DownloadTableModel model;

        public StatusRenderer(DownloadTableModel model) {
            this.model = model;
            setBorder(new EmptyBorder(0, 6, 0, 6));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            DownloadTask task = model.getTaskAt(row);
            if (task != null) {
                DownloadStatus status = task.getStatus();
                setIcon(AppIcons.getStatusIcon(status, 14));
                setIconTextGap(6);

                if (!isSelected) {
                    switch (status) {
                        case COMPLETED:
                            setForeground(new Color(34, 197, 94)); // Green
                            break;
                        case DOWNLOADING:
                        case CONNECTING:
                            setForeground(new Color(59, 130, 246)); // Blue
                            break;
                        case PAUSED:
                            setForeground(new Color(245, 158, 11)); // Amber
                            break;
                        case ERROR:
                            setForeground(new Color(239, 68, 68)); // Red
                            break;
                        default:
                            setForeground(new Color(148, 163, 184)); // Slate
                            break;
                    }
                }
            }
            setFont(getFont().deriveFont(Font.BOLD, 12f));
            return this;
        }
    }
}
