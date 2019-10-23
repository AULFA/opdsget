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

import java.io.InputStream;
import java.net.URI;

/**
 * A provider of manifest readers.
 */

public interface OPDSManifestReaderProviderType
{
  /**
   * Create a new writer that will read a manifest from the given input stream.
   *
   * @param errors      An error receiver
   * @param uri         The URI of the input, for diagnostic messages
   * @param inputStream The input stream
   *
   * @return A new reader
   */

  OPDSManifestReaderType createReader(
    OPDSManifestReaderErrorReceiverType errors,
    URI uri,
    InputStream inputStream);
}
