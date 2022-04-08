// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.nio.ByteBuffer;

import javax.swing.JComponent;

import org.infinity.datatype.AnimateBitmap;
import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.HexNumber;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.gui.StructViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;
import org.infinity.resource.cre.CreResource;
import org.infinity.util.io.StreamUtils;

public final class Actor extends AbstractStruct implements AddRemovable, HasViewerTabs {
  // ARE/Actor-specific field labels
  public static final String ARE_ACTOR                      = "Actor";
  public static final String ARE_ACTOR_NAME                 = "Name";
  public static final String ARE_ACTOR_POS_X                = "Position: X";
  public static final String ARE_ACTOR_POS_Y                = "Position: Y";
  public static final String ARE_ACTOR_DEST_X               = "Destination: X";
  public static final String ARE_ACTOR_DEST_Y               = "Destination: Y";
  public static final String ARE_ACTOR_FLAGS                = "Flags";
  public static final String ARE_ACTOR_IS_SPAWNED           = "Is spawned?";
  public static final String ARE_ACTOR_RESREF_LETTER        = "First letter of CRE resref";
  public static final String ARE_ACTOR_DIFFICULTY           = "Difficulty";
  public static final String ARE_ACTOR_ANIMATION            = "Animation";
  public static final String ARE_ACTOR_ORIENTATION          = "Orientation";
  public static final String ARE_ACTOR_EXPIRY_TIME          = "Expiry time";
  public static final String ARE_ACTOR_WANDER_DISTANCE      = "Wander distance";
  public static final String ARE_ACTOR_FOLLOW_DISTANCE      = "Follow distance";
  public static final String ARE_ACTOR_PRESENT_AT           = "Present at";
  public static final String ARE_ACTOR_NUM_TIMES_TALKED_TO  = "# times talked to";
  public static final String ARE_ACTOR_DIALOG               = "Dialogue";
  public static final String ARE_ACTOR_SCRIPT_OVERRIDE      = "Override script";
  public static final String ARE_ACTOR_SCRIPT_SPECIAL_1     = "Special 1 script";
  public static final String ARE_ACTOR_SCRIPT_SPECIAL_2     = "Special 2 script";
  public static final String ARE_ACTOR_SCRIPT_SPECIAL_3     = "Special 3 script";
  public static final String ARE_ACTOR_SCRIPT_COMBAT        = "Combat script";
  public static final String ARE_ACTOR_SCRIPT_MOVEMENT      = "Movement script";
  public static final String ARE_ACTOR_SCRIPT_TEAM          = "Team script";
  public static final String ARE_ACTOR_SCRIPT_GENERAL       = "General script";
  public static final String ARE_ACTOR_SCRIPT_CLASS         = "Class script";
  public static final String ARE_ACTOR_SCRIPT_RACE          = "Race script";
  public static final String ARE_ACTOR_SCRIPT_DEFAULT       = "Default script";
  public static final String ARE_ACTOR_SCRIPT_SPECIFICS     = "Specifics script";
  public static final String ARE_ACTOR_SCRIPT_AREA          = "EEex: Area script";
  public static final String ARE_ACTOR_CHARACTER            = "Character";
  public static final String ARE_ACTOR_OFFSET_CRE_STRUCTURE = "CRE structure offset";
  public static final String ARE_ACTOR_SIZE_CRE_STRUCTURE   = "CRE structure size";
  public static final String ARE_ACTOR_CRE_FILE             = "CRE file";
  public static final String ARE_ACTOR_NAME_ALT             = "Alternate actor name";

  public static final String[] FLAGS_ARRAY = { "CRE attached", "CRE not attached", "Has seen party",
      "Toggle invulnerability", "Override script name" };

  public static final String[] FLAGS_IWD_ARRAY = { "CRE attached", "CRE not attached", "Has seen party",
      "Toggle invulnerability" };

  public static final String[] DIFF_ARRAY = { "None", "Level 1", "Level 2", "Level 3" };

  public Actor() throws Exception {
    super(null, ARE_ACTOR, StreamUtils.getByteBuffer(272), 0);
  }

  public Actor(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception {
    super(superStruct, ARE_ACTOR + " " + nr, buffer, offset);
  }

  // --------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove() {
    return true;
  }

  // --------------------- End Interface AddRemovable ---------------------

  // --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount() {
    return 1;
  }

  @Override
  public String getViewerTabName(int index) {
    return StructViewer.TAB_VIEW;
  }

  @Override
  public JComponent getViewerTab(int index) {
    return new ViewerActor(this);
  }

  @Override
  public boolean viewerTabAddedBefore(int index) {
    return true;
  }

  // --------------------- End Interface HasViewerTabs ---------------------

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype) {
    super.datatypeAddedInChild(child, datatype);
    if (child instanceof CreResource) {
      ((DecNumber) getAttribute(ARE_ACTOR_SIZE_CRE_STRUCTURE)).setValue(child.getSize());
    }
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype) {
    if (datatype instanceof CreResource) {
      ((DecNumber) getAttribute(ARE_ACTOR_SIZE_CRE_STRUCTURE)).setValue(0);
      ((HexNumber) getAttribute(ARE_ACTOR_OFFSET_CRE_STRUCTURE)).setValue(0);
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype) {
    super.datatypeRemovedInChild(child, datatype);
    if (child instanceof CreResource) {
      ((DecNumber) getAttribute(ARE_ACTOR_SIZE_CRE_STRUCTURE)).setValue(child.getSize());
    }
  }

  void updateCREOffset() {
    final StructEntry entry = getFields().get(getFields().size() - 1);
    if (entry instanceof CreResource) {
      ((HexNumber) getAttribute(ARE_ACTOR_OFFSET_CRE_STRUCTURE)).setValue(entry.getOffset());
    }
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new TextString(buffer, offset, 32, ARE_ACTOR_NAME));
    addField(new DecNumber(buffer, offset + 32, 2, ARE_ACTOR_POS_X));
    addField(new DecNumber(buffer, offset + 34, 2, ARE_ACTOR_POS_Y));
    addField(new DecNumber(buffer, offset + 36, 2, ARE_ACTOR_DEST_X));
    addField(new DecNumber(buffer, offset + 38, 2, ARE_ACTOR_DEST_Y));
    if (Profile.getEngine() == Profile.Engine.IWD || Profile.getEngine() == Profile.Engine.PST) {
      addField(new Flag(buffer, offset + 40, 4, ARE_ACTOR_FLAGS, FLAGS_IWD_ARRAY));
    } else {
      addField(new Flag(buffer, offset + 40, 4, ARE_ACTOR_FLAGS, FLAGS_ARRAY));
    }
    addField(new Bitmap(buffer, offset + 44, 2, ARE_ACTOR_IS_SPAWNED, OPTION_NOYES));
    addField(new TextString(buffer, offset + 46, 1, ARE_ACTOR_RESREF_LETTER));
    if (Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new Flag(buffer, offset + 47, 1, ARE_ACTOR_DIFFICULTY, DIFF_ARRAY));
    } else {
      addField(new Unknown(buffer, offset + 47, 1));
    }
    addField(new AnimateBitmap(buffer, offset + 48, 4, ARE_ACTOR_ANIMATION));
    addField(new Bitmap(buffer, offset + 52, 2, ARE_ACTOR_ORIENTATION, OPTION_ORIENTATION));
    addField(new Unknown(buffer, offset + 54, 2));
    addField(new DecNumber(buffer, offset + 56, 4, ARE_ACTOR_EXPIRY_TIME));
    addField(new DecNumber(buffer, offset + 60, 2, ARE_ACTOR_WANDER_DISTANCE));
    addField(new DecNumber(buffer, offset + 62, 2, ARE_ACTOR_FOLLOW_DISTANCE));
    addField(new Flag(buffer, offset + 64, 4, ARE_ACTOR_PRESENT_AT, OPTION_SCHEDULE));
    addField(new DecNumber(buffer, offset + 68, 4, ARE_ACTOR_NUM_TIMES_TALKED_TO));
    addField(new ResourceRef(buffer, offset + 72, ARE_ACTOR_DIALOG, "DLG"));
    addField(new ResourceRef(buffer, offset + 80, ARE_ACTOR_SCRIPT_OVERRIDE, "BCS"));
    if (Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new ResourceRef(buffer, offset + 88, ARE_ACTOR_SCRIPT_SPECIAL_3, "BCS"));
      addField(new ResourceRef(buffer, offset + 96, ARE_ACTOR_SCRIPT_SPECIAL_2, "BCS"));
      addField(new ResourceRef(buffer, offset + 104, ARE_ACTOR_SCRIPT_COMBAT, "BCS"));
      addField(new ResourceRef(buffer, offset + 112, ARE_ACTOR_SCRIPT_MOVEMENT, "BCS"));
      addField(new ResourceRef(buffer, offset + 120, ARE_ACTOR_SCRIPT_TEAM, "BCS"));
    } else {
      addField(new ResourceRef(buffer, offset + 88, ARE_ACTOR_SCRIPT_GENERAL, "BCS"));
      addField(new ResourceRef(buffer, offset + 96, ARE_ACTOR_SCRIPT_CLASS, "BCS"));
      addField(new ResourceRef(buffer, offset + 104, ARE_ACTOR_SCRIPT_RACE, "BCS"));
      addField(new ResourceRef(buffer, offset + 112, ARE_ACTOR_SCRIPT_DEFAULT, "BCS"));
      addField(new ResourceRef(buffer, offset + 120, ARE_ACTOR_SCRIPT_SPECIFICS, "BCS"));
    }
    if (buffer.get(offset + 128) == 0x2a) { // *
      addField(new TextString(buffer, offset + 128, 8, ARE_ACTOR_CHARACTER));
    } else {
      addField(new ResourceRef(buffer, offset + 128, ARE_ACTOR_CHARACTER, "CRE"));
    }
    HexNumber creOffset = new HexNumber(buffer, offset + 136, 4, ARE_ACTOR_OFFSET_CRE_STRUCTURE);
    addField(creOffset);
    DecNumber creSize = new DecNumber(buffer, offset + 140, 4, ARE_ACTOR_SIZE_CRE_STRUCTURE);
    addField(creSize);
    if (Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new ResourceRef(buffer, offset + 144, ARE_ACTOR_SCRIPT_SPECIAL_1, "BCS"));
      addField(new Unknown(buffer, offset + 152, 120));
    } else if (Profile.isEnhancedEdition()) {
      addField(new TextString(buffer, offset + 144, 32, ARE_ACTOR_NAME_ALT));
      if ((boolean) Profile.getProperty(Profile.Key.IS_GAME_EEEX)) {
        addField(new ResourceRef(buffer, offset + 176, ARE_ACTOR_SCRIPT_AREA, "BCS"));
        addField(new Unknown(buffer, offset + 184, 88));
      } else {
        addField(new Unknown(buffer, offset + 176, 96));
      }
    } else {
      addField(new Unknown(buffer, offset + 144, 128));
    }

    if (creOffset.getValue() > 0 && creSize.getValue() >= 0x2d4) {
      addField(new CreResource(this, ARE_ACTOR_CRE_FILE, buffer, creOffset.getValue()));
    }

    return offset + 272;
  }
}
