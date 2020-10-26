package Main;

//<editor-fold defaultstate="collapsed" desc="imports">
import com.nikonhacker.Constants;
import com.nikonhacker.emu.EmulationException;
import com.nikonhacker.gui.EmulatorUI;
import java.io.File;
import java.io.IOException;
import static java.lang.System.setProperty;
import static java.lang.Thread.setDefaultUncaughtExceptionHandler;
import javax.swing.UnsupportedLookAndFeelException;
//</editor-fold>

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author David
 */
public class Main {
    
    //<editor-fold defaultstate="collapsed" desc="main">
    //proviene de EmulatorUI en un intento de facilitar el arranque del programa desde el .jar
    public static void main(String[] args) throws EmulationException, IOException, ClassNotFoundException, UnsupportedLookAndFeelException, IllegalAccessException, InstantiationException {
        EmulatorUI eUI = new EmulatorUI();
        
        // Workaround for JDK bug - https://code.google.com/p/nikon-firmware-tools/issues/detail?id=17
        setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        if (args.length > 0) {
            File[] aIF = eUI.getImageFile();
            aIF[Constants.CHIP_FR] = new File(args[0]);
            eUI.setImageFile(aIF);
            if (args.length > 1) {
                aIF = eUI.getImageFile();
                aIF[Constants.CHIP_TX] = new File(args[1]);
                eUI.setImageFile(aIF);
            }
        }

        eUI.initProgrammableTimerAnimationIcons(eUI.getBUTTON_SIZE_SMALL());

        // a lot of calls are made from GUI in AWT thread that exits fast with no error code
        setDefaultUncaughtExceptionHandler(new UEHandler());
        
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        /*
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
        */
                eUI.createAndShowGUI();
        /*
            }
        });
        */
    }
//</editor-fold>
}
