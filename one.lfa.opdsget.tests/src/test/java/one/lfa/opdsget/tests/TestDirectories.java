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

package one.lfa.opdsget.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class TestDirectories
{
  private TestDirectories()
  {

  }

  public static Path temporaryDirectory()
    throws IOException
  {
    final var dir = temporaryBaseDirectory();
    return Files.createTempDirectory(dir, "opdsget-");
  }

  public static Path temporaryBaseDirectory()
    throws IOException
  {
    final var tmpBase = System.getProperty("java.io.tmpdir");
    final var path0 = Paths.get(tmpBase);
    final var path1 = path0.resolve("opdsget");
    Files.createDirectories(path1);
    return path1;
  }
}
