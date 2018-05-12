package org.aulfa.opdsget.api;

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
      connection.setRequestProperty("User-Agent", "org.aulfa.opdsget");

      auth_opt.ifPresent(auth -> configureConnectionAuth(connection, auth));

      final int code = connection.getResponseCode();
      if (LOG.isDebugEnabled()) {
        LOG.debug("GET {} -> {}", uri, Integer.valueOf(code));
      }

      switch (connection.getResponseCode())
      {
        case HttpURLConnection.HTTP_MOVED_PERM:
        case HttpURLConnection.HTTP_MOVED_TEMP:
          final String location = URLDecoder.decode(connection.getHeaderField("Location"), "UTF-8");
          final URL base = new URL(url.toString());
          final URL next = new URL(base, location);
          try {
            return get(next.toURI(), Optional.empty());
          } catch (URISyntaxException e) {
            throw new OPDSHTTPException(e.getCause(), -1, "");
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
}
