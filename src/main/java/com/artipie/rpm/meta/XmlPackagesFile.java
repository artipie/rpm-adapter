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

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

/**
 * Starting tag and ending tag writer for metadata writers.
 *
 * @since 0.6
 */
public final class XmlPackagesFile implements Closeable {

    /**
     * Packages count attribute in the xml.
     */
    private static final String PACKAGES_ATTR = "packages";

    /**
     * Xml file.
     */
    private final XmlFile xml;

    /**
     * Metadata.
     */
    private final Metadata mtd;

    /**
     * Ctor.
     * @param xml Xml file
     * @param mtd Metadata
     */
    public XmlPackagesFile(final XmlFile xml, final Metadata mtd) {
        this.xml = xml;
        this.mtd = mtd;
    }

    /**
     * Start packages section.
     * @throws XMLStreamException On error
     */
    public void startPackages() throws XMLStreamException {
        this.xml.writeStartDocument(StandardCharsets.UTF_8.displayName(), "1.0");
        this.xml.writeStartElement(this.mtd.tag());
        for (final Map.Entry<String, String> namespace: this.mtd.namespaces().entrySet()) {
            this.xml.writeNamespace(namespace.getKey(), namespace.getValue());
        }
        this.xml.writeAttribute(XmlPackagesFile.PACKAGES_ATTR, "-1");
    }

    @Override
    public void close() throws IOException {
        try {
            this.xml.writeEndElement();
            this.xml.writeEndDocument();
            this.xml.close();
        } catch (final XMLStreamException err) {
            throw new IOException("Failed to close", err);
        }
    }
}
