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
      final Path file = local_file.file();
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
