package org.aulfa.opdsget.tests.api;

import org.aulfa.opdsget.api.OPDSAuthenticationBasic;
import org.aulfa.opdsget.api.OPDSAuthenticationPatternMapped;
import org.aulfa.opdsget.api.OPDSAuthenticationType;
import org.aulfa.opdsget.api.OPDSMatchingAuthentication;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class OPDSAuthenticationPatternMappedTest
{
  @Test
  public void testEmpty()
  {
    final OPDSAuthenticationPatternMapped empty =
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

    final OPDSAuthenticationPatternMapped empty =
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

    final OPDSAuthenticationPatternMapped empty =
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
