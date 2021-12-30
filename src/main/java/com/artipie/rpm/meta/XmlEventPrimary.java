/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.misc.UncheckedConsumer;
import com.artipie.rpm.pkg.HeaderTags;
import com.artipie.rpm.pkg.Package;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;

/**
 * Implementation of {@link XmlEvent} to build event for {@link XmlPackage#PRIMARY} package.
 *
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 * @checkstyle MagicNumberCheck (20 lines)
 * @since 1.5
 */
@SuppressWarnings({"PMD.LongVariable", "PMD.AvoidDuplicateLiterals"})
public final class XmlEventPrimary implements XmlEvent {

    /**
     * Legacy prereq dependency.
     */
    private static final int RPMSENSE_PREREQ = 1 << 6;

    /**
     * Pre dependency.
     */
    private static final int RPMSENSE_SCRIPT_PRE = 1 << 9;

    /**
     * Post dependency.
     */
    private static final int RPMSENSE_SCRIPT_POST = 1 << 10;

    /**
     * Xml namespace prefix.
     */
    private static final String PRFX = "rpm";

    /**
     * Primary namespace URL.
     */
    private static final String NS_URL =
        XmlPackage.PRIMARY.xmlNamespaces().get(XmlEventPrimary.PRFX);

    @Override
    public void add(final XMLEventWriter writer, final Package.Meta meta) throws IOException {
        final XMLEventFactory events = XMLEventFactory.newFactory();
        final HeaderTags tags = new HeaderTags(meta);
        try {
            writer.add(events.createStartElement("", "", "package"));
            writer.add(events.createAttribute("type", "rpm"));
            XmlEventPrimary.addElement(writer, "name", tags.name());
            XmlEventPrimary.addElement(writer, "arch", tags.arch());
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
            XmlEventPrimary.addElement(writer, "summary", tags.summary());
            XmlEventPrimary.addElement(writer, "description", tags.description());
            XmlEventPrimary.addElement(writer, "packager", tags.packager());
            XmlEventPrimary.addElement(writer, "url", tags.url());
            XmlEventPrimary.addAttributes(
                writer,
                "time",
                new MapOf<String, String>(
                    new MapEntry<>("file", String.valueOf(tags.fileTimes())),
                    new MapEntry<>("build", String.valueOf(tags.buildTime()))
                )
            );
            XmlEventPrimary.addAttributes(
                writer,
                "size",
                new MapOf<String, String>(
                    new MapEntry<>("package", String.valueOf(meta.size())),
                    new MapEntry<>("installed", String.valueOf(tags.installedSize())),
                    new MapEntry<>("archive", String.valueOf(tags.archiveSize()))
                )
            );
            XmlEventPrimary.addAttributes(
                writer,
                "location",
                new MapOf<String, String>(new MapEntry<>("href", meta.href()))
            );
            writer.add(events.createStartElement("", "", "format"));
            XmlEventPrimary.addElementWithNamespace(writer, "license", tags.license());
            XmlEventPrimary.addElementWithNamespace(writer, "vendor", tags.vendor());
            XmlEventPrimary.addElementWithNamespace(writer, "group", tags.group());
            XmlEventPrimary.addElementWithNamespace(writer, "buildhost", tags.buildHost());
            XmlEventPrimary.addElementWithNamespace(writer, "sourcerpm", tags.sourceRmp());
            XmlEventPrimary.addAttributes(
                writer,
                "header-range", XmlEventPrimary.NS_URL, XmlEventPrimary.PRFX,
                new MapOf<String, String>(
                    new MapEntry<>("start", String.valueOf(meta.range()[0])),
                    new MapEntry<>("end", String.valueOf(meta.range()[1]))
                )
            );
            XmlEventPrimary.addProvides(writer, tags);
            XmlEventPrimary.addRequires(writer, tags);
            // @checkstyle BooleanExpressionComplexityCheck (10 lines)
            new Files(
                name -> name.startsWith("/var") || name.startsWith("/lib64")
                    || name.startsWith("/run") || name.startsWith("/usr")
                    && !(name.contains("/bin/") || name.contains("/sbin/"))
            ).add(writer, meta);
            writer.add(events.createEndElement("", "", "format"));
            writer.add(events.createEndElement("", "", "package"));
        } catch (final XMLStreamException err) {
            throw new IOException(err);
        }
    }

    /**
     * Builds `provides` tag. Attribute `flags` should be present if the version is present and
     * in `provides` the only possible flag value is `EQ`.
     *
     * @param writer Xml event writer
     * @param tags Tag info
     * @throws XMLStreamException On error
     */
    private static void addProvides(final XMLEventWriter writer, final HeaderTags tags)
        throws XMLStreamException {
        final XMLEventFactory events = XMLEventFactory.newFactory();
        writer.add(
            events.createStartElement(XmlEventPrimary.PRFX, XmlEventPrimary.NS_URL, "provides")
        );
        final List<String> names = tags.providesNames();
        final List<Optional<String>> flags = tags.providesFlags();
        final List<HeaderTags.Version> versions = tags.providesVer();
        for (int ind = 0; ind < names.size(); ind = ind + 1) {
            writer.add(
                events.createStartElement(XmlEventPrimary.PRFX, XmlEventPrimary.NS_URL, "entry")
            );
            writer.add(events.createAttribute("name", names.get(ind)));
            XmlEventPrimary.addEntryAttr(
                writer, events, versions, ind, flags, HeaderTags.Flags.EQUAL.notation()
            );
            writer.add(
                events.createEndElement(XmlEventPrimary.PRFX, XmlEventPrimary.NS_URL, "entry")
            );
        }
        writer.add(
            events.createEndElement(XmlEventPrimary.PRFX, XmlEventPrimary.NS_URL, "provides")
        );
    }

    /**
     * Builds `requires` tag. Items with names started on `rpmlib(` or `config(` are excluded,
     * duplicates without version are also excluded.
     * About `flags` attribute check {@link XmlEventPrimary#findFlag(List, Map, String)}.
     *
     * @param writer Xml event writer
     * @param tags Tag info
     * @throws XMLStreamException On error
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    private static void addRequires(final XMLEventWriter writer, final HeaderTags tags)
        throws XMLStreamException {
        final XMLEventFactory events = XMLEventFactory.newFactory();
        writer.add(
            events.createStartElement(XmlEventPrimary.PRFX, XmlEventPrimary.NS_URL, "requires")
        );
        final List<String> names = tags.requires();
        final List<Optional<String>> flags = tags.requireFlags();
        final List<Integer> intflags = tags.requireFlagsInts();
        final List<HeaderTags.Version> versions = tags.requiresVer();
        final Map<String, Integer> items = new HashMap<>(names.size());
        final Set<String> duplicates = new HashSet<>(names.size());
        final List<String> libcso = new ArrayList<>(names.size());
        for (int ind = 0; ind < names.size(); ind = ind + 1) {
            final String name = names.get(ind);
            if (name.startsWith("libc.so.")) {
                libcso.add(name);
                continue;
            }
            int pre = 0;
            if ((intflags.get(ind)
                & (XmlEventPrimary.RPMSENSE_PREREQ
                | XmlEventPrimary.RPMSENSE_SCRIPT_PRE
                | XmlEventPrimary.RPMSENSE_SCRIPT_POST)) != 0) {
                pre = 1;
            }
            String full = name.concat(flags.get(ind).orElse(""))
                .concat(versions.get(ind).toString());
            if (!versions.get(ind).toString().isEmpty()) {
                full = full.concat(String.valueOf(pre));
            }
            if (!name.startsWith("rpmlib(")
                && !name.startsWith("config(") && !duplicates.contains(full)) {
                writer.add(
                    events.createStartElement(XmlEventPrimary.PRFX, XmlEventPrimary.NS_URL, "entry")
                );
                writer.add(events.createAttribute("name", name));
                final String item = String.join("", name, versions.get(ind).toString());
                XmlEventPrimary.addEntryAttr(
                    writer, events, versions, ind, flags,
                    XmlEventPrimary.findFlag(flags, items, item)
                );
                if (pre > 0) {
                    writer.add(events.createAttribute("pre", String.valueOf(pre)));
                }
                items.put(item, ind);
                writer.add(
                    events.createEndElement(XmlEventPrimary.PRFX, XmlEventPrimary.NS_URL, "entry")
                );
            }
            duplicates.add(full);
        }
        if (!libcso.isEmpty()) {
            libcso.sort(new CrCompareDependency());
            writer.add(
                events.createStartElement(XmlEventPrimary.PRFX, XmlEventPrimary.NS_URL, "entry")
            );
            writer.add(events.createAttribute("name", libcso.get(libcso.size() - 1)));
            writer.add(
                events.createEndElement(XmlEventPrimary.PRFX, XmlEventPrimary.NS_URL, "entry")
            );
        }
        writer.add(
            events.createEndElement(XmlEventPrimary.PRFX, XmlEventPrimary.NS_URL, "requires")
        );
    }

    /**
     * Adds tag with the provided name and characters.
     *
     * @param writer Xml event writer
     * @param tag Tag name
     * @param chars Characters
     * @throws XMLStreamException On error
     */
    private static void addElementWithNamespace(final XMLEventWriter writer, final String tag,
        final String chars) throws XMLStreamException {
        final XMLEventFactory events = XMLEventFactory.newFactory();
        writer.add(events.createStartElement(XmlEventPrimary.PRFX, XmlEventPrimary.NS_URL, tag));
        writer.add(events.createCharacters(chars));
        writer.add(events.createEndElement(XmlEventPrimary.PRFX, XmlEventPrimary.NS_URL, tag));
    }

    /**
     * Adds tag with the provided name and characters.
     *
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
     *
     * @param writer Xml event writer
     * @param tag Tag name
     * @param namespace Namespace
     * @param prefix Prefix
     * @param attrs Attributes list
     * @throws XMLStreamException On Error
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    private static void addAttributes(final XMLEventWriter writer, final String tag,
        final String namespace, final String prefix, final Map<String, String> attrs)
        throws XMLStreamException {
        final XMLEventFactory events = XMLEventFactory.newFactory();
        writer.add(events.createStartElement(prefix, namespace, tag));
        for (final Map.Entry<String, String> attr : attrs.entrySet()) {
            writer.add(events.createAttribute(attr.getKey(), attr.getValue()));
        }
        writer.add(events.createEndElement(prefix, namespace, tag));
    }

    /**
     * Adds tag with provided attributes list.
     *
     * @param writer Xml event writer
     * @param tag Tag name
     * @param attrs Attributes list
     * @throws XMLStreamException On Error
     */
    private static void addAttributes(final XMLEventWriter writer, final String tag,
        final Map<String, String> attrs) throws XMLStreamException {
        XmlEventPrimary.addAttributes(writer, tag, "", "", attrs);
    }

    /**
     * Write entry attributes ver, epoch and rel.
     *
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
        final List<HeaderTags.Version> versions, final int ind, final List<Optional<String>> flags,
        final String def) throws XMLStreamException {
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
     * Try to find flag for `requires` entry: if there is en entry with such name and version,
     * use the flag it has. If there is no such entry, write `EQ`.
     *
     * @param flags Flags list
     * @param items Items: names and versions
     * @param item Current name and version
     * @return Flag value
     */
    private static String findFlag(final List<Optional<String>> flags,
        final Map<String, Integer> items, final String item) {
        return Optional.ofNullable(items.get(item)).flatMap(flags::get)
            .orElse(HeaderTags.Flags.EQUAL.notation());
    }
}
