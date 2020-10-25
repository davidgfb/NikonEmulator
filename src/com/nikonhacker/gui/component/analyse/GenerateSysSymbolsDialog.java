package com.nikonhacker.gui.component.analyse;

import com.nikonhacker.Format;
import com.nikonhacker.disassembly.fr.Syscall;
import com.nikonhacker.emu.memory.Memory;
import com.nikonhacker.gui.EmulatorUI;
import com.nikonhacker.gui.swing.PrintWriterArea;
import com.nikonhacker.gui.swing.SearchableTextAreaPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

public class GenerateSysSymbolsDialog extends JDialog {
    private static final int INTERRUPT_VECTOR_BASE_ADDRESS = 0xDFC00;

    private PrintWriterArea printWriterArea;
    private JButton closeButton;
    private final JDialog frame = this;
    private EmulatorUI emulatorUI;
    private Memory memory;

    public GenerateSysSymbolsDialog(EmulatorUI emulatorUI, Memory memory) {
        super(emulatorUI, "System call Symbols generation", true);
        this.emulatorUI = emulatorUI;
        this.memory = memory;

        JPanel panel = new JPanel(new BorderLayout());

        printWriterArea = new PrintWriterArea(25, 70);
        printWriterArea.setAutoScroll(true);

        panel.add(new SearchableTextAreaPanel(printWriterArea), BorderLayout.CENTER);

        closeButton = new JButton("Close");
        closeButton.setEnabled(false);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
        panel.add(closeButton, BorderLayout.SOUTH);

        setContentPane(panel);

        setDefaultCloseOperation(0);
        pack();
        setLocationRelativeTo(null);
    }

    public void startGeneration() {
        Thread disassemblerThread = new Thread(new Runnable() {
            public void run() {
                PrintWriter debugPrintWriter = printWriterArea.getPrintWriter();
                try {
                    Map<Integer,Syscall> syscallMap = Syscall.getMap(memory);
                    debugPrintWriter.println("The following lines can be pasted to a dfr.txt file :");
                    debugPrintWriter.println();
                    for (Syscall syscall : syscallMap.values()) {
                        debugPrintWriter.println("-s 0x" + Format.asHex(syscall.getAddress(), 8) + "=" + syscall.getRawText());
                    }
                    debugPrintWriter.println();
                    debugPrintWriter.println("The lines above can be pasted to a dfr.txt file");
                } catch (Exception e) {
                    e.printStackTrace();
                    debugPrintWriter.println("ERROR : " + e.getClass().getName() + ": " + e.getMessage());
                    debugPrintWriter.println("See console for more information");
                }
                setDefaultCloseOperation(2);
                closeButton.setEnabled(true);
                emulatorUI.updateStates();
            }
        });
        disassemblerThread.start();
    }

}
