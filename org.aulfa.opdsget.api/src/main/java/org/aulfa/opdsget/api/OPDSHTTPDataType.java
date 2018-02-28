package org.aulfa.opdsget.api;

import org.immutables.value.Value;

import java.io.InputStream;

/**
 * The type of remote HTTP data.
 */

@ImmutableStyleType
@Value.Immutable
public interface OPDSHTTPDataType
{
  /**
   * @return The size of the remote content
   */

  @Value.Parameter
  long size();

  /**
   * @return The type of the remote content
   */

  @Value.Parameter
  String contentType();

  /**
   * @return The input stream for the remote content
   */

  @Value.Parameter
  InputStream stream();
}
