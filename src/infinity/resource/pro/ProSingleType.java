// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.pro;

import infinity.datatype.ColorValue;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HashBitmap;
import infinity.datatype.IdsBitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.util.LongIntegerHashMap;

public final class ProSingleType extends AbstractStruct implements AddRemovable
{
  // PRO/Single-specific field labels
  public static final String PRO_SINGLE                               = "Projectile info";
  public static final String PRO_SINGLE_FLAGS                         = "Flags";
  public static final String PRO_SINGLE_PRO_ANIMATION                 = "Projectile animation";
  public static final String PRO_SINGLE_SHADOW_ANIMATION              = "Shadow animation";
  public static final String PRO_SINGLE_PRO_ANIMATION_INDEX           = "Projectile animation number";
  public static final String PRO_SINGLE_SHADOW_ANIMATION_INDEX        = "Shadow animation number";
  public static final String PRO_SINGLE_LIGHT_SPOT_INTENSITY          = "Light spot intensity";
  public static final String PRO_SINGLE_LIGHT_SPOT_WIDTH              = "Light spot width";
  public static final String PRO_SINGLE_LIGHT_SPOT_HEIGHT             = "Light spot height";
  public static final String PRO_SINGLE_PALETTE                       = "Palette";
  public static final String PRO_SINGLE_PRO_COLOR_FMT                 = "Projectile color %d";
  public static final String PRO_SINGLE_SMOKE_PUFF_DELAY              = "Smoke puff delay";
  public static final String PRO_SINGLE_SMOKE_COLOR_FMT               = "Smoke color %d";
  public static final String PRO_SINGLE_FACE_TARGET_GRANULARITY       = "Face target granularity";
  public static final String PRO_SINGLE_SMOKE_ANIMATION               = "Smoke animation";
  public static final String PRO_SINGLE_TRAILING_ANIMATION_FMT        = "Trailing animation %d";
  public static final String PRO_SINGLE_TRAILING_ANIMATION_DELAY_FMT  = "Trailing animation delay %d";
  public static final String PRO_SINGLE_TRAIL_FLAGS                   = "Trail flags";

  public static final LongIntegerHashMap<String> m_facetarget = new LongIntegerHashMap<String>();
  public static final String[] s_flags = {"No flags set", "Colored BAM", "Creates smoke", "Colored smoke",
                                          "Not light source", "Modify for height", "Casts shadow",
                                          "Light spot enabled", "Translucent", "Mid-level brighten", "Blended"};
  public static final String[] s_trail = {"No flags set", "Draw at target", "Draw at source"};

  static {
    m_facetarget.put(new Long(1), "Do not face target");
    m_facetarget.put(new Long(5), "Mirrored east (reduced)");
    m_facetarget.put(new Long(9), "Mirrored east (full)");
    m_facetarget.put(new Long(16), "Not mirrored (full)");
  }


  public ProSingleType() throws Exception
  {
    super(null, PRO_SINGLE, new byte[256], 0);
    setOffset(256);
  }

  public ProSingleType(AbstractStruct superStruct, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, PRO_SINGLE, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return false;   // can not be removed manually
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte[] buffer, int offset) throws Exception
  {
    addField(new Flag(buffer, offset, 4, PRO_SINGLE_FLAGS, s_flags));
    addField(new ResourceRef(buffer, offset + 4, PRO_SINGLE_PRO_ANIMATION, "BAM"));
    addField(new ResourceRef(buffer, offset + 12, PRO_SINGLE_SHADOW_ANIMATION, "BAM"));
    addField(new DecNumber(buffer, offset + 20, 1, PRO_SINGLE_PRO_ANIMATION_INDEX));
    addField(new DecNumber(buffer, offset + 21, 1, PRO_SINGLE_SHADOW_ANIMATION_INDEX));
    addField(new DecNumber(buffer, offset + 22, 2, PRO_SINGLE_LIGHT_SPOT_INTENSITY));
    addField(new DecNumber(buffer, offset + 24, 2, PRO_SINGLE_LIGHT_SPOT_WIDTH));
    addField(new DecNumber(buffer, offset + 26, 2, PRO_SINGLE_LIGHT_SPOT_HEIGHT));
    addField(new ResourceRef(buffer, offset + 28, PRO_SINGLE_PALETTE, "BMP"));
    for (int i = 0; i < 7; i++) {
      addField(new ColorValue(buffer, offset + 36 + i, 1, String.format(PRO_SINGLE_PRO_COLOR_FMT, i+1)));
    }
    addField(new DecNumber(buffer, offset + 43, 1, PRO_SINGLE_SMOKE_PUFF_DELAY));
    for (int i = 0; i < 7; i++) {
      addField(new ColorValue(buffer, offset + 44 + i, 1, String.format(PRO_SINGLE_SMOKE_COLOR_FMT, i+1)));
    }
    addField(new HashBitmap(buffer, offset + 51, 1, PRO_SINGLE_FACE_TARGET_GRANULARITY, m_facetarget));
    addField(new IdsBitmap(buffer, offset + 52, 2, PRO_SINGLE_SMOKE_ANIMATION, "ANIMATE.IDS"));
    for (int i = 0; i < 3; i++) {
      addField(new ResourceRef(buffer, offset + 54 + (i * 8),
                               String.format(PRO_SINGLE_TRAILING_ANIMATION_FMT, i+1), "BAM"));
    }
    for (int i = 0; i < 3; i++) {
      addField(new DecNumber(buffer, offset + 78 + (i * 2), 2,
                             String.format(PRO_SINGLE_TRAILING_ANIMATION_DELAY_FMT, i+1)));
    }
    addField(new Flag(buffer, offset + 84, 4, PRO_SINGLE_TRAIL_FLAGS, s_trail));
    addField(new Unknown(buffer, offset + 88, 168));

    return offset + 256;
  }
}
