package com.doctracker.ui;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Draws the app icon in code (a document with a clock badge), so no image files are needed. */
public final class AppIcon {
    private AppIcon() {}

    public static Image image(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float s = size / 64f;
        g.setColor(new Color(0x2F5D8A));
        g.fillRoundRect(Math.round(10 * s), Math.round(4 * s), Math.round(36 * s), Math.round(48 * s), Math.round(6 * s), Math.round(6 * s));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(Math.max(1f, 3 * s)));
        for (int i = 0; i < 3; i++) {
            int y = Math.round((16 + i * 9) * s);
            g.drawLine(Math.round(17 * s), y, Math.round(39 * s), y);
        }
        int r = Math.round(22 * s), cx = Math.round(46 * s), cy = Math.round(46 * s);
        g.setColor(new Color(0xE0A100));
        g.fillOval(cx - r / 2 - Math.round(2 * s), cy - r / 2 - Math.round(2 * s), r + Math.round(4 * s), r + Math.round(4 * s));
        g.setColor(Color.WHITE);
        g.drawLine(cx, cy, cx, cy - Math.round(7 * s));
        g.drawLine(cx, cy, cx + Math.round(6 * s), cy);
        g.dispose();
        return img;
    }
}
