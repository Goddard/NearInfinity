// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Entrance;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.util.Logger;

/**
 * Handles specific layer type: ARE/Entrance
 */
public class LayerObjectEntrance extends LayerObjectEntranceBase {
  private static final Image[] ICONS = { ViewerIcons.ICON_ITM_ENTRANCE_1.getIcon().getImage(),
                                         ViewerIcons.ICON_ITM_ENTRANCE_2.getIcon().getImage() };

  private static final Point CENTER = ViewerIcons.ICON_ITM_ENTRANCE_1.getCenter();

  private final Entrance entrance;
  private final Point location = new Point();

  private final IconLayerItem item;

  public LayerObjectEntrance(AreResource parent, Entrance entrance) {
    super(parent);
    this.entrance = entrance;
    String msg = null;
    try {
      location.x = ((IsNumeric) entrance.getAttribute(Entrance.ARE_ENTRANCE_LOCATION_X)).getValue();
      location.y = ((IsNumeric) entrance.getAttribute(Entrance.ARE_ENTRANCE_LOCATION_Y)).getValue();
      int o = ((IsNumeric) entrance.getAttribute(Entrance.ARE_ENTRANCE_ORIENTATION)).getValue();
      if (o < 0) {
        o = 0;
      } else if (o >= AbstractStruct.OPTION_ORIENTATION.length) {
        o = AbstractStruct.OPTION_ORIENTATION.length - 1;
      }
      final String name = entrance.getAttribute(Entrance.ARE_ENTRANCE_NAME).toString();
      msg = String.format("%s (%s)", name, AbstractStruct.OPTION_ORIENTATION[o]);
    } catch (Exception e) {
      Logger.error(e);
    }

    // Using cached icons
    final Image[] icons = getIcons(ICONS);

    item = new IconLayerItem(entrance, msg, icons[0], CENTER);
    item.setLabelEnabled(Settings.ShowLabelEntrances);
    item.setName(getCategory());
    item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    item.setVisible(isVisible());
  }

  @Override
  public Viewable getViewable() {
    return entrance;
  }

  @Override
  protected IconLayerItem getLayerItem() {
    return item;
  }

  @Override
  protected Point getLocation() {
    return location;
  }
}
