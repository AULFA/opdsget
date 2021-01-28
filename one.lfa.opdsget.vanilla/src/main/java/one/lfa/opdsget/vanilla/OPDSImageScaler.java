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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static one.lfa.opdsget.vanilla.OPDSManifestFileEntryKind.GENERAL;

/**
 * An image scaler that can change the sizes of images in a directory.
 */

public final class OPDSImageScaler
{
  private static final Logger LOG = LoggerFactory.getLogger(OPDSImageScaler.class);

  private final Path directory;
  private final OPDSManifestChangeRequiredType changeRequired;
  private final double scale;

  /**
   * Construct a new scaler.
   *
   * @param inDirectory      The input directory
   * @param inChangeRequired A function called when an image is scaled
   * @param inScale          The scaling factor
   */

  public OPDSImageScaler(
    final Path inDirectory,
    final OPDSManifestChangeRequiredType inChangeRequired,
    final double inScale)
  {
    this.directory =
      Objects.requireNonNull(inDirectory, "directory");
    this.changeRequired =
      Objects.requireNonNull(inChangeRequired, "changeRequired");

    this.scale = inScale;
  }

  private static BufferedImage resize(
    final BufferedImage image,
    final int newWidth,
    final int newHeight)
  {
    final var tmp =
      image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
    final var targetImage =
      new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

    final var g2d = targetImage.createGraphics();
    g2d.drawImage(tmp, 0, 0, null);
    g2d.dispose();
    return targetImage;
  }

  /**
   * Execute the scaler.
   *
   * @throws IOException On I/O errors
   */

  public void execute()
    throws IOException
  {
    if (this.scale == 1.0) {
      LOG.info("scaling value is {}, so no scaling required", Double.valueOf(this.scale));
      return;
    }

    for (final var name : ImageIO.getWriterFormatNames()) {
      LOG.debug("format: {}", name);
    }

    try (var directoryStream = Files.newDirectoryStream(this.directory)) {
      final var iterator = directoryStream.iterator();
      while (iterator.hasNext()) {
        final var path = iterator.next();

        LOG.debug("scale {}", path);

        final Path temp;
        try (var stream = new BufferedInputStream(Files.newInputStream(path), 8192)) {
          final var image = ImageIO.read(stream);
          if (image != null) {
            final var originalWidth = image.getWidth();
            final var width = (double) originalWidth * this.scale;
            final var originalHeight = image.getHeight();
            final var height = (double) originalHeight * this.scale;

            LOG.info(
              "scale {} {}x{} -> {}x{}", path,
              Integer.valueOf(originalWidth),
              Integer.valueOf(originalHeight),
              Double.valueOf(width),
              Double.valueOf(height));

            temp = path.resolveSibling(path.getFileName() + ".tmp");
            final var newImage = resize(image, (int) width, (int) height);
            final var result = ImageIO.write(newImage, "jpg", temp.toFile());
            if (!result) {
              throw new IOException("ImageIO.write returned false!");
            }
          } else {
            LOG.error("unable to parse image {}", path);
            temp = path;
          }
        }
        Files.move(temp, path, REPLACE_EXISTING, ATOMIC_MOVE);
        this.changeRequired.onFileChanged(GENERAL, path);
      }
    }
  }
}
