// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * @author Jon Heggland
 */
public final class Center {
  public static void center(Component c, Rectangle area) {
    Dimension size = c.getSize();

    int x = area.x + (area.width - size.width >> 1);
    int y = area.y + (area.height - size.height >> 1);

    c.setLocation(Math.max(0, x), Math.max(0, y));
  }

  private Center() {
  }
}
