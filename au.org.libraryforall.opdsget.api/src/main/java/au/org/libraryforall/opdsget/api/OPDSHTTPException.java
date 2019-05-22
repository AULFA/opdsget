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

import java.io.IOException;
import java.util.Objects;

/**
 * The type of exceptions raised when speaking HTTP.
 */

public final class OPDSHTTPException extends IOException
{
  private final int code;
  private final String response;

  /**
   * Construct an exception.
   *
   * @param message     The message
   * @param in_code     The response code
   * @param in_response The response message
   */

  public OPDSHTTPException(
    final String message,
    final int in_code,
    final String in_response)
  {
    super(message);
    this.code = in_code;
    this.response = Objects.requireNonNull(in_response, "response");
  }

  /**
   * Construct an exception.
   *
   * @param cause       The underlying cause
   * @param message     The message
   * @param in_code     The response code
   * @param in_response The response message
   */

  public OPDSHTTPException(
    final String message,
    final Throwable cause,
    final int in_code,
    final String in_response)
  {
    super(message, cause);
    this.code = in_code;
    this.response = Objects.requireNonNull(in_response, "response");
  }

  /**
   * Construct an exception.
   *
   * @param cause       The underlying cause
   * @param in_code     The response code
   * @param in_response The response message
   */

  public OPDSHTTPException(
    final Throwable cause,
    final int in_code,
    final String in_response)
  {
    super(cause);
    this.code = in_code;
    this.response = Objects.requireNonNull(in_response, "response");
  }

  /**
   * @return The HTTP response code
   */

  public int responseCode()
  {
    return this.code;
  }

  /**
   * @return The HTTP response message
   */

  public String responseMessage()
  {
    return this.response;
  }
}
