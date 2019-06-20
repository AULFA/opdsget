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

import au.org.libraryforall.epubsquash.api.EPUBSquasherConfiguration;
import au.org.libraryforall.epubsquash.api.EPUBSquasherProviderType;
import au.org.libraryforall.opdsget.api.OPDSDocumentProcessed;
import au.org.libraryforall.opdsget.api.OPDSGetConfiguration;
import au.org.libraryforall.opdsget.api.OPDSHTTPDefault;
import au.org.libraryforall.opdsget.api.OPDSHTTPType;
import au.org.libraryforall.opdsget.api.OPDSRetrieverProviderType;
import au.org.libraryforall.opdsget.api.OPDSRetrieverType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * The default retriever provider.
 */

public final class OPDSRetrievers implements OPDSRetrieverProviderType
{
  private static final Logger LOG = LoggerFactory.getLogger(OPDSRetrievers.class);

  private final OPDSHTTPType http;
  private final OPDSXMLParsers parsers;
  private final EPUBSquasherProviderType squashers;

  private OPDSRetrievers(
    final EPUBSquasherProviderType in_squashers,
    final OPDSHTTPType in_http)
  {
    this.http = Objects.requireNonNull(in_http, "http");
    this.squashers = Objects.requireNonNull(in_squashers, "squashers");
    this.parsers = new OPDSXMLParsers();
  }

  /**
   * @return A retriever provider
   */

  public static OPDSRetrieverProviderType provider()
  {
    return new OPDSRetrievers(
      ServiceLoader.load(EPUBSquasherProviderType.class)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No available epub squasher provider")),
      new OPDSHTTPDefault());
  }

  /**
   * @param in_squashers A set of EPUB squashers
   * @param in_http      An HTTP request handler
   *
   * @return A retriever provider
   */

  public static OPDSRetrieverProviderType providerWith(
    final EPUBSquasherProviderType in_squashers,
    final OPDSHTTPType in_http)
  {
    return new OPDSRetrievers(in_squashers, in_http);
  }

  @Override
  public OPDSRetrieverType create(final ExecutorService executor)
  {
    return new Retriever(executor, this.parsers, this.http, this.squashers);
  }

  private static final class Retriever implements OPDSRetrieverType
  {
    private final ExecutorService executor;
    private final OPDSHTTPType http;
    private final EPUBSquasherProviderType squashers;
    private final OPDSXMLParsers parsers;

    Retriever(
      final ExecutorService in_executor,
      final OPDSXMLParsers in_parsers,
      final OPDSHTTPType in_http,
      final EPUBSquasherProviderType in_squashers)
    {
      this.executor =
        Objects.requireNonNull(in_executor, "executor");
      this.parsers =
        Objects.requireNonNull(in_parsers, "parsers");
      this.http =
        Objects.requireNonNull(in_http, "http");
      this.squashers =
        Objects.requireNonNull(in_squashers, "squashers");
    }

    @Override
    public CompletableFuture<Void> retrieve(
      final OPDSGetConfiguration in_configuration)
    {
      Objects.requireNonNull(in_configuration, "configuration");

      final var retrieval =
        new Retrieval(in_configuration, this.executor, this.http, this.parsers, this.squashers);

      return retrieval
        .processFeed(Optional.empty(), in_configuration.remoteURI())
        .thenCompose(ignored -> retrieval.indexTask())
        .thenCompose(ignored -> retrieval.squashTask())
        .thenCompose(ignored -> retrieval.scaleImagesTask())
        .thenCompose(ignored -> retrieval.archiveFeedTask());
    }
  }

  private static final class Retrieval
  {
    private static final Pattern WHITESPACE =
      Pattern.compile("\\s+");
    private static final Pattern NOT_UPPERCASE_ALPHA_NUMERIC =
      Pattern.compile("[^\\p{Lu}\\p{Digit}]+");

    private final OPDSGetConfiguration configuration;
    private final ExecutorService executor;
    private final OPDSHTTPType http;
    private final OPDSXMLParsers parsers;
    private final HashSet<URI> retrieved;
    private final Map<URI, OPDSDocumentProcessed> processed;
    private final Object processed_lock;
    private final EPUBSquasherProviderType squashers;

    Retrieval(
      final OPDSGetConfiguration in_configuration,
      final ExecutorService in_executor,
      final OPDSHTTPType in_http,
      final OPDSXMLParsers in_parsers,
      final EPUBSquasherProviderType in_squashers)
    {
      this.configuration =
        Objects.requireNonNull(in_configuration, "configuration");
      this.executor =
        Objects.requireNonNull(in_executor, "executor");
      this.http =
        Objects.requireNonNull(in_http, "http");
      this.parsers =
        Objects.requireNonNull(in_parsers, "parsers");
      this.squashers =
        Objects.requireNonNull(in_squashers, "squashers");

      this.retrieved = new HashSet<>(128);
      this.processed = new HashMap<>(128);
      this.processed_lock = new Object();
    }

    private static Path temporaryFile(final Path path)
    {
      return Paths.get(new StringBuilder(64)
                         .append(path.toString())
                         .append(".tmp")
                         .toString());
    }

    private CompletableFuture<Void> processFeed(
      final Optional<OPDSDocumentProcessed> document,
      final URI uri)
    {
      LOG.debug("processFeed: {}", uri);

      return CompletableFuture
        .supplyAsync(() -> this.processOne(document, uri), this.executor)
        .thenComposeAsync(this::runSubTasksIfNecessary, ForkJoinPool.commonPool());
    }

    private CompletableFuture<Void> runSubTasksIfNecessary(
      final Optional<OPDSDocumentProcessed> next)
    {
      return next.map(this::runSubTasks)
        .orElse(CompletableFuture.completedFuture(null));
    }

    private CompletableFuture<Void> runSubTasks(
      final OPDSDocumentProcessed document)
    {
      final var feed_tasks =
        document.feeds()
          .values()
          .stream()
          .map(f -> this.processFeed(Optional.of(document), f.uri()))
          .collect(Collectors.toList());

      final var image_tasks =
        document.images()
          .values()
          .stream()
          .map(f -> this.downloadImageTask(document, f.uri()))
          .collect(Collectors.toList());

      final var book_tasks =
        document.books()
          .values()
          .stream()
          .map(f -> this.downloadBookTask(document, f.uri()))
          .collect(Collectors.toList());

      final var tasks =
        List.of(feed_tasks, book_tasks, image_tasks)
          .stream()
          .flatMap(List::stream)
          .collect(Collectors.toList());

      return CompletableFuture.allOf(
        tasks.toArray(new CompletableFuture[tasks.size()]));
    }

    private void downloadFile(
      final URI uri,
      final Path path,
      final Path path_tmp)
    {
      try {
        final var data =
          this.http.get(
            uri,
            this.configuration.authenticationSupplier().apply(uri));

        Files.createDirectories(path_tmp.getParent());
        try (var output =
               Files.newOutputStream(path_tmp, CREATE_NEW)) {
          try (var input = data.stream()) {
            input.transferTo(output);
            Files.move(path_tmp, path, ATOMIC_MOVE, REPLACE_EXISTING);
          }
        } catch (final FileAlreadyExistsException e) {
          LOG.debug("file already exists: {}", path_tmp);
        }

      } catch (final IOException e) {
        throw new CompletionException(e);
      }
    }

    private void downloadImage(
      final OPDSDocumentProcessed document,
      final URI uri)
    {
      final var path = this.configuration.imageFileHashed(uri);
      final var path_tmp = temporaryFile(path);
      LOG.info("image GET {} -> {} (from {})", uri, path, document.file().uri());
      this.downloadFile(uri, path, path_tmp);
    }

    private CompletableFuture<Void> downloadImageTask(
      final OPDSDocumentProcessed document,
      final URI uri)
    {
      return CompletableFuture
        .runAsync(() -> this.downloadImage(document, uri), this.executor);
    }

    private void downloadBook(
      final OPDSDocumentProcessed document,
      final URI uri)
    {
      final var path = this.configuration.bookFileHashed(uri);
      final var path_tmp = temporaryFile(path);
      LOG.info("book GET {} -> {} (from {})", uri, path, document.file().uri());
      this.downloadFile(uri, path, path_tmp);
    }

    private CompletableFuture<Void> downloadBookTask(
      final OPDSDocumentProcessed document,
      final URI uri)
    {
      return CompletableFuture.runAsync(() -> this.downloadBook(document, uri), this.executor);
    }

    private Optional<OPDSDocumentProcessed> processOne(
      final Optional<OPDSDocumentProcessed> parent_document,
      final URI uri)
    {
      try {

        /*
         * Don't process URIs that have already been processed.
         */

        synchronized (this.processed_lock) {
          if (this.retrieved.contains(uri)) {
            return Optional.empty();
          }
          this.retrieved.add(uri);
        }

        /*
         * Fetch the remote document.
         */

        LOG.info(
          "feed GET {} {}",
          uri,
          parent_document.map(doc -> "(from " + doc.file().uri() + ")").orElse(""));

        if (!uri.isAbsolute()) {
          throw new IllegalArgumentException("URI " + uri + " is not absolute");
        }

        final var data =
          this.http.get(
            uri,
            this.configuration.authenticationSupplier().apply(uri));

        /*
         * Parse the remote document.
         */

        LOG.debug("processOne: parse: {}", uri);

        final Document document;
        try (var stream = data.stream()) {
          document = this.parsers.parse(uri, stream);
        }

        final var result =
          new OPDSDocumentProcessor()
            .process(this.configuration, document);

        synchronized (this.processed_lock) {
          if (this.processed.containsKey(uri)) {
            throw new IllegalStateException(
              "URI " + uri + " should not already have been processed");
          }
          this.processed.put(uri, result);
        }

        /*
         * Serialize properties about the document if this is the "starting"
         * URI.
         */

        if (Objects.equals(uri, this.configuration.remoteURI())) {
          this.serializeProperties(result);
        }

        /*
         * Serialize the feed.
         */

        LOG.debug("processOne: serialize: {} -> {}", uri, result.file().file());
        this.serializeFeed(document, result);

        return Optional.of(result);
      } catch (final Exception e) {
        throw new CompletionException(e);
      }
    }

    /**
     * Serialize a properties file with details about the OPDS feed.
     */

    private void serializeProperties(
      final OPDSDocumentProcessed result)
      throws IOException
    {
      final var output = this.configuration.output();
      final var file = output.resolve("info.properties");
      final var file_tmp = temporaryFile(file);

      Files.createDirectories(file_tmp.getParent());
      try (var out = Files.newBufferedWriter(file_tmp, TRUNCATE_EXISTING, CREATE)) {
        out.append("initial_file = ");
        out.append(output.relativize(result.file().file()).toString());
        out.newLine();
        out.flush();
      }

      Files.move(file_tmp, file, ATOMIC_MOVE, REPLACE_EXISTING);
    }

    /**
     * Serialize a feed XML document.
     */

    private void serializeFeed(
      final Document document,
      final OPDSDocumentProcessed result)
      throws TransformerException, IOException
    {
      final var transformer_factory =
        TransformerFactory.newInstance();
      final var transformer =
        transformer_factory.newTransformer();

      transformer.setOutputProperty(
        OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(
        OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(
        "{http://xml.apache.org/xslt}indent-amount", "2");

      final var path = result.file().file();
      final var path_tmp = temporaryFile(path);

      Files.createDirectories(path_tmp.getParent());
      try (var output = Files.newOutputStream(path_tmp, CREATE_NEW)) {
        transformer.transform(
          new DOMSource(document),
          new StreamResult(output));
        Files.move(path_tmp, path, ATOMIC_MOVE, REPLACE_EXISTING);
      } catch (final FileAlreadyExistsException e) {
        LOG.debug("file already exists: {}", path_tmp);
      }
    }

    CompletableFuture<Void> archiveFeedTask()
    {
      return CompletableFuture.runAsync(this::archiveFeed, this.executor);
    }

    private void archiveFeed()
    {
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

    private void squash()
    {
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
                  .setMaximumImageHeight(squash.maximumImageHeight())
                  .setMaximumImageWidth(squash.maximumImageWidth())
                  .build());

            squasher.squash();
          } catch (final Exception e) {
            LOG.error("failed to squash {}: ", epub, e);

            if (System.getProperty("au.org.libraryforall.epubsquash.unsupported.IgnoreSquashErrors") != null) {
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

    CompletableFuture<Void> squashTask()
    {
      return CompletableFuture.runAsync(this::squash, this.executor);
    }

    private void index()
    {
      try {
        final Map<URI, OPDSDocumentProcessed> processedCopy;
        synchronized (this.processed_lock) {
          processedCopy = new HashMap<>(this.processed);
        }

        final var rewriter = this.configuration.uriRewriter();
        final Map<String, List<URI>> index = new TreeMap<>();
        for (final var mapEntry : processedCopy.entrySet()) {
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
              writer.newLine();
            }
          }
          writer.flush();
        }
      } catch (final Exception e) {
        throw new CompletionException(e);
      }
    }

    CompletableFuture<Void> indexTask()
    {
      return CompletableFuture.runAsync(this::index, this.executor);
    }

    private void scaleImages()
    {
      final var scaleOpt = this.configuration.scaleImages();
      if (scaleOpt.isEmpty()) {
        LOG.debug("no scaling value specified, no scaling will be performed");
        return;
      }

      try {
        new OPDSImageScaler(this.configuration.imageDirectory(), scaleOpt.getAsDouble())
          .execute();
      } catch (final IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    CompletionStage<Void> scaleImagesTask()
    {
      return CompletableFuture.runAsync(this::scaleImages, this.executor);
    }
  }
}
