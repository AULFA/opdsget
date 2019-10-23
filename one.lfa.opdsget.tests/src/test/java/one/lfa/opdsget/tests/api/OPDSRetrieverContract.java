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

package one.lfa.opdsget.tests.api;

import fi.iki.elonen.NanoHTTPD;
import junit.framework.AssertionFailedError;
import one.lfa.opdsget.api.FileEntry;
import one.lfa.opdsget.api.OPDSAuthenticationType;
import one.lfa.opdsget.api.OPDSGetConfiguration;
import one.lfa.opdsget.api.OPDSHTTPData;
import one.lfa.opdsget.api.OPDSHTTPDefault;
import one.lfa.opdsget.api.OPDSHTTPException;
import one.lfa.opdsget.api.OPDSHTTPType;
import one.lfa.opdsget.api.OPDSManifestDescription;
import one.lfa.opdsget.api.OPDSManifestReaderErrorReceiverType;
import one.lfa.opdsget.api.OPDSRetrieverProviderType;
import one.lfa.opdsget.api.OPDSSquashConfiguration;
import one.lfa.opdsget.tests.TestDirectories;
import one.lfa.opdsget.tests.vanilla.OPDSDocumentProcessorTest;
import one.lfa.opdsget.vanilla.OPDSManifestReaders;
import org.apache.commons.codec.binary.Hex;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class OPDSRetrieverContract
{
  private static final int HTTPD_PORT = 34567;
  @Rule public final ExpectedException expected = ExpectedException.none();
  private ExecutorService exec;
  private Path output;
  private Path output_relative;
  private Path output_archive_relative;
  private NanoHTTPD httpd;

  private static void assertManifestContains(
    final OPDSManifestDescription manifest,
    final String file)
  {
    final var fileURI = URI.create(file);
    final var files = manifest.files();
    if (!files.containsKey(fileURI)) {
      throw new AssertionFailedError("Manifest does not contain " + fileURI);
    }
  }

  private static OPDSHTTPData httpDataOf(final InputStream stream)
  {
    return OPDSHTTPData.of(0L, "application/octet-stream", stream);
  }

  private static InputStream stringStream(final String text)
  {
    return new ByteArrayInputStream(text.getBytes(UTF_8));
  }

  private static void assertFileExists(final Path path)
  {
    Assert.assertTrue(
      "File " + path + " must exist and be a regular file",
      Files.isRegularFile(path));
  }

  private static void assertFileDoesNotExist(final Path path)
  {
    Assert.assertTrue(
      "File " + path + " must not exist",
      !Files.exists(path));
  }

  private static InputStream resourceStream(final String name)
  {
    try {
      final var url =
        OPDSDocumentProcessorTest.class.getResource(
          "/one/lfa/opdsget/tests/vanilla/" + name);
      if (url == null) {
        throw new FileNotFoundException(name);
      }

      return url.openStream();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Before
  public void setUp()
    throws IOException
  {
    this.output = TestDirectories.temporaryDirectory();
    this.output_relative = Paths.get("temporary");
    this.output_archive_relative = Paths.get("temporary.zip");

    this.exec = Executors.newFixedThreadPool(8, r -> {
      final var th = new Thread(r);
      th.setName("one.lfa.opdsget.io[" + th.getId() + "]");
      return th;
    });

    this.httpd = new NanoHTTPD("localhost", HTTPD_PORT)
    {
      private final Map<String, Supplier<InputStream>> streams =
        Map.of(
          "/feed.atom",
          () -> resourceStream("books_and_covers_localhost.xml"),
          "/thumbnail_0.png",
          () -> stringStream("thumbnail_0.txt"),
          "/thumbnail_1.png",
          () -> stringStream("thumbnail_1.txt"),
          "/cover_0.png",
          () -> stringStream("cover_0.txt"),
          "/cover_1.png",
          () -> stringStream("cover_1.txt"),
          "/0.epub",
          () -> stringStream("epub_0.txt"),
          "/1.epub",
          () -> stringStream("epub_1.txt")
        );

      @Override
      public Response serve(final IHTTPSession session)
      {
        OPDSRetrieverContract.this.logger().debug(
          "request: {}",
          session.getUri());

        return newChunkedResponse(
          Response.Status.OK,
          "application/octet-stream",
          this.streams.get(session.getUri()).get());
      }
    };

    this.httpd.start(30, false);
    this.logger().debug("started?");
  }

  @After
  public void tearDown()
  {
    try {
      if (Files.isDirectory(this.output_relative)) {
        Files.walkFileTree(this.output_relative, new SimpleFileVisitor<>()
        {
          @Override
          public FileVisitResult visitFile(
            final Path file,
            final BasicFileAttributes attrs)
            throws IOException
          {
            Files.deleteIfExists(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(
            final Path dir,
            final IOException exc)
            throws IOException
          {
            Files.deleteIfExists(dir);
            return FileVisitResult.CONTINUE;
          }
        });
      }
    } catch (final IOException e) {
      this.logger().error("could not clean up: ", e);
    }

    try {
      Files.deleteIfExists(this.output_archive_relative);
    } catch (final IOException e) {
      this.logger().error("could not clean up: ", e);
    }

    this.exec.shutdown();
    this.httpd.stop();
  }

  protected abstract OPDSRetrieverProviderType retrievers(
    OPDSHTTPType http);

  protected abstract Logger logger();

  /**
   * If the feed is not present, the operation fails.
   *
   * @throws Throwable On errors
   */

  @Test(timeout = 20_000L)
  public void testImmediate404()
    throws Throwable
  {
    final OPDSHTTPType mock_http = (uri, auth) -> {
      throw new OPDSHTTPException("404 - NOT FOUND", 404, "NOT FOUND");
    };

    final var retrievers =
      this.retrievers(mock_http);
    final var retriever =
      retrievers.create(this.exec);

    final var config =
      OPDSGetConfiguration.builder()
        .setOutput(this.output)
        .setRemoteURI(URI.create("https://example.com/catalog.atom"))
        .build();

    this.expected.expect(OPDSHTTPException.class);
    this.expected.expectMessage(StringContains.containsString("404"));

    try {
      retriever.retrieve(config).get();
    } catch (final InterruptedException e) {
      throw e;
    } catch (final ExecutionException e) {
      throw e.getCause();
    }
  }

  /**
   * If the feed is unparseable, the operation fails.
   *
   * @throws Throwable On errors
   */

  @Test(timeout = 20_000L)
  public void testImmediateUnparseable()
    throws Throwable
  {
    final var mock_http =
      new MockingHTTP(Map.of(
        "https://example.com/catalog.atom",
        () -> httpDataOf(resourceStream("notxml.txt"))
      ));

    final var retrievers =
      this.retrievers(mock_http);
    final var retriever =
      retrievers.create(this.exec);

    final var config =
      OPDSGetConfiguration.builder()
        .setOutput(this.output)
        .setRemoteURI(URI.create("https://example.com/catalog.atom"))
        .build();

    this.expected.expect(SAXException.class);

    try {
      retriever.retrieve(config).get();
    } catch (final InterruptedException e) {
      throw e;
    } catch (final ExecutionException e) {
      throw e.getCause();
    }
  }

  /**
   * "next" links are followed correctly.
   *
   * @throws Throwable On errors
   */

  @Test(timeout = 20_000L)
  public void testFollowNextLinks()
    throws Throwable
  {
    final var mock_http =
      new MockingHTTP(Map.of(
        "https://example.com/1.atom",
        () -> httpDataOf(resourceStream("1.xml")),
        "https://example.com/2.atom",
        () -> httpDataOf(resourceStream("2.xml")),
        "https://example.com/3.atom",
        () -> httpDataOf(resourceStream("3.xml"))
      ));

    final var retrievers =
      this.retrievers(mock_http);
    final var retriever =
      retrievers.create(this.exec);

    final var config =
      OPDSGetConfiguration.builder()
        .setOutput(this.output)
        .setRemoteURI(URI.create("https://example.com/1.atom"))
        .build();

    try {
      retriever.retrieve(config).get();
    } catch (final InterruptedException e) {
      throw e;
    } catch (final ExecutionException e) {
      throw e.getCause();
    }

    assertFileExists(this.output.resolve("manifest.xml"));

    final var manifest = this.parseManifest(this.output.resolve("manifest.xml"));
    this.verifyManifest(
      manifest,
      "feeds/EC7DD5867707ED7B2A7E3A57BCF9994E1178AEF0B8C18977FB1011AD10709FA0.atom",
      "feeds/F203ED2393A4D570BCE87CE8D872A27C0299A4CADB24EE0E671CB244D6B7EBE7.atom",
      "feeds/88597F9F4671FEC4A97E200696B066EF9D8DD06644B5089F4C08DB4D45480CC0.atom",
      "index.txt"
    );
  }

  /**
   * Downloading data from a mocked server works.
   *
   * @throws Throwable On errors
   */

  @Test(timeout = 20_000L)
  public void testDownloadBooksAndCoversFromMockedServer()
    throws Throwable
  {
    final var mock_http =
      new MockingHTTP(Map.of(
        "https://example.com/1.atom",
        () -> httpDataOf(resourceStream("books_and_covers.xml")),
        "https://example.com/thumbnail_0.png",
        () -> httpDataOf(stringStream("thumbnail_0.txt")),
        "https://example.com/thumbnail_1.png",
        () -> httpDataOf(stringStream("thumbnail_1.txt")),
        "https://example.com/cover_0.png",
        () -> httpDataOf(stringStream("cover_0.txt")),
        "https://example.com/cover_1.png",
        () -> httpDataOf(stringStream("cover_1.txt")),
        "https://example.com/0.epub",
        () -> httpDataOf(stringStream("henry_v.epub")),
        "https://example.com/1.epub",
        () -> httpDataOf(stringStream("henry_v.epub"))
      ));

    final var retrievers =
      this.retrievers(mock_http);
    final var retriever =
      retrievers.create(this.exec);

    final var config =
      OPDSGetConfiguration.builder()
        .setOutput(this.output)
        .setRemoteURI(URI.create("https://example.com/1.atom"))
        .build();

    try {
      retriever.retrieve(config).get();
    } catch (final InterruptedException e) {
      throw e;
    } catch (final ExecutionException e) {
      throw e.getCause();
    }

    mock_http.checkAllCalled();

    assertFileExists(this.output.resolve("manifest.xml"));

    final var manifest = this.parseManifest(this.output.resolve("manifest.xml"));
    this.verifyManifest(
      manifest,
      "feeds/EC7DD5867707ED7B2A7E3A57BCF9994E1178AEF0B8C18977FB1011AD10709FA0.atom",
      "images/C20256EE994470033BCC12D37F08898A06304557FECA4849BF09DBFAFD9E4B12",
      "images/AA739188B2729F243D0E679A9B76E71957CED3F3E7A59B567B47C4A35C7B4B20",
      "images/36AD7C41A6CDBF7CBD9D6165FFA469A1B142E233F772AFCDA9443716C24E8737",
      "images/1607A41005DB48491D47DDAA77A7BF7979CF25714DF07D06EA358880967015D2",
      "books/CC6BAB78A232CC63D8DE8D8F2F2FFFB452762C0464478409982DB78034FAC80E.epub",
      "books/E6CAB9F69F8408D271A7605C86857F63D38E9AB80964A7BEF9B13053D5B8305E.epub",
      "index.txt"
    );
  }

  /**
   * Downloading data from a mocked server (and squashing and scaling) works.
   *
   * @throws Throwable On errors
   */

  @Test(timeout = 20_000L)
  public void testDownloadBooksAndCoversFromMockedServerScaleSquash()
    throws Throwable
  {
    final var mock_http =
      new MockingHTTP(Map.of(
        "https://example.com/1.atom",
        () -> httpDataOf(resourceStream("books_and_covers.xml")),
        "https://example.com/thumbnail_0.png",
        () -> httpDataOf(stringStream("thumbnail_0.txt")),
        "https://example.com/thumbnail_1.png",
        () -> httpDataOf(stringStream("thumbnail_1.txt")),
        "https://example.com/cover_0.png",
        () -> httpDataOf(stringStream("cover_0.txt")),
        "https://example.com/cover_1.png",
        () -> httpDataOf(stringStream("cover_1.txt")),
        "https://example.com/0.epub",
        () -> httpDataOf(resourceStream("henry_v.epub")),
        "https://example.com/1.epub",
        () -> httpDataOf(resourceStream("henry_v.epub"))
      ));

    final var retrievers =
      this.retrievers(mock_http);
    final var retriever =
      retrievers.create(this.exec);

    final var config =
      OPDSGetConfiguration.builder()
        .setOutput(this.output)
        .setRemoteURI(URI.create("https://example.com/1.atom"))
        .setScaleImages(OptionalDouble.of(0.5))
        .setSquash(
          OPDSSquashConfiguration.builder()
            .setMaximumImageWidth(1024.0)
            .setMaximumImageHeight(1024.0)
            .setScaleFactor(0.5)
            .build())
        .build();

    try {
      retriever.retrieve(config).get();
    } catch (final InterruptedException e) {
      throw e;
    } catch (final ExecutionException e) {
      throw e.getCause();
    }

    mock_http.checkAllCalled();

    assertFileExists(this.output.resolve("manifest.xml"));

    final var manifest = this.parseManifest(this.output.resolve("manifest.xml"));
    this.verifyManifest(
      manifest,
      "feeds/EC7DD5867707ED7B2A7E3A57BCF9994E1178AEF0B8C18977FB1011AD10709FA0.atom",
      "images/C20256EE994470033BCC12D37F08898A06304557FECA4849BF09DBFAFD9E4B12",
      "images/AA739188B2729F243D0E679A9B76E71957CED3F3E7A59B567B47C4A35C7B4B20",
      "images/36AD7C41A6CDBF7CBD9D6165FFA469A1B142E233F772AFCDA9443716C24E8737",
      "images/1607A41005DB48491D47DDAA77A7BF7979CF25714DF07D06EA358880967015D2",
      "books/CC6BAB78A232CC63D8DE8D8F2F2FFFB452762C0464478409982DB78034FAC80E.epub",
      "books/E6CAB9F69F8408D271A7605C86857F63D38E9AB80964A7BEF9B13053D5B8305E.epub",
      "index.txt"
    );
  }

  /**
   * Downloading data from a mocked server, excluding images and books, works.
   *
   * @throws Throwable On errors
   */

  @Test(timeout = 20_000L)
  public void testDownloadNoBooksAndNoCoversFromMockedServer()
    throws Throwable
  {
    final var mock_http =
      new MockingHTTP(Map.of(
        "https://example.com/1.atom",
        () -> httpDataOf(resourceStream("books_and_covers.xml"))
      ));

    final var retrievers =
      this.retrievers(mock_http);
    final var retriever =
      retrievers.create(this.exec);

    final var config =
      OPDSGetConfiguration.builder()
        .setOutput(this.output)
        .setFetchedKinds(List.of())
        .setRemoteURI(URI.create("https://example.com/1.atom"))
        .build();

    try {
      retriever.retrieve(config).get();
    } catch (final InterruptedException e) {
      throw e;
    } catch (final ExecutionException e) {
      throw e.getCause();
    }

    mock_http.checkAllCalled();

    assertFileExists(this.output.resolve(
      "feeds/EC7DD5867707ED7B2A7E3A57BCF9994E1178AEF0B8C18977FB1011AD10709FA0.atom"));
    assertFileDoesNotExist(this.output.resolve("images"));
    assertFileDoesNotExist(this.output.resolve("books"));
    assertFileExists(this.output.resolve(
      "manifest.xml"));

    Assert.assertEquals(
      "Only one file must have downloaded",
      1L,
      Files.list(this.output.resolve("feeds")).count());
  }

  /**
   * Downloading data from a real server, excluding images and books, works.
   *
   * @throws Throwable On errors
   */

  @Test(timeout = 20_000L)
  public void testDownloadNoBooksAndNoCoversFromRealServer()
    throws Throwable
  {
    final var retrievers =
      this.retrievers(new OPDSHTTPDefault());
    final var retriever =
      retrievers.create(this.exec);

    final var config =
      OPDSGetConfiguration.builder()
        .setOutput(this.output)
        .setFetchedKinds(List.of())
        .setRemoteURI(URI.create("http://localhost:" + HTTPD_PORT + "/feed.atom"))
        .build();

    try {
      retriever.retrieve(config).get();
    } catch (final InterruptedException e) {
      throw e;
    } catch (final ExecutionException e) {
      throw e.getCause();
    }

    assertFileExists(this.output.resolve(
      "feeds/26A1A7550A125B02EB199F90C85C37CEEFEFF046FD0A7A90F46EB9B50DEEB857.atom"));
    assertFileDoesNotExist(this.output.resolve("images"));
    assertFileDoesNotExist(this.output.resolve("books"));
    assertFileExists(this.output.resolve(
      "manifest.xml"));

    Assert.assertEquals(
      "Only one file must have downloaded",
      1L,
      Files.list(this.output.resolve("feeds")).count());
  }

  /**
   * Downloading data from a real server, including images and books, works.
   *
   * @throws Throwable On errors
   */

  @Test(timeout = 20_000L)
  public void testDownloadBooksAndCoversFromRealServer()
    throws Throwable
  {
    final var retrievers =
      this.retrievers(new OPDSHTTPDefault());
    final var retriever =
      retrievers.create(this.exec);

    final var config =
      OPDSGetConfiguration.builder()
        .setOutput(this.output)
        .setRemoteURI(URI.create("http://localhost:" + HTTPD_PORT + "/feed.atom"))
        .build();

    try {
      retriever.retrieve(config).get();
    } catch (final InterruptedException e) {
      throw e;
    } catch (final ExecutionException e) {
      throw e.getCause();
    }

    assertFileExists(this.output.resolve("manifest.xml"));

    final var manifest = this.parseManifest(this.output.resolve("manifest.xml"));
    this.verifyManifest(
      manifest,
      "feeds/26A1A7550A125B02EB199F90C85C37CEEFEFF046FD0A7A90F46EB9B50DEEB857.atom",
      "images/A0E6E399D2972A1F558D4F107DF604FF764A9CDC39B8411001549FCCF81FB122",
      "images/D1614C51D455E2209424D432D282CE95BDE160FFDAE4C61D1BABABE649454A2D",
      "images/10F54B076C7CD5321035766A533956E0355DD7198ED613D9A7B88FD026A91A0F",
      "images/16BF055A959114E041C6351EED9982A6DCC1AB07F06101AAC10475F402DA1C90",
      "books/AB30D8632DE2638B8746C3D9E7184F705CB234CD5FDF4CC8CD7456A2C1C39850.epub",
      "books/E541B79B837177FDC14D348067E560DCA5BEBAEB1C07C44B3A8F0D3815D5CE73.epub",
      "index.txt"
    );
  }

  private OPDSManifestDescription parseManifest(
    final Path path)
  {
    try (var stream = Files.newInputStream(path)) {
      final var readers = new OPDSManifestReaders();
      final OPDSManifestReaderErrorReceiverType errorReceiver =
        error -> this.logger().error("parse error: {}", error);

      try (var reader = readers.createReader(
        errorReceiver,
        path.toUri(),
        stream)) {
        return reader.read()
          .orElseThrow(() -> new IllegalStateException("Could not parse manifest"));
      }
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Downloading the Feedbooks sample feed works.
   *
   * @throws Throwable On errors
   */

  @Test(timeout = 90_000L)
  public void testFeedbooksAcquisitions()
    throws Throwable
  {
    final OPDSHTTPType http =
      new OPDSHTTPDefault();
    final var retrievers =
      this.retrievers(http);
    final var retriever =
      retrievers.create(this.exec);

    final var config =
      OPDSGetConfiguration.builder()
        .setOutput(this.output)
        .setScaleImages(OptionalDouble.of(0.5))
        .setRemoteURI(URI.create(
          "http://feedbooks.github.io/opds-test-catalog/catalog/acquisition/main.xml"))
        .build();

    retriever.retrieve(config).get();

    assertFileExists(this.output.resolve("index.txt"));

    final var expectedLines =
      new String(resourceStream("feedbooks-index.txt").readAllBytes(), UTF_8);
    final var receivedLines =
      Files.readString(this.output.resolve("index.txt"), UTF_8);

    Assert.assertEquals(expectedLines, receivedLines);
    final var manifest = this.parseManifest(this.output.resolve("manifest.xml"));
    this.verifyManifest(
      manifest,
      "books/7F23DD5E09652D5ECD65EC8A95EFDBE8C2A24986D77D3241A090D6C9015998C9.epub",
      "books/B63468BD8869F89D585A0CB910DF73347521D057CDD069753401F87AB2FBF67C.epub",
      "feeds/00C2A67573F258CBC6BB9405914A738CC9515EE7B18FA96508C9A3134B084203.atom",
      "feeds/02481DCF1596166C13F36EE974A09D9CCED36A0481422E3847C05F3603B3F0D0.atom",
      "feeds/2407C1D8837B6C28E1CD549BA9F4A58BA1E9903F6FFE79E9B9ECBF36FCAD068E.atom",
      "feeds/4C31EFBE0EED927FFB7EB98B39365302CEAAEA19CB862BDC9CF1793E11673DD3.atom",
      "images/647F20EB0EFDD30743B9D51DA93E2C2FE52F1D15967071966A474C5275272036",
      "images/A92982FE7A7D3070530D053E844F936F9584DCB78F2DBC333B154042B71DC2E4",
      "index.txt"
    );
  }

  private void verifyManifest(
    final OPDSManifestDescription manifest,
    final String... mustExist)
  {
    for (final var existing : mustExist) {
      this.logger().debug("checking {} exists", existing);
      assertManifestContains(manifest, existing);
    }

    for (final var entry : manifest.files().entrySet()) {
      final var file = entry.getValue();
      this.verifyManifestFile(file);
    }

    this.logger().debug("checking {} files are in the manifest", Integer.valueOf(mustExist.length));
    Assert.assertEquals(mustExist.length, manifest.files().size());
  }

  private void verifyManifestFile(final FileEntry file)
  {
    try {
      final var realFile = this.output.resolve(file.path());
      this.logger().debug("checking hash of {}", realFile);
      final var digest = MessageDigest.getInstance(file.hashAlgorithm());

      try (var stream = Files.newInputStream(realFile)) {
        final var buffer = new byte[4096];
        while (true) {
          final var r = stream.read(buffer);
          if (r == -1) {
            break;
          }
          digest.update(buffer, 0, r);
        }

        final var expectedHash = file.hash();
        final var expectedHashText = Hex.encodeHexString(expectedHash, false);
        final var receivedHash = digest.digest();
        final var receivedHashText = Hex.encodeHexString(receivedHash, false);

        Assert.assertEquals(
          String.format("File hash for %s must match", file.path()),
          expectedHashText,
          receivedHashText);
      }
    } catch (final NoSuchAlgorithmException | IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final class MockingHTTP implements OPDSHTTPType
  {
    private final Map<String, Supplier<OPDSHTTPData>> streams;
    private final Map<String, Boolean> called;
    private final Object lock;

    MockingHTTP(final Map<String, Supplier<OPDSHTTPData>> streams)
    {
      this.streams = Objects.requireNonNull(streams, "streams");
      this.called = new HashMap<>();
      this.lock = new Object();
    }

    @Override
    public OPDSHTTPData get(
      final URI uri,
      final Optional<OPDSAuthenticationType> auth)
    {
      synchronized (this.lock) {
        final var text = uri.toString();
        if (this.called.containsKey(text)) {
          throw new IllegalStateException("Already called: " + text);
        }
        this.called.put(text, Boolean.TRUE);

        if (this.streams.containsKey(text)) {
          return this.streams.get(text).get();
        }

        throw new IllegalStateException("No stream for: " + text);
      }
    }

    public void checkAllCalled()
    {
      synchronized (this.lock) {
        for (final var name : this.streams.keySet()) {
          if (!this.called.containsKey(name)) {
            throw new IllegalStateException("Failed to call: " + name);
          }
        }
      }
    }
  }
}
