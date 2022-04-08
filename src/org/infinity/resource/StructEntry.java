// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Readable;

public interface StructEntry extends Comparable<StructEntry>, Cloneable, Writeable, Readable {
  StructEntry clone() throws CloneNotSupportedException;

  void copyNameAndOffset(StructEntry fromEntry);

  String getName();

  void setName(String newName);

  /** Get offset in bytes of this entry from start of the file. */
  int getOffset();

  /** Returns entry which owns this entry or {@code null} if this entry on top of hierarchy. */
  AbstractStruct getParent();

  /**
   * Returns byte count of serialized value of this object.
   *
   * @return Count of bytes that needed to store this object in it's {@link Writable natural format}
   */
  int getSize();

  /** Attempts to retrieve the data of this datatype and returns it as ByteBuffer object. */
  ByteBuffer getDataBuffer();

  /** Creates a list of StructEntry object, starting from root up to this object. */
  List<StructEntry> getStructChain();

  void setOffset(int newoffset);

  void setParent(AbstractStruct parent);
}
