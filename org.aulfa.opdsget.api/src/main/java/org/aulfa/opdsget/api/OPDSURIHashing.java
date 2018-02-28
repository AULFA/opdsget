package org.aulfa.opdsget.api;

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
  private OPDSURIHashing()
  {

  }

  private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();

  private static String hexShow(final byte[] data)
  {
    final StringBuilder sb = new StringBuilder(data.length * 2);
    for (final byte b : data) {
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
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return hexShow(digest.digest(uri.toString().getBytes(UTF_8)));
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
