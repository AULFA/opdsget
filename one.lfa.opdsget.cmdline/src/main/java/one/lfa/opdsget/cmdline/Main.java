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

package one.lfa.opdsget.cmdline;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Console;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import one.lfa.epubsquash.vanilla.EPUBSquashers;
import one.lfa.opdsget.api.OPDSAuthenticationPatternMappedParser;
import one.lfa.opdsget.api.OPDSAuthenticationType;
import one.lfa.opdsget.api.OPDSGetConfiguration;
import one.lfa.opdsget.api.OPDSGetKind;
import one.lfa.opdsget.api.OPDSHTTPDefault;
import one.lfa.opdsget.api.OPDSSquashConfiguration;
import one.lfa.opdsget.api.OPDSURIRewriterType;
import one.lfa.opdsget.api.OPDSURIRewriters;
import one.lfa.opdsget.vanilla.OPDSManifestWriters;
import one.lfa.opdsget.vanilla.OPDSRetrievers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main command line program.
 */

public final class Main
{
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private Main()
  {

  }

  /**
   * Main entry point.
   *
   * @param args Command line arguments
   */

  public static void main(final String[] args)
  {
    final var parsed_arguments = new Arguments();

    final var jcommander =
      JCommander.newBuilder()
        .programName("opdsget")
        .addObject(parsed_arguments)
        .build();

    try {
      jcommander.parse(args);
    } catch (final ParameterException e) {
      LOG.error("could not parse command line arguments: {}", e.getMessage());
      final var sb = new StringBuilder(128);
      jcommander.setConsole(new StringBuilderConsole(sb));
      jcommander.usage();
      System.err.println(sb.toString());
      System.exit(1);
      return;
    }

    {
      final var root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
      root.setLevel(parsed_arguments.log_level.level);
    }

    final List<OPDSGetKind> excluded_kinds;
    try {
      excluded_kinds =
        parsed_arguments.exclude_content_kinds.stream()
          .map(OPDSGetKind::ofName)
          .collect(Collectors.toList());
    } catch (final Exception e) {
      LOG.error(
        "could not parse one or more content kinds: {}",
        e.getMessage());
      final var sb = new StringBuilder(128);
      jcommander.setConsole(new StringBuilderConsole(sb));
      jcommander.usage();
      System.err.println(sb.toString());
      System.exit(1);
      return;
    }

    final var included_kinds =
      Stream.of(OPDSGetKind.values())
        .filter(kind -> !excluded_kinds.contains(kind))
        .collect(Collectors.toSet());

    LOG.debug("excluding content kinds: {}", excluded_kinds);
    LOG.debug("including content kinds: {}", included_kinds);

    final var exec =
      Executors.newFixedThreadPool(
        4,
        runnable -> {
          final var th = new Thread(runnable);
          th.setName("one.lfa.opdsget.io[" + th.getId() + "]");
          return th;
        });

    try {
      final var builder =
        OPDSGetConfiguration.builder()
          .setOutput(parsed_arguments.output_directory.toAbsolutePath())
          .setOutputManifestBaseURI(Optional.ofNullable(parsed_arguments.output_manifest_base_uri))
          .setOutputManifestID(parsed_arguments.output_manifest_uuid)
          .setOutputManifestTitle(parsed_arguments.output_manifest_title)
          .setRemoteURI(parsed_arguments.feed)
          .setFetchedKinds(included_kinds)
          .setUriRewriter(uriRewriterStrategy(
            parsed_arguments,
            parsed_arguments.uri_rewrite_strategy))
          .setOutputArchive(
            Optional.ofNullable(parsed_arguments.output_archive)
              .map(Paths::get)
              .map(Path::toAbsolutePath))
          .setScaleImages(OptionalDouble.of(parsed_arguments.scaleCoverImages))
          .setAuthenticationSupplier(loadAuth(parsed_arguments.auth));

      if (parsed_arguments.squash) {
        builder.setSquash(
          OPDSSquashConfiguration.builder()
            .setMaximumImageHeight(parsed_arguments.image_max_height)
            .setMaximumImageWidth(parsed_arguments.image_max_width)
            .setScaleFactor(parsed_arguments.image_scale)
            .build());
      }

      final var config = builder.build();

      final var retriever =
        OPDSRetrievers.providerWith(
          new EPUBSquashers(),
          new OPDSManifestWriters(),
          new OPDSHTTPDefault())
          .create(exec);

      retriever.retrieve(config).get();
    } catch (final ParseException e) {
      LOG.error("error parsing authentication file: ", e);
      System.exit(1);
    } catch (final ExecutionException e) {
      LOG.error("error retrieving feed: ", e.getCause());
      System.exit(1);
    } catch (final Exception e) {
      LOG.error("error retrieving feed: ", e);
      System.exit(1);
    } finally {
      exec.shutdown();
    }
  }

  private static OPDSURIRewriterType uriRewriterStrategy(
    final Arguments arguments,
    final OPDSURIRewriteStrategy strategy)
  {
    switch (strategy) {
      case NONE:
        return OPDSURIRewriters.noRewriter();
      case NAMED:
        return OPDSURIRewriters.namedSchemeRewriter(
          arguments.uri_rewrite_scheme_name,
          arguments.output_directory.toAbsolutePath());
      case RELATIVE:
        return OPDSURIRewriters.relativeRewriter();
    }

    throw new IllegalStateException("Unreachable code");
  }

  private static Function<URI, Optional<OPDSAuthenticationType>> loadAuth(
    final String auth)
    throws IOException, ParseException
  {
    if (auth != null) {
      final var path = Paths.get(auth);
      try (var stream = Files.newInputStream(path)) {
        return OPDSAuthenticationPatternMappedParser.parse(
          path.toUri(), stream);
      }
    }
    return uri -> Optional.empty();
  }

  enum OPDSLogLevel
  {
    ERROR(Level.ERROR),
    INFO(Level.INFO),
    DEBUG(Level.DEBUG),
    TRACE(Level.TRACE);

    private final Level level;

    OPDSLogLevel(final Level in_level)
    {
      this.level = Objects.requireNonNull(in_level, "level");
    }

    @Override
    public String toString()
    {
      switch (this) {
        case ERROR:
          return "error";
        case INFO:
          return "info";
        case DEBUG:
          return "debug";
        case TRACE:
          return "trace";
      }

      throw new IllegalStateException("Unreachable code");
    }
  }

  enum OPDSURIRewriteStrategy
  {
    /**
     * Links are not rewritten.
     */

    NONE,

    /**
     * Links are rewritten as absolute URIs using a named scheme.
     */

    NAMED,

    /**
     * Links are rewritten to be bare relative links.
     */

    RELATIVE
  }

  private static final class Arguments
  {
    @Parameter(
      names = "--log-level",
      description = "The logging level",
      converter = OPDSLogLevelConverter.class,
      required = false)
    private OPDSLogLevel log_level = OPDSLogLevel.INFO;

    @Parameter(
      names = "--feed",
      description = "The URI of the remote feed",
      required = true)
    private URI feed;

    @Parameter(
      names = "--output-directory",
      description = "The directory that will contain the downloaded feed objects",
      required = true)
    private Path output_directory;

    @Parameter(
      names = "--output-archive",
      description = "The zip archive that will be created for the feed",
      required = false)
    private String output_archive;

    @Parameter(
      names = "--output-manifest-base-uri",
      description = "The base URI that will be placed into manifest files",
      required = false)
    private URI output_manifest_base_uri;

    @Parameter(
      names = "--output-manifest-id",
      description = "The UUID that will be placed into manifest files",
      converter = UUIDConverter.class,
      required = false)
    private UUID output_manifest_uuid = UUID.randomUUID();

    @Parameter(
      names = "--output-manifest-title",
      description = "The title that will be placed into manifest files",
      required = false)
    private String output_manifest_title;

    @Parameter(
      names = "--authentication",
      description = "The file containing authentication information",
      required = false)
    private String auth;

    @Parameter(
      names = "--uri-rewrite-strategy",
      description = "The strategy that will be used to rewrite URIs",
      required = false)
    private OPDSURIRewriteStrategy uri_rewrite_strategy = OPDSURIRewriteStrategy.RELATIVE;

    @Parameter(
      names = "--uri-rewrite-scheme-name",
      description = "The name of the URI scheme used to rewrite URIs (if applicable)",
      required = false)
    private String uri_rewrite_scheme_name = "file";

    @Parameter(
      names = "--exclude-content-kind",
      description = "The kind of content that will not be downloaded (Specify multiple times for multiple kinds)",
      required = false)
    private List<String> exclude_content_kinds = List.of();

    @Parameter(
      names = "--squash-image-max-width",
      required = false,
      description = "The maximum width of images")
    private double image_max_width = 1600.0;

    @Parameter(
      names = "--squash-image-max-height",
      required = false,
      description = "The maximum height of images")
    private double image_max_height = 1170.0;

    @Parameter(
      names = "--squash-image-scale",
      required = false,
      description = "The image scale value")
    private double image_scale = 1.0;

    @Parameter(
      names = "--squash",
      required = false,
      description = "True if EPUB files should be squashed to reduce their size")
    private boolean squash;

    @Parameter(
      names = "--scale-cover-images",
      required = false,
      description = "A scale value in the range (0.0, 1.0] by which to scale cover images")
    private double scaleCoverImages = 1.0;

    Arguments()
    {

    }
  }

  private static final class StringBuilderConsole implements Console
  {
    private final StringBuilder stringBuilder;

    StringBuilderConsole(
      final StringBuilder inStringBuilder)
    {
      this.stringBuilder = Objects.requireNonNull(
        inStringBuilder,
        "inStringBuilder");
    }

    @Override
    public void print(final String msg)
    {
      this.stringBuilder.append(msg);
    }

    @Override
    public void println(final String msg)
    {
      this.stringBuilder.append(msg);
      this.stringBuilder.append(System.lineSeparator());
    }

    @Override
    public char[] readPassword(final boolean echoInput)
    {
      return new char[0];
    }
  }

  final class OPDSLogLevelConverter implements IStringConverter<OPDSLogLevel>
  {
    OPDSLogLevelConverter()
    {

    }

    @Override
    public OPDSLogLevel convert(final String value)
    {
      switch (value) {
        case "error":
          return OPDSLogLevel.ERROR;
        case "info":
          return OPDSLogLevel.INFO;
        case "debug":
          return OPDSLogLevel.DEBUG;
        case "trace":
          return OPDSLogLevel.TRACE;
        default:
          throw new IllegalArgumentException("Could not parse '" + value + "' as log level");
      }
    }
  }
}
