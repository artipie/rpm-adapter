/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;

/**
 * XML {@code primary.xml} metadata imperative writer.
 * <p>
 * This object is not thread safe and depends on order of method calls.
 * </p>
 *
 * @since 0.6
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public final class XmlPrimary implements Closeable {

    /**
     * Xml primary namespace.
     */
    private static final String NAMESPACE = XmlPackage.PRIMARY.xmlNamespaces().get("rpm");

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
     * @param path Path to write primary.xml
     */
    public XmlPrimary(final Path path) {
        this(new XmlFile(path));
    }

    /**
     * Primary ctor.
     * @param xml Xml file
     */
    @SuppressWarnings("unchecked")
    public XmlPrimary(final XmlFile xml) {
        this.xml = xml;
        this.packages = new XmlPackagesFile(xml, XmlPackage.PRIMARY);
    }

    /**
     * Start packages section.
     * @return Self
     * @throws XMLStreamException On error
     */
    public XmlPrimary startPackages() throws XMLStreamException {
        this.packages.startPackages();
        return this;
    }

    /**
     * Start writing a package.
     * @return Package writer
     * @throws XMLStreamException On error
     */
    public Package startPackage() throws XMLStreamException {
        this.xml.writeStartElement("package");
        this.xml.writeAttribute("type", "rpm");
        return new Package(this.xml, this);
    }

    @Override
    public void close() {
        this.packages.close();
    }

    /**
     * XML package writer.
     * @since 0.6
     */
    public static final class Package {

        /**
         * XML stream.
         */
        private final XmlFile xml;

        /**
         * Primary.
         */
        private final XmlPrimary primary;

        /**
         * Ctor.
         * @param xml XML stream
         * @param primary XML primary
         */
        public Package(final XmlFile xml, final XmlPrimary primary) {
            this.xml = xml;
            this.primary = primary;
        }

        /**
         * Set package name.
         * @param name Name of the package
         * @return Self
         * @throws XMLStreamException On XML failure
         */
        public Package name(final String name) throws XMLStreamException {
            this.xml.writeStartElement("name");
            this.xml.writeCharacters(name);
            this.xml.writeEndElement();
            return this;
        }

        /**
         * Set package arch.
         * @param arch Arch name
         * @return Self
         * @throws XMLStreamException On XML failure
         */
        public Package arch(final String arch) throws XMLStreamException {
            this.xml.writeStartElement("arch");
            this.xml.writeCharacters(arch);
            this.xml.writeEndElement();
            return this;
        }

        /**
         * Set package version.
         * @param epoch Epoch millis
         * @param ver Version string
         * @param rel Release name
         * @return Self
         * @throws XMLStreamException On XML failure
         */
        public Package version(final int epoch, final String ver, final String rel)
            throws XMLStreamException {
            this.xml.writeEmptyElement("version");
            this.xml.writeAttribute("epoch", String.valueOf(epoch));
            this.xml.writeAttribute("ver", ver);
            this.xml.writeAttribute("rel", rel);
            return this;
        }

        /**
         * Set package checksum.
         * @param type Digest type
         * @param id Package id
         * @param sum Sum hex
         * @return Self
         * @throws XMLStreamException On XML failure
         */
        public Package checksum(final String type, final String id, final String sum)
            throws XMLStreamException {
            this.xml.writeStartElement("checksum");
            this.xml.writeAttribute("type", type);
            this.xml.writeAttribute("pkgid", id);
            this.xml.writeCharacters(sum);
            this.xml.writeEndElement();
            return this;
        }

        /**
         * Set package summary.
         * @param text Summary text
         * @return Self
         * @throws XMLStreamException On failure
         */
        public Package summary(final String text) throws XMLStreamException {
            this.xml.writeStartElement("summary");
            this.xml.writeCharacters(text);
            this.xml.writeEndElement();
            return this;
        }

        /**
         * Set package description.
         * @param text Description text
         * @return Self
         * @throws XMLStreamException On XML failure
         */
        public Package description(final String text) throws XMLStreamException {
            this.xml.writeStartElement("description");
            this.xml.writeCharacters(text);
            this.xml.writeEndElement();
            return this;
        }

        /**
         * Set packager name.
         * @param name Name
         * @return Self
         * @throws XMLStreamException On XML failure
         */
        public Package packager(final String name) throws XMLStreamException {
            this.xml.writeStartElement("packager");
            this.xml.writeCharacters(name);
            this.xml.writeEndElement();
            return this;
        }

        /**
         * Set package URL.
         * @param url URL string
         * @return Self
         * @throws XMLStreamException On XML failure
         */
        public Package url(final String url) throws XMLStreamException {
            this.xml.writeStartElement("url");
            this.xml.writeCharacters(url);
            this.xml.writeEndElement();
            return this;
        }

        /**
         * Set package time.
         * @param file File timestamp
         * @param build Build timestamp
         * @return Self
         * @throws XMLStreamException On failure
         */
        public Package time(final int file, final int build) throws XMLStreamException {
            this.xml.writeEmptyElement("time");
            this.xml.writeAttribute("file", String.valueOf(file));
            this.xml.writeAttribute("build", String.valueOf(build));
            return this;
        }

        /**
         * Set package size.
         * @param pkg Package size
         * @param installed Installed size
         * @param archive Archive size
         * @return Self
         * @throws XMLStreamException On failure
         */
        public Package size(final long pkg, final int installed, final int archive)
            throws XMLStreamException {
            this.xml.writeEmptyElement("size");
            this.xml.writeAttribute("package", String.valueOf(pkg));
            this.xml.writeAttribute("installed", String.valueOf(installed));
            this.xml.writeAttribute("archive", String.valueOf(archive));
            return this;
        }

        /**
         * Set package location.
         * @param href Location href
         * @return Self
         * @throws XMLStreamException On failure
         */
        public Package location(final String href) throws XMLStreamException {
            this.xml.writeEmptyElement("location");
            this.xml.writeAttribute("href", href);
            return this;
        }

        /**
         * Set package files.
         * @param files Files
         * @param dirs Dirs
         * @param did Directory ids
         * @return Self
         * @throws XMLStreamException On failure
         */
        @SuppressWarnings("PMD.UseVarargs")
        public Package files(final String[] files, final String[] dirs, final int[] did)
            throws XMLStreamException {
            final Set<String> dirset = Arrays.stream(dirs).collect(Collectors.toSet());
            for (int idx = 0; idx < files.length; idx += 1) {
                final String fle = files[idx];
                if (fle.isEmpty() || fle.charAt(0) == '.') {
                    continue;
                }
                final String path = String.format("%s%s", dirs[did[idx]], fle);
                this.xml.writeStartElement("file");
                if (dirset.contains(String.format("%s/", path))) {
                    this.xml.writeAttribute("type", "dir");
                }
                this.xml.writeCharacters(path);
                this.xml.writeEndElement();
            }
            return this;
        }

        /**
         * Start format section.
         * @return Format writer.
         * @throws XMLStreamException On failure
         */
        public Format startFormat() throws XMLStreamException {
            this.xml.writeStartElement("format");
            return new Format(this.xml, this);
        }

        /**
         * Close format.
         * @return Priamry reference
         * @throws XMLStreamException On error
         */
        public XmlPrimary close() throws XMLStreamException {
            this.xml.writeEndElement();
            return this.primary;
        }
    }

    /**
     * Format package writer.
     * @since 0.6
     */
    public static final class Format {

        /**
         * XML writer.
         */
        private final XmlFile xml;

        /**
         * Primary package reference.
         */
        private final XmlPrimary.Package pkg;

        /**
         * Ctor.
         * @param xml XML writer
         * @param pkg Pacakge reference
         */
        Format(final XmlFile xml, final XmlPrimary.Package pkg) {
            this.xml = xml;
            this.pkg = pkg;
        }

        /**
         * Add license.
         * @param license License name
         * @return Self
         * @throws XMLStreamException On XML failure
         */
        public Format license(final String license) throws XMLStreamException {
            return this.writeElem("license", license);
        }

        /**
         * Add vendor.
         * @param vendor Vendor name
         * @return Self
         * @throws XMLStreamException On failure
         */
        public Format vendor(final String vendor) throws XMLStreamException {
            return this.writeElem("vendor", vendor);
        }

        /**
         * Add group.
         * @param group Group name
         * @return Self
         * @throws XMLStreamException On XML failure
         */
        public Format group(final String group) throws XMLStreamException {
            return this.writeElem("group", group);
        }

        /**
         * Add build host.
         * @param host Host name
         * @return Self
         * @throws XMLStreamException On XML failure
         */
        public Format buildHost(final String host) throws XMLStreamException {
            return this.writeElem("buildhost", host);
        }

        /**
         * Add source RPM.
         * @param source Source RPM name
         * @return Self
         * @throws XMLStreamException On XML failure
         */
        public Format sourceRpm(final String source) throws XMLStreamException {
            return this.writeElem("sourcerpm", source);
        }

        /**
         * Add header range.
         * @param start Range start
         * @param end Range end
         * @return Self
         * @throws XMLStreamException On XML failure
         */
        public Format headerRange(final int start, final int end) throws XMLStreamException {
            this.xml.writeEmptyElement(XmlPrimary.NAMESPACE, "header-range");
            this.xml.writeAttribute("start", String.valueOf(start));
            this.xml.writeAttribute("end", String.valueOf(end));
            return this;
        }

        /**
         * Adds `provides` element.
         * @param names Libraries list provided by the package
         * @param versions Versions
         * @return Self
         * @throws XMLStreamException On XML error
         * @todo #220:30min We need to experimentally check that names and versions do not
         *  mix up in `provides`: download oxygen-gtk2-1.3.4-3.el7.x86_64.rpm from test bundle100
         *  (see TestBundle.class), add it to resources folder and create xml primary from it.
         *  This rpm has not full `provides` information, entry tags are:
         *  - rpm:entry name="liboxygen-gtk.so()(64bit)"
         *  - rpm:entry name="oxygen-gtk2" flags="EQ" epoch="0" ver="1.3.4" rel="3.el7"
         *  - rpm:entry name="oxygen-gtk2(x86-64)" flags="EQ" epoch="0" ver="1.3.4" rel="3.el7"
         *  Make sure we write it the same way.
         * @todo #220:30min Provides entry also can have `flags`, `epoch` and `rel` attributes.
         *  Find a way to obtain this information from rpm and add it here. Do not forget about
         *  test.
         */
        public Format provides(final List<String> names, final List<String> versions)
            throws XMLStreamException {
            this.xml.writeStartElement(XmlPrimary.NAMESPACE, "provides");
            for (int ind = 0; ind < names.size(); ind = ind + 1) {
                this.xml.writeStartElement(XmlPrimary.NAMESPACE, "entry");
                this.xml.writeAttribute("name", names.get(ind));
                if (ind < versions.size()) {
                    this.xml.writeAttribute("ver", versions.get(ind));
                }
                this.xml.writeEndElement();
            }
            this.xml.writeEndElement();
            return this;
        }

        /**
         * Add list of requires.
         * @param requires Requires entries
         * @return Self
         * @throws XMLStreamException On XML error
         */
        public Format requires(final List<String> requires) throws XMLStreamException {
            this.xml.writeStartElement(XmlPrimary.NAMESPACE, "requires");
            final List<String> filtered = requires.stream()
                .filter(nme -> !nme.startsWith("rpmlib("))
                .collect(Collectors.toList());
            for (final String name : filtered) {
                this.xml.writeStartElement(XmlPrimary.NAMESPACE, "entry");
                this.xml.writeAttribute("name", name);
                this.xml.writeEndElement();
            }
            this.xml.writeEndElement();
            return this;
        }

        /**
         * Close format section.
         * @return Parent package
         * @throws XMLStreamException On XML error
         */
        public Package close() throws XMLStreamException {
            this.xml.writeEndElement();
            return this.pkg;
        }

        /**
         * Write standard format element.
         * @param name Element name
         * @param value Element value
         * @return Itself
         * @throws XMLStreamException On error
         */
        private Format writeElem(final String name, final String value) throws XMLStreamException {
            this.xml.writeStartElement(XmlPrimary.NAMESPACE, name);
            this.xml.writeCharacters(value);
            this.xml.writeEndElement();
            return this;
        }
    }
}
