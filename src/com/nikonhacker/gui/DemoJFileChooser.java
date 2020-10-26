package com.nikonhacker.gui;

//<editor-fold defaultstate="collapsed" desc="imports">
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import static java.lang.System.exit;
import static java.lang.System.out;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
//</editor-fold>



public class DemoJFileChooser extends JPanel implements ActionListener {
    
    public DemoJFileChooser() {
        JButton go = new JButton("Do it");
        go.addActionListener(this);
        add(go);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        chooser.setDialogTitle("");
        chooser.setFileSelectionMode(DIRECTORIES_ONLY);
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
        //
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            out.println("getCurrentDirectory(): " +  chooser.getCurrentDirectory()+
                        "\ngetSelectedFile() : " +  chooser.getSelectedFile());
        } else {
            out.println("No Selection ");
        }
    }

    @Override
    public Dimension getPreferredSize(){
        return new Dimension(200, 200);
    }

    /*
    public static void main(String s[]) {
        JFrame frame = new JFrame("");
        DemoJFileChooser panel = new DemoJFileChooser();
        //nueva
        frame.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        exit(0);
                    }
                }
        );
        frame.getContentPane().add(panel,"Center");
        frame.setSize(panel.getPreferredSize());
        frame.setVisible(true);
    }
    */
}