/*
 * Copyright © 2018 Library For All
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package au.org.libraryforall.opdsget.api;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Functions to produce hashes of URIs.
 */

public final class OPDSURIHashing
{
  private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();

  private OPDSURIHashing()
  {

  }

  private static String hexShow(final byte[] data)
  {
    final var sb = new StringBuilder(data.length * 2);
    for (final var b : data) {
      sb.append(HEX_CODE[((int) b >> 4) & 0xF]);
      sb.append(HEX_CODE[((int) b & 0xF)]);
    }
    return sb.toString();
  }

  /**
   * @param uri The input URI
   *
   * @return A fixed-length collision-resistant hash of the given URI
   */

  public static String hashOf(final URI uri)
  {
    Objects.requireNonNull(uri, "uri");

    try {
      final var digest = MessageDigest.getInstance("SHA-256");
      return hexShow(digest.digest(uri.toString().getBytes(UTF_8)));
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
