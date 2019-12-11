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

package one.lfa.opdsget.tests.api;

import one.lfa.opdsget.api.OPDSAuthenticationBasic;
import one.lfa.opdsget.api.OPDSAuthenticationPatternMapped;
import one.lfa.opdsget.api.OPDSAuthenticationType;
import one.lfa.opdsget.api.OPDSMatchingAuthentication;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public final class OPDSAuthenticationPatternMappedTest
{
  @Test
  public void testEmpty()
  {
    final var empty =
      OPDSAuthenticationPatternMapped.of(List.of());

    Assert.assertEquals(
      Optional.empty(),
      empty.apply(URI.create("http://www.example.com")));
  }

  @Test
  public void testMatchSimple()
  {
    final OPDSAuthenticationType auth =
      OPDSAuthenticationBasic.of("someone", "abcdefgh");

    final var empty =
      OPDSAuthenticationPatternMapped.of(List.of(
        OPDSMatchingAuthentication.of(
          "http[s]?://(.*\\.)example\\.com",
          Optional.of(auth))
      ));

    Assert.assertEquals(
      Optional.of(auth),
      empty.apply(URI.create("http://www.example.com")));
    Assert.assertEquals(
      Optional.of(auth),
      empty.apply(URI.create("https://www.example.com")));
    Assert.assertEquals(
      Optional.empty(),
      empty.apply(URI.create("http://www.example.com/abc")));
    Assert.assertEquals(
      Optional.empty(),
      empty.apply(URI.create("https://www.example.com/")));
  }

  @Test
  public void testMatchMultiple()
  {
    final OPDSAuthenticationType auth0 =
      OPDSAuthenticationBasic.of("0", "0");
    final OPDSAuthenticationType auth1 =
      OPDSAuthenticationBasic.of("1", "1");
    final OPDSAuthenticationType auth2 =
      OPDSAuthenticationBasic.of("2", "2");

    final var empty =
      OPDSAuthenticationPatternMapped.of(List.of(
        OPDSMatchingAuthentication.of(
          "http[s]?://(.*\\.)example\\.com/a",
          Optional.of(auth0)),
        OPDSMatchingAuthentication.of(
          "http[s]?://(.*\\.)example\\.com/b",
          Optional.of(auth1)),
        OPDSMatchingAuthentication.of(
          "http[s]?://(.*\\.)example\\.com/c",
          Optional.of(auth2))
      ));

    Assert.assertEquals(
      Optional.of(auth0),
      empty.apply(URI.create("http://www.example.com/a")));
    Assert.assertEquals(
      Optional.of(auth1),
      empty.apply(URI.create("https://www.example.com/b")));
    Assert.assertEquals(
      Optional.of(auth2),
      empty.apply(URI.create("http://www.example.com/c")));
    Assert.assertEquals(
      Optional.empty(),
      empty.apply(URI.create("https://www.example.com/d")));
  }
}
