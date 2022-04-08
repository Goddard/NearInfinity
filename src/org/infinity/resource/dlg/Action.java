// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.nio.ByteBuffer;

public final class Action extends AbstractCode {
  // DLG/Action-specific field labels
  public static final String DLG_ACTION = "Action";

  private int nr;

  public Action() {
    super(DLG_ACTION);
  }

  public Action(ByteBuffer buffer, int offset, int count) {
    super(buffer, offset, DLG_ACTION + " " + count);
    this.nr = count;
  }

  public int getNumber() {
    return nr;
  }
}
