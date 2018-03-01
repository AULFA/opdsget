package org.aulfa.opdsget.api;

import org.w3c.dom.Document;

/**
 * A processor for a single OPDS document.
 */

public interface OPDSDocumentProcessorType
{
  /**
   * Process an input document. Document processors are responsible for
   * doing various tasks such as rewriting link elements to point to locally
   * downloaded files, modifying time-related fields to ensure reproducibility,
   * and so on. Processor implementations modify {@code document} in-place and
   * so callers should call {@link org.w3c.dom.Document#cloneNode(boolean)}
   * on {@code document} if they want the processor to operate on an isolated
   * copy instead of modifying the original.
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
