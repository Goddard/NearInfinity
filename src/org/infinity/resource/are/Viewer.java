// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.LinkButton;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.are.viewer.AreaViewer;

final class Viewer extends JPanel implements ActionListener {
  private static final String CMD_VIEWAREA = "ViewArea";

  private final AreResource are;

  private JComponent makeFieldPanel() {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldBasePanel = new JPanel(new BorderLayout());
    JPanel fieldPanel = new JPanel(gbl);
    fieldBasePanel.add(fieldPanel, BorderLayout.CENTER);

    gbc.insets = new Insets(4, 4, 4, 16);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_AREA_NORTH), gbl, gbc, true, 80);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_AREA_EAST), gbl, gbc, true, 80);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_AREA_SOUTH), gbl, gbc, true, 80);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_AREA_WEST), gbl, gbc, true, 80);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_WED_RESOURCE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_PROBABILITY_RAIN), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_PROBABILITY_SNOW), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_PROBABILITY_FOG), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_PROBABILITY_LIGHTNING), gbl, gbc, true);

    // Special: IWD and IWD2 may fall back to a default area script if the script field is empty in the ARE resource
    final boolean allowAlternate = (Profile.getEngine() == Profile.Engine.IWD) ||
                                   (Profile.getEngine() == Profile.Engine.IWD2);
    final ResourceRef scriptRef = (ResourceRef) are.getAttribute(AreResource.ARE_AREA_SCRIPT);
    String scriptName = scriptRef.getResourceName();
    boolean isAlternate = false;
    if (allowAlternate && scriptRef.isEmpty()) {
      String bcsName = null;
      if (Profile.getEngine() == Profile.Engine.IWD) {
        // IWD only: draws fallback area script name from WED resource name
        // (IWD2 is hardcoded to use only WED resources of the same name as the ARE resource)
        final String wedResref = ((IsTextual)are.getAttribute(AreResource.ARE_WED_RESOURCE)).getText();
        if (!wedResref.isEmpty()) {
          bcsName = wedResref + ".BCS";
        }
      }
      if (bcsName == null) {
        bcsName = are.getResourceEntry().getResourceRef() + ".BCS";
      }
      if (ResourceFactory.resourceExists(bcsName)) {
        scriptName = bcsName;
        isAlternate = true;
      }
    }
    ViewerUtil.addLabelFieldPair(fieldPanel, new JLabel(AreResource.ARE_AREA_SCRIPT),
        new LinkButton(scriptName, 0, isAlternate), gbl, gbc, true);

    // IWDs and PSTs also have area INI spawn files
    if ((Boolean) Profile.getProperty(Profile.Key.IS_SUPPORTED_INI)) {
      String iniFile = are.getResourceEntry().getResourceRef() + ".INI";
      ViewerUtil.addLabelFieldPair(fieldPanel, new JLabel(AreResource.ARE_AREA_INI_FILE),
          new LinkButton(iniFile, 0, false), gbl, gbc, true);
    }

    JButton bView = new JButton("View Area", Icons.ICON_VOLUME_16.getIcon());
    bView.setActionCommand(CMD_VIEWAREA);
    bView.addActionListener(this);
    bView.setEnabled(AreaViewer.isValid(are));
    fieldBasePanel.add(bView, BorderLayout.SOUTH);

    JScrollPane scrollPane = new JScrollPane(fieldBasePanel);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setPreferredSize(scrollPane.getMinimumSize());
    return scrollPane;
  }

  private JComponent makeFlagsPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

    JPanel areaTypePanel = ViewerUtil.makeCheckPanel((Flag) are.getAttribute(AreResource.ARE_AREA_TYPE), 1);
    panel.add(areaTypePanel);

    JPanel locationPanel = ViewerUtil.makeCheckPanel((Flag) are.getAttribute(AreResource.ARE_LOCATION), 1);
    panel.add(locationPanel);

    JScrollPane scrollPane = new JScrollPane(panel);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setPreferredSize(scrollPane.getMinimumSize());

    return scrollPane;
  }

  Viewer(AreResource are) {
    this.are = are;

    // row 0, column 0
    JComponent fieldPanel = makeFieldPanel();
    // row 0, column 1
    JPanel actorPanel = ViewerUtil.makeListPanel("Actors", are, Actor.class, Actor.ARE_ACTOR_NAME);
    // row 0, column 2
    JPanel containerPanel = ViewerUtil.makeListPanel("Containers", are, Container.class, Container.ARE_CONTAINER_NAME);
    // row 1, column 0
    JComponent boxPanel = makeFlagsPanel();
    // row 1, column 1
    JPanel doorPanel = ViewerUtil.makeListPanel("Doors", are, Door.class, Door.ARE_DOOR_NAME);
    // row 1, column 2
    JPanel itePanel = ViewerUtil.makeListPanel("Points of interest", are, ITEPoint.class, ITEPoint.ARE_TRIGGER_NAME);

    setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    setLayout(new GridLayout(2, 3, 4, 4));
    add(fieldPanel);
    add(actorPanel);
    add(containerPanel);
    add(boxPanel);
    add(doorPanel);
    add(itePanel);
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getActionCommand().equals(CMD_VIEWAREA)) {
      are.showAreaViewer(this);
    }
  }

  // --------------------- End Interface ActionListener ---------------------
}
