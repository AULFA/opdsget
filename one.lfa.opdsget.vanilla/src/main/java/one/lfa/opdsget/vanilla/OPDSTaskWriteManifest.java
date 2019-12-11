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

import one.lfa.opdsget.api.OPDSGetConfiguration;
import one.lfa.opdsget.api.OPDSManifestDescription;
import one.lfa.opdsget.api.OPDSManifestWriterProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

final class OPDSTaskWriteManifest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OPDSTaskWriteManifest.class);

  private final OPDSGetConfiguration configuration;
  private final OPDSManifestWriterProviderType manifestWriters;
  private final OPDSManifestDescription manifest;

  private OPDSTaskWriteManifest(
    final OPDSGetConfiguration inConfiguration,
    final OPDSManifestWriterProviderType inManifestWriters,
    final OPDSManifestDescription inManifest)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.manifestWriters =
      Objects.requireNonNull(inManifestWriters, "manifestWriters");
    this.manifest =
      Objects.requireNonNull(inManifest, "manifest");
  }

  public static CompletionStage<Void> task(
    final OPDSGetConfiguration configuration,
    final OPDSManifestWriterProviderType manifestWriters,
    final OPDSManifestDescription inManifest,
    final ExecutorService executor)
  {
    return CompletableFuture.runAsync(
      () -> new OPDSTaskWriteManifest(configuration, manifestWriters, inManifest).execute(),
      executor);
  }

  private void execute()
  {
    LOG.debug("executing manifest writing task");

    final var manifestPath = this.configuration.output().resolve("manifest.xml");
    LOG.info("manifest file {}", manifestPath);

    try (var stream = Files.newOutputStream(manifestPath, TRUNCATE_EXISTING, CREATE)) {
      try (var writer = this.manifestWriters.createWriter(this.manifest, stream)) {
        writer.write();
        stream.flush();
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
