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

/**
 * Authentication values for the OPDS retriever.
 */

public interface OPDSAuthenticationType
{
  /**
   * @return The precise kind of authentication data
   */

  Kind kind();

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
