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

package one.lfa.opdsget.vanilla;

import one.lfa.opdsget.api.OPDSManifestReaderErrorReceiverType;
import one.lfa.opdsget.api.OPDSManifestReaderProviderType;
import one.lfa.opdsget.api.OPDSManifestReaderType;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

/**
 * The default provider of manifest readers.
 */

public final class OPDSManifestReaders implements OPDSManifestReaderProviderType
{
  /**
   * Construct a provider.
   */

  public OPDSManifestReaders()
  {

  }

  @Override
  public OPDSManifestReaderType createReader(
    final OPDSManifestReaderErrorReceiverType errors,
    final URI inURI,
    final InputStream inputStream)
  {
    Objects.requireNonNull(errors, "errors");
    Objects.requireNonNull(inURI, "inURI");
    Objects.requireNonNull(inputStream, "inputStream");
    return new OPDSManifestReader(errors, inURI, inputStream);
  }
}
