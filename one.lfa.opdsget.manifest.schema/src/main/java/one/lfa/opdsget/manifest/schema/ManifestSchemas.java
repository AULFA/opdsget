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

package one.lfa.opdsget.manifest.schema;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * Functions to retrieve schemas.
 */

public final class ManifestSchemas
{
  private ManifestSchemas()
  {

  }

  /**
   * @return The 1.0 schema namespace
   */

  public static URI schema1p0Namespace()
  {
    return URI.create("urn:one.lfa.opdsget.manifest.xml:1:0");
  }

  /**
   * @return The 1.0 schema
   *
   * @throws IOException On I/O errors
   */

  public static InputStream schema1p0()
    throws IOException
  {
    return schema1p0URL().openStream();
  }

  /**
   * @return The 1.0 schema
   */

  public static URL schema1p0URL()
  {
    return ManifestSchemas.class.getResource(
      "/one/lfa/opdsget/manifest/schema/schema-1.0.xsd");
  }

  /**
   * @return The 1.1 schema namespace
   */

  public static URI schema1p1Namespace()
  {
    return URI.create("urn:one.lfa.opdsget.manifest.xml:1:1");
  }

  /**
   * @return The 1.1 schema
   *
   * @throws IOException On I/O errors
   */

  public static InputStream schema1p1()
    throws IOException
  {
    return schema1p1URL().openStream();
  }

  /**
   * @return The 1.1 schema
   */

  public static URL schema1p1URL()
  {
    return ManifestSchemas.class.getResource(
      "/one/lfa/opdsget/manifest/schema/schema-1.1.xsd");
  }
}
