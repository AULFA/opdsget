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

    for (final OPDSGetKind kind : OPDSGetKind.values()) {
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
