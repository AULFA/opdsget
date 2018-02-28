package org.aulfa.opdsget.tests.api;

import org.aulfa.opdsget.api.OPDSLocalFile;
import org.aulfa.opdsget.api.OPDSURIRewriters;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Paths;

public final class OPDSURIRewritersTest
{
  @Test
  public void testNamedRewrite()
  {
    final OPDSLocalFile file =
      OPDSLocalFile.of(
        URI.create("http://example.com/feed.atom"),
        Paths.get("examples/feeds/xyz.atom"));

    final URI output =
      OPDSURIRewriters.namedSchemeRewriter(
        "example",
        Paths.get("examples"))
        .apply(file);

    Assert.assertEquals(
      URI.create("example://feeds/xyz.atom"),
      output);
  }
}
