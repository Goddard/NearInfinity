// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.infinity.util.ArrayUtil;
import org.infinity.util.DynamicArray;
import org.infinity.util.io.StreamUtils;

public final class Compressor {
  /**
   * Compresses the data of the specified ByteBuffer object and creates a simple header.
   *
   * @param buffer    Contains the data to compress.
   * @param signature Signature ID for the header.
   * @param version   Version ID for the header.
   * @return The compressed data including header as {@link ByteBuffer}.
   */
  public static ByteBuffer compress(ByteBuffer buffer, String signature, String version) {
    ByteBuffer retVal = null;
    if (buffer != null && buffer.remaining() > 0 && signature.length() == 4 && version.length() == 4) {
      buffer.position(0);
      byte[] data = new byte[buffer.remaining()];
      buffer.get(data);
      data = compress(data, 0, data.length, false);
      if (data != null) {
        retVal = StreamUtils.getByteBuffer(12 + data.length);
        retVal.put(signature.getBytes());
        retVal.put(version.getBytes());
        retVal.putInt(buffer.limit());
        retVal.put(data);
        retVal.position(0);
      }
    }
    return retVal;
  }

  /**
   * Compresses the specified data and creates a simple header.
   *
   * @param data      The data to compress
   * @param signature Signature ID for the header.
   * @param version   Version ID for the header.
   * @return The compressed data including header.
   */
  public static byte[] compress(byte data[], String signature, String version) {
    byte header[] = ArrayUtil.mergeArrays(signature.getBytes(), version.getBytes());
    header = ArrayUtil.mergeArrays(header, DynamicArray.convertInt(data.length));
    byte[] result = compress(data, 0, data.length, false);
    if (result != null) {
      byte[] output = new byte[header.length + result.length];
      System.arraycopy(header, 0, output, 0, header.length);
      System.arraycopy(result, 0, output, header.length, result.length);
      return output;
    }
    return null;
  }

  /**
   * Compresses the specified data without creating a header.
   *
   * @param data        The data block to compress.
   * @param ofs         Start offset of the data.
   * @param len         Length of data to compress in bytes.
   * @param prependSize If {@code true} the uncompressed size will be written to the output block right before the
   *                    compressed data.
   * @return The compressed data as byte array.
   */
  public static byte[] compress(byte[] data, int ofs, int len, boolean prependSize) {
    byte[] result = null;
    if (data != null && ofs >= 0 && ofs < data.length) {
      if (ofs + len > data.length) {
        len = data.length - ofs;
      }
      int dstOfs = 0;
      result = new byte[len * 2];
      if (prependSize) {
        dstOfs = 4;
        DynamicArray.putInt(result, 0, len);
      }
      Deflater deflater = new Deflater();
      deflater.setInput(data, ofs, len);
      deflater.finish();
      int clength = deflater.deflate(result, dstOfs, result.length - dstOfs);
      return Arrays.copyOfRange(result, 0, clength + dstOfs);
    }
    return result;
  }

  public static ByteBuffer decompress(ByteBuffer buffer) throws IOException {
    return decompress(buffer, 8);
  }

  public static ByteBuffer decompress(ByteBuffer buffer, int offset) throws IOException {
    ByteBuffer retVal = null;
    if (buffer != null && offset < buffer.limit()) {
      byte[] src = StreamUtils.toArray(buffer);
      byte[] dst = decompress(src, offset);
      retVal = StreamUtils.getByteBuffer(dst);
    }
    return retVal;
  }

  public static byte[] decompress(byte buffer[]) throws IOException {
    return decompress(buffer, 8);
  }

  public static byte[] decompress(byte buffer[], int ofs) throws IOException {
    Inflater inflater = new Inflater();
    byte result[] = new byte[DynamicArray.getInt(buffer, ofs)];
    ofs += 4;
    inflater.setInput(buffer, ofs, buffer.length - ofs);
    try {
      inflater.inflate(result);
    } catch (DataFormatException e) {
      throw new IOException();
    }
    inflater.reset();
    return result;
  }

  private Compressor() {
  }
}
