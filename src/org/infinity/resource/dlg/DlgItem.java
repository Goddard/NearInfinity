// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.TreeNode;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.SectionCount;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.icon.Icons;
import org.infinity.resource.StructEntry;

/** Meta class for identifying dialogue node. */
final class DlgItem extends StateOwnerItem implements Iterable<StateItem>
{
  private static final ImageIcon ICON = Icons.getIcon(Icons.ICON_ROW_INSERT_AFTER_16);

  private final DlgTreeModel parent;
  /** Dialog which represents this tree. */
  private final DlgResource dlg;
  /** States from which dialog can start. */
  private final ArrayList<StateItem> states = new ArrayList<>();

  private final int numStates;
  private final int numTransitions;
  private final int numStateTriggers;
  private final int numResponseTriggers;
  private final int numActions;
  private final String flags;

  public DlgItem(DlgTreeModel parent, DlgResource dlg)
  {
    this.parent = parent;
    this.dlg = dlg;
    numStates           = getAttribute(DlgResource.DLG_NUM_STATES);
    numTransitions      = getAttribute(DlgResource.DLG_NUM_RESPONSES);
    numStateTriggers    = getAttribute(DlgResource.DLG_NUM_STATE_TRIGGERS);
    numResponseTriggers = getAttribute(DlgResource.DLG_NUM_RESPONSE_TRIGGERS);
    numActions          = getAttribute(DlgResource.DLG_NUM_ACTIONS);

    final StructEntry entry = dlg.getAttribute(DlgResource.DLG_THREAT_RESPONSE);
    flags = entry instanceof Flag ? ((Flag)entry).toString() : null;

    final boolean alwaysShow = BrowserMenuBar.getInstance().alwaysShowState0();
    // finding and storing initial states
    int count = 0;
    for (final StructEntry e : dlg.getFields()) {
      if (e instanceof State) {
        final State s = (State)e;
        // First state always under root, if setting is checked
        if (alwaysShow && count == 0 || s.getTriggerIndex() >= 0) {
          states.add(new StateItem(s, this, null));
        }
        if (++count >= numStates) {
          // All states read, so break cycle
          break;
        }
      }
    }
    // Sort by weight or natural order
    Collections.sort(states, new StateComparator());
  }

  @Override
  public TreeItemEntry getEntry() { return null; }

  @Override
  public ItemBase getMain() { return null; }

  @Override
  public DlgResource getDialog() { return dlg; }

  @Override
  public Icon getIcon() { return ICON; }

  @Override
  public boolean removeChild(ItemBase child) { return states.remove(child); }

  @Override
  public void traverseChildren(Consumer<ItemBase> action) { states.forEach(action); }

  @Override
  public StateItem getChildAt(int childIndex) { return states.get(childIndex); }

  @Override
  public int getChildCount() { return states.size(); }

  @Override
  public TreeNode getParent() { return parent; }

  @Override
  public int getIndex(TreeNode node) { return states.indexOf(node); }

  @Override
  public boolean getAllowsChildren() { return true; }

  @Override
  public boolean isLeaf() { return states.isEmpty(); }

  @Override
  public Enumeration<? extends StateItem> children() { return Collections.enumeration(states); }

  @Override
  public Iterator<StateItem> iterator() { return states.iterator(); }

  /**
   * Extracts specified {@link SectionCount} attribute from dialog.
   *
   * @param attrName Attribute name
   * @return value of the attribute or 0 if attribute does not exists
   */
  private int getAttribute(String attrName)
  {
    final StructEntry entry = getDialog().getAttribute(attrName, false);
    if (entry instanceof IsNumeric) {
      return ((IsNumeric)entry).getValue();
    }
    return 0;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getDialogName());
    sb.append(" (states: ").append(Integer.toString(numStates));
    sb.append(", responses: ").append(Integer.toString(numTransitions));
    sb.append(", state triggers: ").append(Integer.toString(numStateTriggers));
    sb.append(", response triggers: ").append(Integer.toString(numResponseTriggers));
    sb.append(", actions: ").append(Integer.toString(numActions));
    if (flags != null) {
      sb.append(", flags: ").append(flags);
    }
    sb.append(")");

    return sb.toString();
  }


//-------------------------- INNER CLASSES --------------------------

  /**
   * Performs comparison based on global dialog tree option "Sort states by weight".
   */
  private static class StateComparator implements Comparator<StateItem>
  {
    private final boolean sortByWeight;

    public StateComparator()
    {
      sortByWeight = BrowserMenuBar.getInstance().sortStatesByWeight();
    }

    @Override
    public int compare(StateItem state1, StateItem state2)
    {
      if (sortByWeight) {
        int idx1 = state1.getEntry().getTriggerIndex();
        int idx2 = state2.getEntry().getTriggerIndex();
        if (idx1 < 0) {
          return (idx1 == idx2) ? 0 : 1;
        } else if (idx2 < 0) {
          return (idx1 == idx2) ? 0 : -1;
        } else {
          return idx1 - idx2;
        }
      } else {
        return (state1.getEntry().getOffset() - state2.getEntry().getOffset());
      }
    }
  }
}
