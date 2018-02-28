package org.aulfa.opdsget.tests.vanilla;

import org.aulfa.opdsget.api.OPDSDocumentProcessed;
import org.aulfa.opdsget.api.OPDSGetConfiguration;
import org.aulfa.opdsget.vanilla.OPDSDocumentProcessor;
import org.aulfa.opdsget.vanilla.OPDSXMLParsers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public final class OPDSDocumentProcessorTest
{
  private OPDSDocumentProcessor processor;
  private Path output;
  private XPathFactory xpath_factory;
  private XPath xpath;
  private XPathExpression xpath_links;
  private XPathExpression xpath_updated;

  @Before
  public void setUp()
    throws IOException
  {
    this.output = Files.createTempDirectory("opdsget-tests");
    this.processor = new OPDSDocumentProcessor();
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

  @After
  public void tearDown()
  {

  }

  @Test
  public void testRewriteSimple()
    throws Exception
  {
    final OPDSGetConfiguration config =
      OPDSGetConfiguration.builder()
        .setOutput(this.output)
        .setRemoteURI(URI.create("http://www.example.com"))
        .build();

    final Document document = parse("links.xml");

    final NodeList updateds_before =
      (NodeList) this.xpath_updated.evaluate(document, XPathConstants.NODESET);

    Assert.assertTrue(
      "Document contains 'updated' elements",
      (long) updateds_before.getLength() > 0L);

    final NodeList links_before =
      (NodeList) this.xpath_links.evaluate(document, XPathConstants.NODESET);

    Assert.assertTrue(
      "Document contains more than five links",
      (long) links_before.getLength() > 5L);

    final OPDSDocumentProcessed result =
      this.processor.process(config, document);

    Assert.assertTrue(
      result.feeds().containsKey(URI.create("http://example.com/1.atom")));
    Assert.assertTrue(
      result.feeds().containsKey(URI.create("http://example.com/1.entry")));
    Assert.assertTrue(
      result.images().containsKey(URI.create("http://example.com/cover.png")));
    Assert.assertTrue(
      result.images().containsKey(URI.create("http://example.com/thumbnail.png")));
    Assert.assertTrue(
      result.books().containsKey(URI.create("http://example.com/0.epub")));

    final NodeList updateds_after =
      (NodeList) this.xpath_updated.evaluate(document, XPathConstants.NODESET);

    Assert.assertEquals(
      "Document contains no 'updated' elements",
      0L,
      (long) updateds_after.getLength());

    final NodeList links_after =
      (NodeList) this.xpath_links.evaluate(document, XPathConstants.NODESET);

    Assert.assertEquals(
      "Document contains exactly five links",
      5L,
      (long) links_after.getLength());
  }

  private static Document parse(final String name)
    throws Exception
  {
    final URL url =
      OPDSDocumentProcessorTest.class.getResource(
        "/org/aulfa/opdsget/tests/vanilla/" + name);
    if (url == null) {
      throw new FileNotFoundException(name);
    }

    try (InputStream stream = url.openStream()) {
      return new OPDSXMLParsers().parse(url.toURI(), stream);
    }
  }
}
