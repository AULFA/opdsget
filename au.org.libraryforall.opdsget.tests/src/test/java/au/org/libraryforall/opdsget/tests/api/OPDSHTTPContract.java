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

import au.org.libraryforall.opdsget.api.OPDSHTTPException;
import au.org.libraryforall.opdsget.api.OPDSHTTPType;
import fi.iki.elonen.NanoHTTPD;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.net.URI;
import java.util.Optional;

public abstract class OPDSHTTPContract
{
  private NanoHTTPD httpd;
  private Logger logger;

  protected abstract Logger logger();

  protected abstract OPDSHTTPType http();

  @Before
  public void setup()
  {
    this.logger = this.logger();
  }

  @After
  public void tearDown()
  {
    if (this.httpd != null) {
      this.httpd.stop();
    }
  }

  @Test
  public final void testRetry()
    throws Exception
  {
    this.httpd =
      new NanoHTTPD(20000)
      {
        @Override
        public Response serve(final IHTTPSession session)
        {
          OPDSHTTPContract.this.logger.debug("request: {}", session);
          return NanoHTTPD.newFixedLengthResponse(
            Response.Status.SERVICE_UNAVAILABLE,
            "text/plain",
            "Bad news");
        }
      };

    this.httpd.start();

    final var http = this.http();
    Assert.assertThrows(OPDSHTTPException.class, () -> {
      http.get(URI.create("http://localhost:20000/index.html"), Optional.empty());
    });
  }

  @Test
  public final void testRetryIOException()
  {
    final var http = this.http();
    Assert.assertThrows(OPDSHTTPException.class, () -> {
      http.get(URI.create("http://localhost:20000/index.html"), Optional.empty());
    });
  }
}
