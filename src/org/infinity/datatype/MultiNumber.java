// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import org.infinity.gui.StructViewer;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.resource.AbstractStruct;
import org.infinity.util.Logger;
import org.infinity.util.Misc;

/**
 * A Number object consisting of multiple values of a given number of bits.
 *
 * <h2>Bean property</h2> When this field is child of {@link AbstractStruct}, then changes of its internal value
 * reported as {@link PropertyChangeEvent}s of the {@link #getParent() parent} struct.
 * <ul>
 * <li>Property name: {@link #getName() name} of this field</li>
 * <li>Property type: {@code int}</li>
 * <li>Value meaning: OR'ed values of each individual subfield of this field</li>
 * </ul>
 */
public class MultiNumber extends Datatype implements Editable, IsNumeric {
  private final ValueTableModel mValues;

  private int value;
  private JTable tValues;

  /**
   * Constructs a Number object consisting of multiple unsigned values of a given number of bits.
   *
   * @param buffer     The buffer containing resource data for this type.
   * @param offset     Resource offset
   * @param length     Resource length in bytes. Supported lengths: 1, 2, 3, 4
   * @param name       Field name
   * @param numBits    Number of bits for each value being part of the Number object.
   * @param numValues  Number of values to consider. Supported range: [1, length*8/numBits]
   * @param valueNames List of individual field names for each contained value.
   */
  public MultiNumber(ByteBuffer buffer, int offset, int length, String name, int numBits, int numValues,
      String[] valueNames) {
    this(buffer, offset, length, name, numBits, numValues, valueNames, false);
  }

  /**
   * Constructs a Number object consisting of multiple values of a given number of bits.
   *
   * @param buffer     The buffer containing resource data for this type.
   * @param offset     Resource offset
   * @param length     Resource length in bytes. Supported lengths: 1, 2, 3, 4
   * @param name       Field name
   * @param numBits    Number of bits for each value being part of the Number object.
   * @param numValues  Number of values to consider. Supported range: [1, length*8/numBits]
   * @param valueNames List of individual field names for each contained value.
   * @param signed     Whether values are signed.
   */
  public MultiNumber(ByteBuffer buffer, int offset, int length, String name, int numBits, int numValues,
      String[] valueNames, boolean signed) {
    super(offset, length, name);

    read(buffer, offset);

    if (numBits < 1 || numBits > (length * 8)) {
      numBits = length * 8;
    }

    if (numValues < 1) {
      numValues = 1;
    } else if (numValues > (length * 8 / numBits)) {
      numValues = length * 8 / numBits;
    }

    mValues = new ValueTableModel(value, numBits, numValues, valueNames, signed);
  }

  // --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container) {
    tValues = new JTable(mValues);
    tValues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tValues.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getOptions().getScriptFont()));
    tValues.getTableHeader().setFont(tValues.getTableHeader().getFont().deriveFont(Font.BOLD));
    tValues.setRowHeight(tValues.getFontMetrics(tValues.getFont()).getHeight() + 1);
    tValues.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    tValues.getTableHeader().setBorder(BorderFactory.createLineBorder(Color.GRAY));
    tValues.getTableHeader().setReorderingAllowed(false);
    tValues.getTableHeader().setResizingAllowed(true);
    tValues.setRowHeight(tValues.getRowHeight() * 5 / 4);
    tValues.setIntercellSpacing(new Dimension(4, 0));
    tValues.setPreferredScrollableViewportSize(tValues.getPreferredSize());
    tValues.setCellSelectionEnabled(true);
    tValues.getModel().addTableModelListener(e -> {
      // update field value automatically
      if (e.getType() == TableModelEvent.UPDATE) {
        ActionEvent ae = new ActionEvent(tValues, 0, StructViewer.UPDATE_VALUE);
        container.actionPerformed(ae);
      }
    });

    JScrollPane scroll = new JScrollPane(tValues);
    scroll.setBorder(BorderFactory.createEmptyBorder());

    JPanel panel = new JPanel(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    panel.add(scroll, gbc);

    Dimension dim = Misc.getScaledDimension(DIM_MEDIUM);
    panel.setPreferredSize(dim);

    // making "Attribute" column wider
    tValues.getColumnModel().getColumn(0).setPreferredWidth(dim.width * 3 / 4);
    tValues.getColumnModel().getColumn(1).setPreferredWidth(dim.width / 4);

    tValues.changeSelection(0, 1, false, false);

    return panel;
  }

  @Override
  public void select() {
    mValues.setValue(value);
  }

  @Override
  public boolean updateValue(AbstractStruct struct) {
    long oldValue = getLongValue();
    setValueImpl(mValues.getValue());

    // notifying listeners
    if (getLongValue() != oldValue) {
      fireValueUpdated(new UpdateEvent(this, struct));
    }

    return true;
  }

  // --------------------- End Interface Editable ---------------------

  // --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException {
    writeInt(os, value);
  }

  // --------------------- End Interface Writeable ---------------------

  // --------------------- Begin Interface Readable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset) {
    buffer.position(offset);
    switch (getSize()) {
      case 1:
        value = buffer.get();
        break;
      case 2:
        value = buffer.getShort();
        break;
      case 3:
        value = buffer.getInt() & 0xffffff;
        value <<= 8;
        value >>= 8; // sign-extend
        break;
      case 4:
        value = buffer.getInt();
        break;
      default:
        throw new IllegalArgumentException();
    }

    return offset + getSize();
  }

  // --------------------- End Interface Readable ---------------------

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < getValueCount(); i++) {
      sb.append(String.format("%s: %d", getValueName(i), getValue(i)));
      if (i + 1 < getValueCount()) {
        sb.append(", ");
      }
    }
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(value);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MultiNumber other = (MultiNumber) obj;
    return value == other.value;
  }

  /** Returns number of bits per value. */
  public int getBits() {
    return mValues.getBitsPerValue();
  }

  /** Returns number of values stored in the Number object. */
  public int getValueCount() {
    return mValues.getValueCount();
  }

  /** Returns the label associated with the specified value. */
  public String getValueName(int idx) {
    return mValues.getValueName(idx);
  }

  /** Returns whether numbers are treated as signed values. */
  public boolean isSigned() {
    return mValues.isSigned();
  }

  // --------------------- Begin Interface IsNumeric ---------------------

  @Override
  public long getLongValue() {
    return value & 0xffffffffL;
  }

  @Override
  public int getValue() {
    return value;
  }

  // --------------------- End Interface IsNumeric ---------------------

  /** Returns the specified value. */
  public int getValue(int idx) {
    if (idx >= 0 && idx < mValues.getValueCount()) {
      if (getBits() < 32) {
        return bitRangeAsNumber(getValue(), getBits(), idx * getBits(), isSigned());
      } else {
        return getValue();
      }
    }
    return 0;
  }

  /** Set the value for the whole Number object. */
  public void setValue(int value) {
    mValues.setValue(value);
    setValueImpl(mValues.getValue());
  }

  /** Sets the specified value. */
  public void setValue(int idx, int value) {
    mValues.setValue(idx, value);
    setValueImpl(mValues.getValue());
  }

  /**
   * Helper function: Returns the bit range defined by the parameters as an individual number.
   *
   * @param data   The source value to extract a bit range from.
   * @param bits   Number of bits to extract.
   * @param pos    Starting bit position of the new value.
   * @param signed Whether the returned number is signed.
   */
  public static int bitRangeAsNumber(int data, int bits, int pos, boolean signed) {
    if (pos < 0) {
      pos = 0;
    }
    int retVal = (data >> pos) & ((1 << bits) - 1);
    if (signed && (retVal & (1 << (bits - 1))) != 0) {
      retVal |= -(1 << bits);
    }
    return retVal;
  }

  private void setValueImpl(int newValue) {
    final int oldValue = value;
    value = newValue;
    if (oldValue != newValue) {
      firePropertyChange(oldValue, newValue);
    }
  }

  // -------------------------- INNER CLASSES --------------------------

  /** Manages a fixed two columns table with a given number of rows. */
  private static class ValueTableModel extends AbstractTableModel {
    private static final int ATTRIBUTE = 0;
    private static final int VALUE = 1;

    private final Object[][] data;

    private final int bits;
    private final int numValues;
    private final boolean signed;

    public ValueTableModel(int value, int bits, int numValues, String[] labels, boolean signed) {
      if (bits < 1) {
        bits = 1;
      } else if (bits > 32) {
        bits = 32;
      }
      if (numValues < 1 || numValues > (32 / bits)) {
        numValues = 32 / bits;
      }

      this.bits = bits;
      this.numValues = numValues;
      this.signed = signed;
      data = new Object[2][numValues];
      for (int i = 0; i < numValues; i++) {
        if (labels != null && i < labels.length && labels[i] != null) {
          data[ATTRIBUTE][i] = labels[i];
        } else {
          data[ATTRIBUTE][i] = "Value " + (i + 1);
        }
        data[VALUE][i] = bitRangeAsNumber(value, bits, i * bits, this.signed);
      }
    }

    // --------------------- Begin Class AbstractTableModel ---------------------

    @Override
    public int getRowCount() {
      return numValues;
    }

    @Override
    public int getColumnCount() {
      return 2; // fixed
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if (rowIndex >= 0 && rowIndex < numValues && columnIndex >= 0 && columnIndex < 2) {
        return data[columnIndex][rowIndex];
      }
      return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (columnIndex == 1) {
        if (rowIndex >= 0 && rowIndex < numValues) {
          try {
            int newVal = Integer.parseInt(aValue.toString());
            if (signed) {
              newVal = Math.min((1 << (bits - 1)) - 1, Math.max(-(1 << (bits - 1)), newVal));
            } else {
              newVal = Math.min((1 << bits) - 1, Math.max(0, newVal));
            }
            data[VALUE][rowIndex] = newVal;
            fireTableCellUpdated(rowIndex, columnIndex);
          } catch (NumberFormatException e) {
            Logger.error(e);
          }
        }
      }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return (columnIndex == VALUE);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      if (columnIndex >= 0 && columnIndex < 2) {
        return Objects.requireNonNull(getValueAt(0, columnIndex)).getClass();
      } else {
        return Object.class;
      }
    }

    @Override
    public String getColumnName(int columnIndex) {
      switch (columnIndex) {
        case ATTRIBUTE:
          return "Attribute";
        case VALUE:
          return "Value";
        default:
          return "";
      }
    }

    // --------------------- End Class AbstractTableModel ---------------------

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < numValues; i++) {
        if (i != 0) {
          sb.append(", ");
        }
        sb.append(data[ATTRIBUTE][i]).append(": ").append(data[VALUE][i]);
      }
      return sb.toString();
    }

    public int getValue() {
      int retVal = 0;
      for (int i = 0; i < numValues; i++) {
        retVal |= (getValue(i) & ((1 << bits) - 1)) << (i * bits);
      }
      return retVal;
    }

    public int getValue(int rowIndex) {
      if (rowIndex >= 0 && rowIndex < numValues) {
        return ((Integer) data[VALUE][rowIndex]);
      }
      return 0;
    }

    public void setValue(int v) {
      for (int i = 0; i < numValues; i++, v >>>= bits) {
        data[VALUE][i] = bitRangeAsNumber(v, bits, 0, signed);
      }
    }

    public void setValue(int rowIndex, int v) {
      if (rowIndex >= 0 && rowIndex < numValues) {
        data[VALUE][rowIndex] = bitRangeAsNumber(v, bits, 0, signed);
      }
    }

    public int getValueCount() {
      return numValues;
    }

    public int getBitsPerValue() {
      return bits;
    }

    public String getValueName(int rowIndex) {
      if (rowIndex >= 0 && rowIndex < numValues) {
        return (String) data[ATTRIBUTE][rowIndex];
      }
      return "";
    }

    public boolean isSigned() {
      return signed;
    }
  }
}
