// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class Entrance extends AbstractStruct implements AddRemovable
{
  // ARE/Entrance-specific field labels
  public static final String ARE_ENTRANCE             = "Entrance";
  public static final String ARE_ENTRANCE_NAME        = "Name";
  public static final String ARE_ENTRANCE_LOCATION_X  = "Location: X";
  public static final String ARE_ENTRANCE_LOCATION_Y  = "Location: Y";
  public static final String ARE_ENTRANCE_ORIENTATION = "Orientation";

  Entrance() throws Exception
  {
    super(null, ARE_ENTRANCE, new byte[104], 0);
  }

  Entrance(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, ARE_ENTRANCE + " " + number, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 32, ARE_ENTRANCE_NAME));
    addField(new DecNumber(buffer, offset + 32, 2, ARE_ENTRANCE_LOCATION_X));
    addField(new DecNumber(buffer, offset + 34, 2, ARE_ENTRANCE_LOCATION_Y));
    addField(new Bitmap(buffer, offset + 36, 4, ARE_ENTRANCE_ORIENTATION, Actor.s_orientation));
    addField(new Unknown(buffer, offset + 40, 64));
    return offset + 104;
  }
}

