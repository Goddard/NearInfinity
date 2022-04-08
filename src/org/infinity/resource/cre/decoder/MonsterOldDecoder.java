// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.tables.SpriteTables;
import org.infinity.resource.cre.decoder.util.AnimationInfo;
import org.infinity.resource.cre.decoder.util.DirDef;
import org.infinity.resource.cre.decoder.util.SegmentDef;
import org.infinity.resource.cre.decoder.util.SeqDef;
import org.infinity.resource.cre.decoder.util.Sequence;
import org.infinity.resource.cre.decoder.util.SpriteUtils;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;
import org.infinity.util.tuples.Couple;

/**
 * Creature animation decoder for processing type 7000 (monster_old) animations. Available ranges: (using notation
 * slot/range where slot can be a formula with range definitions as [x,y])
 *
 * <pre>
 * (0x7000 | ([0x00,0x1f] << 4))/0x1
 * (0x7000 | ([0x20,0x2f] << 4))/0x3
 * (0x7000 | ([0x40,0x4f] << 4))/0x2
 * (0x7000 | ([0x50,0x5f] << 4))/0x1
 * (0x7000 | ([0x60,0x6f] << 4))/0xf
 * (0x7000 | ([0x70,0x7f] << 4))/0x2
 * (0x7000 | ([0x80,0x8f] << 4))/0xf
 * (0x7000 | ([0x90,0xaf] << 4))/0x4
 * (0x7000 | ([0xb0,0xbf] << 4))/0x6
 * (0x7000 | ([0xc0,0xcf] << 4))/0x1
 * (0x7000 | ([0xd0,0xdf] << 4))/0xf
 * (0x7000 | ([0xe0,0xef] << 4))/0x1
 * </pre>
 */
public class MonsterOldDecoder extends SpriteDecoder {
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.MONSTER_OLD;

  private static final HashMap<Sequence, Couple<String, Integer>> SUFFIX_MAP = new HashMap<Sequence, Couple<String, Integer>>();

  static {
    SUFFIX_MAP.put(Sequence.WALK, Couple.with("G1", 0));
    SUFFIX_MAP.put(Sequence.STANCE, Couple.with("G1", 8));
    SUFFIX_MAP.put(Sequence.STAND, Couple.with("G1", 16));
    SUFFIX_MAP.put(Sequence.GET_HIT, Couple.with("G1", 24));
    SUFFIX_MAP.put(Sequence.DIE, Couple.with("G1", 32));
    SUFFIX_MAP.put(Sequence.SLEEP, SUFFIX_MAP.get(Sequence.DIE));
    SUFFIX_MAP.put(Sequence.GET_UP, Couple.with("!G1", 32));
    SUFFIX_MAP.put(Sequence.TWITCH, Couple.with("G1", 40));
    SUFFIX_MAP.put(Sequence.ATTACK, Couple.with("G2", 0));
    SUFFIX_MAP.put(Sequence.ATTACK_2, Couple.with("G2", 8));
    SUFFIX_MAP.put(Sequence.ATTACK_3, Couple.with("G2", 16));
  }

  /**
   * A helper method that parses the specified data array and generates a {@link IniMap} instance out of it.
   *
   * @param data a String array containing table values for a specific table entry.
   * @return a {@code IniMap} instance with the value derived from the specified data array. Returns {@code null} if no
   *         data could be derived.
   */
  public static IniMap processTableData(String[] data) {
    IniMap retVal = null;
    if (data == null || data.length < 16) {
      return retVal;
    }

    String resref = SpriteTables.valueToString(data, SpriteTables.COLUMN_RESREF, "");
    if (resref.isEmpty()) {
      return retVal;
    }
    int falseColor = SpriteTables.valueToInt(data, SpriteTables.COLUMN_CLOWN, 0);
    int translucent = SpriteTables.valueToInt(data, SpriteTables.COLUMN_TRANSLUCENT, 0);

    List<String> lines = SpriteUtils.processTableDataGeneral(data, ANIMATION_TYPE);
    lines.add("[monster_old]");
    lines.add("false_color=" + falseColor);
    lines.add("translucent=" + translucent);
    lines.add("resref=" + resref);

    retVal = IniMap.from(lines);

    return retVal;
  }

  public MonsterOldDecoder(int animationId, IniMap ini) throws Exception {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public MonsterOldDecoder(CreResource cre) throws Exception {
    super(ANIMATION_TYPE, cre);
  }

  @Override
  public List<String> getAnimationFiles(boolean essential) {
    String resref = getAnimationResref();
    ArrayList<String> retVal = new ArrayList<String>() {
      {
        add(resref + "G1.BAM");
        add(resref + "G1E.BAM");
        add(resref + "G2.BAM");
        add(resref + "G2E.BAM");
      }
    };
    return retVal;
  }

  @Override
  public boolean isSequenceAvailable(Sequence seq) {
    return (getSequenceDefinition(seq) != null);
  }

  @Override
  protected void init() throws Exception {
    // setting properties
    initDefaults(getAnimationInfo());
    IniMapSection section = getSpecificIniSection();
    setFalseColor(section.getAsInteger(KEY_FALSE_COLOR.getName(), 0) != 0);
    setDetectedByInfravision(section.getAsInteger(KEY_DETECTED_BY_INFRAVISION.getName(), 0) != 0);
    setTranslucent(section.getAsInteger(KEY_TRANSLUCENT.getName(), 0) != 0);
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq) {
    SeqDef retVal = null;
    String resref = getAnimationResref();
    Couple<String, Integer> data = SUFFIX_MAP.get(seq);
    if (data != null) {
      SegmentDef.Behavior behavior = SegmentDef.getBehaviorOf(data.getValue0());
      String suffix = SegmentDef.fixBehaviorSuffix(data.getValue0());
      ResourceEntry entry = ResourceFactory.getResourceEntry(resref + suffix + ".BAM");
      int cycle = data.getValue1();
      ResourceEntry entryE = ResourceFactory.getResourceEntry(resref + suffix + "E.BAM");
      int cycleE = cycle + SeqDef.DIR_REDUCED_W.length;
      if (SpriteUtils.bamCyclesExist(entry, cycle, SeqDef.DIR_REDUCED_W.length)
          && SpriteUtils.bamCyclesExist(entryE, cycleE, SeqDef.DIR_REDUCED_E.length)) {
        retVal = SeqDef.createSequence(seq, SeqDef.DIR_REDUCED_W, false, entry, cycle, null, behavior);
        SeqDef tmp = SeqDef.createSequence(seq, SeqDef.DIR_REDUCED_E, false, entryE, cycleE, null, behavior);
        retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
      }
    }
    return retVal;
  }
}
