package com.idm.ui;

import com.idm.model.DownloadCategory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class CategoryListCellRenderer extends JPanel implements ListCellRenderer<Object> {

    private final JLabel lblIcon = new JLabel();
    private final JLabel lblName = new JLabel();
    private final JLabel lblBadge = new JLabel() {
        @Override
        protected void paintComponent(Graphics g) {
            if (!getText().isEmpty() && !"0".equals(getText())) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
            super.paintComponent(g);
        }
    };

    public CategoryListCellRenderer() {
        setLayout(new BorderLayout(10, 0));
        setBorder(new EmptyBorder(6, 12, 6, 12));
        setOpaque(true);

        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblBadge.setHorizontalAlignment(SwingConstants.CENTER);
        lblBadge.setBorder(new EmptyBorder(2, 6, 2, 6));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(lblIcon);
        leftPanel.add(lblName);

        add(leftPanel, BorderLayout.WEST);
        add(lblBadge, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        DownloadCategory cat = DownloadCategory.ALL;
        int count = 0;

        if (value instanceof MainFrame.CategoryItem) {
            MainFrame.CategoryItem item = (MainFrame.CategoryItem) value;
            cat = item.category;
            count = item.count;
        }

        lblIcon.setIcon(AppIcons.getCategoryIcon(cat, 18));
        lblName.setText(cat.getDisplayName());
        lblBadge.setText(String.valueOf(count));

        if (isSelected) {
            setBackground(new Color(37, 99, 235)); // Modern royal blue
            lblName.setForeground(Color.WHITE);
            lblName.setFont(lblName.getFont().deriveFont(Font.BOLD));
            lblBadge.setForeground(new Color(37, 99, 235));
            lblBadge.setBackground(Color.WHITE);
            lblBadge.setOpaque(false);
        } else {
            setBackground(list.getBackground());
            lblName.setForeground(new Color(229, 231, 235)); // Soft white
            lblName.setFont(lblName.getFont().deriveFont(Font.PLAIN));
            lblBadge.setForeground(new Color(156, 163, 175));
            lblBadge.setBackground(new Color(55, 65, 81));
            lblBadge.setOpaque(false);
        }

        lblBadge.setVisible(count > 0);
        return this;
    }
}
