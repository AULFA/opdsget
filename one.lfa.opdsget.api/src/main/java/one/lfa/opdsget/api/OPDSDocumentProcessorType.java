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

package one.lfa.opdsget.api;

import org.w3c.dom.Document;

/**
 * A processor for a single OPDS document.
 */

public interface OPDSDocumentProcessorType
{
  /**
   * Process an input document. Document processors are responsible for doing various tasks such as
   * rewriting link elements to point to locally downloaded files, modifying time-related fields to
   * ensure reproducibility, and so on. Processor implementations modify {@code document} in-place
   * and so callers should call {@link org.w3c.dom.Document#cloneNode(boolean)} on {@code document}
   * if they want the processor to operate on an isolated copy instead of modifying the original.
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
