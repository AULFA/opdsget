package org.aulfa.opdsget.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A parser for pattern-mapped authentication.
 */

public final class OPDSAuthenticationPatternMappedParser
{
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  private OPDSAuthenticationPatternMappedParser()
  {

  }

  /**
   * Parse a pattern-mapped authentication matcher from the given stream.
   *
   * @param file   The URI of the stream, for diagnostics
   * @param stream The input stream
   *
   * @return A matcher
   *
   * @throws IOException    On I/O errors
   * @throws ParseException On parse errors
   */

  public static OPDSAuthenticationPatternMapped parse(
    final URI file,
    final InputStream stream)
    throws IOException, ParseException
  {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(stream, "stream");

    final List<OPDSMatchingAuthentication> patterns = new ArrayList<>(8);
    try (BufferedReader reader =
           new BufferedReader(new InputStreamReader(stream, UTF_8))) {

      int line_number = 1;
      while (true) {
        try {
          final String line = reader.readLine();
          if (line == null) {
            return OPDSAuthenticationPatternMapped.of(patterns);
          }

          if (line.startsWith("#")) {
            continue;
          }

          final String trimmed = line.trim();
          if (trimmed.isEmpty()) {
            continue;
          }

          final List<String> segments = List.of(WHITESPACE.split(trimmed));
          if (segments.size() != 2) {
            throw syntaxError(
              file,
              line_number,
              "<pattern> ( <authentication-value> | 'none' )",
              line);
          }

          final Pattern pattern =
            Pattern.compile(segments.get(0));
          final Optional<OPDSAuthenticationType> auth =
            parseAuthentication(file, line_number, segments.get(1));

          patterns.add(OPDSMatchingAuthentication.of(pattern.pattern(), auth));
        } finally {
          ++line_number;
        }
      }
    }
  }

  private static Optional<OPDSAuthenticationType> parseAuthentication(
    final URI file,
    final int line_number,
    final String text)
    throws ParseException
  {
    if (text.endsWith("none")) {
      return Optional.empty();
    }

    if (text.startsWith("basic:")) {
      final List<String> segments = List.of(text.split(":"));
      if (segments.size() != 3) {
        throw syntaxError(
          file,
          line_number,
          "'basic' , ':' , <username> , ':' , <password>",
          text);
      }

      return Optional.of(
        OPDSAuthenticationBasic.of(segments.get(1), segments.get(2)));
    }

    throw syntaxError(
      file,
      line_number,
      "( <authentication-value> | 'none' )",
      text);
  }

  private static ParseException syntaxError(
    final URI uri,
    final int line_number,
    final String expected,
    final String received)
  {
    return new ParseException(
      new StringBuilder(64)
        .append(uri)
        .append(":")
        .append(line_number)
        .append(": Parse error.")
        .append(System.lineSeparator())
        .append("  Expected: ")
        .append(expected)
        .append(System.lineSeparator())
        .append("  Received: ")
        .append(received)
        .toString(),
      line_number);
  }
}
