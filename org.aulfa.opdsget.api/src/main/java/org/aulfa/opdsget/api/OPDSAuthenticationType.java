package org.aulfa.opdsget.api;

import org.immutables.value.Value;

/**
 * Authentication values for the OPDS retriever.
 */

public interface OPDSAuthenticationType
{
  /**
   * The kind of authentication data.
   */

  enum Kind
  {

    /**
     * HTTP Basic auth.
     *
     * @see OPDSAuthenticationBasicType
     */

    AUTHENTICATION_BASIC
  }

  /**
   * @return The precise kind of authentication data
   */

  Kind kind();

  /**
   * HTTP Basic auth.
   */

  @ImmutableStyleType
  @Value.Immutable
  interface OPDSAuthenticationBasicType extends OPDSAuthenticationType
  {
    @Override
    default Kind kind()
    {
      return Kind.AUTHENTICATION_BASIC;
    }

    /**
     * @return The username
     */

    @Value.Parameter
    String user();

    /**
     * @return The password
     */

    @Value.Parameter
    String password();
  }
}
