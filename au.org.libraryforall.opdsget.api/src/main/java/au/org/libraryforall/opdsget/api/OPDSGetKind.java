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

import java.util.Objects;

/**
 * The kinds of objects that will be fetched.
 */

public enum OPDSGetKind
{
  /**
   * Images will be fetched.
   */

  OPDS_GET_IMAGES("images"),

  /**
   * Books will be fetched.
   */

  OPDS_GET_BOOKS("books");

  private final String name;

  OPDSGetKind(
    final String in_name)
  {
    this.name = Objects.requireNonNull(in_name, "name");
  }

  /**
   * @param name The name of the kind
   *
   * @return The kind for the given name
   */

  public static OPDSGetKind ofName(
    final String name)
  {
    Objects.requireNonNull(name, "name");

    for (final var kind : OPDSGetKind.values()) {
      if (Objects.equals(kind.name, name)) {
        return kind;
      }
    }

    throw new IllegalArgumentException(
      new StringBuilder(32)
        .append("No such kind: ")
        .append(name)
        .toString());
  }

  /**
   * @return The short name of the kind
   */

  public String kindName()
  {
    return this.name;
  }
}
