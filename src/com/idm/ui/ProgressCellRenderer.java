package com.idm.ui;

import com.idm.model.DownloadStatus;
import com.idm.model.DownloadTask;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ProgressCellRenderer extends JPanel implements TableCellRenderer {

    private int progress = 0;
    private DownloadStatus status = DownloadStatus.QUEUED;
    private String customText = "";
    private boolean isSelected = false;

    public ProgressCellRenderer() {
        setOpaque(true);
        setBorder(new EmptyBorder(4, 8, 4, 8));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.isSelected = isSelected;
        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

        if (value instanceof DownloadTask) {
            DownloadTask task = (DownloadTask) value;
            this.progress = (int) Math.round(task.getProgressPercentage());
            this.status = task.getStatus();
            this.customText = progress + "%";

            if (status == DownloadStatus.ERROR) {
                customText = "Failed";
            } else if (status == DownloadStatus.QUEUED) {
                customText = "Queued";
            } else if (status == DownloadStatus.COMPLETED) {
                customText = "100%";
            }
        } else {
            this.progress = 0;
            this.status = DownloadStatus.QUEUED;
            this.customText = "";
        }

        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int padX = 6;
        int padY = 5;
        int width = getWidth() - padX * 2;
        int height = getHeight() - padY * 2;
        if (width <= 0 || height <= 0) {
            g2.dispose();
            return;
        }

        int arc = Math.min(height, 8);

        // Track background
        g2.setColor(isSelected ? new Color(40, 50, 65) : new Color(31, 41, 55));
        g2.fill(new RoundRectangle2D.Float(padX, padY, width, height, arc, arc));

        // Border outline
        g2.setColor(new Color(75, 85, 99, 80));
        g2.draw(new RoundRectangle2D.Float(padX, padY, width - 1, height - 1, arc, arc));

        // Fill bar
        int fillW = (int) Math.round((progress / 100.0) * width);
        fillW = Math.max(0, Math.min(width, fillW));

        if (fillW > 0) {
            Paint fillPaint;
            switch (status) {
                case COMPLETED:
                    fillPaint = new GradientPaint(padX, padY, new Color(34, 197, 94), padX + fillW, padY, new Color(16, 185, 129));
                    break;
                case DOWNLOADING:
                case CONNECTING:
                    fillPaint = new GradientPaint(padX, padY, new Color(59, 130, 246), padX + fillW, padY, new Color(37, 99, 235));
                    break;
                case PAUSED:
                    fillPaint = new GradientPaint(padX, padY, new Color(245, 158, 11), padX + fillW, padY, new Color(217, 119, 6));
                    break;
                case ERROR:
                    fillPaint = new GradientPaint(padX, padY, new Color(239, 68, 68), padX + fillW, padY, new Color(220, 38, 38));
                    break;
                default:
                    fillPaint = new Color(100, 116, 139);
                    break;
            }

            Shape origClip = g2.getClip();
            g2.clip(new RoundRectangle2D.Float(padX, padY, width, height, arc, arc));
            g2.setPaint(fillPaint);
            g2.fill(new RoundRectangle2D.Float(padX, padY, fillW, height, arc, arc));
            g2.setClip(origClip);
        }

        // Percentage text
        g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(customText);
        int textH = fm.getAscent();
        int textX = padX + (width - textW) / 2;
        int textY = padY + (height + textH) / 2 - 2;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 140));
        g2.drawString(customText, textX + 1, textY + 1);

        // Foreground text
        g2.setColor(Color.WHITE);
        g2.drawString(customText, textX, textY);

        g2.dispose();
    }
}
