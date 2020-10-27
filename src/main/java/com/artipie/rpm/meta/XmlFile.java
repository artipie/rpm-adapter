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
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Xml file.
 *
 * @since 1.0
 */
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
     * Primary ctor.
     * @param path File path
     */
    XmlFile(final Path path) {
        this(outputStream(path));
    }

    /**
     * Primary ctor.
     * @param out Underlying output stream
     */
    private XmlFile(final OutputStream out) {
        super(xmlStreamWriter(out));
        this.stream = out;
    }

    @Override
    public void close() throws XMLStreamException {
        try {
            super.close();
            this.stream.close();
        } catch (final IOException ex) {
            throw new XMLStreamException("Failed to close", ex);
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
     * @param stream Output stream
     * @return XML stream writer
     */
    private static XMLStreamWriter xmlStreamWriter(final OutputStream stream) {
        try {
            return XmlFile.FACTORY.createXMLStreamWriter(
                stream,
                StandardCharsets.UTF_8.name()
            );
        } catch (final XMLStreamException err) {
            throw new XmlException("Failed to create XML stream", err);
        }
    }
}
