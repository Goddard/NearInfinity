// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import infinity.datatype.IdsBitmap;
import infinity.datatype.StringRef;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.resource.Viewable;
import infinity.resource.are.viewer.icon.ViewerIcons;
import infinity.resource.cre.CreResource;
import infinity.resource.key.ResourceEntry;
import infinity.resource.text.PlainTextResource;
import infinity.util.IniMapEntry;
import infinity.util.IniMapSection;

/**
 * Handles specific layer type: INI/Actor
 * @author argent77
 */
public class LayerObjectIniActor extends LayerObjectActor
{
  private static final Image[] IconGood = new Image[]{Icons.getImage(ViewerIcons.class, "itm_IniActorG1.png"),
                                                      Icons.getImage(ViewerIcons.class, "itm_IniActorG2.png")};
  private static final Image[] IconNeutral = new Image[]{Icons.getImage(ViewerIcons.class, "itm_IniActorB1.png"),
                                                         Icons.getImage(ViewerIcons.class, "itm_IniActorB2.png")};
  private static final Image[] IconEvil = new Image[]{Icons.getImage(ViewerIcons.class, "itm_IniActorR1.png"),
                                                      Icons.getImage(ViewerIcons.class, "itm_IniActorR2.png")};
  private static final Point Center = new Point(12, 40);

  private final PlainTextResource ini;
  private final IniMapSection creData;
  private final int creIndex;

  public LayerObjectIniActor(PlainTextResource ini, IniMapSection creData) throws Exception
  {
    this(ini, creData, 0);
  }

  public LayerObjectIniActor(PlainTextResource ini, IniMapSection creData, int creIndex) throws Exception
  {
    super(CreResource.class, null);
    this.ini = ini;
    this.creData = creData;
    this.creIndex = creIndex;
    init();
  }

  @Override
  public void reload()
  {
    try {
      init();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public Viewable getViewable()
  {
    return ini;
  }

  @Override
  public Viewable[] getViewables()
  {
    return new Viewable[]{ini};
  }

  @Override
  public boolean isScheduled(int schedule)
  {
    return true;  // always active
  }


  private void init() throws Exception
  {
    if (ini != null && creData != null && creIndex >= 0) {
      // preparations
      IniMapEntry entrySpec = creData.getEntry("spec");
      int[] object = (entrySpec != null) ? IniMapEntry.splitObjectValue(entrySpec.getValue()) : null;

      IniMapEntry entryPoint = creData.getEntry("spawn_point");
      if (entryPoint == null) {
        throw new Exception(creData.getName() + ": Invalid spawn point");
      }
      String[] position = IniMapEntry.splitValues(entryPoint.getValue(), IniMapEntry.REGEX_POSITION);
      if (position == null || creIndex >= position.length) {
        throw new Exception(creData.getName() + ": Invalid spawn point index (" + creIndex + ")");
      }
      int[] pos = IniMapEntry.splitPositionValue(position[creIndex]);
      if (pos == null || pos.length < 2) {
        throw new Exception(creData.getName() + ": Invalid spawn point value");
      }

      String sectionName = creData.getName();
      String[] creNames = IniMapEntry.splitValues(creData.getEntry("cre_file").getValue());
      String creName = (creNames.length > 0) ? (creNames[0] + ".cre") : null;
      ResourceEntry creEntry = ResourceFactory.getResourceEntry(creName);
      if (creEntry == null) {
        throw new Exception(creData.getName() + ": Invalid CRE resref (" + creName + ")");
      }
      CreResource cre = null;
      try {
        cre = new CreResource(creEntry);
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (cre == null) {
        throw new Exception(creData.getName() + ": Invalid CRE resource");
      }

      // initializations
      Image[] icon;
      String msg = ((StringRef)cre.getAttribute(CreResource.CRE_NAME)).toString() + " [" + sectionName + "]";
      int ea = (int)((IdsBitmap)cre.getAttribute(CreResource.CRE_ALLEGIANCE)).getValue();
      location.x = pos[0];
      location.y = pos[1];

      // checking for overridden allegiance
      if (object != null && object.length > 0 && object[0] != 0) {
        ea = object[0];
      }

      if (ea >= 2 && ea <= 30) {
        icon = IconGood;
      } else if (ea >= 200) {
        icon = IconEvil;
      } else {
        icon = IconNeutral;
      }

      // Using cached icons
      String keyIcon = String.format("%1$s%2$s", SharedResourceCache.createKey(icon[0]),
                                                 SharedResourceCache.createKey(icon[1]));
      if (SharedResourceCache.contains(SharedResourceCache.Type.Icon, keyIcon)) {
        icon = ((ResourceIcon)SharedResourceCache.get(SharedResourceCache.Type.Icon, keyIcon)).getData();
        SharedResourceCache.add(SharedResourceCache.Type.Icon, keyIcon);
      } else {
        SharedResourceCache.add(SharedResourceCache.Type.Icon, keyIcon, new ResourceIcon(keyIcon, icon));
      }

      ini.setHighlightedLine(creData.getLine() + 1);
      item = new IconLayerItem(location, ini, msg, icon[0], Center);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.setVisible(isVisible());
    }
  }
}
