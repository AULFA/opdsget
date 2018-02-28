package org.aulfa.opdsget.api;

import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Style settings for generated immutable types.
 */

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@Value.Style(
  get = {"is*", "get*"},
  init = "set*",
  typeAbstract = {"Abstract*", "*Type"},
  typeImmutable = "*",
  typeModifiable = "*Mutable",
  builder = "builder",
  build = "build",
  visibility = Value.Style.ImplementationVisibility.PUBLIC,
  defaults = @Value.Immutable(copy = false))
public @interface ImmutableStyleType
{
  // No value-level representation
}

