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
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

final class OPDSTaskImageScale
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OPDSTaskImageScale.class);

  private final OPDSGetConfiguration configuration;
  private final OPDSManifestChangeRequiredType changeRequired;

  private OPDSTaskImageScale(
    final OPDSGetConfiguration inConfiguration,
    final OPDSManifestChangeRequiredType inChangeRequired)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.changeRequired =
      Objects.requireNonNull(inChangeRequired, "inChangeRequired");
  }

  public static CompletionStage<Void> task(
    final OPDSGetConfiguration configuration,
    final OPDSManifestChangeRequiredType inChangeRequired,
    final ExecutorService executor)
  {
    return CompletableFuture.runAsync(
      () -> new OPDSTaskImageScale(configuration, inChangeRequired).execute(),
      executor);
  }

  private void execute()
  {
    LOG.debug("executing image scale task");

    final var scaleOpt = this.configuration.scaleImages();
    if (scaleOpt.isEmpty()) {
      LOG.debug("no scaling value specified, no scaling will be performed");
      return;
    }

    try {
      final var scaler =
        new OPDSImageScaler(
          this.configuration.imageDirectory(),
          this.changeRequired,
          scaleOpt.getAsDouble());
      scaler.execute();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
