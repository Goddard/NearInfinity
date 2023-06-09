// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.InfinityTextArea;
import org.infinity.gui.StructViewer;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.util.Misc;
import org.infinity.util.io.StreamUtils;

/**
 * Field that represents binary data in hexademical format in their editor.
 *
 * <h2>Bean property</h2> When this field is child of {@link AbstractStruct}, then changes of its internal value
 * reported as {@link PropertyChangeEvent}s of the {@link #getParent() parent} struct.
 * <ul>
 * <li>Property name: {@link #getName() name} of this field</li>
 * <li>Property type: {@code byte[]}</li>
 * <li>Value meaning: raw bytes of this field</li>
 * </ul>
 */
public class Unknown extends Datatype implements Editable, IsBinary {
  protected InfinityTextArea textArea;
  protected ByteBuffer buffer;

  public Unknown(ByteBuffer buffer, int offset, int length) {
    this(buffer, offset, length, AbstractStruct.COMMON_UNKNOWN);
  }

  public Unknown(ByteBuffer buffer, int offset, int length, String name) {
    super(offset, length, name);
    this.buffer = StreamUtils.getByteBuffer(length);
    read(buffer, offset);
  }

  // --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container) {
    if (buffer.limit() > 0) {
      JButton bUpdate;
      if (textArea == null) {
        textArea = new InfinityTextArea(15, 5, true);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setEOLMarkersVisible(false);
        textArea.setMargin(new Insets(3, 3, 3, 3));
      }
      String s = toString();
      textArea.setText(s.substring(0, s.length() - 2));
      textArea.discardAllEdits();
      textArea.setCaretPosition(0);
      InfinityScrollPane scroll = new InfinityScrollPane(textArea, true);
      scroll.setLineNumbersEnabled(false);

      bUpdate = new JButton("Update value", Icons.ICON_REFRESH_16.getIcon());
      bUpdate.addActionListener(container);
      bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);

      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints gbc = new GridBagConstraints();
      JPanel panel = new JPanel(gbl);

      gbc.weightx = 1.0;
      gbc.weighty = 1.0;
      gbc.fill = GridBagConstraints.BOTH;
      gbl.setConstraints(scroll, gbc);
      panel.add(scroll);

      gbc.weightx = 0.0;
      gbc.fill = GridBagConstraints.NONE;
      gbc.insets.left = 6;
      gbl.setConstraints(bUpdate, gbc);
      panel.add(bUpdate);

      panel.setMinimumSize(Misc.getScaledDimension(DIM_BROAD));
      panel.setPreferredSize(Misc.getScaledDimension(DIM_BROAD));
      return panel;
    } else {
      JPanel panel = new JPanel();
      return panel;
    }
  }

  @Override
  public void select() {
  }

  @Override
  public boolean updateValue(AbstractStruct struct) {
    final byte[] newData = calcValue(2, 16);
    if (newData == null) {
      return false;
    }
    ByteBuffer oldBuffer = getData();
    setValue(newData);

    // notifying listeners
    if (getData().compareTo(oldBuffer) != 0) {
      fireValueUpdated(new UpdateEvent(this, struct));
    }

    return true;
  }

  // --------------------- End Interface Editable ---------------------

  // --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException {
    buffer.position(0);
    StreamUtils.writeBytes(os, buffer);
  }

  // --------------------- End Interface Writeable ---------------------

  // --------------------- Begin Interface Readable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset) {
    StreamUtils.copyBytes(buffer, offset, this.buffer, 0, getSize());

    return offset + getSize();
  }

  // --------------------- End Interface Readable ---------------------

  // --------------------- Begin Interface IsBinary ---------------------

  @Override
  public ByteBuffer getData() {
    ByteBuffer bb = ByteBuffer.allocate(buffer.capacity());
    buffer.rewind();
    bb.put(buffer);
    buffer.rewind();
    bb.flip();
    return bb;
  }

  // --------------------- End Interface IsBinary ---------------------

  @Override
  public String toString() {
    if (buffer.limit() > 0) {
      final StringBuilder sb = new StringBuilder(3 * buffer.limit() + 1);
      buffer.position(0);
      while (buffer.remaining() > 0) {
        int v = buffer.get() & 0xff;
        String text = Integer.toHexString(v);
        if (text.length() < 2) {
          sb.append('0');
        }
        sb.append(text).append(' ');
      }
      sb.append('h');
      return sb.toString();
    } else {
      return "";
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(buffer);
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
    Unknown other = (Unknown) obj;
    return Objects.equals(buffer, other.buffer);
  }

  protected void setValue(byte[] newValue) {
    buffer.position(0);
    final byte[] oldValue = new byte[buffer.remaining()];
    buffer.get(oldValue);

    buffer.position(0);
    buffer.put(newValue);

    if (!Arrays.equals(oldValue, newValue)) {
      firePropertyChange(oldValue, newValue);
    }
  }

  /**
   * Parses string from {@link #textArea editor} and returns it as byte array. All spaces and newline characters
   * stripped from string before conversion.
   *
   * @param unit  Length of each byte in characters in the input string
   * @param radix The radix to be used while parsing input string
   *
   * @return {@code null}, if length of the input string not equals field size or text has unconvertible symbols
   */
  protected byte[] calcValue(int unit, int radix) {
    final String value = textArea.getText().trim().replaceAll("\r?\n| ", "");
    final int length = buffer.limit();
    if (value.length() != unit * length) {
      return null;
    }
    try {
      final byte[] data = new byte[length];
      for (int i = 0; i < length; ++i) {
        final int from = i * unit;
        final String chars = value.substring(from, from + unit);
        data[i] = (byte) Integer.parseInt(chars, radix);
      }
      return data;
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
