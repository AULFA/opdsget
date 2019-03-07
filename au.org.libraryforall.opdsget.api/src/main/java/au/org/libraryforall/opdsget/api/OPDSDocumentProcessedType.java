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

import org.immutables.value.Value;

import java.net.URI;
import java.util.Map;

/**
 * The result of processing a single OPDS feed document.
 */

@ImmutableStyleType
@Value.Immutable
public interface OPDSDocumentProcessedType
{
  /**
   * @return The set of feeds that need to be fetched
   */

  @Value.Default
  @Value.Parameter
  default Map<URI, OPDSLocalFile> feeds()
  {
    return Map.of();
  }

  /**
   * @return The set of images that need to be fetched
   */

  @Value.Default
  @Value.Parameter
  default Map<URI, OPDSLocalFile> images()
  {
    return Map.of();
  }

  /**
   * @return The set of books that need to be fetched
   */

  @Value.Default
  @Value.Parameter
  default Map<URI, OPDSLocalFile> books()
  {
    return Map.of();
  }

  /**
   * @return The path to the saved feed file
   */

  @Value.Parameter
  OPDSLocalFile file();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    for (final var uri : this.feeds().keySet()) {
      if (!uri.isAbsolute()) {
        throw new IllegalArgumentException("Non-absolute feed URI: " + uri);
      }
    }

    for (final var uri : this.images().keySet()) {
      if (!uri.isAbsolute()) {
        throw new IllegalArgumentException("Non-absolute image URI: " + uri);
      }
    }

    for (final var uri : this.books().keySet()) {
      if (!uri.isAbsolute()) {
        throw new IllegalArgumentException("Non-absolute book URI: " + uri);
      }
    }
  }
}
