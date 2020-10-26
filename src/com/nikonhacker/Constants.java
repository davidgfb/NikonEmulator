package com.nikonhacker;

//<editor-fold defaultstate="collapsed" desc="imports">
import java.awt.Color;
import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.ORANGE;
import static java.awt.Color.RED;
//</editor-fold>

public interface Constants {
    
    //<editor-fold defaultstate="collapsed" desc="vars">
    /* These 2 constants are used to index everything pertaining to one chip or the other */
    int      CHIP_NONE             = -1,
             CHIP_FR               = 0,
             CHIP_TX               = 1,
             CHIP_ARM              = 2;
    String[] CHIP_LABEL            = {"FR80", "TX19"};
    Color[]  CHIP_BACKGROUND_COLOR = {new Color(240, 240, 255), new Color(248, 255, 248)};

    Color COLOR_HI  = RED,
          COLOR_HIZ = ORANGE,
          COLOR_LO  = BLUE,
          COLOR_PULLUP = GREEN;

    String LABEL_HI  = "VCC",
           LABEL_HIZ = "---",
           LABEL_LO  = "GND";
//</editor-fold>
}
