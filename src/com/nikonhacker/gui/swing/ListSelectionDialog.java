package com.nikonhacker.gui.swing;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

public class ListSelectionDialog extends JDialog {

    int selection = -1;

    public ListSelectionDialog(JDialog owner, String title, DefaultListModel model) {
        super(owner, title, true);

        JPanel mainPanel = new JPanel(new BorderLayout());

        final JList list = new JList(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(10);
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(250, 300));
        add(listScroller, BorderLayout.CENTER);

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selection = list.locationToIndex(e.getPoint());
                    dispose();
                }
            }
        });

        pack();
    }

    public final int showListSelectionDialog () {
        setVisible(true);
        return selection;
    }

}
