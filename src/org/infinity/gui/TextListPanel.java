// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.AbstractBitmap;
import org.infinity.datatype.IwdRef;
import org.infinity.datatype.ResourceBitmap;
import org.infinity.datatype.ResourceRef;
import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.FilteredListModel;
import org.infinity.util.IconCache;
import org.infinity.util.Logger;
import org.infinity.util.Misc;

public class TextListPanel<E> extends JPanel
    implements DocumentListener, ListSelectionListener, ActionListener, ChangeListener {
  private static boolean filterEnabled = false;

  private final FilteredListModel<E> listmodel = new FilteredListModel<>(filterEnabled);
  private final JList<E> list = new JList<>();
  private final JScrollPane scrollPane;
  private final JTextField tfield = new JTextField();
  private final JToggleButton tbFilter = new JToggleButton(Icons.ICON_FILTER_16.getIcon(), filterEnabled);

  private final boolean sortValues;

  public TextListPanel(List<? extends E> values) {
    this(values, true, false);
  }

  public TextListPanel(List<? extends E> values, boolean sortValues) {
    this(values, sortValues, false);
  }

  public TextListPanel(List<? extends E> values, boolean sortValues, boolean showIcons) {
    super(new BorderLayout());
    this.sortValues = sortValues;
    setValues(values);
    if (showIcons) {
      list.setCellRenderer(new IconCellRenderer());
    }
    list.setModel(listmodel);
    list.setSelectedIndex(0);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(this);
    list.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    listmodel.addFilterChangeListener(this);
    tfield.getDocument().addDocumentListener(this);

    scrollPane = new JScrollPane(list);

    tbFilter.setToolTipText("Toggle filtering on or off");
    tbFilter.addActionListener(this);
    // Horizontal margins are too wasteful on default l&f
    Insets margin = tbFilter.getMargin();
    margin.left = Math.min(4, margin.left);
    margin.right = Math.min(4, margin.right);
    margin.top = Math.min(4, margin.top);
    margin.bottom = Math.min(4, margin.bottom);
    tbFilter.setMargin(margin);

    JPanel pInput = new JPanel(new BorderLayout());
    pInput.add(tfield, BorderLayout.CENTER);
    pInput.add(tbFilter, BorderLayout.EAST);

    add(pInput, BorderLayout.NORTH);
    add(scrollPane, BorderLayout.CENTER);
    ensurePreferredComponentWidth(list, true);
    ensurePreferredComponentWidth(tfield, false);
  }

  // --------------------- Begin Interface DocumentListener ---------------------

  @Override
  public void insertUpdate(DocumentEvent event) {
    if (tfield.hasFocus()) {
      if (!filterEnabled) {
        selectClosest(tfield.getText());
      }
      listmodel.setPattern(tfield.getText());
    }
  }

  @Override
  public void removeUpdate(DocumentEvent event) {
    if (tfield.hasFocus()) {
      if (!filterEnabled) {
        selectClosest(tfield.getText());
      }
      listmodel.setPattern(tfield.getText());
    }
  }

  @Override
  public void changedUpdate(DocumentEvent event) {
  }

  // --------------------- End Interface DocumentListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event) {
    if (list.hasFocus() && list.getSelectedValue() != null) {
      if (!tfield.getText().equals(list.getSelectedValue().toString())) {
        tfield.setText(list.getSelectedValue().toString());
        listmodel.setPattern(tfield.getText());
      }
    }
  }

  // --------------------- End Interface ListSelectionListener ---------------------

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent e) {
    // fixes issues with scrollbar visibility and visibility of selected entry
    calculatePreferredComponentHeight(list);
  }

  // --------------------- End Interface ChangeListener ---------------------

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == tbFilter) {
      filterEnabled = tbFilter.isSelected();

      if (filterEnabled) {
        listmodel.setPattern(tfield.getText());
      }
      listmodel.setFiltered(filterEnabled);

      ensurePreferredComponentWidth(list, true);
      ensurePreferredComponentWidth(tfield, false);

      int idx = list.getSelectedIndex();
      E item = null;
      try {
        item = listmodel.get(idx);
      } catch (Exception ex) {
        Logger.trace(ex);
      }
      if (item == null || !item.toString().equals(tfield.getText())) {
        selectClosest(tfield.getText());
        idx = list.getSelectedIndex();
      }

      if (idx >= 0) {
        ensureIndexIsVisible(idx);
      }
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  @Override
  public synchronized void addMouseListener(MouseListener listener) {
    list.addMouseListener(listener);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    list.setEnabled(enabled);
    tfield.setEditable(enabled);
    tbFilter.setEnabled(enabled);
  }

  public void addListSelectionListener(ListSelectionListener listener) {
    list.addListSelectionListener(listener);
  }

  public void removeListSelectionListener(ListSelectionListener listener) {
    list.removeListSelectionListener(listener);
  }

  public void ensureIndexIsVisible(int i) {
    list.ensureIndexIsVisible(i);
  }

  public ListModel<E> getModel() {
    return listmodel;
  }

  public int getSelectedIndex() {
    return list.getSelectedIndex();
  }

  public E getSelectedValue() {
    return list.getSelectedValue();
  }

  public void setSelectedIndex(int index) {
    list.setSelectedIndex(index);
    list.ensureIndexIsVisible(index);
    tfield.setText(list.getSelectedValue().toString());
    listmodel.setPattern(list.getSelectedValue().toString());
  }

  public void setSelectedValue(E value, boolean shouldScroll) {
    list.setSelectedValue(value, shouldScroll);
    tfield.setText(value.toString());
    listmodel.setPattern(value.toString());
  }

  public void setValues(List<? extends E> values) {
    if (this.sortValues) {
      values.sort(Misc.getIgnoreCaseComparator());
    }
    listmodel.baseClear();
    listmodel.baseAddAll(values);
    tfield.setText("");
    if (list != null) {
      list.setSelectedIndex(0);
      list.ensureIndexIsVisible(0);
    }
  }

  /**
   * Selects the first list item starting with the specified text. Returns the index of the selected item or -1 if not
   * available.
   */
  private int selectClosest(String text) {
    int retVal = -1;
    if (!text.isEmpty() && listmodel.getSize() > 0) {
      final String pattern = text.toUpperCase(Locale.ENGLISH);
      E item = listmodel.elements().stream().filter(f -> f.toString().toUpperCase(Locale.ENGLISH).startsWith(pattern))
          .findFirst().orElse(listmodel.firstElement());
      retVal = listmodel.indexOf(item);
      if (retVal >= 0) {
        list.setSelectedIndex(retVal);
        list.ensureIndexIsVisible(retVal);
      }
    }
    return retVal;
  }

  /** Recalculates preferred control width enough to fit all list items horizontally. */
  private void ensurePreferredComponentWidth(JComponent c, boolean includeScrollBar) {
    if (c == null) {
      return;
    }

    final Graphics g = c.getGraphics() != null ? c.getGraphics() : NearInfinity.getInstance().getGraphics();
    final FontMetrics fm = c.getFontMetrics(c.getFont());
    if (fm == null) {
      return;
    }

    final E item = listmodel.baseElements().stream().max((e1, e2) -> {
      double w1 = fm.getStringBounds(e1.toString(), g).getWidth();
      double w2 = fm.getStringBounds(e2.toString(), g).getWidth();
      return (int) (w1 - w2);
    }).orElse(null);
    if (item != null) {
      int cw = (int) fm.getStringBounds(item.toString(), g).getWidth();
      cw += c.getInsets().left;
      cw += c.getInsets().right;
      if (includeScrollBar) {
        int sbWidth;
        try {
          sbWidth = ((Integer) UIManager.get("ScrollBar.width"));
        } catch (Exception ex) {
          // not all l&f styles provide UIManager value
          sbWidth = (new JScrollBar(Adjustable.VERTICAL)).getWidth();
        }
        cw += sbWidth;
      }
      Dimension d = c.getPreferredSize();
      d.width = cw;
      c.setPreferredSize(d);
      c.invalidate();
    }
  }

  /** Enforces recalculation of preferred control height. */
  private void calculatePreferredComponentHeight(JComponent c) {
    if (c == null) {
      return;
    }

    int width = c.getPreferredSize().width;
    c.setPreferredSize(null);
    Dimension d = c.getPreferredSize();
    d.width = width;
    c.setPreferredSize(d);
    c.invalidate();
  }

  // -------------------------- INNER CLASSES --------------------------

  private static class IconCellRenderer extends DefaultListCellRenderer {
    public IconCellRenderer() {
      super();
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
        boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value instanceof ResourceRef.ResourceRefEntry) {
        // resolving Resource Reference
        final ResourceRef.ResourceRefEntry entry = (ResourceRef.ResourceRefEntry) value;
        setIcon(IconCache.get(entry.getEntry(), IconCache.getDefaultListIconSize()));
      } else if (value instanceof AbstractBitmap.FormattedData<?>) {
        // resolving Resource Bitmap
        final AbstractBitmap.FormattedData<?> fmt = (AbstractBitmap.FormattedData<?>) value;
        // Limit icon preview to parent type: IwdRef
        if (fmt.getParent() instanceof IwdRef) {
          final AbstractBitmap<?> bmp = fmt.getParent();
          Object o = bmp.getDataOf(fmt.getValue());
          if (o instanceof ResourceBitmap.RefEntry) {
            final ResourceBitmap.RefEntry entry = (ResourceBitmap.RefEntry) o;
            ResourceEntry iconEntry = null;
            Icon defIcon = null;
            try {
              iconEntry = ResourceFactory.getResourceIcon(entry.getResourceEntry());
            } catch (FileNotFoundException e) {
              defIcon = ResourceFactory.getKeyfile().getIcon("BMP");
            }
            setIcon(IconCache.getIcon(iconEntry, IconCache.getDefaultListIconSize(), defIcon));
          }
        }
      }
      return this;
    }
  }
}
