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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * The default implementation of the {@link OPDSHTTPType} interface.
 */

public final class OPDSHTTPDefault implements OPDSHTTPType
{
  private static final Logger LOG = LoggerFactory.getLogger(OPDSHTTPDefault.class);
  private static final long RETRY_WAIT_SECONDS = 6L;
  private static final int RETRY_MAX_ATTEMPTS = 10;

  /**
   * Create an http provider.
   */

  public OPDSHTTPDefault()
  {

  }

  private static void configureConnectionAuth(
    final HttpURLConnection connection,
    final OPDSAuthenticationType auth)
  {
    switch (auth.kind()) {
      case AUTHENTICATION_BASIC: {
        final var basic = (OPDSAuthenticationBasic) auth;
        final var text =
          new StringBuilder(64)
            .append(basic.user())
            .append(":")
            .append(basic.password())
            .toString();

        final var encoded =
          Base64.getEncoder()
            .encodeToString(text.getBytes(StandardCharsets.US_ASCII));

        connection.addRequestProperty("Authorization", "Basic " + encoded);
        break;
      }
    }
  }

  @Override
  public OPDSHTTPData get(
    final URI uri,
    final Optional<OPDSAuthenticationType> auth_opt)
    throws OPDSHTTPException
  {
    try {
      LOG.debug("GET {}", uri);

      final URL url;
      try {
        url = uri.toURL();
      } catch (final MalformedURLException e) {
        throw new OPDSHTTPException(e, -1, "");
      }

      for (var attempt = 0; attempt < RETRY_MAX_ATTEMPTS; ++attempt) {
        try {
          final var connection = (HttpURLConnection) url.openConnection();
          connection.setInstanceFollowRedirects(false);
          connection.setRequestMethod("GET");
          connection.setRequestProperty("User-Agent", "au.org.libraryforall.opdsget");

          auth_opt.ifPresent(auth -> configureConnectionAuth(connection, auth));

          final var code = connection.getResponseCode();
          if (LOG.isDebugEnabled()) {
            LOG.debug(
              "GET {} -> {} ({} of {})",
              uri,
              Integer.valueOf(code),
              Integer.valueOf(attempt + 1),
              Integer.valueOf(RETRY_MAX_ATTEMPTS));
          }

          switch (connection.getResponseCode()) {
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
              final var location = URLDecoder.decode(connection.getHeaderField("Location"), "UTF-8");
              final var base = new URL(url.toString());
              final var next = new URL(base, location);

              try {
                return this.get(
                  new URI(next.getProtocol(), next.getHost(), next.getPath(), next.getQuery()),
                  Optional.empty());
              } catch (final URISyntaxException e) {
                throw new OPDSHTTPException(e, -1, "");
              }
          }

          if (code >= 500) {
            final var message = connection.getResponseMessage();
            final var failure_message = failureMessage(uri, code, message);
            LOG.error("{}", failure_message);
            delay();
            continue;
          }

          if (code >= 400) {
            final var message = connection.getResponseMessage();
            final var failure_message = failureMessage(uri, code, message);
            throw new OPDSHTTPException(failure_message, code, message);
          }

          return OPDSHTTPData.of(
            connection.getContentLengthLong(),
            connection.getContentType(),
            connection.getInputStream());
        } catch (final IOException e) {
          LOG.error("i/o error: GET {}: ", uri, e);
          delay();
        }
      }

      throw new OPDSHTTPException(
        new StringBuilder(128)
          .append("Failed to retrieve URI after repeated attempts")
          .append(System.lineSeparator())
          .append("  URI: ")
          .append(uri)
          .append(System.lineSeparator())
          .append("  Attempts: ")
          .append(RETRY_MAX_ATTEMPTS)
          .append(System.lineSeparator())
          .toString(),
        -1,
        "");

    } catch (final OPDSHTTPException e) {
      throw e;
    }
  }

  private static void delay()
  {
    try {
      final var seconds = RETRY_WAIT_SECONDS;
      LOG.debug("waiting for {} seconds", Long.valueOf(seconds));
      Thread.sleep(seconds * 1000L);
    } catch (final InterruptedException e) {
      LOG.error("delay interrupted: ", e);
    }
  }

  private static String failureMessage(
    final URI uri,
    final int code,
    final String message)
  {
    return new StringBuilder(128)
      .append("GET failed: ")
      .append(code)
      .append(" ")
      .append(message)
      .append(System.lineSeparator())
      .append("  URI: ")
      .append(uri)
      .append(System.lineSeparator())
      .toString();
  }
}
