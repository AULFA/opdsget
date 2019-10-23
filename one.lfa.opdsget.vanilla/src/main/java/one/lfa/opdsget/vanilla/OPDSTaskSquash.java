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

import one.lfa.epubsquash.api.EPUBSquasherConfiguration;
import one.lfa.epubsquash.api.EPUBSquasherProviderType;
import one.lfa.opdsget.api.OPDSGetConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static one.lfa.opdsget.vanilla.OPDSManifestFileEntryKind.GENERAL;

final class OPDSTaskSquash
{
  private static final Logger LOG = LoggerFactory.getLogger(OPDSTaskSquash.class);

  private final OPDSGetConfiguration configuration;
  private final EPUBSquasherProviderType squashers;
  private final OPDSManifestChangeRequiredType onChangeRequired;

  private OPDSTaskSquash(
    final OPDSGetConfiguration inConfiguration,
    final EPUBSquasherProviderType inSquashers,
    final OPDSManifestChangeRequiredType inOnChangeRequired)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.squashers =
      Objects.requireNonNull(inSquashers, "squashers");
    this.onChangeRequired =
      Objects.requireNonNull(inOnChangeRequired, "onChangeRequired");
  }

  public static CompletionStage<Void> task(
    final OPDSGetConfiguration configuration,
    final EPUBSquasherProviderType inSquashers,
    final OPDSManifestChangeRequiredType onChangeRequired,
    final ExecutorService executor)
  {
    return CompletableFuture.runAsync(
      () -> new OPDSTaskSquash(configuration, inSquashers, onChangeRequired).execute(),
      executor);
  }

  private void execute()
  {
    LOG.debug("executing epub squashing task");

    try {
      final var squash_opt = this.configuration.squash();
      if (!squash_opt.isPresent()) {
        return;
      }

      final var squash = squash_opt.get();
      if (!Files.isDirectory(this.configuration.bookDirectory())) {
        return;
      }

      final var epubs =
        Files.list(this.configuration.bookDirectory())
          .collect(Collectors.toList());

      for (final var epub : epubs) {
        LOG.info("squash: {}", epub);

        try {
          final var squasher =
            this.squashers.createSquasher(
              EPUBSquasherConfiguration.builder()
                .setInputFile(epub)
                .setTemporaryDirectory(Files.createTempDirectory("opdsget-retriever-"))
                .setOutputFile(epub)
                .setScale(squash.scaleFactor())
                .setMaximumImageHeight(squash.maximumImageHeight())
                .setMaximumImageWidth(squash.maximumImageWidth())
                .build());

          squasher.squash();
          this.onChangeRequired.onFileChanged(GENERAL, epub.toAbsolutePath());
        } catch (final Exception e) {
          LOG.error("failed to squash {}: ", epub, e);

          if (System.getProperty("one.lfa.epubsquash.unsupported.IgnoreSquashErrors") != null) {
            LOG.warn("ignoring epubsquash errors");
            continue;
          }

          throw e;
        }
      }
    } catch (final Exception e) {
      throw new CompletionException(e);
    }
  }
}
