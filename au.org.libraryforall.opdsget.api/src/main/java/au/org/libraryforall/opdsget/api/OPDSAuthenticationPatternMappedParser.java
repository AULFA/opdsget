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

package au.org.libraryforall.opdsget.api;

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
    try (var reader =
           new BufferedReader(new InputStreamReader(stream, UTF_8))) {

      var line_number = 1;
      while (true) {
        try {
          final var line = reader.readLine();
          if (line == null) {
            return OPDSAuthenticationPatternMapped.of(patterns);
          }

          if (line.startsWith("#")) {
            continue;
          }

          final var trimmed = line.trim();
          if (trimmed.isEmpty()) {
            continue;
          }

          final var segments = List.of(WHITESPACE.split(trimmed));
          if (segments.size() != 2) {
            throw syntaxError(
              file,
              line_number,
              "<pattern> ( <authentication-value> | 'none' )",
              line);
          }

          final var pattern =
            Pattern.compile(segments.get(0));
          final var auth =
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
      final var segments = List.of(text.split(":"));
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
