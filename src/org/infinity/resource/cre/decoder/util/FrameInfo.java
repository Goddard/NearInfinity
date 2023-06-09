// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.Stroke;
import java.util.EnumMap;
import java.util.Objects;

import org.infinity.resource.graphics.BamV1Decoder.BamV1Control;

/**
 * A structure that fully describes the location of a single BAM frame.
 */
public class FrameInfo {
  /** Predefined stroke instance for drawing bounding boxes around creature sprite elements. */
  public static final Stroke STROKE_BOUNDING_BOX = new BasicStroke(1.0f);

  /** Predefined colors for the bounding box around creature sprite elements. */
  public static final EnumMap<SegmentDef.SpriteType, Color> SPRITE_COLOR = new EnumMap<SegmentDef.SpriteType, Color>(
      SegmentDef.SpriteType.class);

  static {
    SPRITE_COLOR.put(SegmentDef.SpriteType.AVATAR, new Color(0x800000ff, true)); // blue
    SPRITE_COLOR.put(SegmentDef.SpriteType.WEAPON, new Color(0x80ff0000, true)); // red
    SPRITE_COLOR.put(SegmentDef.SpriteType.SHIELD, new Color(0x8000ff00, true)); // green
    SPRITE_COLOR.put(SegmentDef.SpriteType.HELMET, new Color(0x8000ffff, true)); // cyan
  }

  /** Color definition as fallback solution. */
  public static final Color SPRITE_COLOR_DEFAULT = new Color(0x80808080, true); // gray

  private final BamV1Control bamControl;
  private final SegmentDef segmentDef;
  private final Point centerShift;

  public FrameInfo(BamV1Control bamControl, SegmentDef sd) {
    this(bamControl, sd, null);
  }

  public FrameInfo(BamV1Control bamControl, SegmentDef sd, Point centerShift) {
    this.bamControl = Objects.requireNonNull(bamControl, "BAM controller cannot be null");
    this.segmentDef = Objects.requireNonNull(sd, "Segment definition cannot be null");
    this.centerShift = (centerShift != null) ? new Point(centerShift) : new Point();
  }

  /** Returns the BAM control instance. */
  public BamV1Control getController() {
    return bamControl;
  }

  public SegmentDef getSegmentDefinition() {
    return segmentDef;
  }

  /** Returns the absolute cycle index. */
  public int getCycle() {
    return segmentDef.getCycleIndex();
  }

  /** Returns the frame index relative to the cycle. */
  public int getFrame() {
    return segmentDef.getCurrentFrame();
  }

  /** Returns the amount of pixels the frame center deviates from the original position. */
  public Point getCenterShift() {
    return centerShift;
  }

  @Override
  public String toString() {
    return "cycle=" + getCycle() + ", frame=" + getFrame() + ", centerShift=" + centerShift.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(bamControl, centerShift, segmentDef);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    FrameInfo other = (FrameInfo) obj;
    return Objects.equals(bamControl, other.bamControl) && Objects.equals(centerShift, other.centerShift)
        && Objects.equals(segmentDef, other.segmentDef);
  }
}
