// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.infinity.util.tuples.Couple;

/**
 * A class that allows you to define numeric ranges.
 */
public class NumberRange {
  private final List<Couple<Integer, Integer>> ranges = new ArrayList<>();

  /**
   * Defines a range that starts at <code>base</code> and ends at <code>base + range</code> (inclusive).
   *
   * @param base  start value of the range.
   * @param range length of the range.
   */
  public NumberRange(int base, int range) {
    this(base, range, 0, 0, 0);
  }

  /**
   * Defines a set of common ranges. The resolved list of ranges can be defined as:<br>
   * <br>
   * <code>base + ([subBase, subBase+subRange] << subPos) + [0, range]</code><br>
   * <br>
   * where [x, y] defines a range from x to y (inclusive).
   *
   * @param base  start value of the range.
   * @param range length of the range.
   */
  public NumberRange(int base, int range, int subBase, int subRange, int subPos) {
    init(base, range, subBase, subRange, subPos);
  }

  /** Returns whether the range covers the specified value. */
  public boolean contains(int value) {
    return ranges.stream().anyMatch(
        c -> (value >= c.getValue0().intValue() && value <= (c.getValue0().intValue() + c.getValue1().intValue())));
  }

  private void init(int base, int range, int subBase, int subRange, int subPos) {
    range = Math.abs(range);
    subRange = Math.abs(subRange);
    subPos = Math.max(0, Math.min(32, subPos));
    for (int i = 0; i <= subRange; i++) {
      int curBase = base + ((subBase + i) << subPos);
      ranges.add(Couple.with(curBase, range));
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(ranges);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    NumberRange other = (NumberRange) obj;
    return Objects.equals(ranges, other.ranges);
  }
}
