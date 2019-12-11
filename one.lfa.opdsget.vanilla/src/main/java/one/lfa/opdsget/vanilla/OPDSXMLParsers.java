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

package one.lfa.opdsget.vanilla;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

/**
 * Functions to parse XML.
 */

public final class OPDSXMLParsers
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OPDSXMLParsers.class);

  private final DocumentBuilderFactory builders;

  /**
   * Construct a new parser provider.
   */

  public OPDSXMLParsers()
  {
    try {
      this.builders = DocumentBuilderFactory.newDefaultInstance();
      this.builders.setValidating(false);
      this.builders.setNamespaceAware(true);
      this.builders.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    } catch (final ParserConfigurationException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Parse a document from the given stream.
   *
   * @param uri    The source URI
   * @param stream The input stream
   *
   * @return A parsed document
   *
   * @throws ParserConfigurationException On parser configuration errors
   * @throws IOException                  On I/O errors
   * @throws SAXException                 On parse errors
   */

  public Document parse(
    final URI uri,
    final InputStream stream)
    throws ParserConfigurationException, IOException, SAXException
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");

    LOG.debug("parse {}", uri);
    final var builder = this.builders.newDocumentBuilder();
    try {
      return builder.parse(stream, uri.toString());
    } finally {
      LOG.debug("parsed {}", uri);
    }
  }
}
