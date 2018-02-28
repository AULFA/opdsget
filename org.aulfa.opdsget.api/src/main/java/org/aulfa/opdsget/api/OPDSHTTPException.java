package org.aulfa.opdsget.api;

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
}
