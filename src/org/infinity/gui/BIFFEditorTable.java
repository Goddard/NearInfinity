// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;

final class BIFFEditorTable extends JPanel implements ActionListener {
  private static final ImageIcon UPDATED_ICON_20 = Icons.ICON_BLUE_CIRCLE_20.getIcon();
  private static final ImageIcon NEW_ICON_20 = Icons.ICON_YELLOW_CIRCLE_20.getIcon();
  private static final ImageIcon BIF_ICON_20 = Icons.ICON_CIRCLE_20.getIcon();

  private final BifEditorTableModel tablemodel;
  private final JTable table;
  private final JToggleButton bbif;
  private final JToggleButton bupdated;
  private final JToggleButton bnew;

  private boolean sortreverse;
  private int sortbycolumn = 2;

  enum State {
    BIF(Icons.ICON_GREEN_CIRCLE_16), NEW(Icons.ICON_YELLOW_CIRCLE_16), UPD(Icons.ICON_BLUE_CIRCLE_16);

    private final ImageIcon icon;

    private State(Icons icon) {
      this.icon = icon.getIcon();
    }
  }

  BIFFEditorTable() {
    tablemodel = new BifEditorTableModel(this);
    bbif = new JToggleButton(BIF_ICON_20, true);
    bbif.setPreferredSize(new Dimension(BIF_ICON_20.getIconHeight() + 4, BIF_ICON_20.getIconWidth() + 4));
    bupdated = new JToggleButton(UPDATED_ICON_20, true);
    bupdated.setPreferredSize(new Dimension(UPDATED_ICON_20.getIconHeight() + 4, UPDATED_ICON_20.getIconWidth() + 4));
    bnew = new JToggleButton(NEW_ICON_20, true);
    bnew.setPreferredSize(new Dimension(NEW_ICON_20.getIconHeight() + 4, NEW_ICON_20.getIconWidth() + 4));
    bbif.addActionListener(this);
    bupdated.addActionListener(this);
    bnew.addActionListener(this);
    bbif.setToolTipText("Show files from the BIF file");
    bupdated.setToolTipText("Show updated files");
    bnew.setToolTipText("Show new files");
    table = new JTable(tablemodel);
    table.setDefaultRenderer(Object.class, new ToolTipTableCellRenderer());
    table.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
    table.getColumnModel().setColumnSelectionAllowed(false);
    TableCellRenderer renderer = (table, o, b, b1, i, i1) -> new JLabel((ImageIcon) o);
    table.getColumnModel().getColumn(0).setCellRenderer(renderer);
    table.getColumnModel().getColumn(1).setCellRenderer(renderer);
    table.getColumnModel().getColumn(0).setPreferredWidth(20);
    table.getColumnModel().getColumn(1).setPreferredWidth(20);
    table.getColumnModel().getColumn(2).setPreferredWidth(200);
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
          BifEditorTableLine line = tablemodel.get(table.getSelectedRow());
          NearInfinity.getInstance().showResourceEntry(line.entry);
        }
      }
    });
    table.getTableHeader().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        TableColumnModel columnmodel = table.getColumnModel();
        int viewcolumn = columnmodel.getColumnIndexAtX(event.getX());
        int column = table.convertColumnIndexToModel(viewcolumn);
        if (event.getClickCount() == 1 && column != -1) {
          if (sortbycolumn == column) {
            sortreverse = !sortreverse;
          } else {
            sortbycolumn = column;
            sortreverse = false;
          }
          tablemodel.sort();
          table.repaint();
        }
      }
    });

    JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    bpanel.add(bbif);
    bpanel.add(bupdated);
    bpanel.add(bnew);

    setLayout(new BorderLayout());
    add(bpanel, BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == bbif) {
      tablemodel.fireTableDataChanged();
    } else if (event.getSource() == bupdated) {
      tablemodel.fireTableDataChanged();
    } else if (event.getSource() == bnew) {
      tablemodel.fireTableDataChanged();
    }
  }

  public void addEntry(ResourceEntry entry, State state) {
    tablemodel.add(new BifEditorTableLine(entry, state));
  }

  public void addListSelectionListener(ListSelectionListener listener) {
    table.getSelectionModel().addListSelectionListener(listener);
  }

  public void moveSelectedTo(BIFFEditorTable other) {
    for (final BifEditorTableLine value : getSelectedValues()) {
      if (other.tablemodel.add(value)) {
        tablemodel.remove(value);
      }
    }
    fireChanges();
    other.fireChanges();
  }

  public BifEditorTableLine[] getSelectedValues() {
    final BifEditorTableLine[] selected = new BifEditorTableLine[table.getSelectedRowCount()];
    int isel[] = table.getSelectedRows();
    for (int i = 0; i < isel.length; i++) {
      selected[i] = tablemodel.get(isel[i]);
    }
    return selected;
  }

  public List<ResourceEntry> getValueList(State state) {
    final List<ResourceEntry> list = new ArrayList<>();
    for (final BifEditorTableLine line : tablemodel.getEntries()) {
      if (line.state == state) {
        list.add(line.entry);
      }
    }
    return list;
  }

  public boolean isEmpty() {
    return tablemodel.getEntries().isEmpty();
  }

  public void sortTable() {
    tablemodel.sort();
  }

  private void fireChanges() {
    table.clearSelection();
    tablemodel.sort();
    tablemodel.fireTableDataChanged();
  }

  // -------------------------- INNER CLASSES --------------------------

  static final class BifEditorTableLine {
    private final State state;
    private final ResourceEntry entry;

    private BifEditorTableLine(ResourceEntry entry, State state) {
      this.entry = entry;
      this.state = state;
    }
  }

  private final class BifEditorTableModel extends AbstractTableModel implements Comparator<BifEditorTableLine> {
    private final List<BifEditorTableLine> entries = new ArrayList<>();
    private final List<BifEditorTableLine> hiddenentries = new ArrayList<>();
    private final Component parent;

    private BifEditorTableModel(Component parent) {
      this.parent = parent;
    }

    private boolean add(BifEditorTableLine line) {
      if (line.state == State.NEW) {
        entries.add(line);
        return true;
      }
      for (Iterator<BifEditorTableLine> i = entries.iterator(); i.hasNext();) {
        BifEditorTableLine oldline = i.next();
        if (oldline.entry.getResourceName().equalsIgnoreCase(line.entry.getResourceName())) {
          if (line.state == State.UPD) {
            i.remove();
            hiddenentries.add(oldline);
            entries.add(line);
            return true;
          } else if (line.state == State.BIF) {
            String options[] = { "Keep updated", "Overwrite updated", "Cancel" };
            int choice = JOptionPane.showOptionDialog(parent, "An updated version of this file already exists.",
                "Updated version exists", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options,
                options[0]);
            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
              return false;
            }
            if (choice == 0) {
              return true;
            }
            i.remove();
            entries.add(line);
            return true;
          }
          break;
        }
      }
      entries.add(line);
      return true;
    }

    private void remove(BifEditorTableLine line) {
      if (line.state == State.UPD) {
        for (Iterator<BifEditorTableLine> i = hiddenentries.iterator(); i.hasNext();) {
          BifEditorTableLine hidden = i.next();
          if (line.entry.getResourceName().equalsIgnoreCase(hidden.entry.getResourceName())) {
            entries.remove(line);
            entries.add(hidden);
            i.remove();
            return;
          }
        }
      }
      entries.remove(line);
    }

    @Override
    public int getRowCount() {
      int count = 0;
      for (final BifEditorTableLine line : entries) {
        if (line.state == State.BIF && bbif.isSelected()) {
          count++;
        } else if (line.state == State.NEW && bnew.isSelected()) {
          count++;
        } else if (line.state == State.UPD && bupdated.isSelected()) {
          count++;
        }
      }
      return count;
    }

    @Override
    public Object getValueAt(int row, int column) {
      BifEditorTableLine line = get(row);
      if (column == 2) {
        return line.entry;
      }
      if (column == 1) {
        return line.entry.getIcon();
      }
      return line.state.icon;
    }

    public BifEditorTableLine get(int row) {
      final List<BifEditorTableLine> newlist = new ArrayList<>();
      for (final BifEditorTableLine line : entries) {
        if (line.state == State.BIF && bbif.isSelected()) {
          newlist.add(line);
        } else if (line.state == State.NEW && bnew.isSelected()) {
          newlist.add(line);
        } else if (line.state == State.UPD && bupdated.isSelected()) {
          newlist.add(line);
        }
        if (row < newlist.size()) {
          return newlist.get(row);
        }
      }
      return newlist.get(row);
    }

    private List<BifEditorTableLine> getEntries() {
      return entries;
    }

    @Override
    public int getColumnCount() {
      return 3;
    }

    @Override
    public String getColumnName(int i) {
      if (i == 0 || i == 1) {
        return "  ";
      }
      return "Filename";
    }

    public void sort() {
      Collections.sort(entries, this);
    }

    @Override
    public int compare(BifEditorTableLine line1, BifEditorTableLine line2) {
      int result = 0;
      if (sortbycolumn == 0) {
        result = Integer.compare(line1.state.ordinal(), line2.state.ordinal());
      } else if (sortbycolumn == 1) {
        result = line1.entry.getExtension().compareTo(line2.entry.getExtension());
      } else if (sortbycolumn == 2) {
        result = line1.entry.getResourceName().compareTo(line2.entry.getResourceName());
      }
      if (sortreverse) {
        result = -result;
      }
      return result;
    }
  }
}
