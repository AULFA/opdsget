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

package one.lfa.opdsget.api;

import org.immutables.value.Value;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * An authentication value that is applied when a URI matches the given pattern.
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
