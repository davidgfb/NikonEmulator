package com.nikonhacker.gui.component.sourceCode;

//<editor-fold defaultstate="collapsed" desc="imports">
import com.nikonhacker.Constants;
import com.nikonhacker.Format;
import com.nikonhacker.disassembly.CPUState;
import com.nikonhacker.disassembly.CodeSegment;
import com.nikonhacker.disassembly.CodeStructure;
import com.nikonhacker.disassembly.Function;
import com.nikonhacker.disassembly.Statement;
import com.nikonhacker.disassembly.tx.TxCPUState;
import com.nikonhacker.emu.EmulationFramework;
import com.nikonhacker.emu.trigger.BreakTrigger;
import com.nikonhacker.gui.EmulatorUI;
import com.nikonhacker.gui.component.breakTrigger.BreakTriggerEditDialog;
import com.nikonhacker.gui.swing.DocumentFrame;
import com.nikonhacker.gui.swing.ListSelectionDialog;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import java.awt.BorderLayout;
import java.awt.Color;
import static java.awt.Color.BLACK;
import static java.awt.Color.CYAN;
import static java.awt.Color.GREEN;
import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.StringWriter;
import static java.lang.System.err;
import static java.lang.System.out;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static javax.imageio.ImageIO.read;
import javax.swing.DefaultListModel;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaHighlighter;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
//</editor-fold>


public class SourceCodeFrame extends DocumentFrame implements ActionListener, KeyListener, PopupMenuListener {
    private static final int FRAME_WIDTH  = 400,
                             FRAME_HEIGHT = 500;

    private final RSyntaxTextArea listingArea;
    private final ImageIcon icons[][][][][][][][][] = new ImageIcon[2][2][2][2][2][2][2][2][2];

    private Gutter gutter;
    private Object pcHighlightTag = null;
    private final JTextField searchField;
    private       JCheckBox  regexCB,
                             matchCaseCB;

    private CPUState      cpuState;
    private CodeStructure codeStructure;

    /** Contains, for each line number, the address of the instruction it contains, or null if it's not an instruction */
    private List<Integer> lineAddresses = new ArrayList<Integer>();

    private final JTextField        targetField;
    private       int               lastClickedTextPosition;
    private final JCheckBox         followPcCheckBox;
    private       JMenuItem         addBreakPointMenuItem,
                                    removeBreakPointMenuItem,
                                    editBreakPointMenuItem,
                                    toggleBreakPointMenuItem,
                                    runToHereMenuItem,
                                    debugToHereMenuItem,
                                    jumpHereMenuItem,
                                    copyAddressMenuItem;
    private       JCheckBoxMenuItem breakCheckBoxMenuItem,
                                    logCheckBoxMenuItem;
    

    private boolean enabled = true;

    private BufferedImage stopImg,
                          noStopImg,
                          logImg,
                          startLogImg,
                          endLogImg,
                          jumpImg,
                          interruptImg,
                          noInterruptImg,
                          registerImg;

    public SourceCodeFrame(String title, String imageName, boolean resizable, boolean closable, boolean maximizable, boolean iconifiable, final int chip, final EmulatorUI ui, final CPUState cpuState, final CodeStructure codeStructure) {
        super(title, imageName, resizable, closable, maximizable, iconifiable, chip, ui);

        // Load icons
        try {
            stopImg = read(EmulatorUI.class.getResource("images/triggerStop.png"));
            noStopImg = read(EmulatorUI.class.getResource("images/triggerNoStop.png"));
            logImg = read(EmulatorUI.class.getResource("images/triggerLog.png"));
            startLogImg = read(EmulatorUI.class.getResource("images/triggerStartLog.png"));
            endLogImg = read(EmulatorUI.class.getResource("images/triggerEndLog.png"));
            jumpImg = read(EmulatorUI.class.getResource("images/triggerJump.png"));
            interruptImg = read(EmulatorUI.class.getResource("images/triggerInterrupt.png"));
            noInterruptImg = read(EmulatorUI.class.getResource("images/triggerNoInterrupt.png"));
            registerImg = read(EmulatorUI.class.getResource("images/triggerRegister.png"));
        } catch (IOException e) {
            out.println("e: "+e);
            err.println("Error initializing source code break trigger icons");
        }

        this.cpuState = cpuState;
        this.codeStructure = codeStructure;

        setSize(FRAME_WIDTH, FRAME_HEIGHT);


        // Create top toolbar

        JPanel topToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        topToolbar.add(new JLabel("Name/address/regex:"));
        targetField = new JTextField(7);
        topToolbar.add(targetField);
        final JButton exploreButton = new JButton("Explore");
        topToolbar.add(exploreButton);
        JButton goToPcButton = new JButton("Go to PC");
        topToolbar.add(goToPcButton);
        followPcCheckBox = new JCheckBox("Follow PC");
        followPcCheckBox.setSelected(ui.getPrefs().isSourceCodeFollowsPc(chip));
        topToolbar.add(followPcCheckBox);

        // Add listeners
        //nuevo
        ActionListener exploreExecutor = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = targetField.getText();
                Integer address = null;

                if (str.indexOf('*') !=-1 || str.indexOf('+') !=-1 || str.indexOf('\\') !=-1 || str.indexOf('[') !=-1) {
                    // regular expression
                    List<Function> matchedSymbols = codeStructure.getAddressFromExpression(str);

                    if (matchedSymbols!=null) {
                        DefaultListModel listModel = new DefaultListModel();
                        for (Function func : matchedSymbols) {
                            listModel.addElement(func.getName());
                        }

                        final int selection = new ListSelectionDialog(null, "Found symbols:", listModel).showListSelectionDialog();
                        if (selection==-1) {
                            // user canceled selection
                            return;
                        }
                        address = matchedSymbols.get(selection).getAddress();
                    }
                } else {
                    address = codeStructure.getAddressFromString(str);
                }
                if (address == null) {
                    targetField.setBackground(RED);
                }
                else {
                    targetField.setBackground(WHITE);
                    if (!exploreAddress(address, true)) {
                        JOptionPane.showMessageDialog(SourceCodeFrame.this, "No function found matching address 0x" + Format.asHex(address, 8), "Cannot explore function", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        targetField.addActionListener(exploreExecutor);
        exploreButton.addActionListener(exploreExecutor);
        goToPcButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!exploreAddress(cpuState.pc, false)) {
                    JOptionPane.showMessageDialog(SourceCodeFrame.this, "No function found at address 0x" + Format.asHex(cpuState.pc, 8), "Cannot explore function", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        targetField.addKeyListener(this);
        exploreButton.addKeyListener(this);
        followPcCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ui.getPrefs().setSourceCodeFollowsPc(chip, followPcCheckBox.isSelected());
                if (followPcCheckBox.isSelected()) {
                    reachAndHighlightPc();
                }
            }
        });


        // Create listing

        listingArea = new RSyntaxTextArea(50, 80);
        prepareAreaFormat(chip, listingArea);
        JComponent listingComponent = prepareListingPane();


        // Create a bottom toolbar with searching options.

        JPanel searchBar = new JPanel();
        searchField = new JTextField(30);
        searchField.addKeyListener(this);
        searchBar.add(searchField);
        final JButton nextButton = new JButton("Find Next");
        nextButton.setActionCommand("FindNext");
        nextButton.addActionListener(this);
        nextButton.addKeyListener(this);
        searchBar.add(nextButton);
        searchField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nextButton.doClick(0);
            }
        });
        JButton prevButton = new JButton("Find Previous");
        prevButton.setActionCommand("FindPrev");
        prevButton.addActionListener(this);
        prevButton.addKeyListener(this);
        searchBar.add(prevButton);
        regexCB = new JCheckBox("Regex");
        regexCB.addKeyListener(this);
        searchBar.add(regexCB);
        matchCaseCB = new JCheckBox("Match Case");
        matchCaseCB.addKeyListener(this);
        searchBar.add(matchCaseCB);

        // Create and fill main panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(topToolbar, BorderLayout.NORTH);
        contentPanel.add(listingComponent, BorderLayout.CENTER);
        contentPanel.add(searchBar, BorderLayout.SOUTH);

        getContentPane().add(contentPanel);

        if (ui.getPrefs().isSourceCodeFollowsPc(chip)) {
            reachAndHighlightPc();
        }

        pack();
    }

    /**
     * Returns true if requested function was found
     * @param address
     * @return
     */
    public boolean exploreAddress(int address, final boolean moveCursor) {
        address = address & 0xFFFFFFFE; // ignore LSB (error in FR, ISA mode in TX)

        Function function = codeStructure.getFunction(address);
        if (function == null) {
            function = codeStructure.findFunctionIncluding(address);
        }
        if (function != null) {
            writeFunction(function);
            final Integer line = getLineFromAddress(address);
            if (line != null) {
                // must be called after repaint that is not done yet
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // this is faster way
                        setLineContextVisible(line);
                        // set cursor only if necessary, because it is very slowly
                        if (moveCursor) {
                            try {
                                listingArea.setCaretPosition(listingArea.getLineStartOffset(line));
                            } catch (BadLocationException e1) {
                                out.println("e: "+e1);
                            }
                        }
                    }
                });
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Format as expected for Fr source display
     * @param chip
     * @param listingArea
     */
    public static void prepareAreaFormat(int chip, RSyntaxTextArea listingArea) {
        listingArea.setEditable(false);
        listingArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        listingArea.setCodeFoldingEnabled(true);
        listingArea.setAntiAliasingEnabled(true);

        listingArea.setMarkOccurrences(true);
        listingArea.setMarkOccurrencesColor(GREEN);
//        // When one clicks on a term, highlight all occurrences (not only the ones within the same syntactic group)
//        MarkAllOccurrencesSupport support = new MarkAllOccurrencesSupport();
//        support.setColor(GREEN);
//        support.install(listingArea);

        // Make current line transparent so PC line highlight passes through
        listingArea.setCurrentLineHighlightColor(new Color(255,255,0,64));

        // Register our assembly syntax highlighter
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        if (chip == Constants.CHIP_FR) {
            atmf.putMapping("text/frasm", "com.nikonhacker.gui.component.sourceCode.syntaxHighlighter.AssemblerFrTokenMaker");
        }
        else {
            atmf.putMapping("text/txasm", "com.nikonhacker.gui.component.sourceCode.syntaxHighlighter.AssemblerTxTokenMaker");
        }
        TokenMakerFactory.setDefaultInstance(atmf);

        SyntaxScheme ss = listingArea.getSyntaxScheme();
        Style functionStyle = ss.getStyle(Token.FUNCTION);

        Style addressStyle = (Style) functionStyle.clone();
        ss.setStyle(Token.LITERAL_NUMBER_HEXADECIMAL, addressStyle);
        addressStyle.foreground = BLACK;

        Style instructionStyle = (Style) functionStyle.clone();
        ss.setStyle(Token.ANNOTATION, instructionStyle);
        instructionStyle.foreground = LIGHT_GRAY;

        Style variableStyle = ss.getStyle(Token.VARIABLE);
        variableStyle.foreground = new Color(155, 22, 188);

        Style reservedWordStyle = ss.getStyle(Token.RESERVED_WORD);
        reservedWordStyle.foreground = new Color(0, 0, 255);

        Style reservedWord2Style = ss.getStyle(Token.RESERVED_WORD_2);
        reservedWord2Style.foreground = new Color(0, 150, 150);

        // Assign it to our area
        if (chip == Constants.CHIP_FR) {
            listingArea.setSyntaxEditingStyle("text/frasm");
        } else {
            listingArea.setSyntaxEditingStyle("text/txasm");
        }
        RSyntaxTextAreaHighlighter rSyntaxTextAreaHighlighter = new RSyntaxTextAreaHighlighter();
        listingArea.setHighlighter(rSyntaxTextAreaHighlighter);
    }

    /**
     * Add behaviours
     * @return
     */
    private JComponent prepareListingPane() {
        listingArea.addKeyListener(this); // For search keys

        // This is to make sure the right mouse button also moves the caret,
        // so that right-click + Toggle breakpoint acts on the clicked line
        listingArea.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {
                listingArea.requestFocusInWindow();
                lastClickedTextPosition = listingArea.viewToModel(e.getPoint());
            }
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });

        // Customize menu, copying over interesting preexisting entries
        JPopupMenu oldPopupMenu = listingArea.getPopupMenu();
        JPopupMenu newPopupMenu = new JPopupMenu();
        List<Integer> itemsToKeep = Arrays.asList(3, 6);
        MenuElement[] subElements = oldPopupMenu.getSubElements();
        for (int i = 0; i < subElements.length; i++) {
            MenuElement menuElement = subElements[i];
            JMenuItem item = (JMenuItem) menuElement;
            if (itemsToKeep.contains(i)) {
                newPopupMenu.add(item);
            }
        }
        newPopupMenu.addSeparator();

        newPopupMenu.add(new JMenuItem(new FindTextAction()));

        newPopupMenu.addSeparator();

        addBreakPointMenuItem = new JMenuItem("Add trigger");
        newPopupMenu.add(addBreakPointMenuItem);
        addBreakPointMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Integer address = getClickedAddress();
                    if (address != null) {
                        BreakTrigger matchedTrigger = getBreakTrigger(address);
                        if (matchedTrigger == null) {
                            // No match. Create a new one
                            String triggerName;
                            if (codeStructure.isFunction(address)) {
                                triggerName = codeStructure.getFunctionName(address) + "()";
                            }
                            else {
                                triggerName = "Breakpoint at 0x" + Format.asHex(address, 8);
                            }
                            BreakTrigger breakTrigger = new BreakTrigger(chip, triggerName);
                            breakTrigger.getCpuStateValues().setPc(address);
                            breakTrigger.getCpuStateFlags().pc = 1;
                            ui.getPrefs().getTriggers(chip).add(breakTrigger);

                            ui.onBreaktriggersChange(chip);
                        }
                    }
                } catch (BadLocationException ble) {
                    out.println("e: "+ble);
                }
            }
        });

        editBreakPointMenuItem = new JMenuItem("Edit trigger");
        newPopupMenu.add(editBreakPointMenuItem);
        editBreakPointMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BreakTrigger trigger = getClickedTrigger();
                if (trigger != null) {
                    new BreakTriggerEditDialog(null, chip, trigger, "Edit trigger").setVisible(true);
                    ui.onBreaktriggersChange(chip);
                }
            }
        });

        removeBreakPointMenuItem = new JMenuItem("Delete trigger");
        newPopupMenu.add(removeBreakPointMenuItem);
        removeBreakPointMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BreakTrigger trigger = getClickedTrigger();
                if (trigger != null) {
                    if (JOptionPane.showConfirmDialog(null, "Are you sure you want to delete the trigger '" + trigger.getName() + "' ?", "Delete ?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        ui.getPrefs().getTriggers(chip).remove(trigger);
                        ui.onBreaktriggersChange(chip);
                    }
                }
            }
        });

        toggleBreakPointMenuItem = new JMenuItem("Toggle trigger");
        newPopupMenu.add(toggleBreakPointMenuItem);
        toggleBreakPointMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BreakTrigger trigger = getClickedTrigger();
                if (trigger != null) {
                    trigger.setEnabled(!trigger.isEnabled());
                    ui.onBreaktriggersChange(chip);
                }
            }
        });

        newPopupMenu.addSeparator();

        breakCheckBoxMenuItem = new JCheckBoxMenuItem("Break");
        newPopupMenu.add(breakCheckBoxMenuItem);
        breakCheckBoxMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BreakTrigger trigger = getClickedTrigger();
                if (trigger != null) {
                    trigger.setMustBreak(!trigger.mustBreak());
                    ui.onBreaktriggersChange(chip);
                }
            }
        });

        logCheckBoxMenuItem = new JCheckBoxMenuItem("Log");
        newPopupMenu.add(logCheckBoxMenuItem);
        logCheckBoxMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BreakTrigger trigger = getClickedTrigger();
                if (trigger != null) {
                    trigger.setMustBeLogged(!trigger.mustBeLogged());
                    ui.onBreaktriggersChange(chip);
                }
            }
        });

        newPopupMenu.addSeparator();

        debugToHereMenuItem = new JMenuItem(new RunToHereAction(EmulationFramework.ExecutionMode.DEBUG, true));
        newPopupMenu.add(debugToHereMenuItem);

        runToHereMenuItem = new JMenuItem(new RunToHereAction(EmulationFramework.ExecutionMode.RUN, false));
        newPopupMenu.add(runToHereMenuItem);

        newPopupMenu.addSeparator();

        jumpHereMenuItem = new JMenuItem(new TextAction("Jump here") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JTextComponent textComponent = getTextComponent(e);
                    if (textComponent instanceof JTextArea) {
                        JTextArea textArea = (JTextArea) textComponent;
                        Integer addressFromLine = getAddressFromLine(textArea.getLineOfOffset(lastClickedTextPosition));
                        if (addressFromLine != null) {
                            if (cpuState instanceof TxCPUState) {
                                // do not allow to set PC outside of current function, because ISA type can be different
                                Integer lineFromAddress = getLineFromAddress(cpuState.pc);
                                if (lineFromAddress == null)
                                    return;
                                addressFromLine |= (((TxCPUState)cpuState).is16bitIsaMode ? 1 : 0);
                            }
                            cpuState.setPc(addressFromLine);
                            ui.updateState(chip);
                            exploreAddress(cpuState.pc,false);
                        }
                    }
                } catch (BadLocationException ble) {
                    out.println("e: "+ble);
                }
            }
        });
        newPopupMenu.add(jumpHereMenuItem);

        copyAddressMenuItem = new JMenuItem(new TextAction("Copy address to clipboard") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JTextComponent textComponent = getTextComponent(e);
                    if (textComponent instanceof JTextArea) {
                        JTextArea textArea = (JTextArea) textComponent;
                        Integer addressFromLine = getAddressFromLine(textArea.getLineOfOffset(lastClickedTextPosition));
                        if (addressFromLine != null) {
                            StringSelection stringSelection = new StringSelection(Format.asHex(addressFromLine, 8));
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(stringSelection, new ClipboardOwner() {
                                @Override
                                public void lostOwnership(Clipboard clipboard, Transferable contents) {}
                            });
                        }
                    }
                } catch (BadLocationException ble) {
                    out.println("e: "+ble);
                }
            }
        });
        newPopupMenu.add(copyAddressMenuItem);


        newPopupMenu.addPopupMenuListener(this);

        listingArea.setPopupMenu(newPopupMenu);

        RTextScrollPane scrollPane = new RTextScrollPane(listingArea);
        scrollPane.setIconRowHeaderEnabled(true);
        scrollPane.setFoldIndicatorEnabled(false);
        scrollPane.setLineNumbersEnabled(false);

        gutter = scrollPane.getGutter();
        // disabling bookmark for now, because it is easier to clear all icons when updating bookmarks
        // they're not very useful until they are persisted anyway...
        gutter.setBookmarkingEnabled(false);
        // gutter.setBookmarkIcon(bookmarkIcon);
        gutter.setLineNumberColor(LIGHT_GRAY);

        return scrollPane;
    }

    private BreakTrigger getClickedTrigger() {
        try {
            Integer address = getClickedAddress();
            if (address != null) {
                return getBreakTrigger(address);
            }
        } catch (BadLocationException ble) {
            out.println("e: "+ble);
            // noop
        }
        return null;
    }


    private void reachAndHighlightPc() {
        if (pcHighlightTag != null) {
            listingArea.removeLineHighlight(pcHighlightTag);
        }
        try {
            Integer lineFromAddress = getLineFromAddress(cpuState.pc);
            if (lineFromAddress == null) {
                // PC is not found in current function. Try to find the correct function
                // ExploreAddress will take care of calling this function to highlight PC
                exploreAddress(cpuState.pc,false);
            }
            if (lineFromAddress != null) {
                pcHighlightTag = listingArea.addLineHighlight(lineFromAddress, CYAN);
                setLineContextVisible(lineFromAddress);
            }
        } catch (BadLocationException e) {
            out.println("e: "+e);
            pcHighlightTag = null;
        }
    }

    /**
       check if line to be displayed is visible and make it visible if not
       coderat: any standard call trigger reparsing of the text, so do optimized work.
       @param line   line number to be shown
     */
    private final void setLineContextVisible(int line) {
        Rectangle visibleRect = listingArea.getVisibleRect();

        int lineHeight = listingArea.getFontMetrics(listingArea.getFont()).getHeight();

        int firstLine = (int) Math.ceil((double) visibleRect.y / lineHeight);
        int lastLine = (int) Math.floor((double) (visibleRect.y + visibleRect.height) / lineHeight);

        // do nothing if inside visible area
        if (firstLine<=line && line<lastLine)
            return;
        // scroll to position where line gets visible
        visibleRect.y = lineHeight * (line>0 ? line-1 : 0);
        listingArea.scrollRectToVisible(visibleRect);
    }

    public void setEditable(boolean enabled) {
        this.enabled = enabled;
    }

    private void highlightPc() {
        if (pcHighlightTag != null) {
            listingArea.removeLineHighlight(pcHighlightTag);
        }
        try {
            Integer lineFromAddress = getLineFromAddress(cpuState.pc);
            if (lineFromAddress != null) {
                pcHighlightTag = listingArea.addLineHighlight(lineFromAddress, CYAN);
            }
        } catch (BadLocationException e) {
            out.println("e: "+e);
            pcHighlightTag = null;
        }
    }

    public void onEmulatorStop() {
        if (followPcCheckBox.isSelected()) {
            reachAndHighlightPc();
        }
        else {
            highlightPc();
        }
    }


    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        addBreakPointMenuItem.setVisible(false);
        editBreakPointMenuItem.setVisible(false);
        removeBreakPointMenuItem.setVisible(false);
        toggleBreakPointMenuItem.setVisible(false);
        breakCheckBoxMenuItem.setVisible(false);
        logCheckBoxMenuItem.setVisible(false);
        debugToHereMenuItem.setEnabled(false);
        runToHereMenuItem.setEnabled(false);
        jumpHereMenuItem.setEnabled(false);
        copyAddressMenuItem.setEnabled(false);
        if (enabled) {
            try {
                Integer address = getClickedAddress();
                if (address != null) {
                    BreakTrigger trigger = getBreakTrigger(address);
                    if (trigger != null) {
                        editBreakPointMenuItem.setVisible(true);
                        removeBreakPointMenuItem.setVisible(true);
                        toggleBreakPointMenuItem.setVisible(true);
                        toggleBreakPointMenuItem.setText(trigger.isEnabled() ? "Disable trigger" : "EnableTrigger");
                        breakCheckBoxMenuItem.setVisible(true);
                        breakCheckBoxMenuItem.setSelected(trigger.mustBreak());
                        logCheckBoxMenuItem.setVisible(true);
                        logCheckBoxMenuItem.setSelected(trigger.mustBeLogged());
                    }
                    else {
                        addBreakPointMenuItem.setVisible(true);
                    }

                    debugToHereMenuItem.setEnabled(true);
                    runToHereMenuItem.setEnabled(true);
                    jumpHereMenuItem.setEnabled(true);
                    copyAddressMenuItem.setEnabled(true);
                }
            } catch (BadLocationException ble) {
                out.println("e: "+e);
                // noop
            }
        }
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {}


    /**
     * An action that gets the text at the current caret position and searches it
     */
    private class FindTextAction extends TextAction {
        public FindTextAction() {
            super("Find");
        }

        public void actionPerformed(ActionEvent e) {
            findSelectedText();
        }
    }


    /**
     * An action that allows to run/debug code up to the click position
     */
    private class RunToHereAction extends TextAction {

        private EmulationFramework.ExecutionMode executionMode;
        private boolean                          debugMode;

        public RunToHereAction(EmulationFramework.ExecutionMode executionMode, boolean debugMode) {
            super(executionMode.getLabel() + " to this line");
            this.executionMode = executionMode;
            this.debugMode = debugMode;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                JTextComponent textComponent = getTextComponent(e);
                if (textComponent instanceof JTextArea) {
                    JTextArea textArea = (JTextArea) textComponent;
                    Integer addressFromLine = getAddressFromLine(textArea.getLineOfOffset(lastClickedTextPosition));
                    if (addressFromLine != null) {
                        ui.playToAddress(chip, executionMode, addressFromLine);
                    }
                }
            } catch (BadLocationException ble) {
                out.println("e: "+e);
            }
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        // "FindNext" => search forward, "FindPrev" => search backward
        String command = e.getActionCommand();
        performSearch("FindNext".equals(command));
    }

    private void performSearch(boolean isForward) {
        // Create an object defining our search parameters.
        SearchContext context = new SearchContext();
        String text = searchField.getText();
        if (text.length() == 0) {
            return;
        }
        context.setSearchFor(text);
        context.setMatchCase(matchCaseCB.isSelected());
        context.setRegularExpression(regexCB.isSelected());
        context.setSearchForward(isForward);
        context.setWholeWord(false);

        boolean found = SearchEngine.find(listingArea, context);
        if (!found) {
            JOptionPane.showMessageDialog(this, "Text not found");
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        // CTRL-F
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_F) {
            // TODO show if hidden
            findSelectedText();
        }
        else if (e.getKeyCode() == KeyEvent.VK_F3) {
            performSearch(!e.isShiftDown());
        }
        else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            // TODO hide if shown
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }


    private void findSelectedText() {
        try {
            // Search implementation
            int selStart = listingArea.getSelectionStart();
            int selEnd = listingArea.getSelectionEnd();
            if (selStart != selEnd) {
                searchField.setText(listingArea.getText(selStart, selEnd - selStart));
            }
            performSearch(true);
        } catch (BadLocationException e) {
            out.println("e: "+e);
        }
    }


    // Real source code handling methods


    public void writeFunction(Function function) {
        listingArea.setText("");
        lineAddresses.clear();
        List<CodeSegment> segments = function.getCodeSegments();
        if (segments.size() == 0) {
            listingArea.setText("; function at address 0x" + Format.asHex(function.getAddress(), 8) + " was not disassembled (not in CODE range)");
            lineAddresses.add(null);
        }
        else {
            for (int i = 0; i < segments.size(); i++) {
                CodeSegment codeSegment = segments.get(i);
                if (segments.size() > 1) {
                    listingArea.append("; Segment " + (i + 1) + "/" + segments.size() + "\n");
                    lineAddresses.add(null);
                }
                for (int address = codeSegment.getStart(); address <= codeSegment.getEnd(); address = codeStructure.getAddressOfStatementAfter(address)) {
                    Statement statement = codeStructure.getStatement(address);
                    try {
                        StringWriter writer = new StringWriter();
                        codeStructure.writeStatement(writer, address, statement, 0, ui.getPrefs().getOutputOptions(chip));
                        String str = writer.toString();
                        for (String line : str.split("\n")) {
                            if (line.length() > 0 && isCodeLine(line)) {
                                lineAddresses.add(address);
                            }
                            else {
                                lineAddresses.add(null);
                            }
                            listingArea.append(line + "\n");
                        }
                    } catch (IOException e) {
                        out.println("e: "+e);
                        listingArea.append("# ERROR decoding instruction at address 0x" + Format.asHex(address, 8) + " : " + e.getMessage() + "\n");
                        lineAddresses.add(null);
                    }
                }
                listingArea.append("\n");
                lineAddresses.add(null);
            }
        }
        listingArea.setCaretPosition(0);
        highlightPc();
        updateBreakTriggers();
    }

    private boolean isCodeLine(String line) {
        char ch = line.charAt(0);
        return Character.isDigit(ch) || (ch >= 'A' && ch <= 'F');
    }


    public void updateBreakTriggers() {
        gutter.removeAllTrackingIcons();
        for (BreakTrigger breakTrigger : ui.getPrefs().getTriggers(chip)) {
            if (breakTrigger.getCpuStateFlags().pc != 0) {
                try {
                    Integer lineFromAddress = getLineFromAddress(breakTrigger.getCpuStateValues().pc);
                    if (lineFromAddress != null) {
                        gutter.addLineTrackingIcon(lineFromAddress, getIcon(breakTrigger));
                    }
                } catch (BadLocationException e) {
                    out.println("e: "+e);
                }
            }
        }
    }

    private ImageIcon getIcon(BreakTrigger breakTrigger) {
        ImageIcon icon = icons[breakTrigger.mustBreak()?1:0]
                [breakTrigger.mustBeLogged()?1:0]
                [breakTrigger.getMustStartLogging()?1:0]
                [breakTrigger.getMustStopLogging()?1:0]
                [breakTrigger.getPcToSet()!=null?1:0]
                [breakTrigger.getInterruptToRequest()!=null?1:0]
                [breakTrigger.getInterruptToWithdraw()!=null?1:0]
                [!breakTrigger.getNewCpuStateFlags().hasAllRegistersZero()?1:0]
                [breakTrigger.isEnabled()?1:0];

        if (icon == null) {
            Image img = new BufferedImage(stopImg.getWidth(), stopImg.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = ((BufferedImage)img).createGraphics();
            g.drawImage(breakTrigger.mustBreak() ? stopImg : noStopImg, 0, 0, null);
            if (breakTrigger.mustBeLogged()) g.drawImage(logImg, 0, 0, null);
            if (breakTrigger.getMustStartLogging()) g.drawImage(startLogImg, 0, 0, null);
            if (breakTrigger.getMustStopLogging()) g.drawImage(endLogImg, 0, 0, null);
            if (breakTrigger.getPcToSet()!=null) g.drawImage(jumpImg, 0, 0, null);
            if (breakTrigger.getInterruptToRequest()!=null) g.drawImage(interruptImg, 0, 0, null);
            if (breakTrigger.getInterruptToWithdraw()!=null) g.drawImage(noInterruptImg, 0, 0, null);
            if (!breakTrigger.getNewCpuStateFlags().hasAllRegistersZero()) g.drawImage(registerImg, 0, 0, null);
            if (!breakTrigger.isEnabled()) {
                img = GrayFilter.createDisabledImage(img);
            }
            icon = new ImageIcon(img);

            icons[breakTrigger.mustBreak()?1:0]
                    [breakTrigger.mustBeLogged()?1:0]
                    [breakTrigger.getMustStartLogging()?1:0]
                    [breakTrigger.getMustStopLogging()?1:0]
                    [breakTrigger.getPcToSet()!=null?1:0]
                    [breakTrigger.getInterruptToRequest()!=null?1:0]
                    [breakTrigger.getInterruptToWithdraw()!=null?1:0]
                    [!breakTrigger.getNewCpuStateFlags().hasAllRegistersZero()?1:0]
                    [breakTrigger.isEnabled()?1:0] = icon;
        }
        return icon;
    }


    public Integer getLineFromAddress(int address) {
        for (int i = 0; i < lineAddresses.size(); i++) {
            if (lineAddresses.get(i) != null && lineAddresses.get(i) == address) {
                return i;
            }
        }

        return null;
    }


    private Integer getAddressFromLine(int line) {
        return lineAddresses.get(line);
    }

    private Integer getClickedAddress() throws BadLocationException {
        return getAddressFromLine(listingArea.getLineOfOffset(lastClickedTextPosition));
    }

    private BreakTrigger getBreakTrigger(int addressFromLine) {
        for (BreakTrigger breakTrigger : ui.getPrefs().getTriggers(chip)) {
            if (breakTrigger.getCpuStateFlags().pc != 0) {
                if (breakTrigger.getCpuStateValues().pc == addressFromLine) {
                    return breakTrigger;
                }
            }
        }
        return null;
    }
}
