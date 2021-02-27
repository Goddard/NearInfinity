// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.internal.CycleDef;
import org.infinity.resource.cre.decoder.internal.DecoderAttribute;
import org.infinity.resource.cre.decoder.internal.DirDef;
import org.infinity.resource.cre.decoder.internal.SegmentDef;
import org.infinity.resource.cre.decoder.internal.SeqDef;
import org.infinity.resource.cre.decoder.tables.SpriteTables;
import org.infinity.resource.itm.ItmResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;
import org.infinity.util.tuples.Couple;

/**
 * Creature animation decoder for processing type 5000/6000 (character_old) animations.
 * Available ranges: [5400,54ff], [5600,5fff], [6400,64ff], [6600,6fff]
 */
public class CharacterOldDecoder extends ArmoredBaseDecoder
{
  /** The animation type associated with this class definition. */
  public static final AnimationType ANIMATION_TYPE = AnimationType.CHARACTER_OLD;

  public static final DecoderAttribute KEY_HIDE_WEAPONS   = DecoderAttribute.with("hide_weapons", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_SHADOW         = DecoderAttribute.with("shadow", DecoderAttribute.DataType.STRING);

 /** Assigns BAM suffix and cycle index to a specific animation sequence. */
  private static final HashMap<Sequence, Couple<String, Integer>> suffixMap =
      new HashMap<Sequence, Couple<String, Integer>>() {{
        put(Sequence.ATTACK_SLASH_1H, Couple.with("A1", 0));
        put(Sequence.ATTACK_SLASH_2H, Couple.with("A2", 0));
        put(Sequence.ATTACK_BACKSLASH_1H, Couple.with("A3", 0));
        put(Sequence.ATTACK_BACKSLASH_2H, Couple.with("A4", 0));
        put(Sequence.ATTACK_JAB_1H, Couple.with("A5", 0));
        put(Sequence.ATTACK_JAB_2H, Couple.with("A6", 0));
        put(Sequence.ATTACK_BOW, Couple.with("SA", 0));
        put(Sequence.ATTACK_CROSSBOW, Couple.with("SX", 0));
        put(Sequence.SPELL, Couple.with("CA", 0));
        put(Sequence.SPELL1, get(Sequence.SPELL));
        put(Sequence.SPELL2, Couple.with("CA", 16));
        put(Sequence.SPELL3, Couple.with("CA", 32));
        put(Sequence.SPELL4, Couple.with("CA", 48));
        put(Sequence.CAST, Couple.with("CA", 8));
        put(Sequence.CAST1, get(Sequence.CAST));
        put(Sequence.CAST2, Couple.with("CA", 24));
        put(Sequence.CAST3, Couple.with("CA", 40));
        put(Sequence.CAST4, Couple.with("CA", 56));
        put(Sequence.WALK, Couple.with("G1", 0));
        put(Sequence.STANCE, Couple.with("G1", 8));
        put(Sequence.STANCE2, Couple.with("G1", 24));
        put(Sequence.STAND, Couple.with("G1", 16));
        put(Sequence.STAND2, Couple.with("G1", 32));
        put(Sequence.GET_HIT, Couple.with("G1", 40));
        put(Sequence.DIE, Couple.with("G1", 48));
        put(Sequence.SLEEP, get(Sequence.DIE));
        put(Sequence.GET_UP, Couple.with("!G1", 48));
        put(Sequence.TWITCH, Couple.with("G1", 56));
      }};

  /** BAM suffix and cycle index for extended walk directions. */
  private static Couple<String, Integer> walkExtra = Couple.with("W2", 0);

  /** Set of invalid attack type / animation sequence combinations. */
  private static final EnumMap<AttackType, EnumSet<Sequence>> forbiddenSequences =
      new EnumMap<AttackType, EnumSet<Sequence>>(AttackType.class) {{
        put(AttackType.ONE_HANDED, EnumSet.of(Sequence.ATTACK_SLASH_2H, Sequence.ATTACK_BACKSLASH_2H, Sequence.ATTACK_JAB_2H,
                                              Sequence.ATTACK_BOW, Sequence.ATTACK_CROSSBOW, Sequence.STANCE2));
        put(AttackType.TWO_HANDED, EnumSet.of(Sequence.ATTACK_SLASH_1H, Sequence.ATTACK_BACKSLASH_1H, Sequence.ATTACK_JAB_1H,
                                              Sequence.ATTACK_BOW, Sequence.ATTACK_CROSSBOW, Sequence.STANCE));
        put(AttackType.TWO_WEAPON, EnumSet.allOf(Sequence.class));
        put(AttackType.THROWING, EnumSet.of(Sequence.ATTACK_BACKSLASH_1H, Sequence.ATTACK_JAB_1H,
                                            Sequence.ATTACK_SLASH_2H, Sequence.ATTACK_BACKSLASH_2H, Sequence.ATTACK_JAB_2H,
                                            Sequence.ATTACK_BOW, Sequence.ATTACK_CROSSBOW, Sequence.STANCE2));
        put(AttackType.BOW, EnumSet.of(Sequence.ATTACK_SLASH_1H, Sequence.ATTACK_BACKSLASH_1H, Sequence.ATTACK_JAB_1H,
                                       Sequence.ATTACK_SLASH_2H, Sequence.ATTACK_BACKSLASH_2H, Sequence.ATTACK_JAB_2H,
                                       Sequence.ATTACK_CROSSBOW, Sequence.STANCE2));
        put(AttackType.SLING, get(AttackType.THROWING));
        put(AttackType.CROSSBOW, EnumSet.of(Sequence.ATTACK_SLASH_1H, Sequence.ATTACK_BACKSLASH_1H, Sequence.ATTACK_JAB_1H,
                                            Sequence.ATTACK_SLASH_2H, Sequence.ATTACK_BACKSLASH_2H, Sequence.ATTACK_JAB_2H,
                                            Sequence.ATTACK_BOW, Sequence.STANCE2));
      }};

  // the default shadow of the animation
  private static final String SHADOW_RESREF         = "CSHD";
  // special shadow for (armored) Sarevok
  private static final String SHADOW_RESREF_SAREVOK = "SSHD";

  /**
   * A helper method that parses the specified data array and generates a {@link IniMap} instance out of it.
   * @param data a String array containing table values for a specific table entry.
   * @return a {@code IniMap} instance with the value derived from the specified data array.
   *         Returns {@code null} if no data could be derived.
   */
  public static IniMap processTableData(String[] data)
  {
    IniMap retVal = null;
    if (data == null || data.length < 16) {
      return retVal;
    }

    String resref = SpriteTables.valueToString(data, SpriteTables.COLUMN_RESREF, "");
    if (resref.isEmpty()) {
      return retVal;
    }
    int equipHelmet = SpriteTables.valueToInt(data, SpriteTables.COLUMN_HELMET, 0);
    int falseColor = SpriteTables.valueToInt(data, SpriteTables.COLUMN_CLOWN, 0);
    int hideWeapons = SpriteTables.valueToInt(data, SpriteTables.COLUMN_WEAPON, 1) != 0 ? 0 : 1;
    String heightCode = SpriteTables.valueToString(data, SpriteTables.COLUMN_HEIGHT, "");
    String heightCodeHelmet = heightCode;
    String shadow = SpriteTables.valueToString(data, SpriteTables.COLUMN_RESREF2, "");

    List<String> lines = processTableDataGeneral(data, ANIMATION_TYPE);
    lines.add("[character_old]");
    lines.add("equip_helmet=" + equipHelmet);
    lines.add("false_color=" + falseColor);
    lines.add("hide_weapons=" + hideWeapons);
    lines.add("resref=" + resref);
    if (!heightCode.isEmpty()) {
      lines.add("height_code=" + heightCode);
    }
    if (!heightCodeHelmet.isEmpty()) {
      lines.add("height_code_helmet=" + heightCodeHelmet);
    }
    if (!shadow.isEmpty()) {
      lines.add("shadow=" + shadow);
    }

    retVal = IniMap.from(lines);

    return retVal;
  }

  public CharacterOldDecoder(int animationId, IniMap ini) throws Exception
  {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public CharacterOldDecoder(CreResource cre) throws Exception
  {
    super(ANIMATION_TYPE, cre);
  }

  /** Returns whether weapon animation overlays are suppressed. */
  public boolean isWeaponsHidden() { return getAttribute(KEY_HIDE_WEAPONS); }
  protected void setWeaponsHidden(boolean b) { setAttribute(KEY_HIDE_WEAPONS, b); }

  /** Returns a separate shadow sprite resref. */
  public String getShadowResref() { return getAttribute(KEY_SHADOW); }
  protected void setShadowResref(String s)
  {
    String shadow;
    // taking care of harcoded shadows
    switch (getAnimationId()) {
      case 0x6400:  // Drizzt
        shadow = SHADOW_RESREF;
        break;
      case 0x6404:  // Sarevok
        shadow = SHADOW_RESREF_SAREVOK;
        break;
      default:
        shadow = !s.isEmpty() ? s : SHADOW_RESREF;
    }
    setAttribute(KEY_SHADOW, shadow);
  }

  /**
   * Sets the maximum armor code value uses as suffix in animation filenames.
   * Specify -1 to detect value automatically.
   */
  @Override
  protected void setMaxArmorCode(int v)
  {
    if (v < 0) {
      // autodetection
      for (int i = 1; i < 10 && v < 0; i++) {
        String resref = getAnimationResref();
        if (!resref.isEmpty() && !ResourceFactory.resourceExists(resref + i + "G1.BAM")) {
          v = i - 1;
        }
      }
    }
    super.setMaxArmorCode(v);
  }

  @Override
  public List<String> getAnimationFiles(boolean essential)
  {
    ArrayList<String> retVal = null;
    String resref = getAnimationResref();

    if (essential) {
      HashSet<String> files = new HashSet<>();
      for (final HashMap.Entry<Sequence, Couple<String, Integer>> entry : suffixMap.entrySet()) {
        String suffix = SegmentDef.fixBehaviorSuffix(entry.getValue().getValue0());
        if (suffix.startsWith("G")) {
          for (int i = 1; i <= getMaxArmorCode(); i++) {
            files.add(resref + i + suffix + ".BAM");
          }
        }
      }
      retVal = new ArrayList<>(Arrays.asList(files.toArray(new String[files.size()])));
    } else {
      // collecting suffixes
      HashSet<String> actionSet = new HashSet<>();
      for (final HashMap.Entry<Sequence, Couple<String, Integer>> entry : suffixMap.entrySet()) {
        String suffix = SegmentDef.fixBehaviorSuffix(entry.getValue().getValue0());
        actionSet.add(suffix);
      }
      actionSet.add(walkExtra.getValue0());

      // generating file list
      retVal = new ArrayList<String>() {{
        for (int i = 1; i <= getMaxArmorCode(); i++) {
          for (final String a : actionSet) {
            add(resref + i + a + ".BAM");
            add(resref + i + a + "E.BAM");
          }
        }
      }};
    }

    return retVal;
  }

  @Override
  public boolean isSequenceAvailable(Sequence seq)
  {
    return (getSequenceDefinition(seq) != null);
  }

  @Override
  protected void init() throws Exception
  {
    super.init();
    IniMapSection section = getSpecificIniSection();
    setWeaponsHidden(section.getAsInteger(KEY_HIDE_WEAPONS.getName(), 0) != 0);
    setShadowResref(section.getAsString(KEY_SHADOW.getName(), ""));
    if (getMaxArmorCode() == 0) {
      setMaxArmorCode(-1);
    }
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq)
  {
    SeqDef retVal = null;

    if (!suffixMap.containsKey(seq)) {
      return retVal;
    }

    // getting armor level
    int armorCode = getArmorCode();
    if (armorCode > getMaxArmorCode()) {
      return retVal;
    }

    // getting attack type
    ItmResource itmWeapon = SpriteUtils.getEquippedWeapon(getCreResource());
    int itmAbility = SpriteUtils.getEquippedWeaponAbility(getCreResource());
    AttackType attackType = getAttackType(itmWeapon, itmAbility, false);

    EnumSet<Sequence> sequences = forbiddenSequences.get(attackType);
    if (sequences != null && sequences.contains(seq)) {
      // sequence not allowed for selected weapon
      return retVal;
    }

    ArrayList<Couple<String, SegmentDef.SpriteType>> resrefList = new ArrayList<>();

    // getting BAM suffix and cycle index
    String suffix = suffixMap.get(seq).getValue0();
    SegmentDef.Behavior behavior = SegmentDef.getBehaviorOf(suffix);
    suffix = SegmentDef.fixBehaviorSuffix(suffix);
    int cycleIdx = suffixMap.get(seq).getValue1().intValue();

    // adding creature shadow
    if (!getShadowResref().isEmpty()) {
      resrefList.add(Couple.with(getShadowResref(), SegmentDef.SpriteType.AVATAR));
    }

    // adding creature sprite
    resrefList.add(Couple.with(getAnimationResref() + armorCode, SegmentDef.SpriteType.AVATAR));

    // adding helmet overlay
    if (isHelmetEquipped()) {
      String prefix = getHelmetHeightCode();
      if (!prefix.isEmpty()) {
        String code = SpriteUtils.getItemAppearance(SpriteUtils.getEquippedHelmet(getCreResource()));
        if (code.length() == 2) {
          resrefList.add(Couple.with(prefix + code, SegmentDef.SpriteType.HELMET));
        }
      }
    }

    if (!isWeaponsHidden()) {
      // adding shield overlay
      String prefix = getHeightCode();
      if (!prefix.isEmpty()) {
        ItmResource itm = SpriteUtils.getEquippedShield(getCreResource());
        String code = SpriteUtils.getItemAppearance(itm);
        if (!code.isEmpty()) {
          resrefList.add(Couple.with(prefix + code, SegmentDef.SpriteType.SHIELD));
        }
      }

      // adding weapon overlay
      prefix = getHeightCode();
      if (!prefix.isEmpty()) {
        String code = SpriteUtils.getItemAppearance(itmWeapon);
        if (code.length() == 2) {
          if (ResourceFactory.resourceExists(prefix + code + suffix + ".BAM")) {
            resrefList.add(Couple.with(prefix + code, SegmentDef.SpriteType.WEAPON));
          } else {
            // weapon animation is crucial
            return retVal;
          }
        }
      }
    }

    retVal = new SeqDef(seq);
    for (final Couple<String, SegmentDef.SpriteType> data : resrefList) {
      String prefix = data.getValue0();
      SegmentDef.SpriteType spriteType = data.getValue1();
      // defining sequences
      ResourceEntry entry = ResourceFactory.getResourceEntry(prefix + suffix + ".BAM");
      ResourceEntry entryE = ResourceFactory.getResourceEntry(prefix + suffix + "E.BAM");
      if (entry != null) {
        if (seq == Sequence.WALK) {
          // special: uses full set of directions spread over two BAM files
          String suffix2 = walkExtra.getValue0();
          int cycleIdx2 = walkExtra.getValue1().intValue();
          ResourceEntry entry2 = ResourceFactory.getResourceEntry(prefix + suffix2 + ".BAM");
          ResourceEntry entry2E = ResourceFactory.getResourceEntry(prefix + suffix2 + "E.BAM");
          if (SpriteUtils.bamCyclesExist(entry, cycleIdx, SeqDef.DIR_REDUCED_W.length) &&
              SpriteUtils.bamCyclesExist(entry2, cycleIdx2, SeqDef.DIR_REDUCED_W.length) &&
              SpriteUtils.bamCyclesExist(entryE, cycleIdx + SeqDef.DIR_REDUCED_W.length, SeqDef.DIR_REDUCED_E.length) &&
              SpriteUtils.bamCyclesExist(entry2E, cycleIdx2 + SeqDef.DIR_REDUCED_W.length, SeqDef.DIR_REDUCED_E.length)) {
            // defining western directions
            Direction[] dirX = new Direction[] { Direction.SSW, Direction.WSW, Direction.WNW, Direction.NNW, Direction.NNE };
            for (int i = 0; i < SeqDef.DIR_REDUCED_W.length; i++) {
              retVal.addDirections(new DirDef(SeqDef.DIR_REDUCED_W[i], false, new CycleDef(entry, cycleIdx + i, spriteType, behavior)));
              retVal.addDirections(new DirDef(dirX[i], false, new CycleDef(entry2, cycleIdx2 + i, spriteType, behavior)));
            }
            // defining eastern directions
            dirX = new Direction[] { Direction.ENE, Direction.ESE, Direction.SSE, };
            for (int i = 0; i < SeqDef.DIR_REDUCED_E.length; i++) {
              retVal.addDirections(new DirDef(SeqDef.DIR_REDUCED_E[i], false, new CycleDef(entryE, cycleIdx + SeqDef.DIR_REDUCED_W.length + i, spriteType, behavior)));
              retVal.addDirections(new DirDef(dirX[i], false, new CycleDef(entry2E, cycleIdx2 + SeqDef.DIR_REDUCED_W.length + i, spriteType, behavior)));
            }
          }
        } else {
          if (SpriteUtils.bamCyclesExist(entry, cycleIdx, SeqDef.DIR_REDUCED_W.length) &&
              SpriteUtils.bamCyclesExist(entry, cycleIdx + SeqDef.DIR_REDUCED_W.length, SeqDef.DIR_REDUCED_E.length)) {
            SeqDef tmp = SeqDef.createSequence(seq, SeqDef.DIR_REDUCED_W, false, entry, cycleIdx, spriteType, behavior);
            retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
            tmp = SeqDef.createSequence(seq, SeqDef.DIR_REDUCED_E, false, entryE, cycleIdx + SeqDef.DIR_REDUCED_W.length, spriteType, behavior);
            retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
          }
        }
      }
    }

    if (retVal.isEmpty()) {
      retVal = null;
    }

    return retVal;
  }
}
