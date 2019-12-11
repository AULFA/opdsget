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

package one.lfa.opdsget.vanilla.parsers.v1;

import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import one.lfa.opdsget.api.FileEntry;
import org.apache.commons.codec.binary.Hex;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A handler for OPDS manifest file elements.
 */

public final class OPDSM1FileHandler implements BTElementHandlerType<Object, FileEntry>
{
  private final FileEntry.Builder builder;

  /**
   * Construct a handler.
   *
   * @param context The parser context
   */

  public OPDSM1FileHandler(
    final BTElementParsingContextType context)
  {
    this.builder = FileEntry.builder();
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXException
  {
    try {
      this.builder.setPath(attributes.getValue("name"));
      this.builder.setHashAlgorithm(attributes.getValue("hashAlgorithm"));
      this.builder.setHash(Hex.decodeHex(attributes.getValue("hash")));
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public FileEntry onElementFinished(
    final BTElementParsingContextType context)
    throws SAXException
  {
    try {
      return this.builder.build();
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }
}
