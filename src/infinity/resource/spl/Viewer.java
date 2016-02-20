// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.spl;

import infinity.datatype.EffectType;
import infinity.datatype.ResourceRef;
import infinity.gui.ViewerUtil;
import infinity.resource.AbstractAbility;
import infinity.resource.Effect;
import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.util.IdsMap;
import infinity.util.IdsMapCache;
import infinity.util.IdsMapEntry;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

public final class Viewer extends JPanel
{
  private static final HashMap<Integer, String> SpellPrefix = new HashMap<Integer, String>();
  private static final HashMap<String, Integer> SpellType = new HashMap<String, Integer>();
  static {
    SpellPrefix.put(Integer.valueOf(1), "SPPR");
    SpellPrefix.put(Integer.valueOf(2), "SPWI");
    SpellPrefix.put(Integer.valueOf(3), "SPIN");
    SpellPrefix.put(Integer.valueOf(4), "SPCL");
    SpellType.put("SPPR", Integer.valueOf(1));
    SpellType.put("SPWI", Integer.valueOf(2));
    SpellType.put("SPIN", Integer.valueOf(3));
    SpellType.put("SPCL", Integer.valueOf(4));
  }

  // Returns an (optionally formatted) String containing the symbolic spell name as defined in SPELL.IDS
  public static String getSymbolicName(ResourceEntry entry, boolean formatted)
  {
    if (entry != null) {
      String resName = entry.getResourceName().toUpperCase(Locale.ENGLISH);
      int idx = resName.lastIndexOf('.');
      String ext = (idx >= 0) ? resName.substring(idx+1) : "";
      String name = (idx >= 0) ? resName.substring(0, idx) : resName;

      if ("SPL".equals(ext) && name.length() >= 7) {
        // fetching spell type
        String s = resName.substring(0, 4);
        int type = -1;
        if (SpellType.containsKey(s)) {
          type = SpellType.get(s).intValue();
        }

        // fetching spell code
        s = name.substring(4);
        int code = -1;
        try {
          code = Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }

        // returning symbolic spell name
        if (type >= 0 && code >= 0) {
          int value = type*1000 + code;
          IdsMap ids = IdsMapCache.get("SPELL.IDS");
          IdsMapEntry idsEntry = ids.getValue(value);
          if (idsEntry != null) {
            if (formatted) {
              return String.format("%1$s (%2$d)", idsEntry.getString(), (int)idsEntry.getID());
            } else {
              return idsEntry.getString();
            }
          }
        }
      }
    }
    return null;
  }

  // Returns the resource filename associated with the specified symbolic name as defined in SPELL.IDS
  public static String getResourceName(String symbol, boolean extension)
  {
    if (symbol != null) {
      IdsMap ids = IdsMapCache.get("SPELL.IDS");
      IdsMapEntry idsEntry = ids.lookup(symbol);
      if (idsEntry != null) {
        int value = (int)idsEntry.getID();
        int type = value / 1000;
        int code = value % 1000;
        if (SpellPrefix.containsKey(Integer.valueOf(type))) {
          String prefix = SpellPrefix.get(Integer.valueOf(type));
          String nameBase = String.format("%1$s%2$03d", prefix, code);
          if (ResourceFactory.resourceExists(nameBase + ".SPL")) {
            return extension ? nameBase + ".SPL" : nameBase;
          }
        }
      }
    }
    return null;
  }

  private static JPanel makeFieldPanel(SplResource spl)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute(SplResource.SPL_NAME), gbl, gbc, true);
    String s = getSymbolicName(spl.getResourceEntry(), true);
    ViewerUtil.addLabelFieldPair(fieldPanel, "Symbolic name", (s != null) ? s : "n/a", gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute(SplResource.SPL_TYPE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute(SplResource.SPL_CASTING_ANIMATION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute(SplResource.SPL_PRIMARY_TYPE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute(SplResource.SPL_SECONDARY_TYPE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute(SplResource.SPL_LEVEL), gbl, gbc, true);

    return fieldPanel;
  }

  Viewer(SplResource spl)
  {
    JComponent iconPanel = ViewerUtil.makeBamPanel((ResourceRef)spl.getAttribute(SplResource.SPL_ICON), 0);
    JPanel globaleffectsPanel = ViewerUtil.makeListPanel("Global effects", spl, Effect.class, EffectType.EFFECT_TYPE);
    JPanel abilitiesPanel = ViewerUtil.makeListPanel("Abilities", spl, Ability.class, AbstractAbility.ABILITY_TYPE);
    JPanel descPanel = ViewerUtil.makeTextAreaPanel(spl.getAttribute(SplResource.SPL_DESCRIPTION));
    JPanel fieldPanel = makeFieldPanel(spl);

    JPanel infoPanel = new JPanel(new BorderLayout());
    infoPanel.add(iconPanel, BorderLayout.NORTH);
    infoPanel.add(fieldPanel, BorderLayout.CENTER);

    setLayout(new GridLayout(2, 2, 6, 6));
    add(infoPanel);
    add(globaleffectsPanel);
    add(descPanel);
    add(abilitiesPanel);
    setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
  }
}

