/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.misc.UncheckedConsumer;
import com.artipie.rpm.pkg.HeaderTags;
import com.artipie.rpm.pkg.Package;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
     * @param writer Event writer where to event
     * @param meta Info to build {@link XMLEvent} with
     * @throws IOException On IO error
     */
    void add(XMLEventWriter writer, Package.Meta meta) throws IOException;

    /**
     * Implementation of {@link XmlEvent} to build event for `package` and `version` tags.
     * @since 1.5
     */
    final class PackageAndVersion implements XmlEvent {

        @Override
        public void add(final XMLEventWriter writer, final Package.Meta meta) throws IOException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            final HeaderTags tags = new HeaderTags(meta);
            final String pkg = "package";
            final String version = "version";
            try {
                writer.add(events.createStartElement("", "", pkg));
                writer.add(events.createAttribute("pkgid", meta.checksum().hex()));
                writer.add(events.createAttribute("name", tags.name()));
                writer.add(events.createAttribute("arch", tags.arch()));
                writer.add(events.createStartElement("", "", version));
                writer.add(events.createAttribute("epoch", String.valueOf(tags.epoch())));
                writer.add(events.createAttribute("ver", tags.version()));
                writer.add(events.createAttribute("rel", tags.release()));
                writer.add(events.createEndElement("", "", version));
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

        @Override
        public void add(final XMLEventWriter writer, final Package.Meta meta) throws IOException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            final HeaderTags tags = new HeaderTags(meta);
            try {
                new PackageAndVersion().add(writer, meta);
                for (final String changelog : tags.changelog()) {
                    final ChangelogEntry entry = new ChangelogEntry(changelog);
                    final String tag = "changelog";
                    writer.add(events.createStartElement("", "", tag));
                    writer.add(events.createAttribute("date", String.valueOf(entry.date())));
                    writer.add(events.createAttribute("author", entry.author()));
                    writer.add(events.createCharacters(entry.content()));
                    writer.add(events.createEndElement("", "", tag));
                }
                writer.add(events.createEndElement("", "", "package"));
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

        @Override
        public void add(final XMLEventWriter writer, final Package.Meta meta) throws IOException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            try {
                new PackageAndVersion().add(writer, meta);
                new Files().add(writer, meta);
                writer.add(events.createEndElement("", "", "package"));
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

        @Override
        public void add(final XMLEventWriter writer, final Package.Meta meta) throws IOException {
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
                    writer.add(events.createStartElement("", "", "file"));
                    if (dirset.contains(String.format("%s/", path))) {
                        writer.add(events.createAttribute("type", "dir"));
                    }
                    writer.add(events.createCharacters(path));
                    writer.add(events.createEndElement("", "", "file"));
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

        @Override
        public void add(final XMLEventWriter writer, final Package.Meta meta) throws IOException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            final HeaderTags tags = new HeaderTags(meta);
            try {
                writer.add(events.createStartElement("", "", "package"));
                writer.add(events.createAttribute("type", "rpm"));
                Primary.addElement(writer, "name", tags.name());
                Primary.addElement(writer, "arch", tags.arch());
                writer.add(events.createStartElement("", "", "version"));
                writer.add(events.createAttribute("epoch", String.valueOf(tags.epoch())));
                writer.add(events.createAttribute("rel", tags.release()));
                writer.add(events.createAttribute("ver", tags.version()));
                writer.add(events.createEndElement("", "", "version"));
                writer.add(events.createStartElement("", "", "checksum"));
                writer.add(events.createAttribute("type", meta.checksum().digest().type()));
                writer.add(events.createAttribute("pkgid", "YES"));
                writer.add(events.createCharacters(meta.checksum().hex()));
                writer.add(events.createEndElement("", "", "checksum"));
                Primary.addElement(writer, "summary", tags.summary());
                Primary.addElement(writer, "description", tags.description());
                Primary.addElement(writer, "packager", tags.packager());
                Primary.addElement(writer, "url", tags.url());
                Primary.addAttributes(
                    writer,
                    "time",
                    new MapOf<String, String>(
                        new MapEntry<>("file", String.valueOf(tags.fileTimes())),
                        new MapEntry<>("build", String.valueOf(tags.buildTime()))
                    )
                );
                Primary.addAttributes(
                    writer,
                    "size",
                    new MapOf<String, String>(
                        new MapEntry<>("package", String.valueOf(meta.size())),
                        new MapEntry<>("installed", String.valueOf(tags.installedSize())),
                        new MapEntry<>("archive", String.valueOf(tags.archiveSize()))
                    )
                );
                Primary.addAttributes(
                    writer,
                    "location",
                    new MapOf<String, String>(new MapEntry<>("href", meta.href()))
                );
                writer.add(events.createStartElement("", "", "format"));
                Primary.addElementWithNamespace(writer, "license", tags.license());
                Primary.addElementWithNamespace(writer, "vendor", tags.vendor());
                Primary.addElementWithNamespace(writer, "group", tags.group());
                Primary.addElementWithNamespace(writer, "buildhost", tags.buildHost());
                Primary.addElementWithNamespace(writer, "sourcerpm", tags.sourceRmp());
                Primary.addAttributes(
                    writer,
                    "header-range", Primary.NS_URL, Primary.PRFX,
                    new MapOf<String, String>(
                        new MapEntry<>("start", String.valueOf(meta.range()[0])),
                        new MapEntry<>("end", String.valueOf(meta.range()[1]))
                    )
                );
                Primary.addProvides(writer, tags);
                Primary.addRequires(writer, tags);
                new Files().add(writer, meta);
                writer.add(events.createEndElement("", "", "format"));
                writer.add(events.createEndElement("", "", "package"));
            } catch (final XMLStreamException err) {
                throw new IOException(err);
            }
        }

        /**
         * Builds `provides` tag. Attribute `flags` should be present if the version is present and
         * in `provides` the only possible flag value is `EQ`.
         * @param writer Xml event writer
         * @param tags Tag info
         * @throws XMLStreamException On error
         */
        private static void addProvides(final XMLEventWriter writer, final HeaderTags tags)
            throws XMLStreamException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            writer.add(events.createStartElement(Primary.PRFX, Primary.NS_URL, "provides"));
            final List<String> names = tags.providesNames();
            final List<Optional<String>> flags = tags.providesFlags();
            final List<HeaderTags.Version> versions = tags.providesVer();
            for (int ind = 0; ind < names.size(); ind = ind + 1) {
                writer.add(events.createStartElement(Primary.PRFX, Primary.NS_URL, "entry"));
                writer.add(events.createAttribute("name", names.get(ind)));
                Primary.addEntryAttr(
                    writer, events, versions, ind, flags, HeaderTags.Flags.EQUAL.notation()
                );
                writer.add(events.createEndElement(Primary.PRFX, Primary.NS_URL, "entry"));
            }
            writer.add(events.createEndElement(Primary.PRFX, Primary.NS_URL, "provides"));
        }

        /**
         * Builds `requires` tag. Items with names started on `rpmlib(` or `config(` are excluded,
         * duplicates without version are also excluded.
         * About `flags` attribute check {@link Primary#findFlag(List, Map, String)}.
         * @param writer Xml event writer
         * @param tags Tag info
         * @throws XMLStreamException On error
         */
        private static void addRequires(final XMLEventWriter writer, final HeaderTags tags)
            throws XMLStreamException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            writer.add(events.createStartElement(Primary.PRFX, Primary.NS_URL, "requires"));
            final List<String> names = tags.requires();
            final List<Optional<String>> flags = tags.requireFlags();
            final List<HeaderTags.Version> versions = tags.requiresVer();
            final Map<String, Integer> items = new HashMap<>(names.size());
            final Set<String> duplicates = new HashSet<>(names.size());
            for (int ind = 0; ind < names.size(); ind = ind + 1) {
                final String name = names.get(ind);
                if (!name.startsWith("rpmlib(")
                    && !name.startsWith("config(") && !duplicates.contains(name)) {
                    writer.add(events.createStartElement(Primary.PRFX, Primary.NS_URL, "entry"));
                    writer.add(events.createAttribute("name", name));
                    final String item = String.join(
                        "", name, versions.get(ind).toString()
                    );
                    Primary.addEntryAttr(
                        writer, events, versions, ind, flags, Primary.findFlag(flags, items, item)
                    );
                    items.put(item, ind);
                    writer.add(events.createEndElement(Primary.PRFX, Primary.NS_URL, "entry"));
                    if (versions.get(ind).ver().isEmpty()) {
                        duplicates.add(name);
                    }
                }
            }
            writer.add(events.createEndElement(Primary.PRFX, Primary.NS_URL, "requires"));
        }

        /**
         * Adds tag with the provided name and characters.
         * @param writer Xml event writer
         * @param tag Tag name
         * @param chars Characters
         * @throws XMLStreamException On error
         */
        private static void addElementWithNamespace(final XMLEventWriter writer, final String tag,
            final String chars) throws XMLStreamException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            writer.add(events.createStartElement(Primary.PRFX, Primary.NS_URL, tag));
            writer.add(events.createCharacters(chars));
            writer.add(events.createEndElement(Primary.PRFX, Primary.NS_URL, tag));
        }

        /**
         * Adds tag with the provided name and characters.
         * @param writer Xml event writer
         * @param tag Tag name
         * @param chars Characters
         * @throws XMLStreamException On error
         */
        private static void addElement(final XMLEventWriter writer, final String tag,
            final String chars) throws XMLStreamException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            writer.add(events.createStartElement("", "", tag));
            writer.add(events.createCharacters(chars));
            writer.add(events.createEndElement("", "", tag));
        }

        /**
         * Adds tag with provided attributes list.
         * @param writer Xml event writer
         * @param tag Tag name
         * @param namespace Namespace
         * @param prefix Prefix
         * @param attrs Attributes list
         * @throws XMLStreamException On Error
         * @checkstyle ParameterNumberCheck (5 lines)
         */
        private static void addAttributes(final XMLEventWriter writer, final String tag,
            final String namespace, final String prefix,
            final Map<String, String> attrs) throws XMLStreamException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            writer.add(events.createStartElement(prefix, namespace, tag));
            for (final Map.Entry<String, String> attr : attrs.entrySet()) {
                writer.add(events.createAttribute(attr.getKey(), attr.getValue()));
            }
            writer.add(events.createEndElement(prefix, namespace, tag));
        }

        /**
         * Adds tag with provided attributes list.
         * @param writer Xml event writer
         * @param tag Tag name
         * @param attrs Attributes list
         * @throws XMLStreamException On Error
         */
        private static void addAttributes(final XMLEventWriter writer, final String tag,
            final Map<String, String> attrs) throws XMLStreamException {
            Primary.addAttributes(writer, tag, "", "", attrs);
        }

        /**
         * Write entry attributes ver, epoch and rel.
         * @param writer Where to write
         * @param events Xml events
         * @param versions Versions
         * @param ind Current index
         * @param flags Entries flags
         * @param def Default flag
         * @throws XMLStreamException On error
         * @checkstyle ParameterNumberCheck (5 lines)
         */
        private static void addEntryAttr(final XMLEventWriter writer, final XMLEventFactory events,
            final List<HeaderTags.Version> versions, final int ind,
            final List<Optional<String>> flags, final String def)
            throws XMLStreamException {
            if (ind < versions.size() && !versions.get(ind).ver().isEmpty()) {
                writer.add(events.createAttribute("ver", versions.get(ind).ver()));
                writer.add(events.createAttribute("epoch", versions.get(ind).epoch()));
                versions.get(ind).rel().ifPresent(
                    new UncheckedConsumer<>(rel -> writer.add(events.createAttribute("rel", rel)))
                );
                writer.add(events.createAttribute("flags", flags.get(ind).orElse(def)));
            }
        }

        /**
         * Try to find flag for `requires` entry: if there en entry with such name and version,
         * use the flag it has. If there is no such entry, write `EQ`.
         * @param flags Flags list
         * @param items Items: names and versions
         * @param item Current name and version
         * @return Flag value
         */
        private static String findFlag(final List<Optional<String>> flags,
            final Map<String, Integer> items, final String item) {
            return Optional.ofNullable(items.get(item))
                .map(index -> flags.get(index).orElse(HeaderTags.Flags.EQUAL.notation()))
                .orElse(HeaderTags.Flags.EQUAL.notation());
        }
    }
}
