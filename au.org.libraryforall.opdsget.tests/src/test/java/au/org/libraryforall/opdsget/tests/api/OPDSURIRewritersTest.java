package au.org.libraryforall.opdsget.tests.api;

import au.org.libraryforall.opdsget.api.OPDSLocalFile;
import au.org.libraryforall.opdsget.api.OPDSURIRewriters;
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
