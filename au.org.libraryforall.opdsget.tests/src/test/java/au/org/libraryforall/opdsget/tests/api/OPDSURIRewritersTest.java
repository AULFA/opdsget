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
