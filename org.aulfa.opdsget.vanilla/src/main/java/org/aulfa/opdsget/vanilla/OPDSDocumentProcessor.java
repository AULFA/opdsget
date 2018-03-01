package org.aulfa.opdsget.vanilla;

import org.aulfa.opdsget.api.OPDSDocumentProcessed;
import org.aulfa.opdsget.api.OPDSDocumentProcessorType;
import org.aulfa.opdsget.api.OPDSGetConfiguration;
import org.aulfa.opdsget.api.OPDSLocalFile;
import org.aulfa.opdsget.api.OPDSURIHashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The default document processor implementation.
 */

public final class OPDSDocumentProcessor implements OPDSDocumentProcessorType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OPDSDocumentProcessor.class);

  private final XPathFactory xpath_factory;
  private final XPath xpath;
  private final XPathExpression xpath_links;
  private final XPathExpression xpath_updated;

  /**
   * Construct a new processor.
   */

  public OPDSDocumentProcessor()
  {
    this.xpath_factory = XPathFactory.newInstance();
    this.xpath = this.xpath_factory.newXPath();

    try {
      this.xpath_links = this.xpath.compile(
        "//*[local-name()='link' and namespace-uri()='http://www.w3.org/2005/Atom']");
      this.xpath_updated = this.xpath.compile(
        "//*[local-name()='updated' and namespace-uri()='http://www.w3.org/2005/Atom']");
    } catch (final XPathExpressionException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void rewriteLinkTarget(
    final Function<OPDSLocalFile, URI> rewriter,
    final Element link,
    final OPDSLocalFile file)
  {
    link.setAttribute("href", rewriter.apply(file).toString());
  }

  /*
   * Collect link elements, rewriting them to point to local files. Remove
   * any link elements that we don't know how to handle.
   */

  private static void removeElement(final Node element)
  {
    final Node parent = element.getParentNode();
    parent.removeChild(element);
  }

  /*
   * Remove all "updated" elements from the document.
   */

  @Override
  public OPDSDocumentProcessed process(
    final OPDSGetConfiguration configuration,
    final Document document)
    throws Exception
  {
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(document, "document");

    final Map<URI, OPDSLocalFile> feeds = new HashMap<>();
    final Map<URI, OPDSLocalFile> images = new HashMap<>();
    final Map<URI, OPDSLocalFile> books = new HashMap<>();

    this.rewriteAndCollectLinks(configuration, document, feeds, images, books);
    this.removeUpdatedElements(document);

    final URI document_uri =
      URI.create(document.getDocumentURI());
    final String hash =
      OPDSURIHashing.hashOf(document_uri);
    final Path file =
      configuration.feedFile(hash + ".atom");

    return OPDSDocumentProcessed.builder()
      .setFeeds(feeds)
      .setBooks(books)
      .setImages(images)
      .setFile(OPDSLocalFile.of(document_uri, file))
      .build();
  }

  private void rewriteAndCollectLinks(
    final OPDSGetConfiguration configuration,
    final Document document,
    final Map<URI, OPDSLocalFile> feeds,
    final Map<URI, OPDSLocalFile> images,
    final Map<URI, OPDSLocalFile> books)
    throws XPathExpressionException
  {
    final NodeList links =
      (NodeList) this.xpath_links.evaluate(document, XPathConstants.NODESET);

    final Function<OPDSLocalFile, URI> rewriter = configuration.uriRewriter();
    for (int index = 0; index < links.getLength(); ++index) {
      final Element link = (Element) links.item(index);

      final String relation = link.getAttribute("rel");
      switch (relation) {
        case "alternate": {
          final String type = link.getAttribute("type");
          if (type != null && type.contains("application/atom+xml")) {
            final URI target = URI.create(link.getAttribute("href"));
            final Path path = configuration.feedFileHashed(target);
            final OPDSLocalFile file = OPDSLocalFile.of(target, path);
            feeds.put(target, file);
            rewriteLinkTarget(rewriter, link, file);
          } else {
            LOG.debug("removing link with rel {} and type {}", relation, type);
            removeElement(link);
          }
          break;
        }

        case "next": {
          final URI target = URI.create(link.getAttribute("href"));
          final Path path = configuration.feedFileHashed(target);
          final OPDSLocalFile file = OPDSLocalFile.of(target, path);
          feeds.put(target, file);
          rewriteLinkTarget(rewriter, link, file);
          break;
        }

        case "http://opds-spec.org/image":
        case "http://opds-spec.org/image/thumbnail": {
          final URI target = URI.create(link.getAttribute("href"));
          final Path path = configuration.imageFileHashed(target);
          final OPDSLocalFile file = OPDSLocalFile.of(target, path);
          images.put(target, file);
          rewriteLinkTarget(rewriter, link, file);
          break;
        }

        case "http://opds-spec.org/acquisition/open-access": {
          final URI target = URI.create(link.getAttribute("href"));
          final Path path = configuration.bookFileHashed(target);
          final OPDSLocalFile file = OPDSLocalFile.of(target, path);
          books.put(target, file);
          rewriteLinkTarget(rewriter, link, file);
          break;
        }

        default: {
          LOG.debug("removing link with rel attribute '{}'", relation);
          removeElement(link);
          break;
        }
      }
    }
  }

  private void removeUpdatedElements(final Document document)
    throws XPathExpressionException
  {
    final NodeList updateds =
      (NodeList) this.xpath_updated.evaluate(document, XPathConstants.NODESET);

    for (int index = 0; index < updateds.getLength(); ++index) {
      final Element updated = (Element) updateds.item(index);
      updated.setTextContent("2000-01-01T00:00:00Z");
    }
  }
}






















