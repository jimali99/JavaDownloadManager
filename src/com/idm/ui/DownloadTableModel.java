package com.idm.ui;

import com.idm.engine.DownloadQueueManager;
import com.idm.model.DownloadCategory;
import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;
import com.idm.util.FormatUtils;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class DownloadTableModel extends AbstractTableModel {

    private final String[] COLUMNS = {
        "File Name", "Size", "Status", "Progress", "Speed", "Time Left", "Connections", "Date Added"
    };

    private final DownloadQueueManager queueManager;
    private final List<DownloadTask> filteredTasks = new ArrayList<>();
    private DownloadCategory currentCategory = DownloadCategory.ALL;

    public DownloadTableModel(DownloadQueueManager queueManager) {
        this.queueManager = queueManager;
        refreshFilteredList();
    }

    public synchronized void setCategory(DownloadCategory category) {
        this.currentCategory = category;
        refreshFilteredList();
        fireTableDataChanged();
    }

    public synchronized void refreshFilteredList() {
        filteredTasks.clear();
        List<DownloadTask> all = queueManager.getTasks();

        for (DownloadTask t : all) {
            if (matchesCategory(t, currentCategory)) {
                filteredTasks.add(t);
            }
        }
    }

    private boolean matchesCategory(DownloadTask t, DownloadCategory cat) {
        if (cat == DownloadCategory.ALL) return true;
        if (cat == DownloadCategory.DOWNLOADING) return t.getStatus().isActive() || t.getStatus() == DownloadStatus.QUEUED;
        if (cat == DownloadCategory.COMPLETED) return t.getStatus() == DownloadStatus.COMPLETED;
        if (cat == DownloadCategory.PAUSED) return t.getStatus() == DownloadStatus.PAUSED;
        if (cat == DownloadCategory.FAILED) return t.getStatus() == DownloadStatus.ERROR || t.getStatus() == DownloadStatus.CANCELLED;
        return t.getCategory() == cat;
    }

    public synchronized DownloadTask getTaskAt(int row) {
        if (row >= 0 && row < filteredTasks.size()) {
            return filteredTasks.get(row);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return filteredTasks.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        DownloadTask t = getTaskAt(rowIndex);
        if (t == null) return null;

        switch (columnIndex) {
            case 0:
                return t.getFileName();
            case 1:
                return FormatUtils.formatBytes(t.getTotalBytes());
            case 2:
                return t.getStatus().getDisplayName();
            case 3:
                return t; // Returned for ProgressCellRenderer
            case 4:
                return t.getStatus().isActive() ? FormatUtils.formatSpeed(t.getCurrentSpeed()) : "-";
            case 5:
                return t.getStatus().isActive() ? FormatUtils.formatTime(t.getEtaSeconds()) : "-";
            case 6:
                return t.getNumConnections() + " threads";
            case 7:
                return FormatUtils.formatDate(t.getDateAdded());
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 3) {
            return DownloadTask.class;
        }
        return String.class;
    }
}
