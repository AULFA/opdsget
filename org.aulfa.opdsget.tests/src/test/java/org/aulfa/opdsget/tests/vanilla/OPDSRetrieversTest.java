package org.aulfa.opdsget.tests.vanilla;

import org.aulfa.opdsget.api.OPDSHTTPType;
import org.aulfa.opdsget.api.OPDSRetrieverProviderType;
import org.aulfa.opdsget.tests.api.OPDSRetrieverContract;
import org.aulfa.opdsget.vanilla.OPDSRetrievers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OPDSRetrieversTest extends OPDSRetrieverContract
{
  @Override
  protected OPDSRetrieverProviderType retrievers(final OPDSHTTPType http)
  {
    return OPDSRetrievers.providerWith(http);
  }

  @Override
  protected Logger logger()
  {
    return LoggerFactory.getLogger(OPDSRetrieversTest.class);
  }
}
