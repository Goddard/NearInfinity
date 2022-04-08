// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp;

import java.nio.ByteBuffer;

import org.infinity.resource.AbstractStruct;

class AreaLinkEast extends AreaLink {
  // WMP/AreaLinkEast-specific field labels
  public static final String WMP_LINK_EAST = "East link";

  AreaLinkEast() throws Exception {
    super(WMP_LINK_EAST);
  }

  AreaLinkEast(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception {
    super(superStruct, buffer, offset, WMP_LINK_EAST + " " + number);
  }
}
