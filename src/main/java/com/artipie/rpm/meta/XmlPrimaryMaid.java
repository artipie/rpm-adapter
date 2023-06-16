/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.RpmMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Maid for primary.xml.
 * @since 0.8
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class XmlPrimaryMaid implements XmlMaid {

    /**
     * File to clear.
     */
    private final Path file;

    /**
     * Ctor.
     * @param file File to clear
     */
    public XmlPrimaryMaid(final Path file) {
        this.file = file;
    }

    @Override
    public long clean(final Collection<String> checksums) throws IOException {
        final Path tmp = this.file.getParent().resolve(
            String.format("%s.part", this.file.getFileName().toString())
        );
        final long res;
        try (InputStream in = Files.newInputStream(this.file);
            OutputStream out = Files.newOutputStream(tmp)) {
            res = new Stream(in, out).clean(checksums);
        } catch (final IOException ex) {
            throw new XmlException(ex);
        }
        Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING);
        return res;
    }

    /**
     * Implementation of {@link XmlMaid} to clean primary.xml and work with streams.
     * Input/output streams are not closed in this implementation, resources
     * should be closed from the outside.
     * @since 1.4
     */
    public static final class Stream implements XmlMaid {

        /**
         * Input.
         */
        private final InputStream input;

        /**
         * Output.
         */
        private final OutputStream out;

        /**
         * Ctor.
         * @param input Input
         * @param out Output
         */
        public Stream(final InputStream input, final OutputStream out) {
            this.input = input;
            this.out = out;
        }

        @Override
        public long clean(final Collection<String> ids) throws IOException {
            final long res;
            try {
                final XMLEventReader reader = RpmMetadata.INPUT_FACTORY
                    .createXMLEventReader(this.input);
                final XMLEventWriter writer = RpmMetadata.OUTPUT_FACTORY
                    .createXMLEventWriter(this.out);
                try {
                    MergedXmlPackage.startDocument(writer, "-1", XmlPackage.PRIMARY);
                    res = Stream.processPackages(ids, reader, writer);
                    writer.add(RpmMetadata.EVENTS_FACTORY.createSpace("\n"));
                    writer.add(
                        RpmMetadata.EVENTS_FACTORY.createEndElement(
                            new QName(XmlPackage.PRIMARY.tag()), Collections.emptyIterator()
                        )
                    );
                } finally {
                    writer.close();
                    reader.close();
                }
            } catch (final XMLStreamException err) {
                throw new IOException(err);
            }
            return res;
        }

        /**
         * Processes packages.
         * @param checksums Checksums to skip
         * @param reader Where to read from
         * @param writer Where to write
         * @return Valid packages count
         * @throws XMLStreamException If fails
         */
        private static long processPackages(final Collection<String> checksums,
            final XMLEventReader reader, final XMLEventWriter writer) throws XMLStreamException {
            XMLEvent event;
            final List<XMLEvent> pckg = new ArrayList<>(10);
            boolean valid = true;
            long cnt = 0;
            while (reader.hasNext()) {
                event = reader.nextEvent();
                if (Stream.isTag(event, "package")) {
                    pckg.clear();
                }
                pckg.add(event);
                if (Stream.isTag(event, "checksum")
                ) {
                    event = reader.nextEvent();
                    pckg.add(event);
                    valid = event.isCharacters()
                        && !checksums.contains(event.asCharacters().getData());
                }
                if (event.isEndElement()
                    && event.asEndElement().getName().getLocalPart().equals("package") && valid) {
                    cnt = cnt + 1;
                    for (final XMLEvent item : pckg) {
                        writer.add(item);
                    }
                }
            }
            return cnt;
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

}
