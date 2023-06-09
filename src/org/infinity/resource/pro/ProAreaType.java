// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.pro;

import java.nio.ByteBuffer;
import java.util.TreeMap;

import org.infinity.datatype.AnimateBitmap;
import org.infinity.datatype.ColorValue;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.HashBitmap;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.ProRef;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.Unknown;
import org.infinity.datatype.UnsignDecNumber;
import org.infinity.datatype.UpdateEvent;
import org.infinity.datatype.UpdateListener;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;
import org.infinity.util.io.StreamUtils;

public final class ProAreaType extends AbstractStruct implements AddRemovable, UpdateListener {
  // PRO/Area-specific field labels
  public static final String PRO_AREA                               = "Area effect info";
  public static final String PRO_AREA_FLAGS                         = "Area flags";
  public static final String PRO_AREA_RAY_COUNT                     = "Ray count";
  public static final String PRO_AREA_TRAP_SIZE                     = "Trap size";
  public static final String PRO_AREA_EXPLOSION_SIZE                = "Explosion size";
  public static final String PRO_AREA_EXPLOSION_SOUND               = "Explosion sound";
  public static final String PRO_AREA_EXPLOSION_FREQUENCY           = "Explosion frequency (frames)";
  public static final String PRO_AREA_FRAGMENT_ANIMATION            = "Fragment animation";
  public static final String PRO_AREA_SECONDARY_PROJECTILE          = "Secondary projectile";
  public static final String PRO_AREA_NUM_REPETITIONS               = "# repetitions";
  public static final String PRO_AREA_EXPLOSION_EFFECT              = "Explosion effect";
  public static final String PRO_AREA_EXPLOSION_COLOR               = "Explosion color";
  public static final String PRO_AREA_EXPLOSION_PROJECTILE          = "Explosion projectile";
  public static final String PRO_AREA_EXPLOSION_ANIMATION           = "Explosion animation";
  public static final String PRO_AREA_CONE_WIDTH                    = "Cone width";
  public static final String PRO_AREA_ANGLE_BETWEEN_RAYS            = "Angle between rays";
  public static final String PRO_AREA_ROTATE_RAYS                   = "Rotate rays clockwise";
  public static final String PRO_AREA_SPREAD_ANIMATION              = "Spread animation";
  public static final String PRO_AREA_RING_ANIMATION                = "Ring animation";
  public static final String PRO_AREA_SOUND                         = "Area sound";
  public static final String PRO_AREA_EX_FLAGS                      = "Extended flags";
  public static final String PRO_AREA_DICE_COUNT                    = "# dice for multiple targets";
  public static final String PRO_AREA_DICE_SIZE                     = "Dice size for multiple targets";
  public static final String PRO_AREA_ANIMATION_GRANULARITY         = "Animation granularity";
  public static final String PRO_AREA_ANIMATION_GRANULARITY_DIVIDER = "Animation granularity divider";

  public static final TreeMap<Long, String> PROJECTILES_MAP = new TreeMap<>();

  public static final String[] AREA_FLAGS_ARRAY = { "Trap not visible", "Trap visible", "Triggered by inanimates",
      "Triggered by condition", "Delayed trigger", "Secondary projectile", "Fragments", "Affect only enemies",
      "Affect only allies*;Only in combination with \"Affect only enemies (6)\"", "Mage-level duration",
      "Cleric-level duration", "Draw animation", "Cone-shaped", "Ignore LOS", "Delayed explosion",
      "Skip first condition", "Single target" };

  public static final String[] AREA_FLAGS_EX_ARRAY = { "No flags set", "Paletted ring", "Random speed",
      "Start scattered", "Paletted center", "Repeat scattering", "Paletted animation", null, null, null,
      "Oriented fireball puffs", "Use hit dice lookup", null, null, "Blend are/ring anim", "Glow area/ring anim",
      "Hit point limit", };

  static {
    PROJECTILES_MAP.put(0L, "Fireball");
    PROJECTILES_MAP.put(1L, "Stinking cloud");
    PROJECTILES_MAP.put(2L, "Cloudkill");
    PROJECTILES_MAP.put(3L, "Ice storm");
    PROJECTILES_MAP.put(4L, "Grease");
    PROJECTILES_MAP.put(5L, "Web");
    PROJECTILES_MAP.put(6L, "Meteor");
    PROJECTILES_MAP.put(7L, "Horrid wilting");
    PROJECTILES_MAP.put(8L, "Teleport field");
    PROJECTILES_MAP.put(9L, "Entangle");
    PROJECTILES_MAP.put(10L, "Color spray");
    PROJECTILES_MAP.put(11L, "Cone of cold");
    PROJECTILES_MAP.put(12L, "Holy smite");
    PROJECTILES_MAP.put(13L, "Unholy blight");
    PROJECTILES_MAP.put(14L, "Prismatic spray");
    PROJECTILES_MAP.put(15L, "Red dragon blast");
    PROJECTILES_MAP.put(16L, "Storm of vengeance");
    PROJECTILES_MAP.put(17L, "Purple fireball");
    PROJECTILES_MAP.put(18L, "Green dragon blast");
    PROJECTILES_MAP.put(254L, "Custom");
    PROJECTILES_MAP.put(255L, "None");
  }

  public ProAreaType() throws Exception {
    super(null, PRO_AREA, StreamUtils.getByteBuffer(256), 0);
    setOffset(512);
  }

  public ProAreaType(AbstractStruct superStruct, ByteBuffer buffer, int offset) throws Exception {
    super(superStruct, PRO_AREA, buffer, offset);
  }

  @Override
  public boolean canRemove() {
    return false; // can not be removed manually
  }

  @Override
  public boolean valueUpdated(UpdateEvent event) {
    boolean retVal = false;
    if (event.getSource() instanceof IsNumeric
        && ((StructEntry) event.getSource()).getName().equals(PRO_AREA_RAY_COUNT)) {
      int rayCount = ((IsNumeric) event.getSource()).getValue();
      AbstractStruct struct = event.getStructure();
      if (rayCount > 0) {
        StructEntry field = struct.getField(null, 0x224);
        if (field instanceof DecNumber && !field.getName().equals(PRO_AREA_ANGLE_BETWEEN_RAYS)) {
          field.setName(PRO_AREA_ANGLE_BETWEEN_RAYS);
          retVal = true;
        }
        field = struct.getField(null, 0x226);
        if (field instanceof DecNumber && !field.getName().equals(PRO_AREA_ROTATE_RAYS)) {
          field.setName(PRO_AREA_ROTATE_RAYS);
          retVal = true;
        }
      } else {
        StructEntry field = struct.getField(null, 0x224);
        if (field instanceof DecNumber && !field.getName().equals(PRO_AREA_CONE_WIDTH)) {
          field.setName(PRO_AREA_CONE_WIDTH);
          retVal = true;
        }
        field = struct.getField(null, 0x226);
        if (field instanceof DecNumber && !field.getName().equals(COMMON_UNUSED)) {
          field.setName(COMMON_UNUSED);
          retVal = true;
        }
      }
    }
    return retVal;
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new Flag(buffer, offset, 2, PRO_AREA_FLAGS, AREA_FLAGS_ARRAY));
    DecNumber rayCount = new DecNumber(buffer, offset + 2, 2, PRO_AREA_RAY_COUNT);
    if (Profile.isEnhancedEdition()) {
      rayCount.addUpdateListener(this);
    }
    addField(rayCount);
    addField(new DecNumber(buffer, offset + 4, 2, PRO_AREA_TRAP_SIZE));
    addField(new DecNumber(buffer, offset + 6, 2, PRO_AREA_EXPLOSION_SIZE));
    addField(new ResourceRef(buffer, offset + 8, PRO_AREA_EXPLOSION_SOUND, "WAV"));
    addField(new DecNumber(buffer, offset + 16, 2, PRO_AREA_EXPLOSION_FREQUENCY));
    addField(new AnimateBitmap(buffer, offset + 18, 2, PRO_AREA_FRAGMENT_ANIMATION));
    addField(new ProRef(buffer, offset + 20, PRO_AREA_SECONDARY_PROJECTILE, false));
    addField(new DecNumber(buffer, offset + 22, 1, PRO_AREA_NUM_REPETITIONS));
    addField(new HashBitmap(buffer, offset + 23, 1, PRO_AREA_EXPLOSION_EFFECT, PROJECTILES_MAP));
    addField(new ColorValue(buffer, offset + 24, 1, PRO_AREA_EXPLOSION_COLOR, false));
    addField(new Unknown(buffer, offset + 25, 1, COMMON_UNUSED));
    addField(new ProRef(buffer, offset + 26, PRO_AREA_EXPLOSION_PROJECTILE));
    addField(new ResourceRef(buffer, offset + 28, PRO_AREA_EXPLOSION_ANIMATION, "VEF", "VVC", "BAM"));
    if (Profile.isEnhancedEdition() && rayCount.getValue() > 0) {
      addField(new DecNumber(buffer, offset + 36, 2, PRO_AREA_ANGLE_BETWEEN_RAYS));
    } else {
      addField(new DecNumber(buffer, offset + 36, 2, PRO_AREA_CONE_WIDTH));
    }
    if (Profile.isEnhancedEdition()) {
      if (rayCount.getValue() > 0) {
        addField(new DecNumber(buffer, offset + 38, 2, PRO_AREA_ROTATE_RAYS));
      } else {
        addField(new DecNumber(buffer, offset + 38, 2, COMMON_UNUSED));
      }
      addField(new ResourceRef(buffer, offset + 40, PRO_AREA_SPREAD_ANIMATION, "BAM"));
      addField(new ResourceRef(buffer, offset + 48, PRO_AREA_RING_ANIMATION, "BAM"));
      addField(new ResourceRef(buffer, offset + 56, PRO_AREA_SOUND, "WAV"));
      addField(new Flag(buffer, offset + 64, 4, PRO_AREA_EX_FLAGS, AREA_FLAGS_EX_ARRAY));
      addField(new UnsignDecNumber(buffer, offset + 68, 2, PRO_AREA_DICE_COUNT));
      addField(new UnsignDecNumber(buffer, offset + 70, 2, PRO_AREA_DICE_SIZE));
      addField(new DecNumber(buffer, offset + 72, 2, PRO_AREA_ANIMATION_GRANULARITY));
      addField(new DecNumber(buffer, offset + 74, 2, PRO_AREA_ANIMATION_GRANULARITY_DIVIDER));
      addField(new Unknown(buffer, offset + 76, 180));
    } else {
      addField(new Unknown(buffer, offset + 38, 218));
    }

    return offset + 256;
  }
}
