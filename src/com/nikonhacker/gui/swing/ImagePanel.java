package com.nikonhacker.gui.swing;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public class ImagePanel extends JPanel {
    BufferedImage image;
    private Dimension dimension;

    public ImagePanel() {
    }

    public ImagePanel(BufferedImage image) {
        this.image = image;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        dimension = new Dimension(image.getWidth(), image.getHeight());
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);
        if(image != null){
            g.drawImage(image, 0, 0, this);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (image != null) {
            return dimension;
        }
        else {
            return super.getSize();
        }
    }
}
