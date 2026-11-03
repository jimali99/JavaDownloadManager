package com.idm.ui;

import com.idm.model.DownloadCategory;
import com.idm.model.DownloadStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class AppIcons {

    private static final Map<String, Icon> CACHE = new HashMap<>();

    public static Icon getAddIcon(int size) {
        return getCached("add_" + size, () -> createBadgeIcon(size, new Color(37, 99, 235), g2 -> {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(Math.max(2f, size * 0.12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float mid = size / 2f;
            float span = size * 0.26f;
            g2.draw(new Line2D.Float(mid, mid - span, mid, mid + span));
            g2.draw(new Line2D.Float(mid - span, mid, mid + span, mid));
        }));
    }

    public static Icon getResumeIcon(int size) {
        return getCached("resume_" + size, () -> createBadgeIcon(size, new Color(16, 185, 129), g2 -> {
            g2.setColor(Color.WHITE);
            float mid = size / 2f;
            float r = size * 0.28f;
            Path2D.Float p = new Path2D.Float();
            p.moveTo(mid - r * 0.65f, mid - r);
            p.lineTo(mid + r * 0.95f, mid);
            p.lineTo(mid - r * 0.65f, mid + r);
            p.closePath();
            g2.fill(p);
        }));
    }

    public static Icon getPauseIcon(int size) {
        return getCached("pause_" + size, () -> createBadgeIcon(size, new Color(245, 158, 11), g2 -> {
            g2.setColor(Color.WHITE);
            float mid = size / 2f;
            float barW = Math.max(2f, size * 0.13f);
            float barH = size * 0.52f;
            float gap = size * 0.1f;
            g2.fill(new RoundRectangle2D.Float(mid - gap - barW, mid - barH / 2, barW, barH, 2, 2));
            g2.fill(new RoundRectangle2D.Float(mid + gap, mid - barH / 2, barW, barH, 2, 2));
        }));
    }

    public static Icon getStopIcon(int size) {
        return getCached("stop_" + size, () -> createBadgeIcon(size, new Color(239, 68, 68), g2 -> {
            g2.setColor(Color.WHITE);
            float mid = size / 2f;
            float s = size * 0.44f;
            g2.fill(new RoundRectangle2D.Float(mid - s / 2, mid - s / 2, s, s, 3, 3));
        }));
    }

    public static Icon getDeleteIcon(int size) {
        return getCached("delete_" + size, () -> createVectorIcon(size, g2 -> {
            float mid = size / 2f;
            g2.setColor(new Color(244, 63, 94)); // Rose red
            g2.setStroke(new BasicStroke(Math.max(1.5f, size * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Can body
            float w = size * 0.46f;
            float h = size * 0.50f;
            float x = mid - w / 2;
            float y = mid - h / 2 + size * 0.1f;
            g2.draw(new RoundRectangle2D.Float(x, y, w, h, 3, 3));

            // Lid & handle
            g2.draw(new Line2D.Float(x - 2, y, x + w + 2, y));
            float handleW = w * 0.4f;
            g2.draw(new Line2D.Float(mid - handleW / 2, y - 2.5f, mid + handleW / 2, y - 2.5f));

            // Inner ribs
            g2.draw(new Line2D.Float(mid - w * 0.2f, y + 3, mid - w * 0.2f, y + h - 3));
            g2.draw(new Line2D.Float(mid + w * 0.2f, y + 3, mid + w * 0.2f, y + h - 3));
        }));
    }

    public static Icon getOpenFileIcon(int size) {
        return getCached("openfile_" + size, () -> createVectorIcon(size, g2 -> {
            float mid = size / 2f;
            g2.setColor(new Color(56, 189, 248)); // Sky blue
            g2.setStroke(new BasicStroke(Math.max(1.5f, size * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float w = size * 0.52f;
            float h = size * 0.66f;
            float x = mid - w / 2;
            float y = mid - h / 2;
            float fold = w * 0.35f;

            Path2D.Float p = new Path2D.Float();
            p.moveTo(x, y);
            p.lineTo(x + w - fold, y);
            p.lineTo(x + w, y + fold);
            p.lineTo(x + w, y + h);
            p.lineTo(x, y + h);
            p.closePath();
            g2.draw(p);

            // Corner fold
            g2.draw(new Line2D.Float(x + w - fold, y, x + w - fold, y + fold));
            g2.draw(new Line2D.Float(x + w - fold, y + fold, x + w, y + fold));

            // Text lines
            g2.draw(new Line2D.Float(x + 3, y + h * 0.45f, x + w - 3, y + h * 0.45f));
            g2.draw(new Line2D.Float(x + 3, y + h * 0.70f, x + w * 0.7f, y + h * 0.70f));
        }));
    }

    public static Icon getOpenFolderIcon(int size) {
        return getCached("openfolder_" + size, () -> createVectorIcon(size, g2 -> {
            float mid = size / 2f;
            g2.setColor(new Color(251, 191, 36)); // Amber gold
            g2.setStroke(new BasicStroke(Math.max(1.5f, size * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float w = size * 0.72f;
            float h = size * 0.50f;
            float x = mid - w / 2;
            float y = mid - h / 2 + size * 0.06f;

            // Folder back tab
            Path2D.Float tab = new Path2D.Float();
            tab.moveTo(x, y);
            tab.lineTo(x, y - size * 0.12f);
            tab.lineTo(x + w * 0.4f, y - size * 0.12f);
            tab.lineTo(x + w * 0.5f, y);
            tab.closePath();
            g2.draw(tab);

            // Folder body
            g2.draw(new RoundRectangle2D.Float(x, y, w, h, 3, 3));
        }));
    }

    public static Icon getSettingsIcon(int size) {
        return getCached("settings_" + size, () -> createVectorIcon(size, g2 -> {
            float mid = size / 2f;
            g2.setColor(new Color(168, 85, 247)); // Purple
            g2.setStroke(new BasicStroke(Math.max(1.6f, size * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Outer teeth
            int numTeeth = 8;
            float rOut = size * 0.38f;
            float rIn = size * 0.28f;
            Path2D.Float cog = new Path2D.Float();

            for (int i = 0; i < numTeeth; i++) {
                double a1 = (i * 2 * Math.PI) / numTeeth;
                double a2 = a1 + (Math.PI / numTeeth) * 0.5;
                double a3 = a1 + (Math.PI / numTeeth);
                double a4 = a1 + (Math.PI / numTeeth) * 1.5;

                float x1 = (float) (mid + rIn * Math.cos(a1));
                float y1 = (float) (mid + rIn * Math.sin(a1));
                float x2 = (float) (mid + rOut * Math.cos(a2));
                float y2 = (float) (mid + rOut * Math.sin(a2));
                float x3 = (float) (mid + rOut * Math.cos(a3));
                float y3 = (float) (mid + rOut * Math.sin(a3));
                float x4 = (float) (mid + rIn * Math.cos(a4));
                float y4 = (float) (mid + rIn * Math.sin(a4));

                if (i == 0) cog.moveTo(x1, y1);
                else cog.lineTo(x1, y1);
                cog.lineTo(x2, y2);
                cog.lineTo(x3, y3);
                cog.lineTo(x4, y4);
            }
            cog.closePath();
            g2.draw(cog);

            // Center hole
            float holeR = size * 0.12f;
            g2.draw(new Ellipse2D.Float(mid - holeR, mid - holeR, holeR * 2, holeR * 2));
        }));
    }

    public static Icon getRestartIcon(int size) {
        return getCached("restart_" + size, () -> createVectorIcon(size, g2 -> {
            float mid = size / 2f;
            g2.setColor(new Color(59, 130, 246));
            g2.setStroke(new BasicStroke(Math.max(1.8f, size * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float r = size * 0.32f;
            Arc2D.Float arc = new Arc2D.Float(mid - r, mid - r, r * 2, r * 2, 45, 270, Arc2D.OPEN);
            g2.draw(arc);

            // Arrow head
            float tipX = (float) (mid + r * Math.cos(Math.toRadians(45)));
            float tipY = (float) (mid - r * Math.sin(Math.toRadians(45)));
            g2.draw(new Line2D.Float(tipX, tipY, tipX + size * 0.16f, tipY));
            g2.draw(new Line2D.Float(tipX, tipY, tipX, tipY - size * 0.16f));
        }));
    }

    public static Icon getCategoryIcon(DownloadCategory category, int size) {
        return getCached("cat_" + category.name() + "_" + size, () -> {
            switch (category) {
                case ALL:
                    return createVectorIcon(size, g2 -> {
                        g2.setColor(new Color(99, 102, 241)); // Indigo
                        drawLayersIcon(g2, size);
                    });
                case DOWNLOADING:
                    return createVectorIcon(size, g2 -> {
                        g2.setColor(new Color(14, 165, 233)); // Sky
                        drawLightningIcon(g2, size);
                    });
                case COMPLETED:
                    return createVectorIcon(size, g2 -> {
                        g2.setColor(new Color(34, 197, 94)); // Green
                        drawCheckmarkBadge(g2, size);
                    });
                case PAUSED:
                    return createVectorIcon(size, g2 -> {
                        g2.setColor(new Color(245, 158, 11)); // Amber
                        drawPauseIcon(g2, size);
                    });
                case FAILED:
                    return createVectorIcon(size, g2 -> {
                        g2.setColor(new Color(239, 68, 68)); // Red
                        drawCrossIcon(g2, size);
                    });
                case COMPRESSED:
                    return createVectorIcon(size, g2 -> {
                        g2.setColor(new Color(168, 85, 247)); // Purple
                        drawPackageIcon(g2, size);
                    });
                case DOCUMENTS:
                    return createVectorIcon(size, g2 -> {
                        g2.setColor(new Color(59, 130, 246)); // Blue
                        drawDocumentIcon(g2, size);
                    });
                case VIDEOS:
                    return createVectorIcon(size, g2 -> {
                        g2.setColor(new Color(244, 63, 94)); // Rose
                        drawFilmPlayIcon(g2, size);
                    });
                case MUSIC:
                    return createVectorIcon(size, g2 -> {
                        g2.setColor(new Color(236, 72, 153)); // Pink
                        drawMusicNoteIcon(g2, size);
                    });
                case PROGRAMS:
                    return createVectorIcon(size, g2 -> {
                        g2.setColor(new Color(20, 184, 166)); // Teal
                        drawAppWindowIcon(g2, size);
                    });
                default:
                    return createVectorIcon(size, g2 -> {
                        g2.setColor(new Color(156, 163, 175)); // Gray
                        drawFolderIcon(g2, size);
                    });
            }
        });
    }

    public static Icon getStatusIcon(DownloadStatus status, int size) {
        return getCached("status_" + status.name() + "_" + size, () -> {
            switch (status) {
                case COMPLETED:
                    return createBadgeIcon(size, new Color(34, 197, 94), g2 -> {
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(Math.max(1.6f, size * 0.12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        float mid = size / 2f;
                        Path2D.Float check = new Path2D.Float();
                        check.moveTo(mid - size * 0.22f, mid);
                        check.lineTo(mid - size * 0.05f, mid + size * 0.18f);
                        check.lineTo(mid + size * 0.24f, mid - size * 0.15f);
                        g2.draw(check);
                    });
                case DOWNLOADING:
                case CONNECTING:
                    return createBadgeIcon(size, new Color(59, 130, 246), g2 -> {
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(Math.max(1.6f, size * 0.12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        float mid = size / 2f;
                        g2.draw(new Line2D.Float(mid, mid - size * 0.2f, mid, mid + size * 0.15f));
                        Path2D.Float arrow = new Path2D.Float();
                        arrow.moveTo(mid - size * 0.15f, mid + size * 0.05f);
                        arrow.lineTo(mid, mid + size * 0.22f);
                        arrow.lineTo(mid + size * 0.15f, mid + size * 0.05f);
                        g2.draw(arrow);
                    });
                case PAUSED:
                    return createBadgeIcon(size, new Color(245, 158, 11), g2 -> {
                        g2.setColor(Color.WHITE);
                        float mid = size / 2f;
                        float bw = size * 0.12f;
                        float bh = size * 0.44f;
                        float gap = size * 0.08f;
                        g2.fill(new RoundRectangle2D.Float(mid - gap - bw, mid - bh / 2, bw, bh, 2, 2));
                        g2.fill(new RoundRectangle2D.Float(mid + gap, mid - bh / 2, bw, bh, 2, 2));
                    });
                case ERROR:
                    return createBadgeIcon(size, new Color(239, 68, 68), g2 -> {
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(Math.max(1.8f, size * 0.13f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        float mid = size / 2f;
                        float s = size * 0.18f;
                        g2.draw(new Line2D.Float(mid - s, mid - s, mid + s, mid + s));
                        g2.draw(new Line2D.Float(mid + s, mid - s, mid - s, mid + s));
                    });
                default:
                    return createBadgeIcon(size, new Color(148, 163, 184), g2 -> {
                        g2.setColor(Color.WHITE);
                        float mid = size / 2f;
                        float r = size * 0.15f;
                        g2.fill(new Ellipse2D.Float(mid - r, mid - r, r * 2, r * 2));
                    });
            }
        });
    }

    // Helper draw routines
    private static void drawLayersIcon(Graphics2D g2, int size) {
        float mid = size / 2f;
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float w = size * 0.65f;
        float h = size * 0.26f;
        drawDiamond(g2, mid, size * 0.35f, w, h);
        drawDiamond(g2, mid, size * 0.55f, w, h);
        drawDiamond(g2, mid, size * 0.75f, w, h);
    }

    private static void drawDiamond(Graphics2D g2, float cx, float cy, float w, float h) {
        Path2D.Float p = new Path2D.Float();
        p.moveTo(cx, cy - h / 2);
        p.lineTo(cx + w / 2, cy);
        p.lineTo(cx, cy + h / 2);
        p.lineTo(cx - w / 2, cy);
        p.closePath();
        g2.draw(p);
    }

    private static void drawLightningIcon(Graphics2D g2, int size) {
        float mid = size / 2f;
        Path2D.Float p = new Path2D.Float();
        p.moveTo(mid + size * 0.05f, size * 0.16f);
        p.lineTo(mid - size * 0.25f, mid + size * 0.05f);
        p.lineTo(mid, mid + size * 0.05f);
        p.lineTo(mid - size * 0.05f, size * 0.84f);
        p.lineTo(mid + size * 0.25f, mid - size * 0.05f);
        p.lineTo(mid, mid - size * 0.05f);
        p.closePath();
        g2.fill(p);
    }

    private static void drawCheckmarkBadge(Graphics2D g2, int size) {
        float mid = size / 2f;
        g2.fill(new Ellipse2D.Float(size * 0.16f, size * 0.16f, size * 0.68f, size * 0.68f));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Float check = new Path2D.Float();
        check.moveTo(mid - size * 0.18f, mid);
        check.lineTo(mid - size * 0.04f, mid + size * 0.15f);
        check.lineTo(mid + size * 0.20f, mid - size * 0.13f);
        g2.draw(check);
    }

    private static void drawPauseIcon(Graphics2D g2, int size) {
        float mid = size / 2f;
        float bw = size * 0.14f;
        float bh = size * 0.54f;
        float gap = size * 0.09f;
        g2.fill(new RoundRectangle2D.Float(mid - gap - bw, mid - bh / 2, bw, bh, 3, 3));
        g2.fill(new RoundRectangle2D.Float(mid + gap, mid - bh / 2, bw, bh, 3, 3));
    }

    private static void drawCrossIcon(Graphics2D g2, int size) {
        float mid = size / 2f;
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float s = size * 0.24f;
        g2.draw(new Line2D.Float(mid - s, mid - s, mid + s, mid + s));
        g2.draw(new Line2D.Float(mid + s, mid - s, mid - s, mid + s));
    }

    private static void drawPackageIcon(Graphics2D g2, int size) {
        float mid = size / 2f;
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float s = size * 0.56f;
        g2.draw(new RoundRectangle2D.Float(mid - s / 2, mid - s / 2, s, s, 3, 3));
        // Zip seam
        g2.draw(new Line2D.Float(mid, mid - s / 2, mid, mid + s / 2));
        g2.draw(new Line2D.Float(mid - s * 0.18f, mid - size * 0.08f, mid + s * 0.18f, mid - size * 0.08f));
        g2.draw(new Line2D.Float(mid - s * 0.18f, mid + size * 0.08f, mid + s * 0.18f, mid + size * 0.08f));
    }

    private static void drawDocumentIcon(Graphics2D g2, int size) {
        float mid = size / 2f;
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float w = size * 0.52f;
        float h = size * 0.64f;
        g2.draw(new RoundRectangle2D.Float(mid - w / 2, mid - h / 2, w, h, 3, 3));
        g2.draw(new Line2D.Float(mid - w * 0.28f, mid - h * 0.22f, mid + w * 0.28f, mid - h * 0.22f));
        g2.draw(new Line2D.Float(mid - w * 0.28f, mid, mid + w * 0.28f, mid));
        g2.draw(new Line2D.Float(mid - w * 0.28f, mid + h * 0.22f, mid + w * 0.1f, mid + h * 0.22f));
    }

    private static void drawFilmPlayIcon(Graphics2D g2, int size) {
        float mid = size / 2f;
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float w = size * 0.66f;
        float h = size * 0.50f;
        g2.draw(new RoundRectangle2D.Float(mid - w / 2, mid - h / 2, w, h, 4, 4));

        // Little play triangle inside
        Path2D.Float play = new Path2D.Float();
        play.moveTo(mid - size * 0.08f, mid - size * 0.12f);
        play.lineTo(mid + size * 0.12f, mid);
        play.lineTo(mid - size * 0.08f, mid + size * 0.12f);
        play.closePath();
        g2.fill(play);
    }

    private static void drawMusicNoteIcon(Graphics2D g2, int size) {
        float mid = size / 2f;
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Stem
        g2.draw(new Line2D.Float(mid + size * 0.08f, size * 0.24f, mid + size * 0.08f, size * 0.68f));
        // Flag
        Path2D.Float flag = new Path2D.Float();
        flag.moveTo(mid + size * 0.08f, size * 0.24f);
        flag.curveTo(mid + size * 0.28f, size * 0.26f, mid + size * 0.26f, size * 0.44f, mid + size * 0.08f, size * 0.50f);
        g2.draw(flag);
        // Note head
        g2.fill(new Ellipse2D.Float(mid - size * 0.18f, size * 0.60f, size * 0.26f, size * 0.18f));
    }

    private static void drawAppWindowIcon(Graphics2D g2, int size) {
        float mid = size / 2f;
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float s = size * 0.62f;
        g2.draw(new RoundRectangle2D.Float(mid - s / 2, mid - s / 2, s, s, 4, 4));
        g2.draw(new Line2D.Float(mid - s / 2, mid - s * 0.2f, mid + s / 2, mid - s * 0.2f));
        // 3 window dots
        g2.fill(new Ellipse2D.Float(mid - s * 0.35f, mid - s * 0.38f, 2.5f, 2.5f));
        g2.fill(new Ellipse2D.Float(mid - s * 0.15f, mid - s * 0.38f, 2.5f, 2.5f));
        g2.fill(new Ellipse2D.Float(mid + s * 0.05f, mid - s * 0.38f, 2.5f, 2.5f));
    }

    private static void drawFolderIcon(Graphics2D g2, int size) {
        float mid = size / 2f;
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float w = size * 0.68f;
        float h = size * 0.48f;
        float x = mid - w / 2;
        float y = mid - h / 2 + size * 0.05f;

        Path2D.Float tab = new Path2D.Float();
        tab.moveTo(x, y);
        tab.lineTo(x, y - size * 0.1f);
        tab.lineTo(x + w * 0.35f, y - size * 0.1f);
        tab.lineTo(x + w * 0.45f, y);
        tab.closePath();
        g2.draw(tab);
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, 3, 3));
    }

    // Badge background with inner custom render
    private static Icon createBadgeIcon(int size, Color bg, IconDrawer drawer) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        setupGraphics(g2);

        // Circular background with subtle gradient
        GradientPaint gp = new GradientPaint(0, 0, bg.brighter(), 0, size, bg.darker());
        g2.setPaint(gp);
        g2.fill(new Ellipse2D.Float(1, 1, size - 2, size - 2));

        // Subtle highlight ring
        g2.setColor(new Color(255, 255, 255, 40));
        g2.draw(new Ellipse2D.Float(1.5f, 1.5f, size - 3f, size - 3f));

        drawer.draw(g2);
        g2.dispose();
        return new ImageIcon(img);
    }

    private static Icon createVectorIcon(int size, IconDrawer drawer) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        setupGraphics(g2);
        drawer.draw(g2);
        g2.dispose();
        return new ImageIcon(img);
    }

    public static java.util.List<Image> getAppIconImages() {
        java.util.List<Image> icons = new java.util.ArrayList<>();
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        for (int size : sizes) {
            Image img = loadResourceImage("/resources/icon" + size + ".png");
            if (img == null) {
                img = loadResourceImage("/icon" + size + ".png");
            }
            if (img != null) {
                icons.add(img);
            }
        }
        if (icons.isEmpty()) {
            Image logo = getAppLogoImage();
            if (logo != null) icons.add(logo);
        }
        return icons;
    }

    public static Image getAppLogoImage() {
        Image img = loadResourceImage("/resources/logo.png");
        if (img == null) img = loadResourceImage("/logo.png");
        if (img == null) img = loadResourceImage("/resources/icon64.png");
        return img;
    }

    public static Image getBannerImage() {
        Image img = loadResourceImage("/resources/logo-banner.png");
        if (img == null) img = loadResourceImage("/logo-banner.png");
        return img;
    }

    public static Icon getLogoIcon(int width, int height) {
        Image img = getAppLogoImage();
        if (img != null) {
            return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
        }
        return getCategoryIcon(DownloadCategory.DOWNLOADING, width);
    }

    private static Image loadResourceImage(String path) {
        try {
            java.io.InputStream in = AppIcons.class.getResourceAsStream(path);
            if (in == null) {
                in = AppIcons.class.getClassLoader().getResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
            }
            if (in != null) {
                try {
                    return javax.imageio.ImageIO.read(in);
                } finally {
                    in.close();
                }
            }
            String clean = path.startsWith("/") ? path.substring(1) : path;
            java.io.File file = new java.io.File(clean);
            if (file.exists()) {
                return javax.imageio.ImageIO.read(file);
            }
            file = new java.io.File("resources/" + new java.io.File(clean).getName());
            if (file.exists()) {
                return javax.imageio.ImageIO.read(file);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void setupGraphics(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static synchronized Icon getCached(String key, IconProducer producer) {
        if (!CACHE.containsKey(key)) {
            CACHE.put(key, producer.produce());
        }
        return CACHE.get(key);
    }

    @FunctionalInterface
    private interface IconDrawer {
        void draw(Graphics2D g2);
    }

    @FunctionalInterface
    private interface IconProducer {
        Icon produce();
    }
}
