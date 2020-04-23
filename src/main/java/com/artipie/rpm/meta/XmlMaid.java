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
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Xml maid.
 * @since 0.3
 */
public interface XmlMaid {

    /**
     * Cleans xml by ids (checksums) and returns actual package count.
     * @param ids Checksums
     * @return Packages count
     * @throws IOException When smth wrong
     */
    long clean(List<String> ids) throws IOException;

    /**
     * Cleans xml by pkgid attribute in package tag.
     * @since 0.3
     */
    final class ByPkgidAttr implements XmlMaid {

        /**
         * Package tag name.
         */
        private static final String TAG = "package";

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
        public ByPkgidAttr(final Path source, final Path target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public long clean(final List<String> ids) throws IOException {
            try (InputStream in = Files.newInputStream(this.source);
                OutputStream out = Files.newOutputStream(this.target)) {
                final XMLEventReader reader =
                    XMLInputFactory.newInstance().createXMLEventReader(in);
                final XMLEventWriter writer =
                    XMLOutputFactory.newInstance().createXMLEventWriter(out);
                XMLEvent event;
                boolean valid = true;
                final AtomicLong cnt = new AtomicLong();
                while (reader.hasNext()) {
                    event = reader.nextEvent();
                    if (event.isStartElement()
                        && event.asStartElement().getName().getLocalPart().equals(ByPkgidAttr.TAG)
                    ) {
                        cnt.incrementAndGet();
                        valid = !ids.contains(
                            event.asStartElement().getAttributeByName(new QName("pkgid")).getValue()
                        );
                    }
                    if (valid) {
                        writer.add(event);
                    }
                    if (event.isEndElement()
                        && event.asEndElement().getName().getLocalPart().equals(ByPkgidAttr.TAG)) {
                        valid = true;
                    }
                }
                return cnt.get();
            } catch (final XMLStreamException | FileNotFoundException ex) {
                throw new IOException(ex);
            }
        }
    }
}
