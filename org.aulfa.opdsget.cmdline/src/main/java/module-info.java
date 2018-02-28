/**
 * Command-line frontend.
 */

module org.aulfa.opdsget.cmdline
{
  requires org.aulfa.opdsget.api;
  requires org.aulfa.opdsget.vanilla;

  requires jcommander;
  requires org.slf4j;
  requires ch.qos.logback.classic;

  uses org.aulfa.opdsget.api.OPDSRetrieverProviderType;

  exports org.aulfa.opdsget.cmdline;
  opens org.aulfa.opdsget.cmdline to jcommander;
}