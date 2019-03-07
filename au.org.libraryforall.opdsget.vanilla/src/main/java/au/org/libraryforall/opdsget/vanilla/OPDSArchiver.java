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

package au.org.libraryforall.opdsget.vanilla;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

/**
 * Functions to create zip archives.
 */

public final class OPDSArchiver
{
  private static final Logger LOG = LoggerFactory.getLogger(OPDSArchiver.class);

  private OPDSArchiver()
  {

  }

  /**
   * Create a zip archive of the given directory. The zip file will be written
   * to {@code output_tmp} and then atomically renamed to {@code output}.
   *
   * @param directory  The directory
   * @param output     The output archive
   * @param output_tmp The temporary file for the output archive
   *
   * @throws IOException On I/O errors
   */

  public static void createArchive(
    final Path directory,
    final Path output,
    final Path output_tmp)
    throws IOException
  {
    Objects.requireNonNull(directory, "directory");
    Objects.requireNonNull(output, "output");
    Objects.requireNonNull(output_tmp, "output_tmp");

    final List<Path> files =
      Files.walk(directory)
        .filter(path -> Files.isRegularFile(path))
        .map(directory::relativize)
        .sorted()
        .collect(Collectors.toList());

    /*
     * Use a fixed time for all zip entries in an attempt to ensure that
     * the produced zip is byte-for-byte reproducible.
     */

    final Instant instant = Instant.parse("2001-01-01T00:00:00.00Z");
    final FileTime time = FileTime.from(instant);

    LOG.debug("create {}", output_tmp);

    try (ZipOutputStream stream =
           new ZipOutputStream(Files.newOutputStream(output_tmp, CREATE_NEW))) {
      for (final Path path : files) {
        compressFile(directory, time, stream, path);
      }
    }

    LOG.debug("rename {} -> {}", output_tmp, output);

    Files.move(
      output_tmp,
      output,
      StandardCopyOption.REPLACE_EXISTING,
      StandardCopyOption.ATOMIC_MOVE);

    Files.deleteIfExists(output_tmp);
  }

  private static void compressFile(
    final Path directory,
    final FileTime time,
    final ZipOutputStream stream,
    final Path path)
    throws IOException
  {
    LOG.debug("compress {}", path);

    final Path real_path = directory.resolve(path).toAbsolutePath();

    final ZipEntry entry = new ZipEntry(path.toString());
    entry.setComment("");
    entry.setCreationTime(time);
    entry.setLastAccessTime(time);
    entry.setLastModifiedTime(time);
    entry.setSize(Files.size(real_path));

    stream.putNextEntry(entry);
    try (InputStream input = Files.newInputStream(real_path)) {
      input.transferTo(stream);
    }
    stream.closeEntry();
  }
}
