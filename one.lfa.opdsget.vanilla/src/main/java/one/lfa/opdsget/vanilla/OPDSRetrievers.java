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

import com.io7m.jaffirm.core.Preconditions;
import one.lfa.epubsquash.api.EPUBSquasherProviderType;
import one.lfa.opdsget.api.FileEntry;
import one.lfa.opdsget.api.OPDSDocumentProcessed;
import one.lfa.opdsget.api.OPDSGetConfiguration;
import one.lfa.opdsget.api.OPDSHTTPDefault;
import one.lfa.opdsget.api.OPDSHTTPType;
import one.lfa.opdsget.api.OPDSManifestDescription;
import one.lfa.opdsget.api.OPDSManifestWriterProviderType;
import one.lfa.opdsget.api.OPDSRetrieverProviderType;
import one.lfa.opdsget.api.OPDSRetrieverType;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

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
  private final OPDSManifestWriterProviderType manifestWriters;

  private OPDSRetrievers(
    final EPUBSquasherProviderType in_squashers,
    final OPDSManifestWriterProviderType in_manifest_writers,
    final OPDSHTTPType in_http)
  {
    this.http =
      Objects.requireNonNull(in_http, "http");
    this.squashers =
      Objects.requireNonNull(in_squashers, "squashers");
    this.manifestWriters =
      Objects.requireNonNull(in_manifest_writers, "in_manifest_writers");

    this.parsers = new OPDSXMLParsers();
  }

  /**
   * @return A retriever provider
   */

  public static OPDSRetrieverProviderType provider()
  {
    final var epubSquashers =
      ServiceLoader.load(EPUBSquasherProviderType.class)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No available epub squasher provider"));

    final var manifestWriters =
      ServiceLoader.load(OPDSManifestWriterProviderType.class)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No available manifest writer provider"));

    return new OPDSRetrievers(epubSquashers, manifestWriters, new OPDSHTTPDefault());
  }

  /**
   * @param in_squashers        A set of EPUB squashers
   * @param in_manifest_writers A provider of manifest writers
   * @param in_http             An HTTP request handler
   *
   * @return A retriever provider
   */

  public static OPDSRetrieverProviderType providerWith(
    final EPUBSquasherProviderType in_squashers,
    final OPDSManifestWriterProviderType in_manifest_writers,
    final OPDSHTTPType in_http)
  {
    return new OPDSRetrievers(in_squashers, in_manifest_writers, in_http);
  }

  @Override
  public OPDSRetrieverType create(final ExecutorService executor)
  {
    return new Retriever(executor, this.parsers, this.http, this.squashers, this.manifestWriters);
  }

  private static final class Retriever implements OPDSRetrieverType
  {
    private final ExecutorService executor;
    private final OPDSHTTPType http;
    private final EPUBSquasherProviderType squashers;
    private final OPDSManifestWriterProviderType manifestWriters;
    private final OPDSXMLParsers parsers;

    Retriever(
      final ExecutorService in_executor,
      final OPDSXMLParsers in_parsers,
      final OPDSHTTPType in_http,
      final EPUBSquasherProviderType in_squashers,
      final OPDSManifestWriterProviderType inManifestWriters)
    {
      this.executor =
        Objects.requireNonNull(in_executor, "executor");
      this.parsers =
        Objects.requireNonNull(in_parsers, "parsers");
      this.http =
        Objects.requireNonNull(in_http, "http");
      this.squashers =
        Objects.requireNonNull(in_squashers, "squashers");
      this.manifestWriters =
        Objects.requireNonNull(inManifestWriters, "inManifestWriters");
    }

    @Override
    public CompletableFuture<Void> retrieve(
      final OPDSGetConfiguration config)
    {
      Objects.requireNonNull(config, "configuration");

      final var retrieval =
        new Retrieval(
          config,
          this.executor,
          this.http,
          this.parsers,
          this.squashers,
          this.manifestWriters);

      final Function<Void, CompletionStage<Void>> indexTask =
        ignored -> OPDSTaskIndex.task(
          config,
          retrieval.processed(),
          retrieval::onFileChanged,
          this.executor);

      final Function<Void, CompletionStage<Void>> squashTask =
        ignored -> OPDSTaskSquash.task(
          config,
          this.squashers,
          retrieval::onFileChanged,
          this.executor);

      final Function<Void, CompletionStage<Void>> imageScaleTask =
        ignored -> OPDSTaskImageScale.task(
          config,
          retrieval::onFileChanged,
          this.executor);

      final Function<Void, CompletionStage<Void>> manifestWriteTask =
        ignored -> OPDSTaskWriteManifest.task(
          config,
          this.manifestWriters,
          retrieval.manifest(),
          this.executor);

      final Function<Void, CompletionStage<Void>> archiveTask =
        ignored -> OPDSTaskArchive.task(config, this.executor);

      return retrieval
        .processFeed(Optional.empty(), config.remoteURI())
        .thenCompose(indexTask)
        .thenCompose(squashTask)
        .thenCompose(imageScaleTask)
        .thenCompose(manifestWriteTask)
        .thenCompose(archiveTask);
    }
  }

  private static final class Retrieval
  {
    private final EPUBSquasherProviderType squashers;
    private final ExecutorService executor;
    private final HashMap<URI, FileEntry> manifestFiles;
    private final HashSet<URI> retrieved;
    private final Map<URI, OPDSDocumentProcessed> processed;
    private final Object manifestLock;
    private final OPDSGetConfiguration configuration;
    private final OPDSHTTPType http;
    private final OPDSManifestDescription.Builder manifestBuilder;
    private final OPDSManifestWriterProviderType manifestWriters;
    private final OPDSXMLParsers parsers;

    Retrieval(
      final OPDSGetConfiguration inConfiguration,
      final ExecutorService inExecutor,
      final OPDSHTTPType inHttp,
      final OPDSXMLParsers inParsers,
      final EPUBSquasherProviderType inSquashers,
      final OPDSManifestWriterProviderType inManifestWriters)
    {
      this.configuration =
        Objects.requireNonNull(inConfiguration, "configuration");
      this.executor =
        Objects.requireNonNull(inExecutor, "executor");
      this.http =
        Objects.requireNonNull(inHttp, "http");
      this.parsers =
        Objects.requireNonNull(inParsers, "parsers");
      this.squashers =
        Objects.requireNonNull(inSquashers, "squashers");
      this.manifestWriters =
        Objects.requireNonNull(inManifestWriters, "manifestWriters");

      this.retrieved = new HashSet<>(128);
      this.processed = new HashMap<>(128);

      this.manifestBuilder = OPDSManifestDescription.builder();
      this.manifestBuilder.setBase(this.configuration.outputManifestBaseURI());
      this.manifestFiles = new HashMap<>(128);
      this.manifestLock = new Object();

      synchronized (this.manifestLock) {
        this.manifestBuilder.setId(this.configuration.remoteURI());
      }
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
      final var feedTasks =
        document.feeds()
          .values()
          .stream()
          .map(f -> this.processFeed(Optional.of(document), f.uri()))
          .collect(Collectors.toList());

      final var imageTasks =
        document.images()
          .values()
          .stream()
          .map(f -> this.downloadImageTask(document, f.uri()))
          .collect(Collectors.toList());

      final var bookTasks =
        document.books()
          .values()
          .stream()
          .map(f -> this.downloadBookTask(document, f.uri()))
          .collect(Collectors.toList());

      final var tasks =
        List.of(feedTasks, bookTasks, imageTasks)
          .stream()
          .flatMap(List::stream)
          .collect(Collectors.toList());

      return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));
    }

    private void downloadFile(
      final URI uri,
      final Path path,
      final Path path_tmp)
    {
      try {
        final var authentication =
          this.configuration.authenticationSupplier().apply(uri);
        final var data =
          this.http.get(uri, authentication);

        Files.createDirectories(path_tmp.getParent());
        try (var output = Files.newOutputStream(path_tmp, CREATE_NEW)) {
          try (var input = data.stream()) {
            input.transferTo(output);
            Files.move(path_tmp, path, ATOMIC_MOVE, REPLACE_EXISTING);
          }
          this.saveFileInManifest(uri, path, OPDSManifestFileEntryKind.GENERAL);
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

        synchronized (this.manifestLock) {
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
          throw new IllegalArgumentException(String.format("URI %s is not absolute", uri));
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

        synchronized (this.manifestLock) {
          if (this.processed.containsKey(uri)) {
            throw new IllegalStateException(
              String.format("URI %s should not already have been processed", uri));
          }
          this.processed.put(uri, result);
        }

        /*
         * Serialize properties about the document if this is the "starting"
         * URI.
         */

        final var isRootFeed = Objects.equals(uri, this.configuration.remoteURI());
        if (isRootFeed) {
          this.serializeProperties(result);
        }

        /*
         * Serialize the feed.
         */

        final var file = result.file().file();
        LOG.debug("processOne: serialize: {} -> {}", uri, file);
        this.serializeFeed(document, result);

        final var entryKind =
          isRootFeed ? OPDSManifestFileEntryKind.ROOT_FEED : OPDSManifestFileEntryKind.GENERAL;

        this.saveFileInManifest(uri, file, entryKind);
        return Optional.of(result);
      } catch (final Exception e) {
        throw new CompletionException(e);
      }
    }

    private void saveFileInManifest(
      final URI uri,
      final Path file,
      final OPDSManifestFileEntryKind kind)
      throws IOException
    {
      Preconditions.checkPreconditionV(
        file,
        file.isAbsolute(),
        "Path %s must be absolute", file);

      final var relative = this.configuration.output().relativize(file);
      final var relativeName = relative.toString();
      final var hash = OPDSHashing.sha256HashOf(file);

      LOG.debug("manifest: {} -> {}", uri, relative);
      synchronized (this.manifestLock) {
        final var entry =
          FileEntry.builder()
            .setHash(hash)
            .setHashAlgorithm("SHA-256")
            .setPath(relativeName)
            .build();

        this.manifestFiles.put(uri, entry);

        switch (kind) {
          case GENERAL: {
            break;
          }
          case ROOT_FEED: {
            this.manifestBuilder.setRootFile(relativeName);
            break;
          }
          case SEARCH_INDEX: {
            this.manifestBuilder.setSearchIndex(relativeName);
            break;
          }
        }
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
      final var transformer_factory = TransformerFactory.newInstance();
      final var transformer = transformer_factory.newTransformer();

      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

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

    Map<URI, OPDSDocumentProcessed> processed()
    {
      synchronized (this.manifestLock) {
        return Map.copyOf(this.processed);
      }
    }

    OPDSManifestDescription manifest()
    {
      synchronized (this.manifestLock) {
        this.manifestBuilder.putAllFiles(this.manifestFiles);
        return this.manifestBuilder.build();
      }
    }

    void onFileChanged(
      final OPDSManifestFileEntryKind kind,
      final Path path)
    {
      LOG.debug("onFileChanged: {} {}", kind, path);

      Preconditions.checkPreconditionV(
        path,
        path.isAbsolute(),
        "Path %s must be absolute", path);

      final var relativeFile =
        this.configuration.output()
          .relativize(path)
          .toString();

      synchronized (this.manifestLock) {
        for (final var entry : this.manifestFiles.entrySet()) {
          final var uri = entry.getKey();
          final var file = entry.getValue();

          if (Objects.equals(file.path(), relativeFile)) {
            LOG.debug("onFileChanged: found existing file for {} ({})", path, uri);

            try {
              this.saveFileInManifest(uri, path, kind);
            } catch (final IOException e) {
              throw new UncheckedIOException(e);
            }
            return;
          }
        }

        try {
          LOG.debug("onFileChanged: could not find existing file for {}", path);
          this.saveFileInManifest(URI.create(path.getFileName().toString()), path, kind);
        } catch (final IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    }
  }
}
