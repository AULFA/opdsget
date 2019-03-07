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

package au.org.libraryforall.opdsget.cmdline;

import au.org.libraryforall.epubsquash.vanilla.EPUBSquashers;
import au.org.libraryforall.opdsget.api.OPDSAuthenticationPatternMappedParser;
import au.org.libraryforall.opdsget.api.OPDSAuthenticationType;
import au.org.libraryforall.opdsget.api.OPDSGetConfiguration;
import au.org.libraryforall.opdsget.api.OPDSGetKind;
import au.org.libraryforall.opdsget.api.OPDSHTTPDefault;
import au.org.libraryforall.opdsget.api.OPDSSquashConfiguration;
import au.org.libraryforall.opdsget.api.OPDSURIRewriters;
import au.org.libraryforall.opdsget.vanilla.OPDSRetrievers;
import ch.qos.logback.classic.Level;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
      jcommander.usage(sb);
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
      LOG.error("could not parse one or more content kinds: {}", e.getMessage());
      final var sb = new StringBuilder(128);
      jcommander.usage(sb);
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
          th.setName("au.org.libraryforall.opdsget.io[" + th.getId() + "]");
          return th;
        });

    try {
      final var builder =
        OPDSGetConfiguration.builder()
          .setOutput(parsed_arguments.output_directory)
          .setRemoteURI(parsed_arguments.feed)
          .setFetchedKinds(included_kinds)
          .setUriRewriter(OPDSURIRewriters.namedSchemeRewriter(
            parsed_arguments.uri_rewrite_scheme,
            parsed_arguments.output_directory))
          .setOutputArchive(
            Optional.ofNullable(parsed_arguments.output_archive)
              .map(Paths::get))
          .setAuthenticationSupplier(loadAuth(parsed_arguments.auth));

      if (parsed_arguments.squash) {
        builder.setSquash(
          OPDSSquashConfiguration.builder()
            .setMaximumImageHeight(parsed_arguments.image_max_height)
            .setMaximumImageWidth(parsed_arguments.image_max_width)
            .build());
      }

      final var config = builder.build();

      final var retriever =
        OPDSRetrievers.providerWith(
          new EPUBSquashers(),
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
      names = "--authentication",
      description = "The file containing authentication information",
      required = false)
    private String auth;
    @Parameter(
      names = "--uri-rewrite-scheme",
      description = "The scheme that will be used for rewritten URIs",
      required = false)
    private String uri_rewrite_scheme = "file";
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
      names = "--squash",
      required = false,
      description = "True if EPUB files should be squashed to reduce their size")
    private boolean squash;

    Arguments()
    {

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
