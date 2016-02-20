// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HexNumber;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.StringRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.Profile;
import infinity.resource.StructEntry;
import infinity.resource.vertex.ClosedVertex;
import infinity.resource.vertex.ClosedVertexImpeded;
import infinity.resource.vertex.OpenVertex;
import infinity.resource.vertex.OpenVertexImpeded;
import infinity.resource.vertex.Vertex;

public final class Door extends AbstractStruct implements AddRemovable, HasVertices, HasAddRemovable
{
  // ARE/Door-specific field labels
  public static final String ARE_DOOR                                   = "Door";
  public static final String ARE_DOOR_NAME                              = "Name";
  public static final String ARE_DOOR_ID                                = "Door ID";
  public static final String ARE_DOOR_FLAGS                             = "Flags";
  public static final String ARE_DOOR_FIRST_VERTEX_INDEX_OPEN           = "First vertex index (open)";
  public static final String ARE_DOOR_NUM_VERTICES_OPEN                 = "# vertices (open)";
  public static final String ARE_DOOR_NUM_VERTICES_CLOSED               = "# vertices (closed)";
  public static final String ARE_DOOR_FIRST_VERTEX_INDEX_CLOSED         = "First vertex index (closed)";
  public static final String ARE_DOOR_OPEN_BOUNDING_BOX_LEFT            = "Bounding box (open): Left";
  public static final String ARE_DOOR_OPEN_BOUNDING_BOX_TOP             = "Bounding box (open): Top";
  public static final String ARE_DOOR_OPEN_BOUNDING_BOX_RIGHT           = "Bounding box (open): Right";
  public static final String ARE_DOOR_OPEN_BOUNDING_BOX_BOTTOM          = "Bounding box (open): Bottom";
  public static final String ARE_DOOR_CLOSED_BOUNDING_BOX_LEFT          = "Bounding box (closed): Left";
  public static final String ARE_DOOR_CLOSED_BOUNDING_BOX_TOP           = "Bounding box (closed): Top";
  public static final String ARE_DOOR_CLOSED_BOUNDING_BOX_RIGHT         = "Bounding box (closed): Right";
  public static final String ARE_DOOR_CLOSED_BOUNDING_BOX_BOTTOM        = "Bounding box (closed): Bottom";
  public static final String ARE_DOOR_FIRST_VERTEX_INDEX_IMPEDED_OPEN   = "First vertex index (impeded, open)";
  public static final String ARE_DOOR_NUM_VERTICES_IMPEDED_OPEN         = "# vertices (impeded, open)";
  public static final String ARE_DOOR_NUM_VERTICES_IMPEDED_CLOSED       = "# vertices (impeded, closed)";
  public static final String ARE_DOOR_FIRST_VERTEX_INDEX_IMPEDED_CLOSED = "First vertex index (impeded, closed)";
  public static final String ARE_DOOR_CURRENT_HP                        = "Current HP";
  public static final String ARE_DOOR_EFFECTIVE_AC                      = "Effective AC";
  public static final String ARE_DOOR_SOUND_OPENING                     = "Opening sound";
  public static final String ARE_DOOR_SOUND_CLOSING                     = "Closing sound";
  public static final String ARE_DOOR_CURSOR_INDEX                      = "Cursor number";
  public static final String ARE_DOOR_TRAP_DETECTION_DIFFICULTY         = "Trap detection difficulty";
  public static final String ARE_DOOR_TRAP_REMOVAL_DIFFICULTY           = "Trap removal difficulty";
  public static final String ARE_DOOR_TRAPPED                           = "Is trapped?";
  public static final String ARE_DOOR_TRAP_DETECTED                     = "Is trap detected?";
  public static final String ARE_DOOR_LAUNCH_POINT_X                    = "Launch point: X";
  public static final String ARE_DOOR_LAUNCH_POINT_Y                    = "Launch point: Y";
  public static final String ARE_DOOR_KEY                               = "Key";
  public static final String ARE_DOOR_SCRIPT                            = "Script";
  public static final String ARE_DOOR_DETECTION_DIFFICULTY              = "Detection difficulty";
  public static final String ARE_DOOR_LOCK_DIFFICULTY                   = "Lock difficulty";
  public static final String ARE_DOOR_LOCATION_OPEN_X                   = "Open location: X";
  public static final String ARE_DOOR_LOCATION_OPEN_Y                   = "Open location: Y";
  public static final String ARE_DOOR_LOCATION_CLOSE_X                  = "Close location: X";
  public static final String ARE_DOOR_LOCATION_CLOSE_Y                  = "Close location: Y";
  public static final String ARE_DOOR_UNLOCK_MESSAGE                    = "Unlock message";
  public static final String ARE_DOOR_TRAVEL_TRIGGER_NAME               = "Travel trigger name";
  public static final String ARE_DOOR_SPEAKER_NAME                      = "Speaker name";
  public static final String ARE_DOOR_DIALOG                            = "Dialogue";

  public static final String[] s_noyes = {"No", "Yes"};
  public static final String[] s_flag = {"No flags set", "Door open", "Door locked", "Trap resets",
                                         "Detectable trap", "Door forced", "Cannot close", "Door located",
                                         "Door secret", "Secret door detected", "Can be looked through",
                                         "Uses key", "Sliding door"};
  public static final String[] s_flag_iwd2 = {"No flags set", "Door open", "Door locked", "Trap resets",
                                              "Detectable trap", "Door forced", "Cannot close", "Door located",
                                              "Door secret", "Secret door detected", "Alternate lock string",
                                              "Can be looked through", "Warn on activate", "Displayed warning",
                                              "Door hidden", "Uses key"};

  public Door() throws Exception
  {
    super(null, ARE_DOOR, new byte[200], 0);
  }

  public Door(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, ARE_DOOR + " " + nr, buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new OpenVertex(), new ClosedVertex(), new ClosedVertexImpeded(),
                              new OpenVertexImpeded()};
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception
  {
    return entry;
  }

  @Override
  public boolean confirmRemoveEntry(AddRemovable entry) throws Exception
  {
    return true;
  }

// --------------------- End Interface HasAddRemovable ---------------------


//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------


// --------------------- Begin Interface HasVertices ---------------------

  @Override
  public void readVertices(byte buffer[], int offset) throws Exception
  {
    DecNumber firstVertex = (DecNumber)getAttribute(ARE_DOOR_FIRST_VERTEX_INDEX_OPEN);
    DecNumber numVertices = (DecNumber)getAttribute(ARE_DOOR_NUM_VERTICES_OPEN);
    for (int i = 0; i < numVertices.getValue(); i++) {
      addField(new OpenVertex(this, buffer, offset + 4 * (firstVertex.getValue() + i), i));
    }

    firstVertex = (DecNumber)getAttribute(ARE_DOOR_FIRST_VERTEX_INDEX_CLOSED);
    numVertices = (DecNumber)getAttribute(ARE_DOOR_NUM_VERTICES_CLOSED);
    for (int i = 0; i < numVertices.getValue(); i++) {
      addField(new ClosedVertex(this, buffer, offset + 4 * (firstVertex.getValue() + i), i));
    }

    firstVertex = (DecNumber)getAttribute(ARE_DOOR_FIRST_VERTEX_INDEX_IMPEDED_OPEN);
    numVertices = (DecNumber)getAttribute(ARE_DOOR_NUM_VERTICES_IMPEDED_OPEN);
    for (int i = 0; i < numVertices.getValue(); i++) {
      addField(new OpenVertexImpeded(this, buffer, offset + 4 * (firstVertex.getValue() + i), i));
    }

    firstVertex = (DecNumber)getAttribute(ARE_DOOR_FIRST_VERTEX_INDEX_IMPEDED_CLOSED);
    numVertices = (DecNumber)getAttribute(ARE_DOOR_NUM_VERTICES_IMPEDED_CLOSED);
    for (int i = 0; i < numVertices.getValue(); i++) {
      addField(new ClosedVertexImpeded(this, buffer, offset + 4 * (firstVertex.getValue() + i), i));
    }
  }

  @Override
  public int updateVertices(int offset, int number)
  {
    // Must assume that the number is correct
    ((DecNumber)getAttribute(ARE_DOOR_FIRST_VERTEX_INDEX_OPEN)).setValue(number);
    int count = ((DecNumber)getAttribute(ARE_DOOR_NUM_VERTICES_OPEN)).getValue();
    ((DecNumber)getAttribute(ARE_DOOR_FIRST_VERTEX_INDEX_CLOSED)).setValue(number + count);
    count += ((DecNumber)getAttribute(ARE_DOOR_NUM_VERTICES_CLOSED)).getValue();
    ((DecNumber)getAttribute(ARE_DOOR_FIRST_VERTEX_INDEX_IMPEDED_OPEN)).setValue(number + count);
    count += ((DecNumber)getAttribute(ARE_DOOR_NUM_VERTICES_IMPEDED_OPEN)).getValue();
    ((DecNumber)getAttribute(ARE_DOOR_FIRST_VERTEX_INDEX_IMPEDED_CLOSED)).setValue(number + count);
    count += ((DecNumber)getAttribute(ARE_DOOR_NUM_VERTICES_IMPEDED_CLOSED)).getValue();

    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
      if (entry instanceof Vertex) {
        entry.setOffset(offset);
        ((Vertex)entry).realignStructOffsets();
        offset += 4;
      }
    }
    return count;
  }

// --------------------- End Interface HasVertices ---------------------

  @Override
  protected void setAddRemovableOffset(AddRemovable datatype)
  {
    int offset = ((HexNumber)getSuperStruct().getAttribute(AreResource.ARE_OFFSET_VERTICES)).getValue();
    if (datatype instanceof OpenVertex) {
      int index = ((DecNumber)getAttribute(ARE_DOOR_FIRST_VERTEX_INDEX_OPEN)).getValue();
      index += ((DecNumber)getAttribute(ARE_DOOR_NUM_VERTICES_OPEN)).getValue();
      datatype.setOffset(offset + 4 * (index - 1));
    }
    else if (datatype instanceof ClosedVertex) {
      int index = ((DecNumber)getAttribute(ARE_DOOR_FIRST_VERTEX_INDEX_CLOSED)).getValue();
      index += ((DecNumber)getAttribute(ARE_DOOR_NUM_VERTICES_CLOSED)).getValue();
      datatype.setOffset(offset + 4 * (index - 1));
    }
    else if (datatype instanceof OpenVertexImpeded) {
      int index = ((DecNumber)getAttribute(ARE_DOOR_FIRST_VERTEX_INDEX_IMPEDED_OPEN)).getValue();
      index += ((DecNumber)getAttribute(ARE_DOOR_NUM_VERTICES_IMPEDED_OPEN)).getValue();
      datatype.setOffset(offset + 4 * (index - 1));
    }
    else if (datatype instanceof ClosedVertexImpeded) {
      int index = ((DecNumber)getAttribute(ARE_DOOR_FIRST_VERTEX_INDEX_IMPEDED_CLOSED)).getValue();
      index += ((DecNumber)getAttribute(ARE_DOOR_NUM_VERTICES_IMPEDED_CLOSED)).getValue();
      datatype.setOffset(offset + 4 * (index - 1));
    }
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 32, ARE_DOOR_NAME));
    addField(new TextString(buffer, offset + 32, 8, ARE_DOOR_ID));
    if (Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new Flag(buffer, offset + 40, 4, ARE_DOOR_FLAGS, s_flag_iwd2));
    } else {
      addField(new Flag(buffer, offset + 40, 4, ARE_DOOR_FLAGS, s_flag));
    }
    addField(new DecNumber(buffer, offset + 44, 4, ARE_DOOR_FIRST_VERTEX_INDEX_OPEN));
    addField(new SectionCount(buffer, offset + 48, 2, ARE_DOOR_NUM_VERTICES_OPEN, OpenVertex.class));
    addField(new SectionCount(buffer, offset + 50, 2, ARE_DOOR_NUM_VERTICES_CLOSED, ClosedVertex.class));
    addField(new DecNumber(buffer, offset + 52, 4, ARE_DOOR_FIRST_VERTEX_INDEX_CLOSED));
    addField(new DecNumber(buffer, offset + 56, 2, ARE_DOOR_OPEN_BOUNDING_BOX_LEFT));
    addField(new DecNumber(buffer, offset + 58, 2, ARE_DOOR_OPEN_BOUNDING_BOX_TOP));
    addField(new DecNumber(buffer, offset + 60, 2, ARE_DOOR_OPEN_BOUNDING_BOX_RIGHT));
    addField(new DecNumber(buffer, offset + 62, 2, ARE_DOOR_OPEN_BOUNDING_BOX_BOTTOM));
    addField(new DecNumber(buffer, offset + 64, 2, ARE_DOOR_CLOSED_BOUNDING_BOX_LEFT));
    addField(new DecNumber(buffer, offset + 66, 2, ARE_DOOR_CLOSED_BOUNDING_BOX_TOP));
    addField(new DecNumber(buffer, offset + 68, 2, ARE_DOOR_CLOSED_BOUNDING_BOX_RIGHT));
    addField(new DecNumber(buffer, offset + 70, 2, ARE_DOOR_CLOSED_BOUNDING_BOX_BOTTOM));
    addField(new DecNumber(buffer, offset + 72, 4, ARE_DOOR_FIRST_VERTEX_INDEX_IMPEDED_OPEN));
    addField(new SectionCount(buffer, offset + 76, 2, ARE_DOOR_NUM_VERTICES_IMPEDED_OPEN,
                              OpenVertexImpeded.class));
    addField(new SectionCount(buffer, offset + 78, 2, ARE_DOOR_NUM_VERTICES_IMPEDED_CLOSED,
                              ClosedVertexImpeded.class));
    addField(new DecNumber(buffer, offset + 80, 4, ARE_DOOR_FIRST_VERTEX_INDEX_IMPEDED_CLOSED));
    addField(new DecNumber(buffer, offset + 84, 2, ARE_DOOR_CURRENT_HP));
    addField(new DecNumber(buffer, offset + 86, 2, ARE_DOOR_EFFECTIVE_AC));
    addField(new ResourceRef(buffer, offset + 88, ARE_DOOR_SOUND_OPENING, "WAV"));
    addField(new ResourceRef(buffer, offset + 96, ARE_DOOR_SOUND_CLOSING, "WAV"));
    addField(new DecNumber(buffer, offset + 104, 4, ARE_DOOR_CURSOR_INDEX));
    addField(new DecNumber(buffer, offset + 108, 2, ARE_DOOR_TRAP_DETECTION_DIFFICULTY));
    addField(new DecNumber(buffer, offset + 110, 2, ARE_DOOR_TRAP_REMOVAL_DIFFICULTY));
    addField(new Bitmap(buffer, offset + 112, 2, ARE_DOOR_TRAPPED, s_noyes));
    addField(new Bitmap(buffer, offset + 114, 2, ARE_DOOR_TRAP_DETECTED, s_noyes));
    addField(new DecNumber(buffer, offset + 116, 2, ARE_DOOR_LAUNCH_POINT_X));
    addField(new DecNumber(buffer, offset + 118, 2, ARE_DOOR_LAUNCH_POINT_Y));
    addField(new ResourceRef(buffer, offset + 120, ARE_DOOR_KEY, "ITM"));
    addField(new ResourceRef(buffer, offset + 128, ARE_DOOR_SCRIPT, "BCS"));
    addField(new DecNumber(buffer, offset + 136, 4, ARE_DOOR_DETECTION_DIFFICULTY));
    addField(new DecNumber(buffer, offset + 140, 4, ARE_DOOR_LOCK_DIFFICULTY));
    addField(new DecNumber(buffer, offset + 144, 2, ARE_DOOR_LOCATION_OPEN_X));
    addField(new DecNumber(buffer, offset + 146, 2, ARE_DOOR_LOCATION_OPEN_Y));
    addField(new DecNumber(buffer, offset + 148, 2, ARE_DOOR_LOCATION_CLOSE_X));
    addField(new DecNumber(buffer, offset + 150, 2, ARE_DOOR_LOCATION_CLOSE_Y));
    addField(new StringRef(buffer, offset + 152, ARE_DOOR_UNLOCK_MESSAGE));
    addField(new TextString(buffer, offset + 156, 24, ARE_DOOR_TRAVEL_TRIGGER_NAME));
//    addField(new Unknown(buffer, offset + 172, 8));
    addField(new StringRef(buffer, offset + 180, ARE_DOOR_SPEAKER_NAME));
    addField(new ResourceRef(buffer, offset + 184, ARE_DOOR_DIALOG, "DLG"));
    addField(new Unknown(buffer, offset + 192, 8));
    return offset + 200;
  }
}

