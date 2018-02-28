package org.aulfa.opdsget.api;

import java.util.concurrent.ExecutorService;

/**
 * The type of retriever providers.
 *
 * @see OPDSRetrieverType
 */

public interface OPDSRetrieverProviderType
{
  /**
   * Create a new OPDS retriever.
   *
   * @param executor The executor that will be used for I/O
   *
   * @return A new retriever
   */

  OPDSRetrieverType create(ExecutorService executor);
}
