package org.aulfa.opdsget.api;

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
    Objects.requireNonNull(output, "output");

    return local_file -> {
      final Path file = local_file.file();
      return URI.create("file://" + output.relativize(file));
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

    return local_file -> {
      final Path file = local_file.file();
      return URI.create("file:///android_asset/" + output.relativize(file));
    };
  }
}
