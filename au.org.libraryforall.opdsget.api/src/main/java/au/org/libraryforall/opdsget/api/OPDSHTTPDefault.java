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
  private static final Logger LOG =
    LoggerFactory.getLogger(OPDSHTTPDefault.class);

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
        final OPDSAuthenticationBasic basic = (OPDSAuthenticationBasic) auth;
        final String text =
          new StringBuilder(64)
            .append(basic.user())
            .append(":")
            .append(basic.password())
            .toString();

        final String encoded =
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

      final URL url = uri.toURL();
      final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setInstanceFollowRedirects(false);
      connection.setRequestMethod("GET");
      connection.setRequestProperty("User-Agent", "au.org.libraryforall.opdsget");

      auth_opt.ifPresent(auth -> configureConnectionAuth(connection, auth));

      final int code = connection.getResponseCode();
      if (LOG.isDebugEnabled()) {
        LOG.debug("GET {} -> {}", uri, Integer.valueOf(code));
      }

      switch (connection.getResponseCode()) {
        case HttpURLConnection.HTTP_MOVED_PERM:
        case HttpURLConnection.HTTP_MOVED_TEMP:
          final String location = URLDecoder.decode(connection.getHeaderField("Location"), "UTF-8");
          final URL base = new URL(url.toString());
          final URL next = new URL(base, location);

          try {
            return this.get(
              new URI(next.getProtocol(), next.getHost(), next.getPath(), next.getQuery()),
              Optional.empty());
          } catch (final URISyntaxException e) {
            throw new OPDSHTTPException(e, -1, "");
          }
      }

      if (code >= 400) {
        final String message = connection.getResponseMessage();
        throw new OPDSHTTPException(
          new StringBuilder(128)
            .append("GET failed: ")
            .append(code)
            .append(" ")
            .append(message)
            .toString(), code, message);
      }

      return OPDSHTTPData.of(
        connection.getContentLengthLong(),
        connection.getContentType(),
        connection.getInputStream());
    } catch (final OPDSHTTPException e) {
      throw e;
    } catch (final IOException e) {
      throw new OPDSHTTPException(e.getCause(), -1, "");
    }
  }
}
