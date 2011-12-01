/**
 *  Copyright 2011 Rapleaf
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.rapleaf.hank.storage.cueball;

import com.rapleaf.hank.compress.CompressionCodec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class CueballStreamBufferMergeSort {

  private final CueballStreamBuffer[] cueballStreamBuffers;
  private final int keyHashSize;
  private final int valueSize;
  private final ValueTransformer transformer;

  public CueballStreamBufferMergeSort(CueballFilePath cueballBase,
                                      List<CueballFilePath> cueballDeltas,
                                      int keyHashSize,
                                      int valueSize,
                                      int hashIndexBits,
                                      CompressionCodec compressionCodec,
                                      ValueTransformer transformer) throws IOException {
    this.keyHashSize = keyHashSize;
    this.valueSize = valueSize;
    this.transformer = transformer;

    cueballStreamBuffers = new CueballStreamBuffer[cueballDeltas.size() + 1];

    // Open the base
    CueballStreamBuffer cueballBaseStreamBuffer = new CueballStreamBuffer(cueballBase.getPath(), 0,
        keyHashSize, valueSize, hashIndexBits, compressionCodec);
    cueballStreamBuffers[0] = cueballBaseStreamBuffer;

    // Open all the deltas
    int i = 1;
    for (CueballFilePath delta : cueballDeltas) {
      CueballStreamBuffer cueballStreamBuffer =
          new CueballStreamBuffer(delta.getPath(), i, keyHashSize, valueSize, hashIndexBits, compressionCodec);
      cueballStreamBuffers[i++] = cueballStreamBuffer;
    }
  }

  public static class KeyHashValuePair {
    public ByteBuffer keyHash;
    public ByteBuffer value;

    public KeyHashValuePair(ByteBuffer keyHash, ByteBuffer value) {
      this.keyHash = keyHash;
      this.value = value;
    }
  }

  // Return null when there is nothing more to use
  public KeyHashValuePair nextKeyValuePair() throws IOException {

    // Find the stream buffer with the next smallest key hash
    CueballStreamBuffer cueballStreamBufferToUse = null;

    for (int i = 0; i < cueballStreamBuffers.length; i++) {
      if (cueballStreamBuffers[i].anyRemaining()) {
        if (cueballStreamBufferToUse == null) {
          cueballStreamBufferToUse = cueballStreamBuffers[i];
        } else {
          int comparison = cueballStreamBufferToUse.compareTo(cueballStreamBuffers[i]);
          if (comparison == 0) {
            // If two equal key hashes are found, use the most recent value (i.e. the one from the lastest delta)
            // and skip (consume) the older ones
            cueballStreamBufferToUse.consume();
            cueballStreamBufferToUse = cueballStreamBuffers[i];
          } else if (comparison == 1) {
            // Found a stream buffer with a smaller key hash
            cueballStreamBufferToUse = cueballStreamBuffers[i];
          }
        }
      }
    }

    if (cueballStreamBufferToUse == null) {
      // Nothing more to read
      return null;
    }

    // Transform if necessary
    if (transformer != null) {
      transformer.transform(cueballStreamBufferToUse.getBuffer(),
          cueballStreamBufferToUse.getCurrentOffset() + keyHashSize,
          cueballStreamBufferToUse.getIndex());
    }

    // Get next key hash and value
    final ByteBuffer keyHash = ByteBuffer.wrap(cueballStreamBufferToUse.getBuffer(),
        cueballStreamBufferToUse.getCurrentOffset(), keyHashSize);
    final ByteBuffer valueBytes = ByteBuffer.wrap(cueballStreamBufferToUse.getBuffer(),
        cueballStreamBufferToUse.getCurrentOffset() + keyHashSize, valueSize);

    cueballStreamBufferToUse.consume();

    return new KeyHashValuePair(keyHash, valueBytes);
  }

  public void close() throws IOException {
    // Close all buffers
    for (CueballStreamBuffer cueballStreamBuffer : cueballStreamBuffers) {
      cueballStreamBuffer.close();
    }
  }
}
