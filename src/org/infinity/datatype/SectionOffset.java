// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.infinity.resource.StructEntry;

public final class SectionOffset extends HexNumber {
  private Class<? extends StructEntry> section;

  public SectionOffset(ByteBuffer buffer, int offset, String desc, Class<? extends StructEntry> section) {
    super(buffer, offset, 4, desc);
    this.section = Objects.requireNonNull(section, "Class for SectionOffset must not be null");
  }

  // --------------------- Begin Interface InlineEditable ---------------------

  @Override
  public boolean update(Object value) {
    // should not be modified by the user
    return false;
  }

  // --------------------- End Interface InlineEditable ---------------------

  public Class<? extends StructEntry> getSection() {
    return section;
  }

  public void setSection(Class<? extends StructEntry> section) {
    if (!this.section.equals(section)) {
      Class<? extends StructEntry> cls = this.section;
      this.section = Objects.requireNonNull(section, "Class for SectionOffset must not be null");
      if (getParent() != null) {
        getParent().updateSectionOffset(cls);
      }
    }
  }
}
