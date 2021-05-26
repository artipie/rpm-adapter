/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.xml.stream.XMLStreamException;

/**
 * XML {@code others.xml} metadata imperative writer.
 * <p>
 * This object is not thread safe and depends on order of method calls.
 * </p>
 * @since 0.6
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class XmlOthers implements Closeable {

    /**
     * Xml file.
     */
    private final XmlFile xml;

    /**
     * XmlPackagesFile writer.
     */
    private final XmlPackagesFile packages;

    /**
     * Ctor.
     * @param file Path to write other.xml
     */
    public XmlOthers(final Path file) {
        this(new XmlFile(file));
    }

    /**
     * Primary ctor.
     * @param xml Xml file
     */
    private XmlOthers(final XmlFile xml) {
        this.xml = xml;
        this.packages = new XmlPackagesFile(xml, XmlPackage.OTHER);
    }

    /**
     * Start packages.
     * @return Self
     * @throws XMLStreamException On error
     */
    public XmlOthers startPackages() throws XMLStreamException {
        this.packages.startPackages();
        return this;
    }

    /**
     * Add new package.
     * @param name Package name
     * @param arch Package arch
     * @param checksum Package checksum
     * @return Package writer
     * @throws XMLStreamException On error
     */
    public XmlOthers.Package addPackage(final String name, final String arch, final String checksum)
        throws XMLStreamException {
        this.xml.writeStartElement("package");
        this.xml.writeAttribute("pkgid", checksum);
        this.xml.writeAttribute("name", name);
        this.xml.writeAttribute("arch", arch);
        return new XmlOthers.Package(this, this.xml);
    }

    @Override
    public void close() throws IOException {
        this.packages.close();
    }

    /**
     * Package writer.
     * @since 0.6
     */
    public static final class Package {

        /**
         * Others reference.
         */
        private final XmlOthers others;

        /**
         * XML writer.
         */
        private final XmlFile xml;

        /**
         * Ctor.
         * @param others Others reference
         * @param xml XML writer
         */
        Package(final XmlOthers others, final XmlFile xml) {
            this.others = others;
            this.xml = xml;
        }

        /**
         * Add version.
         * @param eopch Epoch seconds
         * @param ver Version string
         * @param rel Release name
         * @return Self
         * @throws XMLStreamException On error
         */
        public Package version(final int eopch, final String ver, final String rel)
            throws XMLStreamException {
            this.xml.writeEmptyElement("version");
            this.xml.writeAttribute("epoch", String.valueOf(eopch));
            this.xml.writeAttribute("ver", ver);
            this.xml.writeAttribute("rel", rel);
            return this;
        }

        /**
         * Add changelog.
         * @param changelogs List of changelog items
         * @return Self
         * @throws XMLStreamException On error
         */
        public Package changelog(final List<String> changelogs) throws XMLStreamException {
            for (final String changelog : changelogs) {
                final ChangelogEntry entry = new ChangelogEntry(changelog);
                this.changelog(entry.author(), entry.date(), entry.content());
            }
            return this;
        }

        /**
         * Adds changelog tag to others.xml file.
         * @param author Changelog author
         * @param date Epoch seconds
         * @param content Changelog content
         * @return Self
         * @throws XMLStreamException On error
         */
        public Package changelog(final String author, final int date, final String content)
            throws XMLStreamException {
            this.xml.writeStartElement("changelog");
            this.xml.writeAttribute("date", String.valueOf(date));
            this.xml.writeAttribute("author", String.valueOf(author));
            this.xml.writeCharacters(content);
            this.xml.writeEndElement();
            return this;
        }

        /**
         * Close packages.
         * @return Others
         * @throws XMLStreamException On error
         */
        public XmlOthers close() throws XMLStreamException {
            this.xml.writeEndElement();
            return this.others;
        }
    }
}
