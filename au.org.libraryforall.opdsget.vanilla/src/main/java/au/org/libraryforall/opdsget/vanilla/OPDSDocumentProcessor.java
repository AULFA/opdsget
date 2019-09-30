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

package au.org.libraryforall.opdsget.vanilla;

import au.org.libraryforall.opdsget.api.OPDSDocumentProcessed;
import au.org.libraryforall.opdsget.api.OPDSDocumentProcessorType;
import au.org.libraryforall.opdsget.api.OPDSGetConfiguration;
import au.org.libraryforall.opdsget.api.OPDSGetKind;
import au.org.libraryforall.opdsget.api.OPDSLocalFile;
import au.org.libraryforall.opdsget.api.OPDSURIHashing;
import au.org.libraryforall.opdsget.api.OPDSURIRewriterType;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
  private final XPathExpression xpath_title;

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
      this.xpath_title = this.xpath.compile(
        "//*[local-name()='title' and namespace-uri()='http://www.w3.org/2005/Atom']");
    } catch (final XPathExpressionException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void rewriteLinkTarget(
    final OPDSURIRewriterType rewriter,
    final Element link,
    final OPDSLocalFile source,
    final OPDSLocalFile target)
  {
    link.setAttribute("href", rewriter.rewrite(Optional.of(source), target).toString());
  }

  /*
   * Collect link elements, rewriting them to point to local files. Remove
   * any link elements that we don't know how to handle.
   */

  private static void removeElement(final Node element)
  {
    final var parent = element.getParentNode();
    parent.removeChild(element);
  }

  /*
   * Remove all "updated" elements from the document.
   */

  private static URI makeAbsolute(
    final URI base,
    final URI input)
  {
    return base.resolve(input);
  }

  private static URI constructLinkURI(
    final Document document,
    final Element link)
  {
    return makeAbsolute(
      URI.create(document.getDocumentURI()),
      URI.create(link.getAttribute("href")));
  }

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

    final var document_uri =
      URI.create(document.getDocumentURI());
    final var hash =
      OPDSURIHashing.hashOf(document_uri);
    final var file =
      configuration.feedFile(hash + ".atom");
    final var currentFile =
      OPDSLocalFile.of(document_uri, file);

    final var classification = this.findTitle(document);
    this.rewriteAndCollectLinks(configuration, currentFile, document, feeds, images, books);
    this.removeUpdatedElements(document);

    return OPDSDocumentProcessed.builder()
      .setFeeds(feeds)
      .setBooks(books)
      .setImages(images)
      .setFile(currentFile)
      .setTitle(classification.title)
      .setEntry(classification.isEntry)
      .build();
  }

  private Classification findTitle(final Document document)
    throws XPathExpressionException
  {
    final var root = document.getDocumentElement();
    final var isEntry = Objects.equals(root.getLocalName(), "entry");
    final String title;
    final var titles = (NodeList) this.xpath_title.evaluate(document, XPathConstants.NODESET);
    if (titles.getLength() > 0) {
      title = titles.item(0).getTextContent();
    } else {
      title = "";
    }
    return new Classification(isEntry, title);
  }

  private void rewriteAndCollectLinks(
    final OPDSGetConfiguration configuration,
    final OPDSLocalFile source,
    final Document document,
    final Map<URI, OPDSLocalFile> feeds,
    final Map<URI, OPDSLocalFile> images,
    final Map<URI, OPDSLocalFile> books)
    throws XPathExpressionException
  {
    final var links = (NodeList) this.xpath_links.evaluate(document, XPathConstants.NODESET);
    final var rewriter = configuration.uriRewriter();
    for (var index = 0; index < links.getLength(); ++index) {
      final var link = (Element) links.item(index);

      final var relation = link.getAttribute("rel");
      switch (relation) {
        case "http://opds-spec.org/featured":
        case "subsection":
        case "collection":
        case "alternate": {
          final var type = link.getAttribute("type");
          if (type != null && type.contains("application/atom+xml")) {
            final var target = constructLinkURI(document, link);
            final var path = configuration.feedFileHashed(target);
            final var file = OPDSLocalFile.of(target, path);
            feeds.put(target, file);
            rewriteLinkTarget(rewriter, link, source, file);
          } else {
            LOG.debug("removing link with rel {} and type {}", relation, type);
            removeElement(link);
          }
          break;
        }

        case "next": {
          final var target = constructLinkURI(document, link);
          final var path = configuration.feedFileHashed(target);
          final var file = OPDSLocalFile.of(target, path);
          feeds.put(target, file);
          rewriteLinkTarget(rewriter, link, source, file);
          break;
        }

        case "http://opds-spec.org/image":
        case "http://opds-spec.org/image/thumbnail": {
          if (configuration.fetchedKinds().contains(OPDSGetKind.OPDS_GET_IMAGES)) {
            final var target = constructLinkURI(document, link);
            final var path = configuration.imageFileHashed(target);
            final var file = OPDSLocalFile.of(target, path);
            images.put(target, file);
            rewriteLinkTarget(rewriter, link, source, file);
          }
          break;
        }

        case "http://opds-spec.org/acquisition":
        case "http://opds-spec.org/acquisition/open-access": {
          if (configuration.fetchedKinds().contains(OPDSGetKind.OPDS_GET_BOOKS)) {
            final var target = constructLinkURI(document, link);
            final var path = configuration.bookFileHashed(target);
            final var file = OPDSLocalFile.of(target, path);
            books.put(target, file);
            rewriteLinkTarget(rewriter, link, source, file);
          }
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
    final var updateds =
      (NodeList) this.xpath_updated.evaluate(document, XPathConstants.NODESET);

    for (var index = 0; index < updateds.getLength(); ++index) {
      final var updated = (Element) updateds.item(index);
      updated.setTextContent("2000-01-01T00:00:00Z");
    }
  }

  private static final class Classification
  {
    private final boolean isEntry;
    private final String title;

    Classification(
      final boolean inIsEntry,
      final String inTitle)
    {
      this.isEntry = inIsEntry;
      this.title = Objects.requireNonNull(inTitle, "title");
    }
  }
}






















