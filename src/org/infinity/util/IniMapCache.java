// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.HashMap;
import java.util.Locale;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;

public class IniMapCache {
  private static final HashMap<ResourceEntry, IniMap> MAP = new HashMap<>();

  public static void cacheInvalid(ResourceEntry entry) {
    if (entry != null) {
      MAP.remove(entry);
    }
  }

  public static void clearCache() {
    MAP.clear();
  }

  public static IniMap get(String name) {
    return get(name, false);
  }

  public static IniMap get(String name, boolean ignoreComments) {
    IniMap retVal = null;
    if (name != null) {
      name = name.trim().toUpperCase(Locale.ENGLISH);
      ResourceEntry entry = ResourceFactory.getResourceEntry(name);
      if (entry != null) {
        retVal = get(entry, ignoreComments);
      } else {
        Logger.warn("Could not find {}", name);
      }
    }
    return retVal;
  }

  public static synchronized IniMap get(ResourceEntry entry) {
    return get(entry, false);
  }

  public static synchronized IniMap get(ResourceEntry entry, boolean ignoreComments) {
    IniMap retVal = null;
    if (entry != null) {
      retVal = MAP.get(entry);
      if (retVal == null) {
        retVal = new IniMap(entry, ignoreComments);
        MAP.put(entry, retVal);
      }
    }
    return retVal;
  }

  private IniMapCache() {
  }
}
