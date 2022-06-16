/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.pkg.Package;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.fasterxml.aalto.stax.OutputFactoryImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Merged primary xml: appends provided information to primary.xml,
 * excluding duplicated packages by `location` tag.
 * @since 1.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class MergedXmlPrimary implements MergedXml {

    /**
     * From where to read primary.xml.
     */
    private final Optional<InputStream> input;

    /**
     * Where to write the result.
     */
    private final OutputStream out;

    /**
     * Ctor.
     * @param input Input stream
     * @param out Output stream
     */
    public MergedXmlPrimary(final Optional<InputStream> input, final OutputStream out) {
        this.input = input;
        this.out = out;
    }

    /**
     * Ctor.
     * @param input Input stream
     * @param out Output stream
     */
    public MergedXmlPrimary(final InputStream input, final OutputStream out) {
        this(Optional.of(input), out);
    }

    // @checkstyle ExecutableStatementCountCheck (100 lines)
    @Override
    public Result merge(final Collection<Package.Meta> packages, final XmlEvent event)
        throws IOException {
        final AtomicLong res = new AtomicLong();
        Collection<String> checksums = Collections.emptyList();
        try {
            Optional<XMLEventReader> reader = Optional.empty();
            if (this.input.isPresent()) {
                reader = Optional.of(new InputFactoryImpl().createXMLEventReader(this.input.get()));
            }
            final XMLEventWriter writer = new OutputFactoryImpl().createXMLEventWriter(this.out);
            try {
                final XMLEventFactory events = XMLEventFactory.newFactory();
                MergedXmlPackage.startDocument(writer, "-1", XmlPackage.PRIMARY);
                if (reader.isPresent()) {
                    checksums = MergedXmlPrimary.processPackages(
                        packages.stream().map(Package.Meta::href).collect(Collectors.toSet()),
                        reader.get(), writer, res
                    );
                }
                for (final Package.Meta item : packages) {
                    event.add(writer, item);
                    res.incrementAndGet();
                }
                writer.add(events.createSpace("\n"));
                writer.add(
                    events.createEndElement(
                        new QName(XmlPackage.PRIMARY.tag()), Collections.emptyIterator()
                    )
                );
            } finally {
                writer.close();
                if (reader.isPresent()) {
                    reader.get().close();
                }
            }
        } catch (final XMLStreamException err) {
            throw new IOException(err);
        }
        return new MergedXml.Result(res.get(), checksums);
    }

    /**
     * Processes packages. Header and root tag opening are written by method
     * {@link MergedXmlPackage#startDocument(XMLEventWriter, String, XmlPackage)} call in
     * {@link MergedXmlPrimary#merge(Collection, XmlEvent)}, that's why
     * we skip first two events here.
     * @param locations Locations to skip
     * @param reader Where to read from
     * @param writer Where to write
     * @param cnt Valid packages count
     * @return Checksums of the skipped packages
     * @throws XMLStreamException If fails
     * @checkstyle ParameterNumberCheck (5 lines)
     * @checkstyle CyclomaticComplexityCheck (20 lines)
     */
    @SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.CyclomaticComplexity"})
    private static Collection<String> processPackages(final Set<String> locations,
        final XMLEventReader reader, final XMLEventWriter writer, final AtomicLong cnt)
        throws XMLStreamException {
        XMLEvent event;
        final List<XMLEvent> pckg = new ArrayList<>(10);
        boolean valid = true;
        final Collection<String> res = new ArrayList<>(locations.size());
        String checksum = "123";
        reader.nextEvent();
        reader.nextEvent();
        while (reader.hasNext()) {
            event = reader.nextEvent();
            if (MergedXmlPrimary.isTag(event, "package")) {
                pckg.clear();
            }
            pckg.add(event);
            if (MergedXmlPrimary.isTag(event, "checksum")) {
                event = reader.nextEvent();
                pckg.add(event);
                checksum = event.asCharacters().getData();
            }
            if (MergedXmlPrimary.isTag(event, "location")) {
                valid = event.isStartElement()
                    && !locations.contains(
                        event.asStartElement().getAttributeByName(new QName("href")).getValue()
                );
            }
            final boolean endpackage = event.isEndElement()
                && event.asEndElement().getName().getLocalPart().equals("package");
            if (endpackage && valid) {
                cnt.incrementAndGet();
                for (final XMLEvent item : pckg) {
                    writer.add(item);
                }
            } else if (endpackage) {
                res.add(checksum);
            }
        }
        return res;
    }

    /**
     * Checks event.
     * @param event Event
     * @param tag Xml tag
     * @return True is this event is given xml tag
     */
    private static boolean isTag(final XMLEvent event, final String tag) {
        return event.isStartElement()
            && event.asStartElement().getName().getLocalPart().equals(tag);
    }

}
