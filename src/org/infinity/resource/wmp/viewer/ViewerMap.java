// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp.viewer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.IsNumeric;
import org.infinity.gui.RenderCanvas;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.ViewerUtil.StructListPanel;
import org.infinity.gui.WindowBlocker;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.MosDecoder;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.wmp.AreaEntry;
import org.infinity.resource.wmp.MapEntry;
import org.infinity.util.Logger;
import org.infinity.util.Misc;
import org.infinity.util.SimpleListModel;
import org.infinity.util.StringTable;
import org.infinity.util.io.StreamUtils;

/**
 * Provides a visual representation of a single WMP map instance.
 */
public class ViewerMap extends JPanel {
  /** Needed to determine map edges to travel from/to. */
  public enum Direction {
    NORTH, WEST, SOUTH, EAST
  }

  private static final EnumMap<Direction, Color> TRAVEL_COLORS = new EnumMap<>(Direction.class);

  static {
    TRAVEL_COLORS.put(Direction.NORTH, Color.GREEN);
    TRAVEL_COLORS.put(Direction.WEST, Color.RED);
    TRAVEL_COLORS.put(Direction.SOUTH, Color.CYAN);
    TRAVEL_COLORS.put(Direction.EAST, Color.YELLOW);
  }

  private final JPopupMenu pmOptions = new JPopupMenu("Options");
  private final JMenuItem miExportMap = new JMenuItem("Export as PNG...");
  private final JCheckBoxMenuItem miShowIcons = new JCheckBoxMenuItem("Show all map icons", true);
  private final JCheckBoxMenuItem miShowIconLabels = new JCheckBoxMenuItem("Show icons labels", true);
  private final JCheckBoxMenuItem miShowDistances = new JCheckBoxMenuItem("Show travel distances", false);
  private final JCheckBoxMenuItem miScaling = new JCheckBoxMenuItem("Scale map icons",
      Profile.getGame() == Profile.Game.PSTEE);
  private final BufferedImage iconDot;
  private final Listeners listeners = new Listeners();
  private final WmpMapInfo mapInfo;

  private RenderCanvas rcMap;
  private BufferedImage mapOrig;
  private float mapScaleX, mapScaleY;
  private StructListPanel listPanel;
  private BufferedImage dotBackup;
  private int dotX, dotY;
  private JLabel lInfoSize, lInfoPos;

  public ViewerMap(MapEntry wmpMap) throws Exception {
    super();
    WindowBlocker.blockWindow(true);
    try {
      mapInfo = new WmpMapInfo(wmpMap);

      // creating marker for selected map icon
      iconDot = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = iconDot.createGraphics();
      try {
        g.setColor(Color.RED);
        g.fillRect(0, 0, iconDot.getWidth(), iconDot.getHeight());
        g.setColor(Color.YELLOW);
        g.drawRect(0, 0, iconDot.getWidth() - 1, iconDot.getHeight() - 1);
      } finally {
        g.dispose();
        g = null;
      }

      dotBackup = new BufferedImage(iconDot.getWidth(), iconDot.getHeight(), iconDot.getType());
      dotX = dotY = -1;

      miExportMap.setMnemonic('x');
      miExportMap.addActionListener(listeners);
      miShowIcons.setMnemonic('i');
      miShowIcons.addActionListener(listeners);
      miShowIconLabels.setMnemonic('l');
      miShowIconLabels.addActionListener(listeners);
      miShowDistances.setMnemonic('d');
      miShowDistances.addActionListener(listeners);
      miScaling.setMnemonic('s');
      miScaling.addActionListener(listeners);
      miScaling.setToolTipText(
          "Scales map icon locations according to the worldmap's scaling factor. (Needed for some games)");
      pmOptions.add(miExportMap);
      pmOptions.addSeparator();
      pmOptions.add(miShowIcons);
      pmOptions.add(miShowIconLabels);
      pmOptions.add(miShowDistances);
      pmOptions.add(miScaling);

      try {
        mapOrig = loadMap();

        int mapTargetWidth = mapInfo.getWidth();
        if (mapTargetWidth <= 0) {
          mapTargetWidth = (mapOrig != null) ? mapOrig.getWidth() : 640;
        }
        int mapTargetHeight = mapInfo.getHeight();
        if (mapTargetHeight <= 0) {
          mapTargetHeight = (mapOrig != null) ? mapOrig.getHeight() : 480;
        }

        if (mapOrig == null) {
          mapOrig = ColorConvert.createCompatibleImage(mapTargetWidth, mapTargetHeight, false);
        }

        rcMap = new RenderCanvas(ColorConvert.cloneImage(mapOrig));
        rcMap.addMouseListener(listeners);
        rcMap.addMouseMotionListener(listeners);

        final JScrollPane mapScroll = new JScrollPane(rcMap);
        mapScroll.getVerticalScrollBar().setUnitIncrement(16);
        mapScroll.getHorizontalScrollBar().setUnitIncrement(16);
        mapScroll.setBorder(BorderFactory.createEmptyBorder());

        mapScaleX = (float) mapOrig.getWidth() / (float) mapTargetWidth;
        mapScaleY = (float) mapOrig.getHeight() / (float) mapTargetHeight;

        listPanel = ViewerUtil.makeListPanel("Areas", wmpMap, AreaEntry.class, AreaEntry.WMP_AREA_CURRENT,
            new AreaListCellRenderer(mapInfo.getMapIcons()), listeners);
        for (final StructEntry se : mapInfo.getVirtualAreas()) {
          listPanel.getListModel().add(se);
        }
        listPanel.getList().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapScroll, listPanel);
        int viewerWidth = NearInfinity.getInstance().getWidth() - NearInfinity.getInstance().getResourceTree().getWidth();
        split.setDividerLocation(viewerWidth - viewerWidth / 4); // have area list occupy ca. 25% of resource view width
        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);

        JPanel pInfo = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        lInfoSize = new JLabel(String.format("Worldmap size: %d x %d pixels", mapOrig.getWidth(), mapOrig.getHeight()));
        lInfoSize.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        lInfoPos = new JLabel();
        lInfoPos.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        pInfo.add(lInfoSize);
        pInfo.add(lInfoPos);
        add(pInfo, BorderLayout.SOUTH);
      } catch (Throwable t) {
        Logger.error(t);
      }

      // applying preselected overlays
      showOverlays(miShowIcons.isSelected(), miShowIconLabels.isSelected(), miShowDistances.isSelected());
    } finally {
      WindowBlocker.blockWindow(false);
    }
  }

  /** Returns current map entry structure. */
  private MapEntry getEntry() {
    return mapInfo.getMapEntry();
  }

  /** Load and return map graphics. */
  private BufferedImage loadMap() {
    if (mapInfo.getBackgroundResource() != null) {
      MosDecoder mos = MosDecoder.loadMos(mapInfo.getBackgroundResource());
      if (mos != null) {
        return (BufferedImage) mos.getImage();
      }
    }
    return null;
  }

  /** Show popup menu. */
  private void showPopup(Component invoker, int x, int y) {
    pmOptions.show(invoker, x, y);
  }

  /** Display either or both map icons and travel distances. */
  private void showOverlays(boolean showIcons, boolean showIconLabels, boolean showDistances) {
    resetMap();
    if (showIcons) {
      showMapIcons();
      if (showIconLabels) {
        showMapIconLabels();
      }
      if (showDistances) {
        showMapDistances(listPanel.getList().getSelectedIndices());
      }
    }
    showDot(listPanel.getList().getSelectedValue(), false);
    rcMap.repaint();
  }

  /** Draws map icons onto the map. */
  private void showMapIcons() {
    if (mapInfo.getMapIcons() != null) {
      Graphics2D g = ((BufferedImage) rcMap.getImage()).createGraphics();
      try {
        for (int i = 0, count = listPanel.getListModel().size(); i < count; i++) {
          int iconIndex = -1;
          Point p = null;

          final WmpAreaInfo wai = getAreaInfo(i, true);
          if (wai != null) {
            iconIndex = wai.getIconIndex();
            p = getAreaEntryPosition(wai);
          }

          drawMapIcon(iconIndex, p, g);
        }
      } finally {
        g.dispose();
        g = null;
      }
    }
  }

  /**
   * Draws a single map icon onto the map.
   *
   * @param iconIndex Index of the map icon.
   * @param coords    Map icon coordinates.
   * @param g         Graphics context to draw the icon onto.
   */
  private void drawMapIcon(int iconIndex, Point coords, Graphics2D g) {
    if (iconIndex < 0 || coords == null || g == null) {
      return;
    }

    final int frameIndex = mapInfo.getMapIconsControl().cycleGetFrameIndexAbsolute(iconIndex, 0);
    if (frameIndex >= 0) {
      final Image mapIcon = mapInfo.getMapIcons().frameGet(mapInfo.getMapIconsControl(), frameIndex);
      int width = mapInfo.getMapIcons().getFrameInfo(frameIndex).getWidth();
      int height = mapInfo.getMapIcons().getFrameInfo(frameIndex).getHeight();
      int cx = mapInfo.getMapIcons().getFrameInfo(frameIndex).getCenterX();
      int cy = mapInfo.getMapIcons().getFrameInfo(frameIndex).getCenterY();
      coords.x -= cx;
      coords.y -= cy;
      g.drawImage(mapIcon, coords.x, coords.y, coords.x + width, coords.y + height, 0, 0, width, height, null);
    }
  }

  /** Draws map icon labels onto the map. */
  private void showMapIconLabels() {
    if (mapInfo.getMapIcons() != null) {
      Graphics2D g = ((BufferedImage) rcMap.getImage()).createGraphics();
      try {
        g.setFont(Misc.getScaledFont(g.getFont()));
        g.setFont(g.getFont().deriveFont(g.getFont().getSize2D() * 0.9f));
        for (int i = 0, count = listPanel.getListModel().size(); i < count; i++) {
          int iconIndex = -1;
          int strref = -1;
          String mapCode = null;
          Point p = null;

          final WmpAreaInfo wai = getAreaInfo(i, true);
          if (wai != null) {
            iconIndex = wai.getIconIndex();
            strref = wai.getAreaNameStrref();
            if (strref < 0) {
              strref = wai.getAreaTooltipStrref();
            }
            if (wai.getCurrentArea() != null) {
              mapCode = wai.getCurrentArea().getResourceName();
            }
            p = getAreaEntryPosition(wai);
          }

          drawMapIconLabel(iconIndex, strref, mapCode, p, g);
        }
      } finally {
        g.dispose();
        g = null;
      }
    }
  }

  /**
   * Draws a single map icon label onto the map.
   *
   * @param iconIndex     Index of the map icon.
   * @param mapNameStrref Strref of the worldmap location.
   * @param mapCode       Area code of the worldmap location.
   * @param coords        Map icon coordinates.
   * @param g             Graphics context to draw the label onto.
   */
  private void drawMapIconLabel(int iconIndex, int mapNameStrref, String mapCode, Point coords, Graphics2D g) {
    if (iconIndex < 0 || coords == null || g == null) {
      return;
    }

    int frameIndex = mapInfo.getMapIconsControl().cycleGetFrameIndexAbsolute(iconIndex, 0);
    if (frameIndex < 0) {
      return;
    }

    String mapName = (mapNameStrref >= 0) ? StringTable.getStringRef(mapNameStrref) : null;
    if (mapName != null && mapName.trim().isEmpty()) {
      mapName = null;
    }

    if (ResourceFactory.resourceExists(mapCode)) {
      mapCode = mapCode.replace(".ARE", "");
    } else {
      mapCode = "";
    }

    int width = mapInfo.getMapIcons().getFrameInfo(frameIndex).getWidth();
    int height = mapInfo.getMapIcons().getFrameInfo(frameIndex).getHeight();
    int cx = mapInfo.getMapIcons().getFrameInfo(frameIndex).getCenterX();
    int cy = mapInfo.getMapIcons().getFrameInfo(frameIndex).getCenterY();
    coords.x -= cx;
    coords.y -= cy;

    // printing label
    if (!mapCode.isEmpty()) {
      String[] labels;
      if (mapName != null) {
        labels = new String[2];
        labels[0] = mapName;
        labels[1] = "(" + mapCode + ")";
      } else {
        labels = new String[1];
        labels[0] = mapCode;
      }

      final Color whiteBlended = new Color(0xc0ffffff, true);
      int ofsY = 0;
      for (final String label : labels) {
        Rectangle2D rectText = g.getFont().getStringBounds(label, g.getFontRenderContext());
        int boxWidth = rectText.getBounds().width;
        int boxHeight = rectText.getBounds().height;
        int textX = coords.x + (width - boxWidth) / 2;
        int textY = coords.y + height + ofsY;

        g.setColor(whiteBlended);
        g.fillRect(textX - 2, textY, boxWidth + 4, boxHeight);

        g.setColor(Color.BLACK);
        LineMetrics lm = g.getFont().getLineMetrics(label, g.getFontRenderContext());
        g.drawString(label, textX, textY + lm.getAscent() + lm.getLeading());
        ofsY += boxHeight;
      }
    }

  }

  /**
   * Displays all map distances from the specified area (by index).
   *
   * @param areaIndices Sequence of map indices for showing distances. Specify no parameters to show distances for all
   *                    available maps.
   */
  private void showMapDistances(int[] areaIndices) {
    final List<Integer> areaIndicesList = new ArrayList<>();
    if (areaIndices.length == 0) {
      for (int i = 0, count = mapInfo.getAreaList().size(); i < count; i++) {
        areaIndicesList.add(i);
      }
    } else {
      for (int i = 0; i < areaIndices.length; i++) {
        final WmpAreaInfo wai = getAreaInfo(areaIndices[i], true);
        if (wai != null) {
          final int areaIndex = wai.getAreaIndex();
          areaIndicesList.add(areaIndex);
        }
      }
    }

    Graphics2D g = ((BufferedImage)rcMap.getImage()).createGraphics();
    try {
      g.setFont(g.getFont().deriveFont(g.getFont().getSize2D() * 0.8f));
      for (final int curAreaIndex : areaIndicesList) {
        final WmpAreaInfo srcAreaInfo = getAreaInfo(curAreaIndex, false);
        if (srcAreaInfo != null) {
          for (final WmpLinkInfo wli : srcAreaInfo.getLinksList()) {
            final Point ptOrigin = getMapIconCoordinate(curAreaIndex, wli.getDirection(), false);
            final int targetAreaIndex = wli.getTargetAreaIndex();
            if (targetAreaIndex < 0 || targetAreaIndex >= mapInfo.getAreaList().size()) {
              Logger.warn("Invalid target area link: {}", targetAreaIndex);
              continue;
            }
            final WmpAreaInfo dstAreaInfo = mapInfo.getAreaList().get(wli.getTargetAreaIndex());
            final int dstAreaIndex = dstAreaInfo.getAreaIndex();
            if (areaIndicesList.size() > 1 && areaIndices.length != 0) {
              // finding corresponding travel distances between selected areas
              boolean found = false;
              for (final int idx : areaIndicesList) {
                if (idx == dstAreaIndex) {
                  found = true;
                  break;
                }
              }
              if (!found) {
                continue;
              }
            }

            final Direction dstDir;
            switch (wli.getDefaultEntrance()) {
              case 2:
                dstDir = Direction.EAST;
                break;
              case 4:
                dstDir = Direction.SOUTH;
                break;
              case 8:
                dstDir = Direction.WEST;
                break;
              default:
                dstDir = Direction.NORTH;
                break;
            }
            final Point ptTarget = getMapIconCoordinate(dstAreaIndex, dstDir, false);

            // checking random encounters
            int rndEncProb = (wli.getRandomEncounterProbability() > 0 && wli.getRandomEncounterAreaCount() != 0)
                ? wli.getRandomEncounterProbability()
                : 0;

            drawMapDistance(ptOrigin, ptTarget, wli.getDistanceScale() * 4, rndEncProb,
                TRAVEL_COLORS.get(wli.getDirection()), g);
          }
        }
      }
    } finally {
      g.dispose();
      g = null;
    }
  }

  /**
   * Visualizes a single travel distance.
   *
   * @param origin     Coordinate of the starting location.
   * @param target     Coordinate of the target location.
   * @param distance   Distance, in hours.
   * @param rndEncProb Probability of random encounters (0 = no encounters).
   * @param color      Color of the visualized travel route.
   * @param g          Graphics context to draw the travel distance onto.
   */
  private void drawMapDistance(Point origin, Point target, int distance, int rndEncProb, Color color, Graphics2D g) {
    if (origin == null || target == null || color == null || g == null) {
      return;
    }

    // drawing line
    g.setColor(color);
    if (rndEncProb > 0) {
      g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f,
          new float[] { 6.0f, 4.0f }, 0.0f));
    } else {
      g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
    }
    g.drawLine(origin.x, origin.y, target.x, target.y);

    // printing travel time (in hours)
    final String duration;
    if (rndEncProb > 0) {
      duration = String.format("%d h (%d%%)", distance, rndEncProb);
    } else {
      duration = String.format("%d h", distance);
    }
    final LineMetrics lm = g.getFont().getLineMetrics(duration, g.getFontRenderContext());
    final Rectangle2D rectText = g.getFont().getStringBounds(duration, g.getFontRenderContext());
    int textX = origin.x + ((target.x - origin.x) - rectText.getBounds().width) / 3;
    int textY = origin.y + ((target.y - origin.y) - rectText.getBounds().height) / 3;
    int textWidth = rectText.getBounds().width;
    int textHeight = rectText.getBounds().height;
    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(textX - 2, textY, textWidth + 4, textHeight);
    g.setColor(Color.BLUE);
    g.drawString(duration, textX, textY + lm.getAscent() + lm.getLeading());
  }

  /** Returns a properly scaled map icon position. */
  private Point getAreaEntryPosition(WmpAreaInfo areaInfo) {
    final Point p = new Point();
    if (areaInfo != null) {
      p.x = (int) (areaInfo.getLocationX() * getScaleFactorX());
      p.y = (int) (areaInfo.getLocationY() * getScaleFactorY());
    }
    return p;
  }

  /** Returns a pixel coordinate for one of the edges of the specified area icon. */
  private Point getMapIconCoordinate(int areaIndex, Direction dir, boolean byPanel) {
    final WmpAreaInfo wai = getAreaInfo(areaIndex, byPanel);
    if (wai != null) {
      final Point p = getAreaEntryPosition(wai);
      int iconIndex = wai.getIconIndex();
      int frameIndex = mapInfo.getMapIconsControl().cycleGetFrameIndexAbsolute(iconIndex, 0);
      int width, height;
      if (frameIndex >= 0) {
        width = mapInfo.getMapIcons().getFrameInfo(frameIndex).getWidth();
        height = mapInfo.getMapIcons().getFrameInfo(frameIndex).getHeight();
        p.x -= mapInfo.getMapIcons().getFrameInfo(frameIndex).getCenterX();
        p.y -= mapInfo.getMapIcons().getFrameInfo(frameIndex).getCenterY();
      } else {
        width = height = 0;
      }
      final Point retVal = new Point();
      switch (dir) {
        case NORTH:
          retVal.x = p.x + (width / 2);
          retVal.y = p.y;
          break;
        case WEST:
          retVal.x = p.x;
          retVal.y = p.y + (height / 2);
          break;
        case SOUTH:
          retVal.x = p.x + (width / 2);
          retVal.y = p.y + height - 1;
          break;
        case EAST:
          retVal.x = p.x + width - 1;
          retVal.y = p.y + (height / 2);
          break;
      }
      return retVal;
    }
    return null;
  }

  /** Returns area info structure of specified item index. */
  private WmpAreaInfo getAreaInfo(int index, boolean byPanel) {
    WmpAreaInfo retVal = null;
    if (byPanel) {
      final SimpleListModel<StructEntry> listModel = listPanel.getListModel();
      if (index >= 0 && index < listModel.size()) {
        final int areaIndex = mapInfo.indexOfArea(listModel.get(index));
        if (areaIndex >= 0) {
          retVal = mapInfo.getAreaList().get(areaIndex);
        }
      }
    } else {
      if (index >= 0 && index < mapInfo.getAreaList().size()) {
        retVal = mapInfo.getAreaList().get(index);
      }
    }
    return retVal;
  }

  /** Show "dot" on specified map icon, optionally restore background graphics. */
  private void showDot(StructEntry entry, boolean restore) {
    if (restore) {
      restoreDot();
    }

    int x = -1;
    int y = -1;
    if (entry instanceof AreaEntry) {
      final AreaEntry areaEntry = (AreaEntry)entry;
      storeDot(areaEntry);
      x = ((IsNumeric) areaEntry.getAttribute(AreaEntry.WMP_AREA_COORDINATE_X)).getValue();
      x = (int) (x * getScaleFactorX());
      y = ((IsNumeric) areaEntry.getAttribute(AreaEntry.WMP_AREA_COORDINATE_Y)).getValue();
      y = (int) (y * getScaleFactorY());
    } else if (entry instanceof VirtualAreaEntry) {
      final VirtualAreaEntry vae = (VirtualAreaEntry)entry;
      x = (int) (vae.getAreaLocationX() * getScaleFactorX());
      y = (int) (vae.getAreaLocationY() * getScaleFactorY());
    }

    if (x >= 0 && y >= 0) {
      int width = iconDot.getWidth();
      int height = iconDot.getHeight();
      int xofs = width / 2;
      int yofs = height / 2;
      Graphics2D g = ((BufferedImage)rcMap.getImage()).createGraphics();
      try {
        g.setComposite(AlphaComposite.Src);
        g.drawImage(iconDot, x - xofs, y - yofs, x - xofs + width, y - yofs + height, 0, 0, width, height, null);
      } finally {
        g.dispose();
        g = null;
      }
    }
  }

  /** Stores background graphics of "dot". */
  private void storeDot(StructEntry entry) {
    int x = -1;
    int y = -1;

    if (entry instanceof AreaEntry) {
      final AreaEntry areaEntry = (AreaEntry)entry;
      x = ((IsNumeric) areaEntry.getAttribute(AreaEntry.WMP_AREA_COORDINATE_X)).getValue();
      x = (int) (x * getScaleFactorX());
      y = ((IsNumeric) areaEntry.getAttribute(AreaEntry.WMP_AREA_COORDINATE_Y)).getValue();
      y = (int) (y * getScaleFactorY());
    } else if (entry instanceof VirtualAreaEntry) {
      final VirtualAreaEntry vae = (VirtualAreaEntry)entry;
      x = (int) (vae.getAreaLocationX() * getScaleFactorX());
      y = (int) (vae.getAreaLocationY() * getScaleFactorY());
    }

    if (x >= 0 && y >= 0) {
      int width = dotBackup.getWidth();
      int height = dotBackup.getHeight();
      int xofs = width / 2;
      int yofs = height / 2;
      Graphics2D g = dotBackup.createGraphics();
      try {
        g.setComposite(AlphaComposite.Src);
        g.drawImage(rcMap.getImage(), 0, 0, width, height, x - xofs, y - yofs, x - xofs + width, y - yofs + height,
            null);
        dotX = x - xofs;
        dotY = y - yofs;
      } finally {
        g.dispose();
        g = null;
      }
    }
  }

  /** Restores background graphics of "dot". */
  private void restoreDot() {
    if (dotX != -1 && dotY != -1) {
      int x = dotX;
      int y = dotY;
      int width = dotBackup.getWidth();
      int height = dotBackup.getHeight();
      Graphics2D g = ((BufferedImage)rcMap.getImage()).createGraphics();
      try {
        g.setComposite(AlphaComposite.Src);
        g.drawImage(dotBackup, x, y, x + width, y + height, 0, 0, width, height, null);
        dotX = -1;
        dotY = -1;
      } finally {
        g.dispose();
        g = null;
      }
    }
  }

  /** Attempts to restore the whole map graphics. */
  private void resetMap() {
    Graphics2D g = ((BufferedImage) rcMap.getImage()).createGraphics();
    try {
      final Composite comp = g.getComposite();
      g.setComposite(AlphaComposite.Src);
      g.drawImage(mapOrig, 0, 0, null);
      g.setComposite(comp);
    } finally {
      g.dispose();
      g = null;
    }
  }

  /** Returns whether to apply map scaling factor. */
  private boolean isScaling() {
    return miScaling.isSelected();
  }

  /** Returns the current map scaling factor for X axis. */
  private float getScaleFactorX() {
    return isScaling() ? mapScaleX : 1.0f;
  }

  /** Returns the current map scaling factor for Y axis. */
  private float getScaleFactorY() {
    return isScaling() ? mapScaleY : 1.0f;
  }

  /** Shows specified coordinates as text info. Hides display for negative coordinates. */
  private void updateCursorInfo(int x, int y) {
    if (lInfoPos != null) {
      if (x >= 0 && y >= 0) {
        lInfoPos.setText(String.format("Cursor at (%d, %d)", x, y));
      } else {
        lInfoPos.setText("");
      }
    }
  }

  /** Exports current map to PNG. */
  private void exportMap() {
    final Window wnd = SwingUtilities.getWindowAncestor(this);
    WindowBlocker.blockWindow(wnd, true);

    SwingUtilities.invokeLater(() -> {
      try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
        boolean bRet = false;
        try {
          restoreDot();
          BufferedImage srcImage = (BufferedImage) rcMap.getImage();
          BufferedImage dstImage = ColorConvert.createCompatibleImage(srcImage.getWidth(), srcImage.getHeight(),
              srcImage.getTransparency());
          Graphics2D g = dstImage.createGraphics();
          g.drawImage(srcImage, 0, 0, null);
          g.dispose();
          srcImage = null;
          bRet = ImageIO.write(dstImage, "png", os);
          dstImage.flush();
          dstImage = null;
        } catch (Exception e) {
          Logger.error(e);
        } finally {
          showDot(listPanel.getList().getSelectedValue(), false);
          WindowBlocker.blockWindow(wnd, false);
        }
        if (bRet) {
          final ResourceEntry entry = getEntry().getParent().getResourceEntry();
          final String fileName = StreamUtils.replaceFileExtension(entry.getResourceName(), "PNG");
          ResourceFactory.exportResource(entry, StreamUtils.getByteBuffer(os.toByteArray()), fileName, wnd);
        } else {
          JOptionPane.showMessageDialog(wnd, "Error while exporting map as graphics.", "Error",
              JOptionPane.ERROR_MESSAGE);
        }
      } catch (IOException e) {
        Logger.error(e);
      }
    });
  }

  // -------------------------- INNER CLASSES --------------------------

  private class Listeners implements ActionListener, MouseListener, MouseMotionListener, ListSelectionListener {
    public Listeners() {
    }

    // --------------------- Begin Interface ActionListener ---------------------

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == miShowIcons) {
        try {
          WindowBlocker.blockWindow(true);
          if (!miShowIcons.isSelected() && miShowDistances.isSelected()) {
            miShowDistances.setSelected(false);
          }
          showOverlays(miShowIcons.isSelected(), miShowIconLabels.isSelected(), miShowDistances.isSelected());
        } finally {
          WindowBlocker.blockWindow(false);
        }
      } else if (e.getSource() == miShowIconLabels) {
        WindowBlocker.blockWindow(true);
        try {
          if (miShowIconLabels.isSelected() && !miShowIcons.isSelected()) {
            miShowIcons.setSelected(true);
          }
          showOverlays(miShowIcons.isSelected(), miShowIconLabels.isSelected(), miShowDistances.isSelected());
        } finally {
          WindowBlocker.blockWindow(false);
        }
      } else if (e.getSource() == miShowDistances) {
        try {
          WindowBlocker.blockWindow(true);
          if (miShowDistances.isSelected() && !miShowIcons.isSelected()) {
            miShowIcons.setSelected(true);
          }
          showOverlays(miShowIcons.isSelected(), miShowIconLabels.isSelected(), miShowDistances.isSelected());
        } finally {
          WindowBlocker.blockWindow(false);
        }
      } else if (e.getSource() == miScaling) {
        try {
          WindowBlocker.blockWindow(true);
          if (miShowIcons.isSelected()) {
            showOverlays(miShowIcons.isSelected(), miShowIconLabels.isSelected(), miShowDistances.isSelected());
          }
        } finally {
          WindowBlocker.blockWindow(false);
        }
      } else if (e.getSource() == miExportMap) {
        exportMap();
      }
    }

    // --------------------- End Interface ActionListener ---------------------

    // --------------------- Begin Interface MouseListener ---------------------

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (e.isPopupTrigger() && e.getComponent() == rcMap) {
        showPopup(e.getComponent(), e.getX(), e.getY());
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (e.isPopupTrigger() && e.getComponent() == rcMap) {
        showPopup(e.getComponent(), e.getX(), e.getY());
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    // --------------------- End Interface MouseListener ---------------------

    // --------------------- Begin Interface MouseMotionListener ---------------------

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      if (e.getSource() == rcMap) {
        int ctrlWidth = rcMap.getWidth();
        int ctrlHeight = rcMap.getHeight();
        Image image = rcMap.getImage();
        int imgWidth = image.getWidth(null);
        int imgHeight = image.getHeight(null);
        int startX = (ctrlWidth - imgWidth) / 2;
        int startY = (ctrlHeight - imgHeight) / 2;
        int x = e.getX();
        int y = e.getY();
        if (x >= startX && x < startX + imgWidth && y >= startY && y < startY + imgHeight) {
          updateCursorInfo(x - startX, y - startY);
        } else {
          updateCursorInfo(-1, -1);
        }
      }
    }

    // --------------------- End Interface MouseMotionListener ---------------------

    // --------------------- Begin Interface ListSelectionListener ---------------------

    @Override
    public void valueChanged(ListSelectionEvent event) {
      if (!event.getValueIsAdjusting()) {
        JList<?> list = (JList<?>) event.getSource();
        if (miShowDistances.isSelected()) {
          showOverlays(miShowIcons.isSelected(), miShowIconLabels.isSelected(), miShowDistances.isSelected());
        } else {
          showDot((StructEntry)list.getSelectedValue(), true);
        }
        repaint();
      }
    }

    // --------------------- End Interface ListSelectionListener ---------------------
  }
}
