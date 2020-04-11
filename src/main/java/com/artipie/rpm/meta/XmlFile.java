/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.rpm.meta;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Xml file.
 *
 * @since 1.0
 * @todo #81:30min Introduce an envelope for XMLStreamWriter so that XmlFile
 *  can extend it and wrap the XMLStreamWriter created in the constructor
 *  instead of exposing its internal state via the writer() method.
 * @todo #81:30min Introduce a class named XmlPackagesFile that should be responsible
 *  of writing the start of the document (as in {XmlFilelists, XmlOthers, XmlPrimary}.startPackages
 *  and XmlRepomd.begin) by taking this information in its constructor + a XmlFile. It should also
 *  be responsible of counting the number of packages added. With all this information,
 *  the alter method can then be moved to XmlPackagesFile and renamed close by exploiting
 *  directly that information.
 */
final class XmlFile {

    /**
     * XML factory.
     */
    private static final XMLOutputFactory FACTORY = XMLOutputFactory.newInstance();

    /**
     * XML stream.
     */
    private final XMLStreamWriter xml;

    /**
     * XML file path.
     */
    private final Path path;

    /**
     * Primary ctor.
     * @param path File path
     */
    XmlFile(final Path path) {
        this(path, xmlStreamWriter(path));
    }

    /**
     * Primary ctor.
     * @param path File path
     * @param xml XML stream writer
     */
    private XmlFile(final Path path, final XMLStreamWriter xml) {
        this.path = path;
        this.xml = xml;
    }

    /**
     * Underlying XML writer.
     *
     * @return XML stream writer
     */
    public XMLStreamWriter writer() {
        return this.xml;
    }

    /**
     * Override attribute value in the specified tag.
     *
     * @param tag Tag to find
     * @param attribute Attribute to find
     * @param value Value to be replace
     * @throws IOException when XML alteration causes error
     */
    public void alter(
        final String tag, final String attribute, final String value
    ) throws IOException {
        final Path trf = Files.createTempFile("", ".xml");
        try {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            final XMLInputFactory factory = XMLInputFactory.newFactory();
            final XMLEventReader reader = new AlterAttributeEventReader(
                factory.createXMLEventReader(Files.newInputStream(this.path)),
                tag, attribute, value
            );
            transformer.transform(
                new StAXSource(reader),
                new StreamResult(Files.newOutputStream(trf, StandardOpenOption.TRUNCATE_EXISTING))
            );
            Files.move(trf, this.path, StandardCopyOption.REPLACE_EXISTING);
        } catch (final XMLStreamException | TransformerException err) {
            throw new IOException("Failed to alter file", err);
        }  finally {
            if (Files.exists(trf)) {
                Files.delete(trf);
            }
        }
    }

    /**
     * New XML stream writer from path.
     * @param path File path
     * @return XML stream writer
     */
    private static XMLStreamWriter xmlStreamWriter(final Path path) {
        try {
            return XmlFile.FACTORY.createXMLStreamWriter(Files.newOutputStream(path), "UTF-8");
        } catch (final XMLStreamException err) {
            throw new IllegalStateException("Failed to create XML stream", err);
        } catch (final IOException err) {
            throw new UncheckedIOException("Failed to open file stream", err);
        }
    }
}
