package au.org.libraryforall.opdsget.tests.vanilla;

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
    return OPDSRetrievers.providerWith(http);
  }

  @Override
  protected Logger logger()
  {
    return LoggerFactory.getLogger(OPDSRetrieversTest.class);
  }
}
