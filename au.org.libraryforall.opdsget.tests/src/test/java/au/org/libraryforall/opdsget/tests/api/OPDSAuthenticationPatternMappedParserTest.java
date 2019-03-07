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

package au.org.libraryforall.opdsget.tests.api;

import au.org.libraryforall.opdsget.api.OPDSAuthenticationBasic;
import au.org.libraryforall.opdsget.api.OPDSAuthenticationPatternMapped;
import au.org.libraryforall.opdsget.api.OPDSAuthenticationPatternMappedParser;
import au.org.libraryforall.opdsget.api.OPDSMatchingAuthentication;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OPDSAuthenticationPatternMappedParserTest
{
  @Test
  public void testEmpty()
    throws Exception
  {
    try (InputStream stream = resourceStream("auth-empty.map")) {
      final OPDSAuthenticationPatternMapped received =
        OPDSAuthenticationPatternMappedParser.parse(
          URI.create("auth-empty.map"),
          stream);

      final OPDSAuthenticationPatternMapped expected =
        OPDSAuthenticationPatternMapped.of(new ArrayList<>());

      Assert.assertEquals(expected, received);
    }
  }

  @Test
  public void testSingleNone()
    throws Exception
  {
    try (InputStream stream = resourceStream("auth-single-none.map")) {
      final OPDSAuthenticationPatternMapped received =
        OPDSAuthenticationPatternMappedParser.parse(
          URI.create("auth-single-none.map"),
          stream);

      final OPDSAuthenticationPatternMapped expected =
        OPDSAuthenticationPatternMapped.of(
          List.of(
            OPDSMatchingAuthentication.of(
              ".*",
              Optional.empty())));

      Assert.assertEquals(expected, received);
    }
  }

  @Test
  public void testMultipleBasic()
    throws Exception
  {
    try (InputStream stream = resourceStream("auth-multi-basic.map")) {
      final OPDSAuthenticationPatternMapped received =
        OPDSAuthenticationPatternMappedParser.parse(
          URI.create("auth-multi-basic.map"),
          stream);

      final OPDSAuthenticationPatternMapped expected =
        OPDSAuthenticationPatternMapped.of(
          List.of(
            OPDSMatchingAuthentication.of(
              "http[s]?://(www\\.)?example\\.com",
              Optional.of(OPDSAuthenticationBasic.of("0", "0"))),
            OPDSMatchingAuthentication.of(
              "http[s]?://(www\\.)?example\\.com/abc",
              Optional.of(OPDSAuthenticationBasic.of("1", "1"))),
            OPDSMatchingAuthentication.of(
              "http[s]?://(www\\.)?example\\.com/def",
              Optional.of(OPDSAuthenticationBasic.of("2", "2")))
          ));

      Assert.assertEquals(expected, received);
    }
  }

  @Rule public ExpectedException exception = ExpectedException.none();

  @Test
  public void testUnparseable0()
    throws Exception
  {
    try (InputStream stream = resourceStream("auth-bad-unparseable-0.map")) {
      this.exception.expect(ParseException.class);
      OPDSAuthenticationPatternMappedParser.parse(
        URI.create("auth-bad-unparseable-0.map"), stream);
    }
  }

  @Test
  public void testUnparseable1()
    throws Exception
  {
    try (InputStream stream = resourceStream("auth-bad-unparseable-1.map")) {
      this.exception.expect(ParseException.class);
      OPDSAuthenticationPatternMappedParser.parse(
        URI.create("auth-bad-unparseable-1.map"), stream);
    }
  }

  @Test
  public void testUnparseable2()
    throws Exception
  {
    try (InputStream stream = resourceStream("auth-bad-unparseable-2.map")) {
      this.exception.expect(ParseException.class);
      OPDSAuthenticationPatternMappedParser.parse(
        URI.create("auth-bad-unparseable-2.map"), stream);
    }
  }

  private static InputStream resourceStream(final String name)
  {
    try {
      final URL url =
        OPDSAuthenticationPatternMappedParserTest.class.getResource(
          "/au/org/libraryforall/opdsget/tests/api/" + name);
      if (url == null) {
        throw new FileNotFoundException(name);
      }

      return url.openStream();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
