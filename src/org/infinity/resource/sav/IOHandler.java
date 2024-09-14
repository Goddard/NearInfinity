// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sav;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.infinity.datatype.TextString;
import org.infinity.resource.Profile;
import org.infinity.resource.Writeable;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.FileDeletionHook;
import org.infinity.util.Logger;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.StreamUtils;

public final class IOHandler implements Writeable {
  private final ResourceEntry entry;
  private final TextString header;
  private final List<SavResourceEntry> fileEntries;

  private Path tempFolder;

  public IOHandler(ResourceEntry entry, boolean sortByName) throws Exception {
    this.entry = entry;
    ByteBuffer buffer = entry.getResourceBuffer(true); // ignoreOverride - no real effect
    header = new TextString(buffer, 0, 8, null);
    if (!header.getText().equals("SAV V1.0")) {
      throw new UnsupportedOperationException("Unsupported version: " + header);
    }
    fileEntries = new ArrayList<>();
    int offset = 8;
    while (offset < buffer.limit()) {
      SavResourceEntry fileEntry = new SavResourceEntry(buffer, offset);
      fileEntries.add(fileEntry);
      offset = fileEntry.getEndOffset();
    }
    if (sortByName) {
      Collections.sort(fileEntries);
    }
  }

  // --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException {
    header.write(os);
    for (final SavResourceEntry entry : fileEntries) {
      entry.write(os);
    }
  }

  // --------------------- End Interface Writeable ---------------------

  public void close() {
    if (tempFolder != null && FileEx.create(tempFolder).isDirectory()) {
      try (DirectoryStream<Path> dstream = Files.newDirectoryStream(tempFolder)) {
        for (final Path file : dstream) {
          try {
            Files.delete(file);
          } catch (AccessDeniedException e) {
            FileDeletionHook.getInstance().registerFile(file);
          }
        }
      } catch (IOException e) {
        Logger.error(e);
      }
      try {
        Files.delete(tempFolder);
      } catch (IOException e) {
        FileDeletionHook.getInstance().registerFile(tempFolder);
        Logger.error(e);
      }
      tempFolder = null;
    }
  }

  public void compress(List<? extends ResourceEntry> entries) throws Exception {
    fileEntries.clear();
    for (final ResourceEntry entry : entries) {
      fileEntries.add(new SavResourceEntry(entry));
    }
    close();
  }

  public List<ResourceEntry> decompress() throws Exception {
    tempFolder = createTempFolder();
    if (tempFolder == null) {
      throw new Exception("Unable to create temp folder");
    }
    Files.createDirectory(tempFolder);

    // placing content of .sav resource in the temporary folder
    final List<ResourceEntry> entries = new ArrayList<>(fileEntries.size());
    for (final SavResourceEntry entry : fileEntries) {
      Path file = tempFolder.resolve(entry.getResourceName());
      try (OutputStream os = StreamUtils.getOutputStream(file, true)) {
        StreamUtils.writeBytes(os, entry.decompress());
      }
      entries.add(new FileResourceEntry(file));
    }

    // placing copy of associated .gam resource in the temporary folder
    String gamFile = Profile.getProperty(Profile.Key.GET_GAM_NAME);
    Path srcFile = entry.getActualPath().getParent().resolve(gamFile);
    if (Files.isRegularFile(srcFile)) {
      Path dstFile = tempFolder.resolve(gamFile);
      Files.copy(srcFile, dstFile);
    }

    return entries;
  }

  public List<SavResourceEntry> getFileEntries() {
    return fileEntries;
  }

  public Path getTempFolder() {
    return tempFolder;
  }

  /** Create a unique temp folder for current baldur.sav. */
  private Path createTempFolder() {
    for (int idx = 0; idx < Integer.MAX_VALUE; idx++) {
      Path path = Profile.getHomeRoot().resolve(String.format("%s.%03d", entry.getTreeFolderName(), idx));
      if (!FileEx.create(path).exists()) {
        return path;
      }
    }
    return null;
  }
}
