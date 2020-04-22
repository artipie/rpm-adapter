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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Maid for primary.xml.
 * @since 0.8
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class XmlPrimaryMaid {

    /**
     * Source, what to clear.
     */
    private final Path source;

    /**
     * Where to write clean data.
     */
    private final Path target;

    /**
     * Ctor.
     * @param source What to clear
     * @param target Where to write
     */
    public XmlPrimaryMaid(final Path source, final Path target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Clears records about packages by given checksums (checksums).
     * @param checksums What to clear
     * @throws IOException When smth wrong
     */
    void clean(final List<String> checksums) throws IOException {
        try (InputStream in = Files.newInputStream(this.source);
            OutputStream out = Files.newOutputStream(this.target)) {
            final XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(in);
            final XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(out);
            final XMLEventFactory events = XMLEventFactory.newFactory();
            writer.add(reader.nextEvent());
            writer.add(events.createSpace("\n"));
            writer.add(reader.nextEvent());
            writer.add(reader.nextEvent());
            XmlPrimaryMaid.processPackages(checksums, reader, writer);
            writer.add(events.createSpace("\n"));
            writer.add(events.createEndElement(new QName("metadata"), Collections.emptyIterator()));
        } catch (final XMLStreamException | FileNotFoundException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Processes packages.
     * @param checksums Checksums to skip
     * @param reader Where to read from
     * @param writer Where to write
     * @throws XMLStreamException If fails
     */
    private static void processPackages(final List<String> checksums,
        final XMLEventReader reader, final XMLEventWriter writer) throws XMLStreamException {
        XMLEvent event;
        final List<XMLEvent> pckg = new ArrayList<>(10);
        boolean valid = true;
        while (reader.hasNext()) {
            event = reader.nextEvent();
            if (XmlPrimaryMaid.isTag(event, "package")) {
                pckg.clear();
            }
            pckg.add(event);
            if (XmlPrimaryMaid.isTag(event, "checksum")
            ) {
                event = reader.nextEvent();
                pckg.add(event);
                valid = event.isCharacters()
                    && !checksums.contains(event.asCharacters().getData());
            }
            if (event.isEndElement()
                && event.asEndElement().getName().getLocalPart().equals("package") && valid) {
                for (final XMLEvent item : pckg) {
                    writer.add(item);
                }
            }
        }
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
