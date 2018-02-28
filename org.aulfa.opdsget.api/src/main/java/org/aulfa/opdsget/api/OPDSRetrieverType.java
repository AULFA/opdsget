package org.aulfa.opdsget.api;

import java.util.concurrent.CompletableFuture;

/**
 * The type of retrievers for OPDS feeds.
 */

public interface OPDSRetrieverType
{
  /**
   * Fetch an OPDS feed.
   *
   * @param configuration The configuration data
   *
   * @return A future representing the retrieval in progress
   */

  CompletableFuture<Void> retrieve(
    OPDSGetConfiguration configuration);
}
