import au.org.libraryforall.opdsget.api.OPDSRetrieverProviderType;

/**
 * Command-line frontend.
 */

module au.org.libraryforall.opdsget.cmdline
{
  requires au.org.libraryforall.opdsget.api;
  requires au.org.libraryforall.opdsget.vanilla;

  requires jcommander;
  requires org.slf4j;
  requires ch.qos.logback.classic;

  uses OPDSRetrieverProviderType;

  exports au.org.libraryforall.opdsget.cmdline;
  opens au.org.libraryforall.opdsget.cmdline to jcommander;
}