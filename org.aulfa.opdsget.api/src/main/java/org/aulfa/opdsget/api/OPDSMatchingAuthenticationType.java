package org.aulfa.opdsget.api;

import org.immutables.value.Value;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * An authentication value that is applied when a URI matches the given
 * pattern.
 */

@ImmutableStyleType
@Value.Immutable
public interface OPDSMatchingAuthenticationType
{
  /**
   * @return The text of the pattern against which a URI is matched
   */

  @Value.Parameter
  String patternText();

  /**
   * @return The pattern against which a URI is matched
   */

  @Value.Auxiliary
  @Value.Derived
  default Pattern pattern()
  {
    return Pattern.compile(this.patternText());
  }

  /**
   * @return The authentication data if any
   */

  @Value.Parameter
  Optional<OPDSAuthenticationType> authentication();
}
