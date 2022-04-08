// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
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
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rtextarea.RTextArea;
import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.InfinityTextArea;
import org.infinity.gui.StructViewer;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.util.Misc;
import org.infinity.util.io.StreamUtils;

/**
 * <h2>Bean property</h2> When this field is child of {@link AbstractStruct}, then changes of its internal value
 * reported as {@link PropertyChangeEvent}s of the {@link #getParent() parent} struct.
 * <ul>
 * <li>Property name: {@link #getName() name} of this field</li>
 * <li>Property type: {@link String}</li>
 * </ul>
 */
public final class TextEdit extends Datatype implements Editable, IsTextual {
  public static enum EOLType {
    UNIX, WINDOWS
  }

  public static enum Align {
    LEFT, RIGHT, TOP, BOTTOM
  }

  private static final EnumMap<EOLType, String> EOL = new EnumMap<>(EOLType.class);

  static {
    EOL.put(EOLType.UNIX, "\n");
    EOL.put(EOLType.WINDOWS, "\r\n");
  }

  private InfinityTextArea textArea;
  private final ByteBuffer buffer;

  private String text;
  private EOLType eolType = EOLType.UNIX;
  private Charset charset = Charset.defaultCharset();
  private boolean terminateString;
  private boolean editable = true;

  public TextEdit(ByteBuffer buffer, int offset, int length, String name) {
    super(offset, length, name);
    this.buffer = StreamUtils.getByteBuffer(getSize());
    read(buffer, offset);
  }

  // --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container) {
    JButton bUpdate;
    if (textArea == null) {
      textArea = new InfinityTextArea(1, 200, true);
      textArea.setHighlightCurrentLine(editable);
      textArea.setWrapStyleWord(true);
      textArea.setLineWrap(true);
      textArea.setMargin(new Insets(3, 3, 3, 3));
      textArea.setDocument(new FixedDocument(textArea, buffer.limit()));
      textArea.setEditable(editable);
    }
    textArea.setText(toString());
    textArea.setCaretPosition(0);
    textArea.discardAllEdits();
    InfinityScrollPane scroll = new InfinityScrollPane(textArea, true);
    scroll.setLineNumbersEnabled(false);

    bUpdate = new JButton("Update value", Icons.ICON_REFRESH_16.getIcon());
    bUpdate.setEnabled(editable);
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    int curGridX = 0;
    int curGridY = 0;
    gbc = ViewerUtil.setGBC(gbc, curGridX, curGridY, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0);
    panel.add(scroll, gbc);

    gbc = ViewerUtil.setGBC(gbc, curGridX + 1, curGridY, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(0, 6, 0, 0), 0, 0);
    panel.add(bUpdate, gbc);

    panel.setMinimumSize(Misc.getScaledDimension(DIM_BROAD));
    panel.setPreferredSize(Misc.getScaledDimension(DIM_BROAD));
    return panel;
  }

  @Override
  public void select() {
  }

  @Override
  public boolean updateValue(AbstractStruct struct) {
    String oldString = getText();
    setValue(textArea.getText());

    // notifying listeners
    if (!getText().equals(oldString)) {
      fireValueUpdated(new UpdateEvent(this, struct));
    }

    return true;
  }

  // --------------------- End Interface Editable ---------------------

  // --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException {
    StreamUtils.writeBytes(os, toBuffer());
  }

  // --------------------- End Interface Writeable ---------------------

  // --------------------- Begin Interface Readable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset) {
    StreamUtils.copyBytes(buffer, offset, this.buffer, 0, getSize());
    return offset + getSize();
  }

  // --------------------- End Interface Readable ---------------------

  // --------------------- Begin Interface IsTextual ---------------------

  @Override
  public String getText() {
    if (text == null) {
      buffer.position(0);
      String s = StreamUtils.readString(buffer, buffer.limit(), charset);
      text = eolConvert(s, Misc.LINE_SEPARATOR);
    }
    return text;
  }

  // --------------------- End Interface IsTextual ---------------------

  @Override
  public String toString() {
    return getText();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(text);
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
    TextEdit other = (TextEdit) obj;
    return Objects.equals(text, other.text);
  }

  public ByteBuffer toBuffer() {
    if (text != null) {
      byte[] buf = eolConvert(text).getBytes();
      if (buf != null) {
        int imax = Math.min(buf.length, buffer.limit());
        buffer.position(0);
        buffer.put(buf, 0, imax);
        while (buffer.remaining() > 0) {
          buffer.put((byte) 0);
        }
        if (terminateString) {
          buffer.position(buffer.position() - 1);
          buffer.put((byte) 0);
        }
      }
    }
    return buffer;
  }

  public EOLType getEolType() {
    return eolType;
  }

  public void setEolType(EOLType type) {
    if (type != null) {
      eolType = type;
    }
  }

  public boolean getStringTerminated() {
    return terminateString;
  }

  public void setStringTerminated(boolean terminated) {
    terminateString = terminated;
  }

  public Charset getCharset() {
    return charset;
  }

  public boolean setCharset(String charsetName) {
    if (Charset.isSupported(charsetName)) {
      this.charset = Charset.forName(charsetName);
      return true;
    } else {
      return false;
    }
  }

  public boolean getEditable() {
    return editable;
  }

  public void setEditable(boolean edit) {
    editable = edit;
  }

  private String eolConvert(String s) {
    if (s != null && s.length() > 0) {
      return s.replaceAll("(\r\n|\n)", EOL.get(eolType));
    } else {
      return s;
    }
  }

  private String eolConvert(String s, String eol) {
    if (s != null && s.length() > 0 && eol != null && eol.length() > 0) {
      return s.replaceAll("(\r\n|\n)", eol);
    } else {
      return s;
    }
  }

  private void setValue(String newValue) {
    final String oldValue = getText();
    text = newValue;
    if (!Objects.equals(oldValue, newValue)) {
      firePropertyChange(oldValue, newValue);
    }
  }

  // -------------------------- INNER CLASSES --------------------------

  // Ensures a size limit on byte level
  private class FixedDocument extends RSyntaxDocument {
    private int maxLength;
    private RTextArea textArea;

    FixedDocument(RTextArea text, int length) {
      super(null);
      textArea = text;
      maxLength = length >= 0 ? length : 0;
    }

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
      if (str == null || textArea == null
          || eolConvert(textArea.getText()).getBytes().length + eolConvert(str).getBytes().length > maxLength) {
        return;
      }
      super.insertString(offs, str, a);
    }
  }
}
