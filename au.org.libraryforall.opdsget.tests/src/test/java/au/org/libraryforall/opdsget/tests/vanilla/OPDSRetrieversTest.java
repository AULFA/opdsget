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

package au.org.libraryforall.opdsget.tests.vanilla;

import au.org.libraryforall.epubsquash.vanilla.EPUBSquashers;
import au.org.libraryforall.opdsget.api.OPDSHTTPType;
import au.org.libraryforall.opdsget.api.OPDSRetrieverProviderType;
import au.org.libraryforall.opdsget.tests.api.OPDSRetrieverContract;
import au.org.libraryforall.opdsget.vanilla.OPDSRetrievers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OPDSRetrieversTest extends OPDSRetrieverContract
{
  @Override
  protected OPDSRetrieverProviderType retrievers(final OPDSHTTPType http)
  {
    return OPDSRetrievers.providerWith(new EPUBSquashers(), http);
  }

  @Override
  protected Logger logger()
  {
    return LoggerFactory.getLogger(OPDSRetrieversTest.class);
  }
}
