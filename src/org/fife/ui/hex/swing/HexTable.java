/*
 * Copyright (c) 2008 Robert Futrell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "HexEditor" nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fife.ui.hex.swing;

//<editor-fold defaultstate="collapsed" desc="imports">
import org.fife.ui.hex.event.SelectionChangedEvent;
import org.fife.ui.hex.event.SelectionChangedListener;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import static java.awt.AWTEvent.KEY_EVENT_MASK;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_END;
import static java.awt.event.KeyEvent.VK_HOME;
import static java.awt.event.KeyEvent.VK_PAGE_DOWN;
import static java.awt.event.KeyEvent.VK_PAGE_UP;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_UP;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static java.lang.Integer.parseInt;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import static javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.DocumentFilter.FilterBypass;
import javax.swing.text.JTextComponent;
import static org.fife.ui.hex.swing.HexEditor.DUMP_COLUMN_WIDTH;
//</editor-fold>



/**
 * The table displaying the hex content of a file.  This is the meat of
 * the hex viewer.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class HexTable extends JTable {

    private static final long serialVersionUID = 1L;

    private final HexEditor hexEditor;
    private HexTableModel model;
    int leadSelectionIndex,
        anchorSelectionIndex;

    private static final Color ANTERNATING_CELL_COLOR = new Color(240,240,240);

    private CellEditor cellEditor = new CellEditor();
    private Color[] colorMap;

	/**
	 * Creates a new table to display hex data.
	 *
	 * @param hexEditor The parent hex editor component.
	 * @param model The table model to use.
	 */
	public HexTable(HexEditor hexEditor, HexTableModel model) {

            super(model);
            
            Font headerFont = UIManager.getFont("TableHeader.font");
            FontMetrics fm = getFontMetrics(getFont()), headerFM = hexEditor.getFontMetrics(headerFont);
            int w = fm.stringWidth("wwww"); // cell contents, 0-255
            
            this.hexEditor = hexEditor;
            this.model = model;
            enableEvents(KEY_EVENT_MASK);
            setAutoResizeMode(AUTO_RESIZE_OFF);
            setFont(new Font("Monospaced", Font.PLAIN, 14));
            //setRowHeight(28);
            setCellSelectionEnabled(true);
            setSelectionMode(SINGLE_INTERVAL_SELECTION);
            setDefaultEditor(Object.class, cellEditor);
            setDefaultRenderer(Object.class, new CellRenderer());
            getTableHeader().setReorderingAllowed(false);
            setShowGrid(false);

            w = max(w, headerFM.stringWidth("+999"));
            for (int i=0; i<getColumnCount(); i++) {
                TableColumn column = getColumnModel().getColumn(i);
                if (i<16) {
                    column.setPreferredWidth(w);
                } else {
                    column.setPreferredWidth(DUMP_COLUMN_WIDTH);
                }
            }

            setPreferredScrollableViewportSize(new Dimension(w*16+DUMP_COLUMN_WIDTH, 25*getRowHeight()));

            anchorSelectionIndex = leadSelectionIndex = 0;

	}


	/**
	 * Registers a prospect who is interested when the text selection from the
	 * hex editor becomes changed.
	 * 
	 * @param l The concerning listener.
	 * @see #removeSelectionChangedListener(org.fife.ui.hex.event.SelectionChangedListener)
	 */
	public void addSelectionChangedListener(SelectionChangedListener l) {
            listenerList.add(SelectionChangedListener.class, l);
	}


	/**
	 * Returns the column for the cell containing data that is the closest
	 * to the specified cell.  This is used when, for example, the user clicks
	 * on an "empty" cell in the last row of the table.
	 *  
	 * @param row The row of the cell clicked on.
	 * @param col The column of the cell clicked on.
	 * @return The column of the closest cell containing data.
	 */
	private int adjustColumn(int row, int col) {
            int column = min(col, getColumnCount()-2);
            if (col<0) {
                column= 0;
            }
            if (row==getRowCount()-1) {
                int lastRowCount = model.getByteCount()%16;
                if (lastRowCount==0) {
                    lastRowCount = 16;
                }
                if (lastRowCount<16) { // Last row's not entirely full
                    column= min(col, (model.getByteCount()%16)-1);
                }
            }
            return column;
	}


	/**
	 * Returns the offset into the bytes being edited represented at the
	 * specified cell in the table, if any.
	 *
	 * @param row The row in the table.
	 * @param col The column in the table.
	 * @return The offset into the byte array, or <code>-1</code> if the
	 *         cell does not represent part of the byte array (such as the
	 *         tailing "ascii dump" column's cells).
	 * @see #offsetToCell(int)
	 */
	public int cellToOffset(int row, int col) {
            // Check row and column individually to prevent them being invalid
            // values but still pointing to a valid offset in the buffer.
            int offs = row*16 + col,
                offset=(offs>=0 && offs<model.getByteCount()) ? offs : -1;
            
            if (row<0 || row>=getRowCount() || col<0 || col>15) { // Don't include last column (ascii dump)
                offset= -1;
            }
            
            return offset;
	}


	/**
	 * Changes the selected byte range.
	 *
	 * @param row
	 * @param col
	 * @param toggle
	 * @param extend
	 * @see #changeSelectionByOffset(int, boolean)
	 * @see #setSelectedRows(int, int)
	 * @see #setSelectionByOffsets(int, int)
	 */
        @Override
	public void changeSelection(int row, int col, boolean toggle, boolean extend) {
	    
            // remind previous selection range
            int prevSmallest = getSmallestSelectionIndex(),
                prevLargest = getLargestSelectionIndex();

            // Don't allow the user to select the "ascii dump" or any
            // empty cells in the last row of the table.
            col = adjustColumn(row, col);
            if (row<0) {
                row = 0;
            }

            // Clear the old selection (may not be necessary).
            repaintSelection();

            if (extend) {
                leadSelectionIndex = cellToOffset(row, col);
            } else {
                anchorSelectionIndex = leadSelectionIndex = cellToOffset(row, col);
            }

            // Scroll after changing the selection as blit scrolling is
            // immediate, so that if we cause the repaint after the scroll we
            // end up painting everything!
            if (getAutoscrolls()) {
                ensureCellIsVisible(row, col);
            }

            // Draw the new selection.
            repaintSelection();

            fireSelectionChangedEvent(prevSmallest, prevLargest);

	}


	/**
	 * Changes the selection by an offset into the bytes being edited.
	 *
	 * @param offset
	 * @param extend
	 * @see #changeSelection(int, int, boolean, boolean)
	 * @see #setSelectedRows(int, int)
	 * @see #setSelectionByOffsets(int, int)
	 */
	public void changeSelectionByOffset(int offset, boolean extend) {
            offset = max(0, offset);
            offset = min(offset, model.getByteCount()-1);
            changeSelection(offset/16, offset%16, false, extend);
	}


	/**
	 * Clears the selection.  The "lead" of the selection is set back to the
	 * position of the "anchor."
	 */
        @Override
	public void clearSelection() {
            if (anchorSelectionIndex>-1) { // Always true unless an error
                leadSelectionIndex = anchorSelectionIndex;
            } else {
                anchorSelectionIndex = leadSelectionIndex = 0;
            }
            repaintSelection();
	}


	/**
	 * Ensures the specified cell is visible.
	 *
	 * @param row The row of the cell.
	 * @param col The column of the cell.
	 */
	private void ensureCellIsVisible(int row, int col) {
            Rectangle cellRect = getCellRect(row, col, false);
            if (cellRect != null) {
                scrollRectToVisible(cellRect);
            }
	}


	/**
	 * Notifies any listeners that the selection has changed.
	 * 
	 * @see #addSelectionChangedListener(org.fife.ui.hex.event.SelectionChangedListener)
	 * @see #removeSelectionChangedListener(org.fife.ui.hex.event.SelectionChangedListener)
	 */
	private void fireSelectionChangedEvent(int prevSmallest, int prevLargest) {

            // Lazily create the event
            SelectionChangedEvent e = null;

            // Guaranteed non-null array
            Object[] listeners = listenerList.getListenerList();
            // Process last to first
            for (int i=listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==SelectionChangedListener.class) {
                    if (e==null) {
                        e = new SelectionChangedEvent(this, prevSmallest, prevLargest, getSmallestSelectionIndex(), getLargestSelectionIndex());
                    }
                    ((SelectionChangedListener)listeners[i+1]).selectionChanged(e);
                }
            }
	}


	/**
	 * Returns the byte at the specified offset.
	 *
	 * @param offset The offset.
	 * @return The byte.
	 */
	public byte getByte(int offset) {
            return model.getByte(offset);
	}


	/**
	 * Returns the number of bytes being edited.
	 *
	 * @return The number of bytes.
	 */
	public int getByteCount() {
            return model.getByteCount();
	}


	/**
	 * Returns the rendering hints for text that will most accurately reflect
	 * those of the native windowing system.
	 *
	 * @return The rendering hints, or <code>null</code> if they cannot be
	 *         determined.
	 */
	private Map getDesktopAntiAliasHints() {
            return (Map)getToolkit().getDesktopProperty("awt.font.desktophints");
	}


	/**
	 * Returns the largest selection index.
	 *
	 * @return The largest selection index.
	 * @see #getSmallestSelectionIndex()
	 */
	public int getLargestSelectionIndex() {
            return max(max(leadSelectionIndex, anchorSelectionIndex), 0); // Don't return -1 if table is empty
	}


	/**
	 * Returns the smallest selection index.
	 *
	 * @return The smallest selection index.
	 * @see #getLargestSelectionIndex()
	 */
	public int getSmallestSelectionIndex() {
            return max(min(leadSelectionIndex, anchorSelectionIndex), 0); // Don't return -1 if table is empty
	}


        @Override
	public boolean isCellEditable(int row, int col) {
            return cellToOffset(row, col)>-1;
	}


        @Override
	public boolean isCellSelected(int row, int col) {
            int offset = cellToOffset(row, col),
                start = getSmallestSelectionIndex(),
                end = getLargestSelectionIndex();
            boolean isSelected=offset>=start && offset<=end;
            
            if (offset==-1) { // "Ascii dump" column
                isSelected= false;
            }
            return isSelected;
	}


	/**
	 * Returns the cell representing the specified offset into the hex
	 * document.
	 *
	 * @param offset The offset into the document.
	 * @return The cell, in the form <code>(row, col)</code>.  If the
	 *         specified offset is invalid, <code>(-1, -1)</code> is returned.
	 * @see #cellToOffset(int, int)
	 */
	public Point offsetToCell(int offset) {
            int row = offset/16,
                col = offset%16;
            if (offset<0 || offset>=model.getByteCount()) {
                return new Point(-1, -1);
            }
            return new Point(row, col);
	}


	/**
	 * Sets the contents in the hex editor to the contents of the specified
	 * file.
	 *
	 * @param fileName The name of the file to open.
	 * @throws java.io.IOException If an IO error occurs.
	 */
	public void open(String fileName) throws IOException {
            model.setBytes(fileName); // Fires tableDataChanged event
	}


	/**
	 * Sets the contents in the hex editor to the contents of the specified
	 * input stream.
	 *
	 * @param in An input stream.
	 * @throws java.io.IOException If an IO error occurs.
	 */
	public void open(InputStream in) throws IOException {
            model.setBytes(in);
	}


        @Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Object value = getValueAt(row, column);
            boolean isSelected = isCellSelected(row, column),
                    hasFocus = cellToOffset(row, column)==leadSelectionIndex;

            return renderer.getTableCellRendererComponent(this, value, isSelected, hasFocus, row, column);
	}


        @Override
	protected void processKeyEvent (KeyEvent e) {
            super.processKeyEvent(e);

            // TODO: Convert into Actions and put into InputMap/ActionMap?
            if (e.getID()==KeyEvent.KEY_PRESSED) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        boolean extend = e.isShiftDown();
                        int offs = max(leadSelectionIndex-1, 0);
                        changeSelectionByOffset(offs, extend);
                        e.consume();
                            break;
                    case VK_RIGHT:
                        extend = e.isShiftDown();
                        offs = min(leadSelectionIndex+1, model.getByteCount()-1);
                        changeSelectionByOffset(offs, extend);
                        e.consume();
                        break;
                    case VK_UP:
                        extend = e.isShiftDown();
                        offs = max(leadSelectionIndex-16, 0);
                        changeSelectionByOffset(offs, extend);
                        e.consume();
                        break;
                    case VK_DOWN:
                        extend = e.isShiftDown();
                        offs = min(leadSelectionIndex+16, model.getByteCount()-1);
                        changeSelectionByOffset(offs, extend);
                        e.consume();
                        break;
                    case VK_PAGE_DOWN:
                        extend = e.isShiftDown();
                        int visibleRowCount = getVisibleRect().height/getRowHeight();
                        offs = min(leadSelectionIndex+visibleRowCount*16, model.getByteCount()-1);
                        changeSelectionByOffset(offs, extend);
                        e.consume();
                        break;
                    case VK_PAGE_UP:
                        extend = e.isShiftDown();
                        visibleRowCount = getVisibleRect().height/getRowHeight();
                        offs = max(leadSelectionIndex-visibleRowCount*16, 0);
                        changeSelectionByOffset(offs, extend);
                        e.consume();
                        break;
                    case VK_HOME:
                        extend = e.isShiftDown();
                        offs = (leadSelectionIndex/16)*16;
                        changeSelectionByOffset(offs, extend);
                        e.consume();
                        break;
                    case VK_END:
                        extend = e.isShiftDown();
                        offs = (leadSelectionIndex/16)*16 + 15;
                        offs = min(offs, model.getByteCount()-1);
                        changeSelectionByOffset(offs, extend);
                        e.consume();
                        break;
                }
            }
        }


	/**
	 * Tries to redo the last action undone.
	 *
	 * @return Whether there is another action to redo after this one.
	 * @see #undo()
	 */
	public boolean redo() {
            return model.redo();
	}


	/**
	 * Removes a range of bytes.
	 *
	 * @param offs The offset of the range of bytes to remove.
	 * @param len The number of bytes to remove.
	 * @see #replaceBytes(int, int, byte[])
	 */
	void removeBytes(int offs, int len) {
            model.removeBytes(offs, len);
	}


	private void repaintSelection() {
            // TODO: Repaint only selected lines.
            repaint();
	}
    

	/**
	 * Removes a listener who isn't any longer interested whether the text
	 * selection from the hex editor becomes changed.
	 * 
	 * @param l The concerning previous prospect.
	 * @see #addSelectionChangedListener(org.fife.ui.hex.event.SelectionChangedListener)
	 */
	public void removeSelectionChangedListener(SelectionChangedListener l) {
            listenerList.remove(SelectionChangedListener.class, l);
	}


	/**
	 * Replaces a range of bytes.
	 *
	 * @param offset The offset of the range of bytes to replace.
	 * @param len The number of bytes to replace.
	 * @param bytes The bytes to replace the range with.
	 * @see #removeBytes(int, int)
	 */
	public void replaceBytes(int offset, int len, byte[] bytes) {
            model.replaceBytes(offset, len, bytes);
	}


	/**
	 * Toggles whether the cells in the hex editor are editable by clicking
	 * in them.
	 *
	 * @param cellEditable Whether individual hex editor cells should be editable.
	 */
	public void setCellEditable(boolean cellEditable) {
            cellEditor.setEditable(cellEditable);
	}


	public void setSelectedRows(int min, int max) {
            if (min<0 || min>=getRowCount() || max<0 || max>=getRowCount()) {
                throw new IllegalArgumentException();
            }
            int startOffs = min*16,
                endOffs = max*16+15;
            // TODO: Have a single call to change selection by a range.
            changeSelectionByOffset(startOffs, false);
            changeSelectionByOffset(endOffs, true);
	}


	/**
	 * Selects the specified range of bytes in the table.
	 *
	 * @param startOffs The "anchor" byte of the selection.
	 * @param endOffs The "lead" byte of the selection.
	 * @see #changeSelection(int, int, boolean, boolean)
	 * @see #changeSelectionByOffset(int, boolean)
	 */
	public void setSelectionByOffsets(int startOffs, int endOffs) {

            startOffs = Math.max(0, startOffs);
            startOffs= Math.min(startOffs, model.getByteCount()-1);

            // Clear the old selection (may not be necessary).
            repaintSelection();

            anchorSelectionIndex = startOffs;
            leadSelectionIndex = endOffs;

            // Scroll after changing the selection as blit scrolling is
            // immediate, so that if we cause the repaint after the scroll we
            // end up painting everything!
            if (getAutoscrolls()) {
                int endRow = endOffs/16,
                    endCol = endOffs%16;
                // Don't allow the user to select the "ascii dump" or any
                // empty cells in the last row of the table.
                endCol = adjustColumn(endRow, endCol);
                if (endRow<0) {
                    endRow = 0;
                }
                ensureCellIsVisible(endRow, endCol);
            }

            // Draw the new selection.
            repaintSelection();

	}

        public Color[] getColorMap() {
            return colorMap;
        }

        public void setColorMap(Color[] colorMap) {
            this.colorMap = colorMap;
        }

        /**
	 * Tries to undo the last action.
	 *
	 * @return Whether there is another action to undo after this one.
	 * @see #redo()
	 */
	public boolean undo() {
            return model.undo();
	}


	/**
	 * Table cell editor that restricts input to byte values
	 * (<code>0 - 255</code>).
	 */
        //aparte
	private static class CellEditor extends DefaultCellEditor implements FocusListener {

            private static final long serialVersionUID = 1L;

            private boolean editable;

            public CellEditor() {
                super(new JTextField());
                AbstractDocument doc = (AbstractDocument) ((JTextComponent)editorComponent).getDocument();
                doc.setDocumentFilter(new EditorDocumentFilter());
                getComponent().addFocusListener(this);
                editable = true;
            }

            @Override
            public void focusGained(FocusEvent e) {
                ((JTextField)getComponent()).selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
                if (editable) {
                    return super.getTableCellEditorComponent(table, value, selected, row, column);
                }
                return null;
            }

            public void setEditable(boolean editable) {
                this.editable = editable;
            }

            @Override
            public boolean stopCellEditing() {
                // Prevent the user from entering empty string as a value.
                String value = (String)getCellEditorValue();
                if (value.length()==0) {
                    UIManager.getLookAndFeel().provideErrorFeedback(null);
                    return false;
                }
                return super.stopCellEditing();
            }

	}


	/**
	 * Custom cell renderer.  This is primarily here for performance,
	 * especially on 1.4 JVM's.  <code>DefaultTableCellRenderer</code>'s
	 * performance was horrible on tables displaying large amounts of rows
	 * and columns in 1.4, so this class helps to alleviate some of that pain.
	 * 1.4 and 1.5 JRE's don't have as much of a performance problem, but
	 * you still can see some lag at times.
	 *
	 * @author Robert Futrell
	 * @version 1.0
	 */
        //debe ir en clase nueva
	private class CellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		private Point highlight;
		private Map desktopAAHints;

		public CellRenderer() {
                    highlight = new Point();
                    desktopAAHints = getDesktopAntiAliasHints();
		}

                @Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                    super.getTableCellRendererComponent(table, value, selected, focus, row, column);

                    highlight.setLocation(-1, -1);
                    if (column==table.getColumnCount()-1 && /* "Ascii dump" */ hexEditor.getHighlightSelectionInAsciiDump()) {
                        int selStart = getSmallestSelectionIndex(),
                            selEnd = getLargestSelectionIndex(),
                            b1 = row*16,
                            b2 = b1 + 15;
                        boolean colorBG = hexEditor.getAlternateRowBG() && (row&1)>0;

                        if (selStart<=b2 && selEnd>=b1) {
                            int start = Math.max(selStart, b1) - b1,
                                end = Math.min(selEnd, b2) - b1;
                            highlight.setLocation(start, end);
                        }

                        setBackground(colorBG ? ANTERNATING_CELL_COLOR : table.getBackground());
                    } else {
                        if (!selected) {
                            if ((hexEditor.getAlternateRowBG() && (row&1)>0) ^ (hexEditor.getAlternateColumnBG() && (column&1)>0)) {
                                setBackground(ANTERNATING_CELL_COLOR);
                            } else {
                                setBackground(table.getBackground());
                            }
                        }
                    }

                    if (colorMap != null) {
                        setForeground(colorMap[row * 16 + column]);
                    }

                    return this;
		}

                @Override
		protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    
                    Graphics2D g2d = (Graphics2D)g;
                    Object oldHints = null;
                    
                    g.setColor(getBackground());
                    g.fillRect(0, 0, getWidth(),getHeight());

                    if (highlight.x>-1) {
                        int w = getFontMetrics(HexTable.this.getFont()).charWidth('w'),
                            x = getInsets().left + highlight.x*w;
                        
                        g.setColor(hexEditor.getHighlightSelectionInAsciiDumpColor());
                        
                        g.fillRect(x, 0, (highlight.y-highlight.x+1)*w, getRowHeight());
                    }

                    g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

                    if (desktopAAHints!=null) {
                        oldHints = g2d.getRenderingHints();
                        g2d.addRenderingHints(desktopAAHints);
                    }

                    g.setColor(getForeground());
                    
                    int x = 2;
                    String text = getText();
                    
                    // not padding low bytes, and this one is in range 00-0f.
                    if (text.length()==1) {
                        x += g.getFontMetrics().charWidth('w');
                    }
                    
                    g.drawString(text, x,11);

                    // Restore rendering hints appropriately.
                    if (desktopAAHints!=null) {
                        g2d.addRenderingHints((Map)oldHints);
                    }
		}
	}


	/**
	 * Filter that ensures the user only enters valid characters in a
	 * byte's cell while editing.
	 *
	 * @author Robert Futrell
	 * @version 1.0
	 */
        //debe ir en clase nueva
	private static class EditorDocumentFilter extends DocumentFilter {

            private boolean ensureByteRepresented(String str) {
                boolean isRepresented=true;
                try {
                    int i = parseInt(str, 16);
                    if (i<0 || i>0xff) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException nfe) {
                    UIManager.getLookAndFeel().provideErrorFeedback(null);
                    isRepresented=false;
                }
                return isRepresented;
            }

            @Override
            public void insertString(FilterBypass fb, int offs, String string, AttributeSet attr) throws BadLocationException {
                Document doc = fb.getDocument();
                String temp = doc.getText(0, offs) + string + doc.getText(offs, doc.getLength()-offs);
                if (ensureByteRepresented(temp)) {
                    fb.insertString(offs, temp, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offs, int len, String text, AttributeSet attrs) throws BadLocationException {
                Document doc = fb.getDocument();
                String temp = doc.getText(0, offs) + text + doc.getText(offs+len, doc.getLength()-(offs+len));
                if (ensureByteRepresented(temp)) {
                    fb.replace(offs, len, text, attrs);
                }
            }
	}
}