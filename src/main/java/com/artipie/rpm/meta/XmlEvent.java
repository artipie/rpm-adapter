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
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Xml event to write to the output stream.
 * @since 1.5
 */
public interface XmlEvent {

    /**
     * Contracts {@link XMLEvent} with provided metadata.
     * @param meta Info to build {@link XMLEvent} with
     * @throws IOException On IO error
     */
    void add(Package.Meta meta) throws IOException;

    /**
     * Implementation of {@link XmlEvent} to build event for {@link XmlPackage#OTHER} package.
     * @since 1.5
     * @checkstyle ExecutableStatementCountCheck (30 lines)
     */
    class Others implements XmlEvent {

        /**
         * Where to write the event.
         */
        private final XMLEventWriter writer;

        /**
         * Ctor.
         * @param writer Writer to write the event
         */
        public Others(final XMLEventWriter writer) {
            this.writer = writer;
        }

        @Override
        public void add(final Package.Meta meta) throws IOException {
            final XMLEventFactory events = XMLEventFactory.newFactory();
            final HeaderTags tags = new HeaderTags(meta);
            try {
                final String pkg = "package";
                final String version = "version";
                this.writer.add(events.createStartElement("", "", pkg));
                this.writer.add(events.createAttribute("pkgid", meta.checksum().hex()));
                this.writer.add(events.createAttribute("name", tags.name()));
                this.writer.add(events.createAttribute("arch", tags.arch()));
                this.writer.add(events.createStartElement("", "", version));
                this.writer.add(events.createAttribute("epoch", String.valueOf(tags.epoch())));
                this.writer.add(events.createAttribute("ver", tags.version()));
                this.writer.add(events.createAttribute("rel", tags.release()));
                this.writer.add(events.createEndElement("", "", version));
                for (final String changelog : tags.changelog()) {
                    final ChangelogEntry entry = new ChangelogEntry(changelog);
                    final String tag = "changelog";
                    this.writer.add(events.createStartElement("", "", tag));
                    this.writer.add(events.createAttribute("date", String.valueOf(entry.date())));
                    this.writer.add(events.createAttribute("author", entry.author()));
                    this.writer.add(events.createCharacters(entry.content()));
                    this.writer.add(events.createEndElement("", "", tag));
                }
                this.writer.add(events.createEndElement("", "", pkg));
            } catch (final XMLStreamException err) {
                throw new IOException(err);
            }
        }
    }
}
