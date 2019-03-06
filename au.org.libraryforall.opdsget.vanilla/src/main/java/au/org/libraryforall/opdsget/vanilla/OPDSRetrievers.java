package au.org.libraryforall.opdsget.vanilla;

import au.org.libraryforall.opdsget.api.OPDSDocumentProcessed;
import au.org.libraryforall.opdsget.api.OPDSGetConfiguration;
import au.org.libraryforall.opdsget.api.OPDSHTTPData;
import au.org.libraryforall.opdsget.api.OPDSHTTPDefault;
import au.org.libraryforall.opdsget.api.OPDSHTTPType;
import au.org.libraryforall.opdsget.api.OPDSRetrieverProviderType;
import au.org.libraryforall.opdsget.api.OPDSRetrieverType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
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

  private OPDSRetrievers(final OPDSHTTPType in_http)
  {
    this.http = Objects.requireNonNull(in_http, "http");
    this.parsers = new OPDSXMLParsers();
  }

  /**
   * @return A retriever provider
   */

  public static OPDSRetrieverProviderType provider()
  {
    return new OPDSRetrievers(new OPDSHTTPDefault());
  }

  /**
   * @param in_http An HTTP request handler
   *
   * @return A retriever provider
   */

  public static OPDSRetrieverProviderType providerWith(
    final OPDSHTTPType in_http)
  {
    return new OPDSRetrievers(in_http);
  }

  @Override
  public OPDSRetrieverType create(final ExecutorService executor)
  {
    return new Retriever(executor, this.parsers, this.http);
  }

  private static final class Retriever implements OPDSRetrieverType
  {
    private final ExecutorService executor;
    private final OPDSHTTPType http;
    private final OPDSXMLParsers parsers;

    Retriever(
      final ExecutorService in_executor,
      final OPDSXMLParsers in_parsers,
      final OPDSHTTPType in_http)
    {
      this.executor =
        Objects.requireNonNull(in_executor, "executor");
      this.parsers =
        Objects.requireNonNull(in_parsers, "parsers");
      this.http =
        Objects.requireNonNull(in_http, "http");
    }

    @Override
    public CompletableFuture<Void> retrieve(
      final OPDSGetConfiguration in_configuration)
    {
      Objects.requireNonNull(in_configuration, "configuration");

      final Retrieval retrieval =
        new Retrieval(in_configuration, this.executor, this.http, this.parsers);

      return retrieval
        .processFeed(in_configuration.remoteURI())
        .thenCompose(ignored -> retrieval.archiveFeedTask());
    }
  }

  private static final class Retrieval
  {
    private final OPDSGetConfiguration configuration;
    private final ExecutorService executor;
    private final OPDSHTTPType http;
    private final OPDSXMLParsers parsers;
    private final Set<URI> processed;
    private final Object processed_lock;

    Retrieval(
      final OPDSGetConfiguration in_configuration,
      final ExecutorService in_executor,
      final OPDSHTTPType in_http,
      final OPDSXMLParsers in_parsers)
    {
      this.configuration =
        Objects.requireNonNull(in_configuration, "configuration");
      this.executor =
        Objects.requireNonNull(in_executor, "executor");
      this.http =
        Objects.requireNonNull(in_http, "http");
      this.parsers =
        Objects.requireNonNull(in_parsers, "parsers");

      this.processed = new HashSet<>(128);
      this.processed_lock = new Object();
    }

    private static Path temporaryFile(final Path path)
    {
      return Paths.get(new StringBuilder(64)
                         .append(path.toString())
                         .append(".tmp")
                         .toString());
    }

    private CompletableFuture<Void> processFeed(final URI uri)
    {
      LOG.debug("processFeed: {}", uri);

      return CompletableFuture
        .supplyAsync(() -> this.processOne(uri), this.executor)
        .thenComposeAsync(
          this::runSubTasksIfNecessary,
          ForkJoinPool.commonPool());
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
      final List<CompletableFuture<Void>> feed_tasks =
        document.feeds()
          .values()
          .stream()
          .map(f -> this.processFeed(f.uri()))
          .collect(Collectors.toList());

      final List<CompletableFuture<Void>> image_tasks =
        document.images()
          .values()
          .stream()
          .map(f -> this.downloadImageTask(f.uri()))
          .collect(Collectors.toList());

      final List<CompletableFuture<Void>> book_tasks =
        document.books()
          .values()
          .stream()
          .map(f -> this.downloadBookTask(f.uri()))
          .collect(Collectors.toList());

      final List<CompletableFuture<Void>> tasks =
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
        final OPDSHTTPData data =
          this.http.get(
            uri,
            this.configuration.authenticationSupplier().apply(uri));

        Files.createDirectories(path_tmp.getParent());
        try (OutputStream output =
               Files.newOutputStream(path_tmp, CREATE_NEW)) {
          try (InputStream input = data.stream()) {
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

    private void downloadImage(final URI uri)
    {
      final Path path = this.configuration.imageFileHashed(uri);
      final Path path_tmp = temporaryFile(path);
      LOG.info("image GET {} -> {}", uri, path);
      this.downloadFile(uri, path, path_tmp);
    }

    private CompletableFuture<Void> downloadImageTask(final URI uri)
    {
      return CompletableFuture
        .runAsync(() -> this.downloadImage(uri), this.executor);
    }

    private void downloadBook(final URI uri)
    {
      final Path path = this.configuration.bookFileHashed(uri);
      final Path path_tmp = temporaryFile(path);
      LOG.info("book GET {} -> {}", uri, path);
      this.downloadFile(uri, path, path_tmp);
    }

    private CompletableFuture<Void> downloadBookTask(final URI uri)
    {
      return CompletableFuture
        .runAsync(() -> this.downloadBook(uri), this.executor);
    }

    private Optional<OPDSDocumentProcessed> processOne(
      final URI uri)
    {
      try {

        /*
         * Don't process URIs that have already been processed.
         */

        synchronized (this.processed_lock) {
          if (this.processed.contains(uri)) {
            return Optional.empty();
          }
          this.processed.add(uri);
        }

        /*
         * Fetch the remote document.
         */

        LOG.info("feed GET {}", uri);

        final OPDSHTTPData data =
          this.http.get(
            uri,
            this.configuration.authenticationSupplier().apply(uri));

        /*
         * Parse the remote document.
         */

        LOG.debug("processOne: parse: {}", uri);

        final Document document;
        try (InputStream stream = data.stream()) {
          document = this.parsers.parse(uri, stream);
        }

        final OPDSDocumentProcessed result =
          new OPDSDocumentProcessor()
            .process(this.configuration, document);

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
      final Path output = this.configuration.output();
      final Path file = output.resolve("info.properties");
      final Path file_tmp = temporaryFile(file);

      Files.createDirectories(file_tmp.getParent());
      try (BufferedWriter out =
             Files.newBufferedWriter(file_tmp, TRUNCATE_EXISTING, CREATE)) {
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
      final TransformerFactory transformer_factory =
        TransformerFactory.newInstance();
      final Transformer transformer =
        transformer_factory.newTransformer();

      transformer.setOutputProperty(
        OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(
        OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(
        "{http://xml.apache.org/xslt}indent-amount", "2");

      final Path path = result.file().file();
      final Path path_tmp = temporaryFile(path);

      Files.createDirectories(path_tmp.getParent());
      try (OutputStream output =
             Files.newOutputStream(path_tmp, CREATE_NEW)) {
        transformer.transform(
          new DOMSource(document),
          new StreamResult(output));
        Files.move(path_tmp, path, ATOMIC_MOVE, REPLACE_EXISTING);
      } catch (final FileAlreadyExistsException e) {
        LOG.debug("file already exists: {}", path_tmp);
      }
    }

    public CompletableFuture<Void> archiveFeedTask()
    {
      return CompletableFuture.runAsync(this::archiveFeed, this.executor);
    }

    private void archiveFeed()
    {
      try {
        final Optional<Path> archive_opt = this.configuration.outputArchive();
        if (archive_opt.isPresent()) {
          final Path archive = archive_opt.get();
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
}
