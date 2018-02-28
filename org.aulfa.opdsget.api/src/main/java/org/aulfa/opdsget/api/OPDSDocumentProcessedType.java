package org.aulfa.opdsget.api;

import org.immutables.value.Value;

import java.net.URI;
import java.util.Map;

/**
 * The result of processing a single OPDS feed document.
 */

@ImmutableStyleType
@Value.Immutable
public interface OPDSDocumentProcessedType
{
  /**
   * @return The set of feeds that need to be fetched
   */

  @Value.Default
  @Value.Parameter
  default Map<URI, OPDSLocalFile> feeds()
  {
    return Map.of();
  }

  /**
   * @return The set of images that need to be fetched
   */

  @Value.Default
  @Value.Parameter
  default Map<URI, OPDSLocalFile> images()
  {
    return Map.of();
  }

  /**
   * @return The set of books that need to be fetched
   */

  @Value.Default
  @Value.Parameter
  default Map<URI, OPDSLocalFile> books()
  {
    return Map.of();
  }

  /**
   * @return The path to the saved feed file
   */

  @Value.Parameter
  OPDSLocalFile file();
}
