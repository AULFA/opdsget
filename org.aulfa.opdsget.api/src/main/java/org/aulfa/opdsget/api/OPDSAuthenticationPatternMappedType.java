package org.aulfa.opdsget.api;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;

/**
 * An authentication matcher that works from a list of patterns. URIs
 * will be matched against the given list of patterns, in order, stopping
 * at the first pattern that matches (or returning no authentication data if no
 * patterns match).
 */

@ImmutableStyleType
@Value.Immutable
public interface OPDSAuthenticationPatternMappedType
  extends Function<URI, Optional<OPDSAuthenticationType>>
{
  /**
   * The logger used to trace matching patterns.
   */

  Logger LOG =
    LoggerFactory.getLogger(OPDSAuthenticationPatternMappedType.class);

  /**
   * @return The list of pattern matchers in application order
   */

  @Value.Parameter
  List<OPDSMatchingAuthentication> patterns();

  @Override
  default Optional<OPDSAuthenticationType> apply(final URI uri)
  {
    Objects.requireNonNull(uri, "uri");

    final List<OPDSMatchingAuthentication> patterns = this.patterns();

    final String uri_text = uri.toString();
    for (int index = 0; index < patterns.size(); ++index) {
      final OPDSMatchingAuthentication uri_matcher = patterns.get(index);
      final Matcher matcher = uri_matcher.pattern().matcher(uri_text);
      if (matcher.matches()) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("{} matches {}",
                    uri_matcher.pattern().pattern(), uri_text);
        }
        return uri_matcher.authentication();
      }

      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "{} does not match {}",
          uri_matcher.pattern().pattern(),
          uri_text);
      }
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("no pattern matches {}", uri_text);
    }
    return Optional.empty();
  }
}