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

import java.io.InputStream;

/**
 * The type of remote HTTP data.
 */

@ImmutableStyleType
@Value.Immutable
public interface OPDSHTTPDataType
{
  /**
   * @return The size of the remote content
   */

  @Value.Parameter
  long size();

  /**
   * @return The type of the remote content
   */

  @Value.Parameter
  String contentType();

  /**
   * @return The input stream for the remote content
   */

  @Value.Parameter
  InputStream stream();
}
