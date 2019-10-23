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

import one.lfa.opdsget.api.OPDSManifestDescription;
import one.lfa.opdsget.api.OPDSManifestWriterProviderType;
import one.lfa.opdsget.api.OPDSManifestWriterType;

import java.io.OutputStream;

/**
 * A provider of manifest writers.
 */

public final class OPDSManifestWriters implements OPDSManifestWriterProviderType
{
  /**
   * Construct a manifest writer.
   */

  public OPDSManifestWriters()
  {

  }

  @Override
  public OPDSManifestWriterType createWriter(
    final OPDSManifestDescription description,
    final OutputStream outputStream)
  {
    return new OPDSManifestWriter(description, outputStream);
  }
}
