// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;

public final class UnknownBinary extends Unknown
{
  public UnknownBinary(byte[] buffer, int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public UnknownBinary(StructEntry parent, byte[] buffer, int offset, int length, String name)
  {
    super(parent, buffer, offset, length, name);
  }

// --------------------- Begin Interface Editable ---------------------

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    String value = textArea.getText().trim();
    value = value.replace('\n', ' ');
    value = value.replace('\r', ' ');
    int index = value.indexOf((int)' ');
    while (index != -1) {
      value = value.substring(0, index) + value.substring(index + 1);
      index = value.indexOf((int)' ');
    }
    if (value.length() != 8 * data.length)
      return false;
    byte newdata[] = new byte[data.length];
    for (int i = 0; i < newdata.length; i++) {
      String bytechars = value.substring(8 * i, 8 * i + 8);
      try {
        newdata[i] = (byte)Integer.parseInt(bytechars, 2);
      } catch (NumberFormatException e) {
        return false;
      }
    }
    data = newdata;
    return true;
  }

// --------------------- End Interface Editable ---------------------

  @Override
  public String toString()
  {
    if (data != null && data.length > 0) {
      StringBuffer sb = new StringBuffer(9 * data.length + 1);
      for (final byte d : data) {
        String text = Integer.toBinaryString((int)d & 0xff);
        for (int j = 0, count = 8 - text.length(); j < count; j++) {
          sb.append('0');
        }
        sb.append(text).append(' ');
      }
      sb.append('b');
      return sb.toString();
    } else
      return new String();
  }
}

