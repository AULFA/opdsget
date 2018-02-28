package org.aulfa.opdsget.cmdline;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.aulfa.opdsget.api.OPDSAuthenticationPatternMappedParser;
import org.aulfa.opdsget.api.OPDSAuthenticationType;
import org.aulfa.opdsget.api.OPDSGetConfiguration;
import org.aulfa.opdsget.api.OPDSHTTPDefault;
import org.aulfa.opdsget.api.OPDSRetrieverType;
import org.aulfa.opdsget.vanilla.OPDSRetrievers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * The main command line program.
 */

public final class Main
{
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private Main()
  {

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

  private static final class Arguments
  {
    Arguments()
    {

    }

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
  }

  /**
   * Main entry point.
   *
   * @param args Command line arguments
   */

  public static void main(final String[] args)
  {
    final Arguments parsed_arguments = new Arguments();

    final JCommander jcommander =
      JCommander.newBuilder()
        .programName("opdsget")
        .addObject(parsed_arguments)
        .build();

    try {
      jcommander.parse(args);
    } catch (final ParameterException e) {
      LOG.error("could not parse command line arguments: {}", e.getMessage());
      final StringBuilder sb = new StringBuilder(128);
      jcommander.usage(sb);
      System.err.println(sb.toString());
      System.exit(1);
    }

    {
      final ch.qos.logback.classic.Logger root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
          Logger.ROOT_LOGGER_NAME);
      root.setLevel(parsed_arguments.log_level.level);
    }

    final ExecutorService exec =
      Executors.newFixedThreadPool(
        4,
        runnable -> {
          final Thread th = new Thread(runnable);
          th.setName("org.aulfa.opdsget.io[" + th.getId() + "]");
          return th;
        });

    try {
      final OPDSGetConfiguration config =
        OPDSGetConfiguration.builder()
          .setOutput(parsed_arguments.output_directory)
          .setRemoteURI(parsed_arguments.feed)
          .setOutputArchive(
            Optional.ofNullable(parsed_arguments.output_archive)
              .map(Paths::get))
          .setAuthenticationSupplier(loadAuth(parsed_arguments.auth))
          .build();

      final OPDSRetrieverType retriever =
        OPDSRetrievers.providerWith(new OPDSHTTPDefault())
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
      final Path path = Paths.get(auth);
      try (InputStream stream = Files.newInputStream(path)) {
        return OPDSAuthenticationPatternMappedParser.parse(
          path.toUri(), stream);
      }
    }
    return uri -> Optional.empty();
  }
}