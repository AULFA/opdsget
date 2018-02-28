package org.aulfa.opdsget.api;

import java.net.URI;
import java.util.Optional;

/**
 * An abstraction over HTTP requests.
 */

public interface OPDSHTTPType
{
  /**
   * Make a GET request.
   *
   * @param uri  The URI
   * @param auth The authentication data, if any
   *
   * @return The remote HTTP data
   *
   * @throws OPDSHTTPException On errors such as I/O errors or error codes from the server
   */

  OPDSHTTPData get(
    URI uri,
    Optional<OPDSAuthenticationType> auth)
    throws OPDSHTTPException;
}
