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

import one.lfa.opdsget.api.FileEntry;
import one.lfa.opdsget.api.OPDSManifestDescription;
import one.lfa.opdsget.api.OPDSManifestWriterType;
import one.lfa.opdsget.manifest.schema.ManifestSchemas;
import org.apache.commons.codec.binary.Hex;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An OPDS manifest writer.
 */

public final class OPDSManifestWriter implements OPDSManifestWriterType
{
  private final OutputStream outputStream;
  private final ByteArrayOutputStream buffer;
  private final OPDSManifestDescription description;

  OPDSManifestWriter(
    final OPDSManifestDescription inDescription,
    final OutputStream inOutputStream)
  {
    this.description =
      Objects.requireNonNull(inDescription, "inDescription");
    this.outputStream =
      Objects.requireNonNull(inOutputStream, "outputStream");
    this.buffer =
      new ByteArrayOutputStream(4096);
  }

  private static void writeFile(
    final XMLStreamWriter writer,
    final String namespaceURI,
    final FileEntry file)
    throws XMLStreamException
  {
    writer.writeStartElement("om", "File", namespaceURI);
    writer.writeAttribute("name", file.path());
    writer.writeAttribute("hashAlgorithm", file.hashAlgorithm());
    writer.writeAttribute("hash", Hex.encodeHexString(file.hash(), false));
    writer.writeEndElement();
  }

  @Override
  public void write()
    throws IOException
  {
    try {
      final var namespaceURI = ManifestSchemas.schema1p0Namespace().toString();
      final var factory = XMLOutputFactory.newFactory();
      final var writer = factory.createXMLStreamWriter(this.buffer, "UTF-8");

      writer.writeStartDocument("UTF-8", "1.0");
      writer.setPrefix("om", namespaceURI);
      writer.writeStartElement(namespaceURI, "Manifest");
      writer.writeNamespace("om", namespaceURI);

      writer.writeAttribute("rootFile", this.description.rootFile());
      writer.writeAttribute("id", this.description.id().toString());
      writer.writeAttribute("updated", this.description.updated().toString());

      final var searchOpt = this.description.searchIndex();
      if (searchOpt.isPresent()) {
        writer.writeAttribute("searchIndex", searchOpt.get());
      }

      final var baseOpt = this.description.base();
      if (baseOpt.isPresent()) {
        writer.writeAttribute("base", baseOpt.get().toString());
      }

      this.writeFiles(namespaceURI, writer);

      writer.writeEndElement();
      writer.writeEndDocument();
      this.buffer.flush();

      final var transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.transform(
        new StreamSource(new ByteArrayInputStream(this.buffer.toByteArray())),
        new StreamResult(this.outputStream));

      this.outputStream.flush();
    } catch (final XMLStreamException | TransformerException e) {
      throw new IOException(e);
    }
  }

  private void writeFiles(
    final String namespaceURI,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    final var files = this.description.files();
    final var entries = files.entrySet();
    final var sorted =
      entries.stream()
        .sorted(Comparator.comparing(e -> e.getValue().path()))
        .collect(Collectors.toUnmodifiableList());

    for (final var entry : sorted) {
      writeFile(writer, namespaceURI, entry.getValue());
    }
  }

  @Override
  public void close()
    throws IOException
  {
    this.outputStream.close();
  }
}
