import org.aulfa.opdsget.api.OPDSRetrieverProviderType;
import org.aulfa.opdsget.vanilla.OPDSRetrievers;

/**
 * The vanilla implementation of the <tt>opdsget</tt> API.
 */

module org.aulfa.opdsget.vanilla
{
  requires static org.immutables.value;

  requires java.xml;

  requires org.aulfa.opdsget.api;
  requires org.slf4j;

  exports org.aulfa.opdsget.vanilla;

  provides OPDSRetrieverProviderType with OPDSRetrievers;
}