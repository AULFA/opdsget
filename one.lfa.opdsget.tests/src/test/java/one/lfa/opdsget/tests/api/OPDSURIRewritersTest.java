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

package one.lfa.opdsget.tests.api;

import one.lfa.opdsget.api.OPDSLocalFile;
import one.lfa.opdsget.api.OPDSURIRewriters;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;

public final class OPDSURIRewritersTest
{
  @Test
  public void testNamedRewrite0()
  {
    final var sourceFile =
      Optional.of(
        OPDSLocalFile.of(
          URI.create("http://example.com/feed.atom"),
          Paths.get("examples/feeds/xyz.atom")));

    final var targetFile =
      OPDSLocalFile.of(
        URI.create("http://example.com/feed.atom"),
        Paths.get("examples/feeds/xyz.atom"));

    final var output =
      OPDSURIRewriters.namedSchemeRewriter(
        "example",
        Paths.get("examples"))
        .rewrite(sourceFile, targetFile);

    Assert.assertEquals(
      URI.create("example://feeds/xyz.atom"),
      output);
  }

  @Test
  public void testNamedRewrite1()
  {
    final var sourceFile =
      Optional.<OPDSLocalFile>empty();

    final var targetFile =
      OPDSLocalFile.of(
        URI.create("http://example.com/feed.atom"),
        Paths.get("examples/feeds/xyz.atom"));

    final var output =
      OPDSURIRewriters.namedSchemeRewriter(
        "example",
        Paths.get("examples"))
        .rewrite(sourceFile, targetFile);

    Assert.assertEquals(
      URI.create("example://feeds/xyz.atom"),
      output);
  }

  @Test
  public void testRelativize0()
  {
    final var sourceFile =
      Optional.of(
        OPDSLocalFile.of(
          URI.create("http://example.com/feed.atom"),
          Paths.get("examples/feeds/abc.atom")));

    final var targetFile =
      OPDSLocalFile.of(
        URI.create("http://example.com/feed.atom"),
        Paths.get("examples/feeds/xyz.atom"));

    final var result =
      OPDSURIRewriters.relativeRewriter()
        .rewrite(sourceFile, targetFile);

    Assert.assertEquals(
      URI.create("xyz.atom"),
      result);
  }

  @Test
  public void testRelativize1()
  {
    final var sourceFile =
      Optional.<OPDSLocalFile>empty();

    final var targetFile =
      OPDSLocalFile.of(
        URI.create("http://example.com/feed.atom"),
        Paths.get("examples/feeds/xyz.atom"));

    final var result =
      OPDSURIRewriters.relativeRewriter()
        .rewrite(sourceFile, targetFile);

    Assert.assertEquals(
      URI.create("feeds/xyz.atom"),
      result);
  }
}
