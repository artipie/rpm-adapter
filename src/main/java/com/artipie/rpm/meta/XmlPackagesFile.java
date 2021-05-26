/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import java.io.Closeable;
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
    private final XmlPackage mtd;

    /**
     * Ctor.
     * @param xml Xml file
     * @param mtd Metadata
     */
    public XmlPackagesFile(final XmlFile xml, final XmlPackage mtd) {
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
        for (final Map.Entry<String, String> namespace: this.mtd.xmlNamespaces().entrySet()) {
            this.xml.writeNamespace(namespace.getKey(), namespace.getValue());
        }
        this.xml.writeAttribute(XmlPackagesFile.PACKAGES_ATTR, "-1");
    }

    @Override
    public void close() {
        try {
            this.xml.writeEndElement();
            this.xml.writeEndDocument();
            this.xml.close();
        } catch (final XMLStreamException err) {
            throw new XmlException("Failed to close", err);
        }
    }
}
