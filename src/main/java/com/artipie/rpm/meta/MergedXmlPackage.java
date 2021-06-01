/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.Digest;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.FilePackageHeader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.fasterxml.aalto.stax.OutputFactoryImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Merged xml: reads provided index (filelist of others xml), excludes items by
 * provided checksums, adds items by provided file paths and updates `packages` attribute value.
 * @since 1.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class MergedXmlPackage implements MergedXml {

    /**
     * From where to read primary.xml.
     */
    private final InputStream input;

    /**
     * Where to write the result.
     */
    private final OutputStream out;

    /**
     * Xml package type.
     */
    private final XmlPackage type;

    /**
     * Result of the primary.xml merging.
     */
    private final MergedXml.Result res;

    /**
     * Should invalid packages be skipped?
     */
    private final boolean skip;

    /**
     * Ctor.
     * @param input Input stream
     * @param out Output stream
     * @param type Xml package type
     * @param res Result of the primary.xml merging
     * @param skip Should invalid packages be skipped?
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public MergedXmlPackage(final InputStream input, final OutputStream out, final XmlPackage type,
        final MergedXml.Result res, final boolean skip) {
        this.input = input;
        this.out = out;
        this.type = type;
        this.res = res;
        this.skip = skip;
    }

    @Override
    public MergedXml.Result merge(final Map<Path, String> packages, final Digest dgst,
        final XmlEvent event) throws IOException {
        try {
            final XMLEventReader reader = new InputFactoryImpl().createXMLEventReader(this.input);
            final XMLEventWriter writer = new OutputFactoryImpl().createXMLEventWriter(this.out);
            try {
                final XMLEventFactory events = XMLEventFactory.newFactory();
                MergedXmlPackage.startDocument(writer, String.valueOf(this.res.count()), this.type);
                this.process(this.res.checksums(), reader, writer);
                for (final Path item : packages.keySet()) {
                    new MergedXml.InvalidPackage(
                        () -> event.add(
                            writer,
                            new FilePackage.Headers(
                                new FilePackageHeader(item).header(), item, dgst
                            )
                        ), this.skip
                    ).handle();
                }
                writer.add(events.createSpace("\n"));
                writer.add(
                    events.createEndElement(new QName(this.type.tag()), Collections.emptyIterator())
                );
            } finally {
                writer.close();
                reader.close();
            }
        } catch (final XMLStreamException err) {
            throw new IOException(err);
        }
        return this.res;
    }

    /**
     * Starts resulting packages index file.
     * @param writer Document writer
     * @param cnt Packages count
     * @param type Package type
     * @throws XMLStreamException On error
     */
    static void startDocument(final XMLEventWriter writer, final String cnt, final XmlPackage type)
        throws XMLStreamException {
        final XMLEventFactory events = XMLEventFactory.newFactory();
        writer.add(events.createStartDocument(StandardCharsets.UTF_8.displayName(), "1.0"));
        writer.add(events.createStartElement("", "", type.tag()));
        for (final Map.Entry<String, String> item : type.xmlNamespaces().entrySet()) {
            writer.add(events.createNamespace(item.getKey(), item.getValue()));
        }
        writer.add(events.createAttribute("packages", cnt));
        writer.add(events.createSpace("\n"));
    }

    /**
     * Process lines.
     * @param ids Not valid ids list
     * @param reader Reader
     * @param writer Writes
     * @throws XMLStreamException When error occurs
     * @checkstyle ParameterNumberCheck (5 lines)
     * @checkstyle CyclomaticComplexityCheck (20 lines)
     */
    private void process(final Collection<String> ids, final XMLEventReader reader,
        final XMLEventWriter writer) throws XMLStreamException {
        boolean valid = true;
        XMLEvent event;
        reader.nextEvent();
        reader.nextEvent();
        while (reader.hasNext()) {
            event = reader.nextEvent();
            if (MergedXmlPackage.isEndTag(event, this.type.tag())) {
                break;
            } else {
                if (event.isStartElement()
                    && event.asStartElement().getName().getLocalPart()
                    .equals(XmlMaid.ByPkgidAttr.TAG)
                ) {
                    valid = !ids.contains(
                        event.asStartElement().getAttributeByName(new QName("pkgid")).getValue()
                    );
                }
                if (valid) {
                    writer.add(event);
                }
                if (MergedXmlPackage.isEndTag(event, XmlMaid.ByPkgidAttr.TAG)) {
                    valid = true;
                }
            }
        }
    }

    /**
     * Is event end tag?
     * @param event Event
     * @param tag Tag name
     * @return True if event is end tag
     */
    private static boolean isEndTag(final XMLEvent event, final String tag) {
        return event.isEndElement()
            && event.asEndElement().getName().getLocalPart().equals(tag);
    }
}
