package com.nikonhacker.gui.component.image;

//<editor-fold defaultstate="collapsed" desc="imports">
import com.nikonhacker.gui.swing.BackgroundImagePanel;

import java.awt.Toolkit;
import javax.swing.JFrame;
//</editor-fold>


public class TestBackgroundImage extends JFrame {
    TestBackgroundImage() {
        BackgroundImagePanel backgroundImagePanel = new BackgroundImagePanel(Toolkit.getDefaultToolkit().getImage(TestBackgroundImage.class.getResource("nh_full.jpg")));
        add(backgroundImagePanel);
        setSize(500, 300);
    }

    /*
    public static void main(String[] args) {
        TestBackgroundImage jrFrame = new TestBackgroundImage();
        jrFrame.setVisible(true);
    }
    */
}

