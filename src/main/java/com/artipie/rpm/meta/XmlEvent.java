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

import com.artipie.rpm.pkg.HeaderTags;
import com.artipie.rpm.pkg.Package;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;

/**
 * Xml event to write to the output stream.
 * @since 1.5
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface XmlEvent {

    /**
     * Contracts {@link XMLEvent} with provided metadata.
     * @param meta Info to build {@link XMLEvent} with
     * @throws IOException On IO error
     */
    void add(Package.Meta meta) throws IOException;

    /**
     * Implementation of {@link XmlEvent} to build event for `package` and `version` tags.
     * @since 1.5
     */
    final class PackageAndVersion implements XmlEvent {

        /**
         * Where to write the event.
         */
        private final XMLEventWriter writer;

        /**
         * Ctor.
         * @param writer Writer to write the event
         */
        public PackageAndVersion(final XMLEventWriter writer) {
            this.writer = writer;
        }

        @Override
        public void add(final Package.Meta meta) throws IOException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            final HeaderTags tags = new HeaderTags(meta);
            final String pkg = "package";
            final String version = "version";
            try {
                this.writer.add(events.createStartElement("", "", pkg));
                this.writer.add(events.createAttribute("pkgid", meta.checksum().hex()));
                this.writer.add(events.createAttribute("name", tags.name()));
                this.writer.add(events.createAttribute("arch", tags.arch()));
                this.writer.add(events.createStartElement("", "", version));
                this.writer.add(events.createAttribute("epoch", String.valueOf(tags.epoch())));
                this.writer.add(events.createAttribute("ver", tags.version()));
                this.writer.add(events.createAttribute("rel", tags.release()));
                this.writer.add(events.createEndElement("", "", version));
            } catch (final XMLStreamException err) {
                throw new IOException(err);
            }
        }
    }

    /**
     * Implementation of {@link XmlEvent} to build event for {@link XmlPackage#OTHER} package.
     * @since 1.5
     */
    final class Other implements XmlEvent {

        /**
         * Where to write the event.
         */
        private final XMLEventWriter writer;

        /**
         * Ctor.
         * @param writer Writer to write the event
         */
        public Other(final XMLEventWriter writer) {
            this.writer = writer;
        }

        @Override
        public void add(final Package.Meta meta) throws IOException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            final HeaderTags tags = new HeaderTags(meta);
            try {
                new PackageAndVersion(this.writer).add(meta);
                for (final String changelog : tags.changelog()) {
                    final ChangelogEntry entry = new ChangelogEntry(changelog);
                    final String tag = "changelog";
                    this.writer.add(events.createStartElement("", "", tag));
                    this.writer.add(events.createAttribute("date", String.valueOf(entry.date())));
                    this.writer.add(events.createAttribute("author", entry.author()));
                    this.writer.add(events.createCharacters(entry.content()));
                    this.writer.add(events.createEndElement("", "", tag));
                }
                this.writer.add(events.createEndElement("", "", "package"));
            } catch (final XMLStreamException err) {
                throw new IOException(err);
            }
        }
    }

    /**
     * Implementation of {@link XmlEvent} to build event for {@link XmlPackage#FILELISTS} package.
     * @since 1.5
     */
    final class Filelists implements XmlEvent {

        /**
         * Where to write the event.
         */
        private final XMLEventWriter writer;

        /**
         * Ctor.
         * @param writer Writer to write the event
         */
        public Filelists(final XMLEventWriter writer) {
            this.writer = writer;
        }

        @Override
        public void add(final Package.Meta meta) throws IOException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            try {
                new PackageAndVersion(this.writer).add(meta);
                new Files(this.writer).add(meta);
                this.writer.add(events.createEndElement("", "", "package"));
            } catch (final XMLStreamException err) {
                throw new IOException(err);
            }
        }
    }

    /**
     * Implementation of {@link XmlEvent} to build event for `files` tag.
     * @since 1.5
     */
    final class Files implements XmlEvent {

        /**
         * Where to write the event.
         */
        private final XMLEventWriter writer;

        /**
         * Ctor.
         * @param writer Writer to write the event
         */
        public Files(final XMLEventWriter writer) {
            this.writer = writer;
        }

        @Override
        public void add(final Package.Meta meta) throws IOException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            final HeaderTags tags = new HeaderTags(meta);
            try {
                final String[] files = tags.baseNames().toArray(new String[0]);
                final String[] dirs = tags.dirNames().toArray(new String[0]);
                final Set<String> dirset = Arrays.stream(dirs).collect(Collectors.toSet());
                final int[] did = tags.dirIndexes();
                for (int idx = 0; idx < files.length; idx += 1) {
                    final String fle = files[idx];
                    // @checkstyle MethodBodyCommentsCheck (2 lines)
                    // @todo #388:30min This condition is not covered with unit test, extend
                    //  the test to check this case and make sure it works properly.
                    if (fle.isEmpty() || fle.charAt(0) == '.') {
                        continue;
                    }
                    final String path = String.format("%s%s", dirs[did[idx]], fle);
                    this.writer.add(events.createStartElement("", "", "file"));
                    if (dirset.contains(String.format("%s/", path))) {
                        this.writer.add(events.createAttribute("type", "dir"));
                    }
                    this.writer.add(events.createCharacters(path));
                    this.writer.add(events.createEndElement("", "", "file"));
                }
            } catch (final XMLStreamException err) {
                throw new IOException(err);
            }
        }
    }

    /**
     * Implementation of {@link XmlEvent} to build event for {@link XmlPackage#PRIMARY} package.
     * @since 1.5
     * @checkstyle ExecutableStatementCountCheck (100 lines)
     */
    final class Primary implements XmlEvent {

        /**
         * Xml namespace prefix.
         */
        private static final String PRFX = "rpm";

        /**
         * Primary namespace URL.
         */
        private static final String NS_URL = XmlPackage.PRIMARY.xmlNamespaces().get(Primary.PRFX);

        /**
         * Where to write the event.
         */
        private final XMLEventWriter writer;

        /**
         * Ctor.
         * @param writer Writer to write the event
         */
        public Primary(final XMLEventWriter writer) {
            this.writer = writer;
        }

        @Override
        public void add(final Package.Meta meta) throws IOException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            final HeaderTags tags = new HeaderTags(meta);
            try {
                this.writer.add(events.createStartElement("", "", "package"));
                this.writer.add(events.createAttribute("type", "rpm"));
                this.addElement("name", tags.name());
                this.addElement("arch", tags.arch());
                this.writer.add(events.createStartElement("", "", "version"));
                this.writer.add(events.createAttribute("epoch", String.valueOf(tags.epoch())));
                this.writer.add(events.createAttribute("rel", tags.release()));
                this.writer.add(events.createAttribute("ver", tags.version()));
                this.writer.add(events.createEndElement("", "", "version"));
                this.writer.add(events.createStartElement("", "", "checksum"));
                this.writer.add(events.createAttribute("type", meta.checksum().digest().type()));
                this.writer.add(events.createAttribute("pkgid", "YES"));
                this.writer.add(events.createCharacters(meta.checksum().hex()));
                this.writer.add(events.createEndElement("", "", "checksum"));
                this.addElement("summary", tags.summary());
                this.addElement("description", tags.description());
                this.addElement("packager", tags.packager());
                this.addElement("url", tags.url());
                this.addAttributes(
                    "time",
                    new MapOf<String, String>(
                        new MapEntry<>("file", String.valueOf(tags.fileTimes())),
                        new MapEntry<>("build", String.valueOf(tags.buildTime()))
                    )
                );
                this.addAttributes(
                    "size",
                    new MapOf<String, String>(
                        new MapEntry<>("package", String.valueOf(meta.size())),
                        new MapEntry<>("installed", String.valueOf(tags.installedSize())),
                        new MapEntry<>("archive", String.valueOf(tags.archiveSize()))
                    )
                );
                this.addAttributes(
                    "location",
                    new MapOf<String, String>(new MapEntry<>("href", meta.href()))
                );
                this.writer.add(events.createStartElement("", "", "format"));
                this.addElementWithNamespace("license", tags.license());
                this.addElementWithNamespace("vendor", tags.vendor());
                this.addElementWithNamespace("group", tags.group());
                this.addElementWithNamespace("buildhost", tags.buildHost());
                this.addElementWithNamespace("sourcerpm", tags.sourceRmp());
                this.addAttributes(
                    "header-range", Primary.NS_URL, Primary.PRFX,
                    new MapOf<String, String>(
                        new MapEntry<>("start", String.valueOf(meta.range()[0])),
                        new MapEntry<>("end", String.valueOf(meta.range()[1]))
                    )
                );
                this.addProvides(tags);
                this.addRequires(tags.requires());
                new Files(this.writer).add(meta);
                this.writer.add(events.createEndElement("", "", "format"));
                this.writer.add(events.createEndElement("", "", "package"));
            } catch (final XMLStreamException err) {
                throw new IOException(err);
            }
        }

        /**
         * Builds `provides` tag.
         * @param tags Tag info
         * @throws XMLStreamException On error
         */
        private void addProvides(final HeaderTags tags) throws XMLStreamException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            this.writer.add(events.createStartElement(Primary.PRFX, Primary.NS_URL, "provides"));
            final List<String> names = tags.providesNames();
            final List<String> versions = tags.providesVer();
            for (int ind = 0; ind < names.size(); ind = ind + 1) {
                this.writer.add(events.createStartElement(Primary.PRFX, Primary.NS_URL, "entry"));
                this.writer.add(events.createAttribute("name", names.get(ind)));
                if (ind < versions.size()) {
                    this.writer.add(events.createAttribute("ver", versions.get(ind)));
                }
                this.writer.add(events.createEndElement(Primary.PRFX, Primary.NS_URL, "entry"));
            }
            this.writer.add(events.createEndElement(Primary.PRFX, Primary.NS_URL, "provides"));
        }

        /**
         * Builds `requires` tag.
         * @param requires Required dependencies list
         * @throws XMLStreamException On error
         */
        private void addRequires(final List<String> requires) throws XMLStreamException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            this.writer.add(events.createStartElement(Primary.PRFX, Primary.NS_URL, "requires"));
            final List<String> filtered = requires.stream()
                .filter(nme -> !nme.startsWith("rpmlib("))
                .collect(Collectors.toList());
            for (final String name : filtered) {
                this.writer.add(events.createStartElement(Primary.PRFX, Primary.NS_URL, "entry"));
                this.writer.add(events.createAttribute("name", name));
                this.writer.add(events.createEndElement(Primary.PRFX, Primary.NS_URL, "entry"));
            }
            this.writer.add(events.createEndElement(Primary.PRFX, Primary.NS_URL, "requires"));
        }

        /**
         * Adds tag with the provided name and characters.
         * @param tag Tag name
         * @param chars Characters
         * @throws XMLStreamException On error
         */
        private void addElementWithNamespace(final String tag, final String chars)
            throws XMLStreamException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            this.writer.add(events.createStartElement(Primary.PRFX, Primary.NS_URL, tag));
            this.writer.add(events.createCharacters(chars));
            this.writer.add(events.createEndElement(Primary.PRFX, Primary.NS_URL, tag));
        }

        /**
         * Adds tag with the provided name and characters.
         * @param tag Tag name
         * @param chars Characters
         * @throws XMLStreamException On error
         */
        private void addElement(final String tag, final String chars) throws XMLStreamException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            this.writer.add(events.createStartElement("", "", tag));
            this.writer.add(events.createCharacters(chars));
            this.writer.add(events.createEndElement("", "", tag));
        }

        /**
         * Adds tag with provided attributes list.
         * @param tag Tag name
         * @param namespace Namespace
         * @param prefix Prefix
         * @param attrs Attributes list
         * @throws XMLStreamException On Error
         * @checkstyle ParameterNumberCheck (5 lines)
         */
        private void addAttributes(final String tag, final String namespace, final String prefix,
            final Map<String, String> attrs) throws XMLStreamException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            this.writer.add(events.createStartElement(prefix, namespace, tag));
            for (final Map.Entry<String, String> attr : attrs.entrySet()) {
                this.writer.add(events.createAttribute(attr.getKey(), attr.getValue()));
            }
            this.writer.add(events.createEndElement(prefix, namespace, tag));
        }

        /**
         * Adds tag with provided attributes list.
         * @param tag Tag name
         * @param attrs Attributes list
         * @throws XMLStreamException On Error
         */
        private void addAttributes(final String tag,
            final Map<String, String> attrs) throws XMLStreamException {
            this.addAttributes(tag, "", "", attrs);
        }
    }
}
