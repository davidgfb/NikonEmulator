package com.nikonhacker.gui;

//<editor-fold defaultstate="collapsed" desc="imports">
import static com.nikonhacker.Format.asHex;
import com.nikonhacker.emu.memory.listener.TrackingMemoryActivityListener;

import javax.swing.JComponent;
import javax.swing.Timer;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.Math.max;
//</editor-fold>


public class MemoryMapComponent extends JComponent {

    private static final int UPDATE_INTERVAL_MS = 1000, // 1fps
                             MAP_WIDTH = 256,
                             MAP_HEIGHT = 256,
                             PAGE_SIZE_BITS = 16,

                             NO_SELECTION = -1;

    //deberia ser privado con get
    public static final int PAGE_MODE = -1;

    private AffineTransform resizeTransform;
    private int previousW, 
                previousH,

                selectedX = NO_SELECTION,
                selectedY = NO_SELECTION,
    
                baseAddress = PAGE_MODE;
    
    private int[] activityMap;

    private Timer refreshTimer;

    

    private BufferedImage img = new BufferedImage(MAP_WIDTH, MAP_HEIGHT, TYPE_INT_RGB);
    private double scaleX,
                   scaleY;

    private TrackingMemoryActivityListener activityListener;
    
    private EmulatorUI emulatorUI;

    /**
     *
     * @param activityListener
     * @param baseAddress the base address this component displays. If displaying the page level, use PAGE_MODE
     * @param emulatorUI
     */
    public MemoryMapComponent(TrackingMemoryActivityListener activityListener, int baseAddress, EmulatorUI emulatorUI) {
        this.activityListener = activityListener;
        this.baseAddress = baseAddress;
        this.emulatorUI = emulatorUI;

        if (baseAddress == PAGE_MODE) {
            activityMap = activityListener.getPageActivityMap();
            setPreferredSize(new Dimension(MAP_WIDTH*2, MAP_HEIGHT*2));
        }
        else {
            activityMap = activityListener.getCellActivityMap(baseAddress >>> PAGE_SIZE_BITS);
            setPreferredSize(new Dimension(MAP_WIDTH, MAP_HEIGHT));
        }
        
        //esto deberia ser una nueva clase que extiende de actionlistener
        refreshTimer = new Timer(UPDATE_INTERVAL_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        refreshTimer.start();

        addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {/* noop */ }

            public void mouseMoved(MouseEvent e) {
                selectedX = e.getX();
                selectedY = e.getY();
            }
        });

        //esto deberia ser una nueva clase que extiende de mouselistener
        addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                // Clear
                for(int i = 0; i < MAP_HEIGHT * MAP_WIDTH; i++) activityMap[i] = 0;
            }

            @Override
            public void mousePressed(MouseEvent e) {/* noop */ }

            @Override
            public void mouseReleased(MouseEvent e) {/* noop */ }

            @Override
            public void mouseEntered(MouseEvent e) {/* noop */ }

            @Override
            public void mouseExited(MouseEvent e) {
                selectedX = NO_SELECTION;
                selectedY = NO_SELECTION;
            }
        });
    }


    // This method is called whenever the contents needs to be painted
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        
        int w = getWidth(),
            h = getHeight();

        // Create the resizing transform upon first call or resize
        if (resizeTransform == null || previousW != w || previousH != h) {
            scaleX = max((double) w / MAP_WIDTH, 1);
            scaleY = max((double) h / MAP_HEIGHT, 1);
            new AffineTransform().scale(scaleX, scaleY);
            previousW = w;
            previousH = h;
        }

        img.setRGB(0, 0, MAP_WIDTH, MAP_HEIGHT, activityMap, 0, MAP_WIDTH);

        g2d.drawImage(img, resizeTransform, null);

        if (selectedX != NO_SELECTION) {
            int x = (int) (selectedX / scaleX),
                y = (int) (selectedY / scaleY),
                value = activityMap[y * MAP_WIDTH + x],
                reads = (value & 0xFF00) >>> 8,
                writes = (value & 0xFF0000) >>> 16,
                execs = (value & 0xFF);
            FontMetrics fm = g.getFontMetrics();
            
            String message = "0x" + (asHex(getAddressFromPosition(x, y), 8) + " : green=" +
                             reads + ((reads==255)?"+":"") + " reads, red=" +
                             writes + ((writes==255)?"+":"") + " writes, blue=" + 
                             execs + ((execs==255)?"+":"") + " execs");
            Rectangle2D stringBounds = fm.getStringBounds(message, g);
            g2d.setPaint(WHITE);
            if (y < MAP_HEIGHT/2) {
                g2d.fillRect(0, (int)(h - stringBounds.getHeight()), fm.stringWidth(message), (int)(stringBounds.getHeight()));
                g2d.setPaint(BLACK);
                g2d.drawString(message, 0, h - fm.getDescent());
            }
            else {
                g2d.fillRect(0, 0, fm.stringWidth(message), fm.getHeight());
                g2d.setPaint(BLACK);
                g2d.drawString(message, 0, (int) stringBounds.getHeight() - fm.getDescent());
            }
        }
    }

    private int getAddressFromPosition(int x, int y) {
        int address = 0;
        if (baseAddress == PAGE_MODE) {
            address = (y * MAP_WIDTH + x) << PAGE_SIZE_BITS;
        } else {
            address = baseAddress + (y * MAP_WIDTH + x);
        }
        return address;
    }
}
