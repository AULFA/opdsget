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

import org.immutables.value.Value;

import java.net.URI;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;

/**
 * Configuration values for the OPDS retriever.
 */

@ImmutableStyleType
@Value.Immutable
public interface OPDSGetConfigurationType
{
  /**
   * @return The output directory
   */

  @Value.Parameter
  Path output();

  /**
   * @return The zip file that will be created containing the contents of the output directory
   */

  @Value.Parameter
  Optional<Path> outputArchive();

  /**
   * @return The starting URI for the OPDS feed
   */

  @Value.Parameter
  URI remoteURI();

  /**
   * A function used to rewrite URIs. The function will be passed the original URI and the path to
   * the local file. The function must return a URI that will be used to refer to the local file.
   * This is used for, for example, prefixing rewritten URIs with {@code file:///android_asset/} for
   * producing feeds bundled into Android applications.
   *
   * @return A URI rewriter
   */

  @Value.Default
  @Value.Parameter
  default OPDSURIRewriterType uriRewriter()
  {
    return OPDSURIRewriters.plainFileRewriter(this.output());
  }

  /**
   * A function that returns authentication information for URIs, if required.
   *
   * @return The authentication data, if any is required
   */

  @Value.Default
  @Value.Parameter
  default Function<URI, Optional<OPDSAuthenticationType>> authenticationSupplier()
  {
    return uri -> Optional.empty();
  }

  /**
   * @param type The type of fetched object
   *
   * @return The directory containing objects of the given type.
   */

  default Path typedDirectory(final String type)
  {
    return this.output().resolve(Objects.requireNonNull(type, "type"));
  }

  /**
   * Return the path to the given feed. Fetched objects are typically renamed to the hash of their
   * original URI.
   *
   * @param name The feed name
   *
   * @return The path to a feed-typed object with the given name
   *
   * @see OPDSURIHashing#hashOf(URI)
   */

  default Path feedFile(final String name)
  {
    Objects.requireNonNull(name, "name");
    return this.feedDirectory().resolve(name);
  }

  /**
   * Return the path to the given feed. Fetched objects are renamed to the hash of their original
   * URI.
   *
   * @param uri The feed URI
   *
   * @return The path to a feed-typed object with the given name
   *
   * @see OPDSURIHashing#hashOf(URI)
   */

  default Path feedFileHashed(final URI uri)
  {
    Objects.requireNonNull(uri, "uri");
    return this.feedFile(OPDSURIHashing.hashOf(uri) + ".atom");
  }

  /**
   * @return The directory containing fetched feeds.
   *
   * @see #feedFile(String)
   */

  default Path feedDirectory()
  {
    return this.typedDirectory("feeds");
  }

  /**
   * Return the path to the given image. Fetched objects are typically renamed to the hash of their
   * original URI.
   *
   * @param name The image name
   *
   * @return The path to a image-typed object with the given name
   *
   * @see OPDSURIHashing#hashOf(URI)
   */

  default Path imageFile(final String name)
  {
    Objects.requireNonNull(name, "name");
    return this.imageDirectory().resolve(name);
  }

  /**
   * Return the path to the given image. Fetched objects are renamed to the hash of their original
   * URI.
   *
   * @param uri The image URI
   *
   * @return The path to a image-typed object with the given name
   *
   * @see OPDSURIHashing#hashOf(URI)
   */

  default Path imageFileHashed(final URI uri)
  {
    Objects.requireNonNull(uri, "uri");
    return this.imageFile(OPDSURIHashing.hashOf(uri));
  }

  /**
   * @return The directory containing fetched images.
   *
   * @see #imageFile(String)
   */

  default Path imageDirectory()
  {
    return this.typedDirectory("images");
  }

  /**
   * Return the path to the given book. Fetched objects are typically renamed to the hash of their
   * original URI.
   *
   * @param name The book name
   *
   * @return The path to a book-typed object with the given name
   *
   * @see OPDSURIHashing#hashOf(URI)
   */

  default Path bookFile(final String name)
  {
    Objects.requireNonNull(name, "name");
    return this.bookDirectory().resolve(name);
  }

  /**
   * Return the path to the given book. Fetched objects are renamed to the hash of their original
   * URI.
   *
   * @param uri The book URI
   *
   * @return The path to a book-typed object with the given name
   *
   * @see OPDSURIHashing#hashOf(URI)
   */

  default Path bookFileHashed(final URI uri)
  {
    Objects.requireNonNull(uri, "uri");
    return this.bookFile(OPDSURIHashing.hashOf(uri) + ".epub");
  }

  /**
   * @return The directory containing fetched books.
   *
   * @see #bookFile(String)
   */

  default Path bookDirectory()
  {
    return this.typedDirectory("books");
  }

  /**
   * @return The set of feed content kinds that will be fetched
   */

  @Value.Default
  @Value.Parameter
  default Set<OPDSGetKind> fetchedKinds()
  {
    return EnumSet.allOf(OPDSGetKind.class);
  }

  /**
   * @return The squash configuration if EPUB squashing should be performed
   */

  @Value.Parameter
  Optional<OPDSSquashConfiguration> squash();

  /**
   * @return The amount by which to scale cover images
   */

  OptionalDouble scaleImages();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    final var outputPath = this.output();
    if (!outputPath.isAbsolute()) {
      throw new IllegalArgumentException(
        String.format("Output path %s must be absolute", outputPath));
    }

    final var outputArchiveOpt = this.outputArchive();
    if (outputArchiveOpt.isPresent()) {
      final var outputArchive = outputArchiveOpt.get();
      if (!outputArchive.isAbsolute()) {
        throw new IllegalArgumentException(
          String.format("Output archive path %s must be absolute", outputArchive));
      }
    }
  }
}
