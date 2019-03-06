package au.org.libraryforall.opdsget.vanilla;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
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
    final DocumentBuilder builder = this.builders.newDocumentBuilder();
    try {
      return builder.parse(stream, uri.toString());
    } finally {
      LOG.debug("parsed {}", uri);
    }
  }
}
