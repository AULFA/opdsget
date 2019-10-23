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
import java.util.Map;
import java.util.Optional;

/**
 * A description of a manifest.
 *
 * A manifest is a list of all of the files that make up an OPDS feed.
 */

@ImmutableStyleType
@Value.Immutable
public interface OPDSManifestDescriptionType
{
  /**
   * @return The unique ID of the feed
   */

  URI id();

  /**
   * @return The file that represents the root of the feed
   */

  String rootFile();

  /**
   * @return The file that represents the search index of the feed
   */

  Optional<String> searchIndex();

  /**
   * @return The base URI of the feed, if one was provided
   */

  Optional<URI> base();

  /**
   * @return The files that make up the feed
   */

  Map<URI, FileEntry> files();

  /**
   * A file entry.
   */

  @ImmutableStyleType
  @Value.Immutable
  interface FileEntryType
  {
    /**
     * @return The path of the file
     */

    String path();

    /**
     * @return The hash algorithm used to produce the file hash
     */

    String hashAlgorithm();

    /**
     * @return The hash of the file content
     */

    byte[] hash();
  }
}