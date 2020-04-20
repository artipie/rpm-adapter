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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
 * @todo #81:30min Introduce a new class named XmlPackagesFile that should be responsible
 *  of writing the start of the document (as in {XmlFilelists, XmlOthers, XmlPrimary}.startPackages
 *  and XmlRepomd.begin) as well as writing the end of the document (as in
 *  {XmlFilelists, XmlOthers, XmlPrimary}.close) by taking the needed information in
 *  its constructor + a XmlFile. It should also be responsible of counting the number
 *  of packages added. With all this information, the alter method can then be moved
 *  to XmlPackagesFile and renamed close by exploiting directly that information.
 *  Once it is done, use XmlPackagesFile in XmlFilelists, XmlOthers, XmlPrimary and keep
 *  using XmlFile in XmlRepomd.
 */
@SuppressWarnings("PMD.TooManyMethods")
final class XmlFile extends XmlWriterWrap {

    /**
     * XML factory.
     */
    private static final XMLOutputFactory FACTORY = XMLOutputFactory.newInstance();

    /**
     * Output stream.
     */
    private final OutputStream stream;

    /**
     * XML file path.
     */
    private final Path path;

    /**
     * Primary ctor.
     * @param path File path
     */
    XmlFile(final Path path) {
        this(path, outputStream(path));
    }

    /**
     * Primary ctor.
     * @param path File path
     * @param out Underlying output stream
     */
    private XmlFile(final Path path, final OutputStream out) {
        this(path, out, xmlStreamWriter(out));
    }

    /**
     * Primary ctor.
     * @param path File path
     * @param out Underlying output stream
     * @param xml XML stream writer
     */
    private XmlFile(final Path path, final OutputStream out, final XMLStreamWriter xml) {
        super(xml);
        this.path = path;
        this.stream = out;
    }

    /**
     * Override attribute value in the specified tag.
     *
     * @param tag Tag to find
     * @param attribute Attribute to find
     * @param value Value to be replace
     * @throws IOException when XML alteration causes error
     */
    public void alterTag(
        final String tag, final String attribute, final String value
    ) throws IOException {
        this.stream.close();
        final Path trf = Files.createTempFile("", ".xml");
        try {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            try (
                InputStream input = Files.newInputStream(this.path);
                OutputStream out = Files.newOutputStream(trf)
            ) {
                transformer.transform(
                    new StAXSource(
                        new AlterAttributeEventReader(
                            XMLInputFactory.newFactory().createXMLEventReader(input),
                            tag, attribute, value
                        )
                    ),
                    new StreamResult(out)
                );
            }
            Files.move(trf, this.path, StandardCopyOption.REPLACE_EXISTING);
        } catch (final XMLStreamException | TransformerException err) {
            throw new IOException("Failed to alter file", err);
        }  finally {
            Files.deleteIfExists(trf);
        }
    }

    /**
     * New stream from path.
     * @param path File path
     * @return Output stream
     */
    private static OutputStream outputStream(final Path path) {
        try {
            return Files.newOutputStream(path);
        } catch (final IOException err) {
            throw new UncheckedIOException("Failed to open file stream", err);
        }
    }

    /**
     * New XML stream writer from path.
     * @param out Output stream
     * @return XML stream writer
     */
    private static XMLStreamWriter xmlStreamWriter(final OutputStream out) {
        try {
            return XmlFile.FACTORY.createXMLStreamWriter(
                out,
                StandardCharsets.UTF_8.name()
            );
        } catch (final XMLStreamException err) {
            throw new IllegalStateException("Failed to create XML stream", err);
        }
    }
}
