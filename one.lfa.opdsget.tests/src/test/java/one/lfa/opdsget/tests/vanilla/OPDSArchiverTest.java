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

package one.lfa.opdsget.tests.vanilla;

import one.lfa.opdsget.vanilla.OPDSArchiver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.charset.StandardCharsets.*;

public final class OPDSArchiverTest
{
  private Path output;
  private Path file;
  private Path file_tmp;

  @Rule public ExpectedException expected = ExpectedException.none();

  @Before
  public void setUp()
    throws IOException
  {
    this.output = Files.createTempDirectory("opdsget-tests-");
    this.file = Files.createTempFile("output", ".zip");
    this.file_tmp = Files.createTempFile("output", ".zip.tmp");
  }

  @After
  public void tearDown()
  {

  }

  @Test
  public void testArchiverEmptyTempExists()
    throws Exception
  {
    this.expected.expect(FileAlreadyExistsException.class);
    OPDSArchiver.createArchive(this.output, this.file, this.file_tmp);
  }

  @Test
  public void testArchiverEmpty()
    throws Exception
  {
    Files.deleteIfExists(this.file_tmp);

    OPDSArchiver.createArchive(this.output, this.file, this.file_tmp);

    Assert.assertTrue(
      this.file + " must exist", Files.exists(this.file));
    Assert.assertTrue(
      this.file_tmp + " must not exist", !Files.exists(this.file_tmp));

    final var zip = new ZipFile(this.file.toFile());
    final List<ZipEntry> entries =
      zip.stream()
        .collect(Collectors.toList());

    Assert.assertEquals("Must have no entries", 0L, (long) entries.size());
  }

  @Test
  public void testArchiverBasic()
    throws Exception
  {
    Files.deleteIfExists(this.file_tmp);

    Files.write(this.output.resolve("hello0.txt"), "Hello 0".getBytes(UTF_8));
    Files.write(this.output.resolve("hello1.txt"), "Hello 1".getBytes(UTF_8));
    Files.write(this.output.resolve("hello2.txt"), "Hello 2".getBytes(UTF_8));

    OPDSArchiver.createArchive(this.output, this.file, this.file_tmp);

    final var zip = new ZipFile(this.file.toFile());
    final List<ZipEntry> entries =
      zip.stream()
        .collect(Collectors.toList());

    Assert.assertTrue(entries.get(0).getName().contains("hello0.txt"));
    Assert.assertTrue(entries.get(1).getName().contains("hello1.txt"));
    Assert.assertTrue(entries.get(2).getName().contains("hello2.txt"));

    Assert.assertEquals(
      "Hello 0",
      new String(zip.getInputStream(entries.get(0)).readAllBytes(), UTF_8));
    Assert.assertEquals(
      "Hello 1",
      new String(zip.getInputStream(entries.get(1)).readAllBytes(), UTF_8));
    Assert.assertEquals(
      "Hello 2",
      new String(zip.getInputStream(entries.get(2)).readAllBytes(), UTF_8));
  }
}
