// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.infinity.NearInfinity;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.menu.BrowserMenuBar;

/**
 * <p>
 * Renderer for dialogue tree, drawing elements of each dialog with its own color (maximum of
 * {@link #OTHER_DIALOG_COLORS}{@code .length} different colors).
 * </p>
 *
 * <p>
 * Also, draws non-main items in gray, broken references (items, that refers from {@link State} or {@link Transition}
 * but that do not exists in the target {@link DlgResource}) in red and transition items in blue.
 * </p>
 *
 * @author Mingun
 */
final class DlgTreeCellRenderer extends DefaultTreeCellRenderer {
  // Color for response entry (if colored)
  private static final Color COLOR_RESPONSE = Color.BLUE;
  private static final Color COLOR_RESPONSE_DARK = Color.CYAN;

  // Color for broken reference
  private static final Color COLOR_BROKEN = Color.RED;
  private static final Color COLOR_BROKEN_DARK = Color.MAGENTA;

  /** Background colors for text in dialogs to that can refer main dialog. */
  private final HashMap<DlgResource, Color> dialogColors = new HashMap<>();

  /** Main dialogue that shown in the tree. */
  private final DlgResource dlg;

  public DlgTreeCellRenderer(DlgResource dlg) {
    this.dlg = dlg;
  }

  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
      int row, boolean focused) {
    final Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);
    // Tree reuse component, so we need to clear background
    setBackgroundNonSelectionColor(null);
    if (!(value instanceof ItemBase)) {
      return c;
    }

    final boolean isDark = NearInfinity.getInstance().isDarkMode();
    final ItemBase item = (ItemBase) value;

    final BrowserMenuBar options = BrowserMenuBar.getInstance();
    setIcon(options.getOptions().showDlgTreeIcons() ? item.getIcon() : null);
    setBackgroundNonSelectionColor(
        options.getOptions().colorizeOtherDialogs() ? getColor(item.getDialog()) : null);

    if (options.getOptions().useDifferentColorForResponses() && item instanceof TransitionItem) {
      setForeground(isDark ? COLOR_RESPONSE_DARK : COLOR_RESPONSE);
    }

    if (item instanceof BrokenReference) {
      setForeground(isDark ? COLOR_BROKEN_DARK : COLOR_BROKEN);// Broken reference
    } else if (item instanceof StateItem) {
      final StateItem state = (StateItem) item;
      final State s = state.getEntry();
      if (s.getNumber() == 0 && s.getTriggerIndex() < 0 && options.getOptions().alwaysShowState0()) {
        setForeground(Color.GRAY);
      }
    }
    if (item.getMain() != null) {
      setForeground(Color.GRAY);
    }
    return c;
  }

  private Color getColor(DlgResource dialog) {
    if (dlg == dialog) {
      return null;
    }

    final Color[] colors = ViewerUtil.getBackgroundColors();
    return dialogColors.computeIfAbsent(dialog,
        d -> colors[dialogColors.size() % colors.length]);
  }
}
