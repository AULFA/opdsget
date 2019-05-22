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

package au.org.libraryforall.opdsget.api;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

/**
 * Various URI rewriting functions.
 */

public final class OPDSURIRewriters
{
  private OPDSURIRewriters()
  {

  }

  /**
   * @param output The output directory against which URIs will be relativized
   *
   * @return A rewriter that simply rewrites to local file URIs.
   */

  public static Function<OPDSLocalFile, URI> plainFileRewriter(
    final Path output)
  {
    return namedSchemeRewriter("file", output);
  }

  /**
   * @param scheme The name of the resulting scheme
   * @param output The output directory against which URIs will be relativized
   *
   * @return A rewriter that simply rewrites to local file URIs.
   */

  public static Function<OPDSLocalFile, URI> namedSchemeRewriter(
    final String scheme,
    final Path output)
  {
    Objects.requireNonNull(scheme, "scheme");
    Objects.requireNonNull(output, "output");

    return local_file -> {
      final var file = local_file.file();
      return URI.create(scheme + "://" + output.relativize(file));
    };
  }

  /**
   * @param output The output directory against which URIs will be relativized
   *
   * @return A rewriter that simply rewrites to local file URIs.
   */

  public static Function<OPDSLocalFile, URI> androidAssetRewriter(
    final Path output)
  {
    Objects.requireNonNull(output, "output");
    return namedSchemeRewriter("android_asset", output);
  }
}
