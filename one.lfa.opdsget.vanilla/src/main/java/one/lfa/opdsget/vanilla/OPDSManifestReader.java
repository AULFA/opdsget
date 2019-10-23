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

package one.lfa.opdsget.vanilla;

import com.io7m.blackthorne.api.BTContentHandler;
import com.io7m.blackthorne.api.BTParseError;
import com.io7m.blackthorne.api.BTParseErrorType;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jxe.core.JXEHardenedSAXParsers;
import com.io7m.jxe.core.JXESchemaDefinition;
import com.io7m.jxe.core.JXESchemaResolutionMappings;
import com.io7m.jxe.core.JXEXInclude;
import one.lfa.opdsget.api.OPDSManifestDescription;
import one.lfa.opdsget.api.OPDSManifestParseError;
import one.lfa.opdsget.api.OPDSManifestParseErrorType;
import one.lfa.opdsget.api.OPDSManifestReaderErrorReceiverType;
import one.lfa.opdsget.api.OPDSManifestReaderType;
import one.lfa.opdsget.manifest.schema.ManifestSchemas;
import one.lfa.opdsget.vanilla.parsers.v1.OPDSM1TopLevelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class OPDSManifestReader implements OPDSManifestReaderType
{
  private static final Logger LOG = LoggerFactory.getLogger(OPDSManifestReader.class);

  private static final JXESchemaDefinition SCHEMA_1_0 =
    JXESchemaDefinition.builder()
      .setFileIdentifier("schema-1.0.xsd")
      .setLocation(ManifestSchemas.schema1p0URL())
      .setNamespace(ManifestSchemas.schema1p0Namespace())
      .build();

  private static final JXESchemaResolutionMappings SCHEMA_MAPPINGS =
    JXESchemaResolutionMappings.builder()
      .putMappings(SCHEMA_1_0.namespace(), SCHEMA_1_0)
      .build();

  private static final JXEHardenedSAXParsers PARSERS =
    new JXEHardenedSAXParsers();

  private final SafeErrorPublisher errors;
  private final InputStream inputStream;
  private final URI uri;

  OPDSManifestReader(
    final OPDSManifestReaderErrorReceiverType inErrors,
    final URI inURI,
    final InputStream inInputStream)
  {
    this.errors =
      new SafeErrorPublisher(inErrors);
    this.uri =
      Objects.requireNonNull(inURI, "inURI");
    this.inputStream =
      Objects.requireNonNull(inInputStream, "inputStream");
  }

  private static OPDSManifestParseError blackthorneToOPDS(
    final URI uri,
    final BTParseError error)
  {
    return OPDSManifestParseError.builder()
      .setException(error.exception())
      .setLexical(error.lexical())
      .setMessage(error.message())
      .setSeverity(blackthorneSeverityToOPDSSeverity(error.severity()))
      .build();
  }

  private static OPDSManifestParseErrorType.Severity blackthorneSeverityToOPDSSeverity(
    final BTParseErrorType.Severity severity)
  {
    switch (severity) {
      case WARNING:
        return OPDSManifestParseErrorType.Severity.WARNING;
      case ERROR:
        return OPDSManifestParseErrorType.Severity.ERROR;
    }
    throw new IllegalStateException("Unreachable code");
  }

  private static OPDSManifestParseError errorGeneral(
    final URI uri,
    final Exception e)
  {
    final LexicalPosition<URI> lexical;
    if (e instanceof SAXParseException) {
      lexical = LexicalPosition.<URI>builder()
        .setColumn(((SAXParseException) e).getColumnNumber())
        .setLine(((SAXParseException) e).getLineNumber())
        .setFile(uri)
        .build();
    } else {
      lexical = LexicalPosition.<URI>builder()
        .setColumn(0)
        .setLine(0)
        .setFile(uri)
        .build();
    }

    return OPDSManifestParseError.builder()
      .setException(e)
      .setLexical(lexical)
      .setMessage(e.getLocalizedMessage())
      .setSeverity(OPDSManifestParseErrorType.Severity.ERROR)
      .build();
  }

  @Override
  public Optional<OPDSManifestDescription> read()
  {
    try {
      LOG.debug("executing parser for {}", this.uri);

      final var parser =
        PARSERS.createXMLReader(Optional.empty(), JXEXInclude.XINCLUDE_DISABLED, SCHEMA_MAPPINGS);

      final var source = new InputSource(this.inputStream);
      final var urlText = this.uri.toString();
      source.setPublicId(urlText);

      final var contentHandler =
        BTContentHandler.<OPDSManifestDescription>builder()
          .addHandler(
            SCHEMA_1_0.namespace().toString(),
            "Manifest",
            OPDSM1TopLevelHandler::new)
          .build(this.uri, error -> this.errors.onError(blackthorneToOPDS(this.uri, error)));

      parser.setErrorHandler(contentHandler);
      parser.setContentHandler(contentHandler);
      parser.parse(source);

      if (this.errors.failed) {
        return Optional.empty();
      }

      return contentHandler.result().map(Function.identity());
    } catch (final ParserConfigurationException | SAXException | IOException e) {
      this.errors.onError(errorGeneral(this.uri, e));
      return Optional.empty();
    }
  }

  @Override
  public void close()
    throws IOException
  {

  }

  private static final class SafeErrorPublisher implements OPDSManifestReaderErrorReceiverType
  {
    private final OPDSManifestReaderErrorReceiverType errors;
    private boolean failed;

    SafeErrorPublisher(
      final OPDSManifestReaderErrorReceiverType inErrors)
    {
      this.errors = Objects.requireNonNull(inErrors, "errors");
    }

    @Override
    public void onError(final OPDSManifestParseError error)
    {
      try {
        switch (error.severity()) {
          case WARNING:
            break;
          case ERROR:
            this.failed = true;
            break;
        }

        this.errors.onError(error);
      } catch (final Exception e) {
        LOG.debug("ignored exception raised by receiver: ", e);
      }
    }
  }
}
