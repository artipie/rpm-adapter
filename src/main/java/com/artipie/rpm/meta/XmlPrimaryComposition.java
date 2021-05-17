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

import com.artipie.rpm.Digest;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.FilePackageHeader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.fasterxml.aalto.stax.OutputFactoryImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Xml primary composition: appends provided information to primary.xml,
 * excluding duplicated packages by `location` tag.
 * @since 1.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class XmlPrimaryComposition {

    /**
     * From where to read primary.xml.
     */
    private final InputStream input;

    /**
     * Where to write the result.
     */
    private final OutputStream out;

    /**
     * Digest algorithm.
     */
    private final Digest dgst;

    /**
     * Ctor.
     * @param input Input stream
     * @param out Output stream
     * @param dgst Digest algorithm
     */
    public XmlPrimaryComposition(final InputStream input, final OutputStream out,
        final Digest dgst) {
        this.input = input;
        this.out = out;
        this.dgst = dgst;
    }

    /**
     * Appends provided packages to the primary.xml.
     * @param packages Packages to append
     * @return Result of the operation: final packages count and ids of the duplicated packages
     * @throws IOException On error
     */
    public Result append(final Map<Path, String> packages) throws IOException {
        final AtomicLong res = new AtomicLong();
        final Collection<String> checksums;
        try {
            final XMLEventReader reader =
                new InputFactoryImpl().createXMLEventReader(this.input);
            final XMLEventWriter writer =
                new OutputFactoryImpl().createXMLEventWriter(this.out);
            try {
                final XMLEventFactory events = XMLEventFactory.newFactory();
                writer.add(reader.nextEvent());
                writer.add(events.createSpace("\n"));
                writer.add(reader.nextEvent());
                writer.add(reader.nextEvent());
                checksums = XmlPrimaryComposition.processPackages(
                    new HashSet<>(packages.values()), reader, writer, res
                );
                for (final Map.Entry<Path, String> item : packages.entrySet()) {
                    new XmlEvent.Primary(writer).add(
                        new FilePackage.Headers(
                            new FilePackageHeader(item.getKey()).header(),
                            item.getKey(), this.dgst, item.getValue()
                        )
                    );
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
                reader.close();
            }
        } catch (final XMLStreamException err) {
            throw new IOException(err);
        }
        return new Result(res.get(), checksums);
    }

    /**
     * Processes packages.
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
        while (reader.hasNext()) {
            event = reader.nextEvent();
            if (XmlPrimaryComposition.isTag(event, "package")) {
                pckg.clear();
            }
            pckg.add(event);
            if (XmlPrimaryComposition.isTag(event, "checksum") && event.isCharacters()) {
                checksum = event.asCharacters().getData();
            }
            if (XmlPrimaryComposition.isTag(event, "location")) {
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

    /**
     * XmlPrimary composition result.
     * @since 1.5
     */
    public static final class Result {

        /**
         * Items count.
         */
        private final long cnt;

        /**
         * Ids of the items to remove.
         */
        private final Collection<String> ids;

        /**
         * Ctor.
         * @param cnt Items count
         * @param ids Ids of the items to remove
         */
        public Result(final long cnt, final Collection<String> ids) {
            this.cnt = cnt;
            this.ids = ids;
        }

        /**
         * Get packages count.
         * @return Count
         */
        public long count() {
            return this.cnt;
        }

        /**
         * Get packages checksums (ids).
         * @return Checksums
         */
        public Collection<String> checksums() {
            return this.ids;
        }
    }
}
