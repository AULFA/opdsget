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

import one.lfa.epubsquash.api.EPUBSquasherProviderType;
import one.lfa.opdsget.api.OPDSManifestReaderProviderType;
import one.lfa.opdsget.api.OPDSManifestWriterProviderType;
import one.lfa.opdsget.api.OPDSRetrieverProviderType;
import one.lfa.opdsget.vanilla.OPDSManifestReaders;
import one.lfa.opdsget.vanilla.OPDSManifestWriters;
import one.lfa.opdsget.vanilla.OPDSRetrievers;

/**
 * The vanilla implementation of the {@code opdsget} API.
 */

module one.lfa.opdsget.vanilla
{
  requires com.io7m.blackthorne.api;
  requires com.io7m.jaffirm.core;
  requires com.io7m.jxe.core;
  requires java.desktop;
  requires java.xml;
  requires one.lfa.epubsquash.api;
  requires one.lfa.opdsget.api;
  requires one.lfa.opdsget.manifest.schema;
  requires org.apache.commons.codec;
  requires org.slf4j;

  uses EPUBSquasherProviderType;
  uses OPDSManifestReaderProviderType;
  uses OPDSManifestWriterProviderType;

  exports one.lfa.opdsget.vanilla;

  provides OPDSManifestReaderProviderType with OPDSManifestReaders;
  provides OPDSManifestWriterProviderType with OPDSManifestWriters;
  provides OPDSRetrieverProviderType with OPDSRetrievers;
}
