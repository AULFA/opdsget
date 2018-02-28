package org.aulfa.opdsget.api;

import org.w3c.dom.Document;

/**
 * A processor for a single OPDS document.
 */

public interface OPDSDocumentProcessorType
{
  /**
   * Process an input document.
   *
   * @param configuration The OPDS retriever configuration
   * @param document      The input document
   *
   * @return A processed document
   *
   * @throws Exception On errors
   */

  OPDSDocumentProcessed process(
    OPDSGetConfiguration configuration,
    Document document)
    throws Exception;
}
