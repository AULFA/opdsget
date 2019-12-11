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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

final class OPDSTaskArchive
{
  private static final Logger LOG = LoggerFactory.getLogger(OPDSTaskArchive.class);

  private final OPDSGetConfiguration configuration;

  private OPDSTaskArchive(
    final OPDSGetConfiguration inConfiguration)
  {
    this.configuration = Objects.requireNonNull(inConfiguration, "configuration");
  }

  public static CompletionStage<Void> task(
    final OPDSGetConfiguration configuration,
    final ExecutorService executor)
  {
    return CompletableFuture.runAsync(
      () -> new OPDSTaskArchive(configuration).execute(),
      executor);
  }

  private static Path temporaryFile(final Path path)
  {
    return Paths.get(new StringBuilder(64)
                       .append(path.toString())
                       .append(".tmp")
                       .toString());
  }

  private void execute()
  {
    LOG.debug("executing archive task");

    try {
      final var archive_opt = this.configuration.outputArchive();
      if (archive_opt.isPresent()) {
        final var archive = archive_opt.get();
        LOG.info("zip {} -> {}", this.configuration.output(), archive);
        OPDSArchiver.createArchive(
          this.configuration.output(),
          archive,
          temporaryFile(archive));
      }
    } catch (final IOException e) {
      throw new CompletionException(e);
    }
  }
}
