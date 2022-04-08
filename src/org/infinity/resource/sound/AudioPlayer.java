// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioPlayer {
  private static final byte BUFFER[] = new byte[8196];

  private AudioFormat audioFormat;
  private SourceDataLine dataLine;
  private boolean playing = true;
  private boolean stopped = true;

  /**
   * Starts playback of audio data associated with the specified audio buffer.
   *
   * @param audioBuffer AudioBuffer object containing audio data.
   * @throws Exception On error
   */
  public void play(AudioBuffer audioBuffer) throws Exception {
    if (audioBuffer == null || audioBuffer.getAudioData() == null) {
      return;
    }

    setPlaying(true);
    setStopped(false);
    try (AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioBuffer.getAudioData()))) {
      if (dataLine == null || !ais.getFormat().matches(audioFormat)) {
        audioFormat = ais.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
          throw new UnsupportedAudioFileException("Unsupported audio format");
        }
        dataLine = (SourceDataLine) AudioSystem.getLine(info);
        dataLine.open(ais.getFormat(), 16384);
      }
      dataLine.start();

      while (isPlaying()) {
        int numBytesRead = ais.read(BUFFER, 0, BUFFER.length);
        if (numBytesRead < 0) {
          break;
        }
        dataLine.write(BUFFER, 0, numBytesRead);
      }
    } catch (Exception e) {
      setStopped(true);
      throw e;
    }

    if (!isPlaying()) {
      dataLine.drain();
    }

    setStopped(true);
  }

  /**
   * Stops audio playback.
   */
  public void stopPlay() {
    setPlaying(false);
    while (!isStopped()) {
      try {
        Thread.sleep(50L);
      } catch (InterruptedException e) {
      }
    }
    try {
      Thread.sleep(150L);
    } catch (InterruptedException e) {
    }
    dataLine = null;
  }

  private synchronized void setPlaying(boolean b) {
    if (b != playing) {
      playing = b;
    }
  }

  private boolean isPlaying() {
    return playing;
  }

  private synchronized void setStopped(boolean b) {
    if (b != stopped) {
      stopped = b;
    }
  }

  private boolean isStopped() {
    return stopped;
  }
}
