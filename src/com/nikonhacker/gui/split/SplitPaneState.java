package com.nikonhacker.gui.split;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

public class SplitPaneState {
    public static void main( String[] args ) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SplitPaneState().createAndSowGUI();
            }
        });
    }

    private int position = -1;
    private Dimension size = new Dimension( 500, 300 );

    private void createAndSowGUI() {
        final JFrame frame = new JFrame("frame");
        frame.setSize( 200, 100 );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setLocationRelativeTo( null );
        frame.getContentPane().add( new JButton( new AbstractAction(){
            {
                putValue( Action.NAME, "Open Dialog" );
            }
            @Override
            public void actionPerformed( ActionEvent e ) {
                final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JLabel("left Component"), new JLabel("right Component"));

                splitPane.setContinuousLayout(true);
                splitPane.setOneTouchExpandable(true);

                if (position != -1) {
                    boolean LeftIsCollapsed = position < splitPane.getMinimumDividerLocation();
                    if (LeftIsCollapsed) {
                        splitPane.getLeftComponent().setMinimumSize(new Dimension()); // fix by Martijn Courteaux
                        splitPane.setDividerLocation(0.0d);                           // fix by Martijn Courteaux
                    }
                    else {
                        boolean RightIsCollapsed = position > splitPane.getMaximumDividerLocation();
                        if (RightIsCollapsed) {
                            splitPane.getRightComponent().setMinimumSize(new Dimension()); // fix by Martijn Courteaux
                            splitPane.setDividerLocation(1.0d);                            // fix by Martijn Courteaux
                        }
                        else {
                            splitPane.setDividerLocation(position);
                        }
                    }
                }

                JDialog dialog = new JDialog(frame, "dialog") {
                    @Override
                    public void dispose() {
                        position = splitPane.getDividerLocation();
                        size = this.getSize();
                        super.dispose();
                    }
                };

                dialog.setSize(size);
                dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                dialog.setLocationRelativeTo(frame);
                dialog.getContentPane().add(splitPane);
                dialog.setVisible(true);
            }
        }
        ));
        frame.setVisible( true );
    }
}