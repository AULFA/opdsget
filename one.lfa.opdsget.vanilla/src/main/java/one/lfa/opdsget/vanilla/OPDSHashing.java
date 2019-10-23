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

package one.lfa.opdsget.vanilla;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class OPDSHashing
{
  private OPDSHashing()
  {

  }

  static byte[] sha256HashOf(
    final Path file)
    throws IOException
  {
    try (var stream = Files.newInputStream(file)) {
      final var digest = MessageDigest.getInstance("SHA-256");
      final var buffer = new byte[4096];
      while (true) {
        final var r = stream.read(buffer);
        if (r == -1) {
          break;
        }
        digest.update(buffer, 0, r);
      }
      return digest.digest();
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
