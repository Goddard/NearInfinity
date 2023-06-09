// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import static java.util.Collections.emptyList;
import static java.util.Collections.enumeration;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.TreeNode;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.icon.Icons;

/** Encapsulates a dialog state entry. */
class StateItem extends TransitionOwnerItem {
  private static final ImageIcon ICON = Icons.ICON_STOP_16.getIcon();

  private final State state;

  /** Tree item that represent visual parent of this state in the tree. */
  protected final StateOwnerItem parent;

  /**
   * Item to which need go to in break cycles tree view mode. This item contains referense to the same state as this one
   * (i.e. {@code this.state == main.ref.state})
   */
  private final DlgElement elem;

  /** Items that represents transition tree nodes from this state. */
  protected ArrayList<TransitionItem> trans;

  /**
   * Constructor for broken references to states.
   *
   * @param parent Transition which contains broken reference
   */
  protected StateItem(TransitionItem parent) {
    this.state = null;
    this.parent = Objects.requireNonNull(parent, "Parent transition of broken state item must be not null");
    this.elem = null;
  }

  public StateItem(State state, StateOwnerItem parent, DlgElement elem) {
    this.state = Objects.requireNonNull(state, "State dialog entry must be not null");
    this.parent = Objects.requireNonNull(parent, "Parent item of state item must be not null");
    this.elem = elem;
  }

  @Override
  public State getEntry() {
    return state;
  }

  @Override
  public ItemBase getMain() {
    return elem == null || elem.main == this ? null : elem.main;
  }

  @Override
  public DlgResource getDialog() {
    return state.getParent();
  }

  @Override
  public Icon getIcon() {
    return ICON;
  }

  @Override
  public boolean removeChild(ItemBase child) {
    return trans.remove(child);
  }

  @Override
  public void traverseChildren(Consumer<ItemBase> action) {
    if (trans != null) {
      trans.forEach(action);
    }
  }

  /** Returns technical name of state item which uniquely identifying it within dialog. */
  public String getName() {
    return state.getName();
  }

  @Override
  public TransitionItem getChildAt(int childIndex) {
    return getAllowsChildren() ? trans.get(childIndex) : null;
  }

  @Override
  public int getChildCount() {
    return getAllowsChildren() ? trans.size() : 0;
  }

  @Override
  public ItemBase getParent() {
    return parent;
  }

  @Override
  public int getIndex(TreeNode node) {
    return getAllowsChildren() ? trans.indexOf(node) : -1;
  }

  @Override
  public boolean getAllowsChildren() {
    return getMain() == null || !BrowserMenuBar.getInstance().breakCyclesInDialogs();
  }

  @Override
  public boolean isLeaf() {
    return getAllowsChildren() ? trans.isEmpty() : true;
  }

  @Override
  public Enumeration<? extends TransitionItem> children() {
    return enumeration(getAllowsChildren() ? trans : emptyList());
  }

  @Override
  public Iterator<TransitionItem> iterator() {
    return trans.iterator();
  }

  @Override
  public String toString() {
    return getText(state);
  }
}
