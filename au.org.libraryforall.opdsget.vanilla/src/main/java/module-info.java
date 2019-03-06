import au.org.libraryforall.opdsget.api.OPDSRetrieverProviderType;
import au.org.libraryforall.opdsget.vanilla.OPDSRetrievers;

/**
 * The vanilla implementation of the {@code opdsget} API.
 */

module au.org.libraryforall.opdsget.vanilla
{
  requires static org.immutables.value;

  requires java.xml;

  requires au.org.libraryforall.opdsget.api;
  requires org.slf4j;

  exports au.org.libraryforall.opdsget.vanilla;

  provides OPDSRetrieverProviderType with OPDSRetrievers;
}
