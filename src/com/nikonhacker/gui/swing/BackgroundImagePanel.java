package com.nikonhacker.gui.swing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.MediaTracker;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import javax.swing.JPanel;

public class BackgroundImagePanel extends JPanel {
    private Image backgroundImage = null;

    public BackgroundImagePanel(Image backgroundImage) {
        super();
        setImage(backgroundImage);
    }

    public BackgroundImagePanel(LayoutManager manager, Image backgroundImage) {
        super(manager);
        setImage(backgroundImage);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        
        //int imwidth = backgroundImage.getWidth(null);
        //int imheight = backgroundImage.getHeight(null);
        g.drawImage(backgroundImage, 1, 1, null);
    }

    private void setImage(Image backgroundImage) {
        this.backgroundImage = backgroundImage;
        MediaTracker mt = new MediaTracker(this);
        mt.addImage(this.backgroundImage, 0);
        try {
            mt.waitForAll();
        } catch (InterruptedException e) {
        }
    }

}
