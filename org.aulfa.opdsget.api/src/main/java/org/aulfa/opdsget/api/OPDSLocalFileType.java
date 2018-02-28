package org.aulfa.opdsget.api;

import org.immutables.value.Value;

import java.net.URI;
import java.nio.file.Path;

/**
 * A file that has been downloaded to the local filesystem.
 */

@ImmutableStyleType
@Value.Immutable
public interface OPDSLocalFileType
{
  /**
   * @return The original URI
   */

  @Value.Parameter
  URI uri();

  /**
   * @return The output file
   */

  @Value.Parameter
  Path file();
}
