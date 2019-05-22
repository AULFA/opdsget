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
