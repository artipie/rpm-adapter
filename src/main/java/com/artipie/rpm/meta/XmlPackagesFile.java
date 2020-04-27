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
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.stream.XMLStreamException;

/**
 * Starting tag and ending tag writer for metadata writers. Counts and writes packages count.
 *
 * @since 0.6
 * @todo #104:30min XmlPackagesFile is now used in XmlOthers and XmlFilelists. Add a support for
 *  additional namespaces (in addition to the default namespace) so it can be used in XmlPrimary
 *  as well.
 */
public final class XmlPackagesFile implements Closeable {

    /**
     * Packages count attribute in the xml.
     */
    private static final String PACKAGES_ATTR = "packages";

    /**
     * Packages counter.
     */
    private final AtomicInteger packages;

    /**
     * Xml file.
     */
    private final XmlFile xml;

    /**
     * The starting tag to write.
     */
    private final String tag;

    /**
     * Default namespace.
     */
    private final String namespace;

    /**
     * Primary ctor.
     * @param xml Xml file
     * @param tag Starting tag
     * @param namespace Default namespace
     */
    public XmlPackagesFile(final XmlFile xml, final String tag, final String namespace) {
        this.xml = xml;
        this.tag = tag;
        this.namespace = namespace;
        this.packages = new AtomicInteger();
    }

    /**
     * Start packages section.
     * @return Self
     * @throws XMLStreamException On error
     */
    public XmlFile startPackages() throws XMLStreamException {
        this.xml.writeStartDocument(StandardCharsets.UTF_8.displayName(), "1.0");
        this.xml.writeStartElement(this.tag);
        this.xml.writeDefaultNamespace(this.namespace);
        this.xml.writeAttribute(XmlPackagesFile.PACKAGES_ATTR, "-1");
        return this.xml;
    }

    @Override
    public void close() throws IOException {
        try {
            this.xml.writeEndElement();
            this.xml.writeEndDocument();
            this.xml.close();
            this.xml.alterTag(
                this.tag,
                XmlPackagesFile.PACKAGES_ATTR,
                String.valueOf(this.packages.get())
            );
        } catch (final XMLStreamException err) {
            throw new IOException("Failed to close", err);
        }
    }

    /**
     * Listener for single package closing.
     */
    public void packageClose() {
        this.packages.incrementAndGet();
    }
}
