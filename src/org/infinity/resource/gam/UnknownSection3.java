// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import java.nio.ByteBuffer;

import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;

final class UnknownSection3 extends AbstractStruct {
  // GAM/Unknown-specific field labels
  public static final String GAM_UNKNOWN = "Unknown section";

  UnknownSection3(AbstractStruct superStruct, ByteBuffer buffer, int offset) throws Exception {
    super(superStruct, GAM_UNKNOWN, buffer, offset);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new Unknown(buffer, offset, 24));
    return offset + 24;
  }
}
