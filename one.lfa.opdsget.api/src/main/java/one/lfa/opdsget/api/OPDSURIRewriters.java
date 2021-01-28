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

package one.lfa.opdsget.api;

import com.io7m.jaffirm.core.Invariants;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Various URI rewriting functions.
 */

public final class OPDSURIRewriters
{
  private OPDSURIRewriters()
  {

  }

  /**
   * @return A rewriter that simply rewrites to local file URIs.
   */

  public static OPDSURIRewriterType relativeRewriter()
  {
    return (sourceFileOpt, targetFile) -> {
      if (sourceFileOpt.isPresent()) {
        final var sourceFile = sourceFileOpt.get();
        final var sourceURI = sourceFile.file().toAbsolutePath().getParent();
        Invariants.checkInvariant(
          sourceURI,
          sourceURI.isAbsolute(),
          u -> "Source must be absolute");
        final var targetURI = targetFile.file().toAbsolutePath();
        Invariants.checkInvariant(
          targetURI,
          targetURI.isAbsolute(),
          u -> "Target must be absolute");
        final var relative = URI.create(sourceURI.relativize(targetURI).toString());
        Invariants.checkInvariant(relative, !relative.isAbsolute(), u -> "Output must be relative");
        return relative;
      }

      final var sourceURI = targetFile.file().getParent().getParent().toAbsolutePath();
      final var targetURI = targetFile.file().toAbsolutePath();
      Invariants.checkInvariant(targetURI, targetURI.isAbsolute(), u -> "Target must be absolute");
      final var relative =
        URI.create(sourceURI.relativize(targetURI).toString().replace('\\', '/'));
      Invariants.checkInvariant(relative, !relative.isAbsolute(), u -> "Output must be relative");
      return relative;
    };
  }

  /**
   * @param output The output directory against which URIs will be relativized
   *
   * @return A rewriter that simply rewrites to local file URIs.
   */

  public static OPDSURIRewriterType plainFileRewriter(
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

  public static OPDSURIRewriterType namedSchemeRewriter(
    final String scheme,
    final Path output)
  {
    Objects.requireNonNull(scheme, "scheme");
    Objects.requireNonNull(output, "output");

    return (sourceFile, targetFile) -> {
      final var file = targetFile.file();
      final var relative = output.relativize(file);
      return URI.create(scheme + "://" + relative.toString().replace("\\", "/"));
    };
  }

  /**
   * @return A rewriter that simply returns the target URI.
   */

  public static OPDSURIRewriterType noRewriter()
  {
    return (sourceFile, targetFile) -> targetFile.uri();
  }
}
