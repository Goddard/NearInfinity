// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public final class ToolTipTableCellRenderer extends DefaultTableCellRenderer {

  // --------------------- Begin Interface TableCellRenderer ---------------------

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

    if (table.getColumnModel().getColumn(column).getWidth() < getFontMetrics(getFont()).stringWidth(getText())) {
      final StringBuilder sb = new StringBuilder("<html>");
      String string = getText();
      int index = 0;
      while (index < string.length()) {
        if (index > 0) {
          sb.append("<br>");
        }
        int delta = string.indexOf(' ', index + 100);
        if (delta == -1) {
          delta = string.length();
        }
        sb.append(string.substring(index, Math.min(delta, string.length())));
        index = delta;
      }
      sb.append("</html>");
      setToolTipText(sb.toString());
    } else {
      setToolTipText(null);
    }
    return this;
  }

  // --------------------- End Interface TableCellRenderer ---------------------
}
