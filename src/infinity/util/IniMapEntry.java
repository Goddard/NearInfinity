// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IniMapEntry
{
  private String key, value;
  private int line;   // line number of ini entry

  public IniMapEntry(String key, String value, int line)
  {
    this.key = (key != null) ? key : "";
    this.value = (value != null) ? value : "";
    this.line = line;
  }

  public boolean hasKey() { return key != null; }
  public String getKey() { return key; }

  public boolean hasValue() { return value != null; }
  public String getValue() { return value; }

  public int getLine() { return line; }

  @Override
  public String toString()
  {
    return key + " = " + value;
  }

  /**
   * Helper routine: Splits values and returns them as array of individual values.
   * Using default separator "<code>,</code>" (comma).
   */
  public static String[] splitValues(String value)
  {
    return splitValues(value, ',');
  }

  /** Helper routine: Splits values and returns them as array of individual values. */
  public static String[] splitValues(String value, char separator)
  {
    String[] retVal = null;
    if (value != null) {
      retVal = value.split(Character.toString(separator));
    } else {
      retVal = new String[0];
    }
    return retVal;
  }

  /**
   * Helper routine: Extracts the object identifiers from a string of the format
   * "<code>[ea.general.race.class.specific.gender.align]</code>" where identifiers after
   * "<code>ea</code>" are optional.
   */
  public static int[] splitObjectValue(String value)
  {
    int[] retVal = null;
    if (value != null && Pattern.matches("^\\[(-?\\d+\\.?)+\\]$", value)) {
      List<String> results = new ArrayList<String>();
      Pattern p = Pattern.compile("-?\\d+");
      Matcher m = p.matcher(value);
      while (m.find()) {
        results.add(value.substring(m.start(), m.end()));
      }
      retVal = new int[results.size()];
      for (int i = 0; i < results.size(); i++) {
        try {
          retVal[i] = Integer.parseInt(results.get(i));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    if (retVal == null) {
      retVal = new int[0];
    }
    return retVal;
  }

  /**
   * Helper routine: Extracts the coordinates of an Infinity Engine position value in the format
   * "<code>[x.y:dir]</code>".
   */
  public static int[] splitPositionValue(String value)
  {
    int[] retVal = null;
    if (value != null && Pattern.matches("^\\[[-0-9]+\\.[-0-9]+(:[0-9]+)?\\]$", value)) {
      List<String> results = new ArrayList<String>();
      Pattern p = Pattern.compile("-?\\d+");
      Matcher m = p.matcher(value);
      while (m.find()) {
        results.add(value.substring(m.start(), m.end()));
      }
      retVal = new int[results.size()];
      for (int i = 0; i < results.size(); i++) {
        try {
          retVal[i] = Integer.parseInt(results.get(i));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    if (retVal == null) {
      retVal = new int[0];
    }
    return retVal;
  }
}