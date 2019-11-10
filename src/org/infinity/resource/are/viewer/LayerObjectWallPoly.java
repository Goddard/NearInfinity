// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.ShapedLayerItem;
import org.infinity.resource.Viewable;
import org.infinity.resource.vertex.Vertex;
import org.infinity.resource.wed.WallPolygon;
import org.infinity.resource.wed.WedResource;

/**
 * Handles specific layer type: ARE/Wall Polygon
 */
public class LayerObjectWallPoly extends LayerObject
{
  private static final Color[] COLOR = {new Color(0xFF005046, true), new Color(0xFF005046, true),
                                        new Color(0x8020A060, true), new Color(0xA030B070, true)};

  private final WallPolygon wall;
  private final Point location = new Point();

  private ShapedLayerItem item;
  private Point[] shapeCoords;

  public LayerObjectWallPoly(WedResource parent, WallPolygon wallPoly)
  {
    super("Wall Poly", WallPolygon.class, parent);
    this.wall = wallPoly;
    init();
  }

  //<editor-fold defaultstate="collapsed" desc="LayerObject">
  @Override
  public Viewable getViewable()
  {
    return wall;
  }

  @Override
  public Viewable[] getViewables()
  {
    return new Viewable[]{wall};
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return item;
  }

  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    return (type == 0) ? item : null;
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return new AbstractLayerItem[]{item};
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public void update(double zoomFactor)
  {
    if (item != null) {
      item.setItemLocation((int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           (int)(location.y*zoomFactor + (zoomFactor / 2.0)));

      Polygon poly = createPolygon(shapeCoords, zoomFactor);
      normalizePolygon(poly);
      item.setShape(poly);
    }
  }

  @Override
  public Point getMapLocation()
  {
    return location;
  }

  @Override
  public Point[] getMapLocations()
  {
    return new Point[]{location};
  }
  //</editor-fold>

  private void init()
  {
    if (wall != null) {
      shapeCoords = null;
      String msg = null;
      Polygon poly = null;
      Rectangle bounds = null;
      try {
        final Flag flags = (Flag)wall.getAttribute(WallPolygon.WED_POLY_FLAGS, false);
        msg = flags.toString();

        final int vNum = ((IsNumeric)wall.getAttribute(WallPolygon.WED_POLY_NUM_VERTICES)).getValue();
        final int vOfs = ((IsNumeric)getParentStructure().getAttribute(WedResource.WED_OFFSET_VERTICES)).getValue();
        int startIdx = flags.isFlagSet(2) ? 2 : 0;  // skipping first two vertices for "hovering walls"
        shapeCoords = loadVertices(wall, vOfs, startIdx, vNum - startIdx, Vertex.class);
        poly = createPolygon(shapeCoords, 1.0);
        bounds = normalizePolygon(poly);
      } catch (Exception e) {
        e.printStackTrace();
        if (shapeCoords == null) {
          shapeCoords = new Point[0];
        }
        if (poly == null) {
          poly = new Polygon();
        }
        if (bounds == null) {
          bounds = new Rectangle();
        }
      }

      location.x = bounds.x; location.y = bounds.y;
      item = new ShapedLayerItem(wall, msg, poly);
      item.setName(getCategory());
      item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, COLOR[0]);
      item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[1]);
      item.setFillColor(AbstractLayerItem.ItemState.NORMAL, COLOR[2]);
      item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[3]);
      item.setStroked(true);
      item.setFilled(true);
      item.setVisible(isVisible());
    }
  }
}
