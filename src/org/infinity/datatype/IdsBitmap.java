// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeMap;
import java.util.function.BiFunction;

import javax.swing.JComponent;

import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;

public class IdsBitmap extends AbstractBitmap<IdsMapEntry> {
  private final BiFunction<Long, IdsMapEntry, String> formatterIdsBitmap = (value, item) -> {
    final String number = isShowAsHex() ? getHexValue(value) : value.toString();
    if (item != null) {
      return item.getSymbol() + " - " + number;
    } else {
      return "Unknown - " + number;
    }
  };

  private static final HashMap<String, TreeMap<Long, IdsMapEntry>> MAP_CACHE = new HashMap<>();

  public IdsBitmap(ByteBuffer buffer, int offset, int length, String name, String resource) {
    this(buffer, offset, length, name, resource, true, false, false);
  }

  public IdsBitmap(ByteBuffer buffer, int offset, int length, String name, String resource, boolean sortByName) {
    this(buffer, offset, length, name, resource, sortByName, false, false);
  }

  public IdsBitmap(ByteBuffer buffer, int offset, int length, String name, String resource, boolean sortByName,
      boolean showAsHex, boolean signed) {
    super(buffer, offset, length, name, createResourceList(resource), null, signed);
    setSortByName(sortByName);
    setShowAsHex(showAsHex);
    setFormatter(formatterIdsBitmap);
  }

  // --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container) {
    if (getDataOf(getLongValue()) == null) {
      putItem(getLongValue(), new IdsMapEntry(getLongValue(), "Unknown"));
    }

    return super.edit(container);
  }

  // --------------------- End Interface Editable ---------------------

  /**
   * Add to bitmap specified entry, id entry with such key not yet registered, otherwise do nothing.
   *
   * @param entry Entry to add. Must not be {@code null}
   */
  public void addIdsMapEntry(IdsMapEntry entry) {
    getBitmap().putIfAbsent(entry.getID(), entry);
  }

  public static void clearCache() {
    MAP_CACHE.clear();
  }

  private static TreeMap<Long, IdsMapEntry> createResourceList(String resource) {
    TreeMap<Long, IdsMapEntry> retVal = null;
    if (resource == null) {
      return retVal;
    }

    resource = resource.trim().toUpperCase(Locale.ENGLISH);
    retVal = MAP_CACHE.get(resource);
    if (retVal != null) {
      return retVal;
    }

    IdsMap idsMap = IdsMapCache.get(resource);
    if (idsMap != null) {
      retVal = new TreeMap<>();
      for (final IdsMapEntry e : idsMap.getAllValues()) {
        final long id = e.getID();
        retVal.put(id, new IdsMapEntry(id, e.getSymbol()));
      }

      // Add a fitting symbol for "0" to IDS list if needed
      if (!retVal.containsKey(0L)) {
        if (resource.equalsIgnoreCase("EA.IDS")) {
          retVal.put(0L, new IdsMapEntry(0L, "ANYONE"));
        } else {
          retVal.put(0L, new IdsMapEntry(0L, "NONE"));
        }
      }

      MAP_CACHE.put(resource, retVal);
    }
    return retVal;
  }
}
