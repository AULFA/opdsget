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

import com.io7m.blackthorne.api.BTElementHandlerConstructorType;
import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.blackthorne.api.BTQualifiedName;
import one.lfa.opdsget.api.FileEntry;
import one.lfa.opdsget.api.OPDSManifestDescription;
import one.lfa.opdsget.manifest.schema.ManifestSchemas;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.net.URI;
import java.util.Map;

/**
 * A top level handler for OPDS manifest elements.
 */

public final class OPDSM1TopLevelHandler
  implements BTElementHandlerType<FileEntry, OPDSManifestDescription>
{
  private final OPDSManifestDescription.Builder builder;

  /**
   * Construct a handler.
   *
   * @param context The parser context
   */

  public OPDSM1TopLevelHandler(
    final BTElementParsingContextType context)
  {
    this.builder = OPDSManifestDescription.builder();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ? extends FileEntry>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.of(
      BTQualifiedName.of(ManifestSchemas.schema1p0Namespace().toString(), "File"),
      OPDSM1FileHandler::new
    );
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXException
  {
    try {
      this.builder.setId(URI.create(attributes.getValue("id")));
      this.builder.setRootFile(attributes.getValue("rootFile"));

      final var base = attributes.getValue("base");
      if (base != null) {
        this.builder.setBase(URI.create(base));
      }

      final var searchIndex = attributes.getValue("searchIndex");
      if (searchIndex != null) {
        this.builder.setSearchIndex(searchIndex);
      }
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public OPDSManifestDescription onElementFinished(
    final BTElementParsingContextType context)
    throws SAXException
  {
    try {
      return this.builder.build();
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final FileEntry result)
    throws SAXException
  {
    try {
      this.builder.putFiles(URI.create(result.path()), result);
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }
}
