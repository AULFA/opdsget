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

import one.lfa.opdsget.api.OPDSDocumentProcessed;
import one.lfa.opdsget.api.OPDSGetConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static one.lfa.opdsget.vanilla.OPDSManifestFileEntryKind.SEARCH_INDEX;

final class OPDSTaskIndex
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OPDSTaskIndex.class);

  private static final Pattern WHITESPACE =
    Pattern.compile("\\s+");
  private static final Pattern NOT_UPPERCASE_ALPHA_NUMERIC =
    Pattern.compile("[^\\p{Lu}\\p{Digit}\\p{sc=Myanmar}\\p{sc=Lao}]+");

  private final OPDSGetConfiguration configuration;
  private final OPDSManifestChangeRequiredType onChangeRequired;
  private final Map<URI, OPDSDocumentProcessed> processed;

  private OPDSTaskIndex(
    final OPDSGetConfiguration inConfiguration,
    final OPDSManifestChangeRequiredType inOnChangeRequired,
    final Map<URI, OPDSDocumentProcessed> inProcessed)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.onChangeRequired =
      Objects.requireNonNull(inOnChangeRequired, "onChangeRequired");
    this.processed =
      Objects.requireNonNull(inProcessed, "processed");
  }

  public static CompletionStage<Void> task(
    final OPDSGetConfiguration configuration,
    final Map<URI, OPDSDocumentProcessed> processed,
    final OPDSManifestChangeRequiredType onChangeRequired,
    final ExecutorService executor)
  {
    return CompletableFuture.runAsync(
      () -> new OPDSTaskIndex(configuration, onChangeRequired, processed).execute(),
      executor);
  }

  private void execute()
  {
    LOG.debug("executing indexing task");

    try {
      final var rewriter = this.configuration.uriRewriter();
      final Map<String, List<URI>> index = new TreeMap<>();
      for (final var mapEntry : this.processed.entrySet()) {
        final var document = mapEntry.getValue();
        if (document.isEntry()) {
          final var terms = List.of(WHITESPACE.split(document.title().toUpperCase()));
          for (final var term : terms) {
            final var termTrimmed =
              term.trim().replaceAll(NOT_UPPERCASE_ALPHA_NUMERIC.pattern(), "");
            if (!termTrimmed.isBlank()) {
              var uris = index.get(termTrimmed);
              if (uris == null) {
                uris = new ArrayList<>(8);
              }

              final var rewriteURI = rewriter.rewrite(Optional.empty(), document.file());
              uris.add(rewriteURI);
              index.put(termTrimmed, uris);
            }
          }
        }
      }

      final var indexPath = this.configuration.output().resolve("index.txt");
      LOG.info("index {}", indexPath);

      try (var writer = Files.newBufferedWriter(indexPath, UTF_8, TRUNCATE_EXISTING, CREATE)) {
        for (final var term : index.keySet()) {
          for (final var uri : index.get(term)) {
            writer.append(term);
            writer.append(" ");
            writer.append(uri.toString());
            writer.append('\n');
          }
        }
        writer.flush();
      }

      this.onChangeRequired.onFileChanged(SEARCH_INDEX, indexPath);
    } catch (final Exception e) {
      throw new CompletionException(e);
    }
  }
}
