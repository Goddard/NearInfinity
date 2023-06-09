// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.ColorValue;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.UpdateListener;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

/**
 * Implemention of opcode 342.
 */
public class Opcode342 extends BaseOpcode {
  private static final String EFFECT_FIELD    = "Field";
  private static final String EFFECT_ENABLED  = "Enabled?";

  private static final String[] FIELDS = {"Unknown", "Body heat", "Blood color", "Unknown", "Personal space"};

  /** Returns the opcode name for the current game variant. */
  private static String getOpcodeName() {
    switch (Profile.getEngine()) {
      case EE:
        return "Override creature data";
      default:
        return null;
    }
  }

  public Opcode342() {
    super(342, getOpcodeName());
  }

  @Override
  protected String makeEffectParamsEE(Datatype parent, ByteBuffer buffer, int offset, List<StructEntry> list,
      boolean isVersion1) {
    final Bitmap bmp = new Bitmap(buffer, offset + 4, 4, EFFECT_FIELD, FIELDS);
    switch (bmp.getValue()) {
      case 1:
        list.add(new Bitmap(buffer, offset, 4, EFFECT_ENABLED, AbstractStruct.OPTION_NOYES));
        break;
      case 2:
        list.add(new ColorValue(buffer, offset, 4, EFFECT_COLOR, false));
        break;
      default:
        list.add(new DecNumber(buffer, offset, 4, EFFECT_VALUE));
    }
    list.add(bmp);
    if (parent != null && parent instanceof UpdateListener) {
      bmp.addUpdateListener((UpdateListener)parent);
    }
    return null;
  }

  @Override
  protected boolean update(AbstractStruct struct) throws Exception {
    if (struct != null && Profile.isEnhancedEdition()) {
      int param2 = ((IsNumeric)getEntry(struct, EffectEntry.IDX_PARAM2)).getValue();
      StructEntry newEntry = null;
      switch (param2) {
        case 1:
          newEntry = new Bitmap(getEntryData(struct, EffectEntry.IDX_PARAM1), 0, 4, EFFECT_ENABLED,
              AbstractStruct.OPTION_NOYES);
          break;
        case 2:
          newEntry = new ColorValue(getEntryData(struct, EffectEntry.IDX_PARAM1), 0, 4, EFFECT_COLOR, false);
          break;
        default:
          newEntry = new DecNumber(getEntryData(struct, EffectEntry.IDX_PARAM1), 0, 4, EFFECT_VALUE);
      }
      replaceEntry(struct, EffectEntry.IDX_PARAM1, EffectEntry.OFS_PARAM1, newEntry);
      return true;
    }
    return false;
  }
}
