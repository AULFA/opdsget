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

package au.org.libraryforall.opdsget.api;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * An authentication matcher that works from a list of patterns. URIs will be matched against the
 * given list of patterns, in order, stopping at the first pattern that matches (or returning no
 * authentication data if no patterns match).
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

    final var patterns = this.patterns();

    final var uri_text = uri.toString();
    for (var index = 0; index < patterns.size(); ++index) {
      final var uri_matcher = patterns.get(index);
      final var matcher = uri_matcher.pattern().matcher(uri_text);
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
