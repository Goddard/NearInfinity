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
 * Creature animation decoder for processing type 9000 (monster_large) animations. Available ranges: [9000,9fff]
 */
public class MonsterLargeDecoder extends SpriteDecoder {
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.MONSTER_LARGE;

  private static final HashMap<Sequence, Couple<String, Integer>> SUFFIX_MAP = new HashMap<Sequence, Couple<String, Integer>>();

  static {
    SUFFIX_MAP.put(Sequence.STAND, Couple.with("G1", 0));
    SUFFIX_MAP.put(Sequence.STANCE, Couple.with("G1", 8));
    SUFFIX_MAP.put(Sequence.WALK, Couple.with("G1", 16));
    SUFFIX_MAP.put(Sequence.ATTACK, Couple.with("G2", 0));
    SUFFIX_MAP.put(Sequence.ATTACK_2, Couple.with("G2", 8));
    SUFFIX_MAP.put(Sequence.ATTACK_3, Couple.with("G3", 0));
    SUFFIX_MAP.put(Sequence.GET_HIT, Couple.with("G3", 8));
    SUFFIX_MAP.put(Sequence.DIE, Couple.with("G3", 16));
    SUFFIX_MAP.put(Sequence.SLEEP, SUFFIX_MAP.get(Sequence.DIE));
    SUFFIX_MAP.put(Sequence.GET_UP, Couple.with("!G3", 16));
    SUFFIX_MAP.put(Sequence.TWITCH, Couple.with("G3", 24));

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

    List<String> lines = SpriteUtils.processTableDataGeneral(data, ANIMATION_TYPE);
    lines.add("[monster_large]");
    lines.add("false_color=" + falseColor);
    lines.add("resref=" + resref);

    retVal = IniMap.from(lines);

    return retVal;
  }

  public MonsterLargeDecoder(int animationId, IniMap ini) throws Exception {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public MonsterLargeDecoder(CreResource cre) throws Exception {
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
        add(resref + "G3.BAM");
        add(resref + "G3E.BAM");
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
