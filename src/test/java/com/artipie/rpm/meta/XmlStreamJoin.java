/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Joins xml files by tag using xml-streams.
 * @since 0.9
 */
public final class XmlStreamJoin {

    /**
     * Tag.
     */
    private final String tag;

    /**
     * Ctor.
     * @param tag Xml tag
     */
    public XmlStreamJoin(final String tag) {
        this.tag = tag;
    }

    /**
     * Appends data from part to target.
     * @param target Target
     * @param part File to append
     * @throws IOException On error
     */
    @SuppressWarnings({"PMD.PrematureDeclaration", "PMD.GuardLogStatement"})
    public void merge(final Path target, final Path part) throws IOException {
        final Path res = target.getParent().resolve(
            String.format("%s.joined", target.getFileName().toString())
        );
        try (OutputStream out = Files.newOutputStream(res)) {
            final XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(out);
            this.writeFirstPart(target, writer);
            this.writeSecondPart(part, writer);
            writer.close();
        } catch (final XMLStreamException ex) {
            Files.delete(res);
            throw new XmlException(ex);
        }
        Files.move(res, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Writes the first part.
     * @param target What to write
     * @param writer Where to write
     * @throws IOException On error
     * @throws XMLStreamException On error
     */
    private void writeFirstPart(final Path target, final XMLEventWriter writer)
        throws IOException, XMLStreamException {
        try (InputStream in = Files.newInputStream(target)) {
            final XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(in);
            writer.add(reader.nextEvent());
            writer.add(XMLEventFactory.newFactory().createSpace("\n"));
            while (reader.hasNext()) {
                final XMLEvent event = reader.nextEvent();
                if (!(event.isEndElement()
                    && event.asEndElement().getName().getLocalPart().equals(this.tag))
                    && !event.isEndDocument()
                ) {
                    writer.add(event);
                }
            }
            reader.close();
        }
    }

    /**
     * Writes second part.
     * @param part What to write
     * @param writer Where to write
     * @throws IOException On error
     * @throws XMLStreamException On error
     */
    private void writeSecondPart(final Path part, final XMLEventWriter writer)
        throws IOException, XMLStreamException {
        try (InputStream in = Files.newInputStream(part)) {
            final XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(in);
            while (reader.hasNext()) {
                final XMLEvent event = reader.nextEvent();
                if (!(event.isStartElement()
                    && event.asStartElement().getName().getLocalPart().equals(this.tag))
                    && !event.isStartDocument()
                ) {
                    writer.add(event);
                }
            }
            reader.close();
        }
    }
}
