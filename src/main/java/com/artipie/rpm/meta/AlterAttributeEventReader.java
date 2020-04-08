/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * XML Event Reader wrapper that allows to override attribute value in the specified tag.
 * @since 0.4
 * @checkstyle ReturnCountCheck (500 lines)
 */
final class AlterAttributeEventReader extends EventReaderDelegate {
    /**
     * Tag to find.
     */
    private final String tag;

    /**
     * Attribute to find.
     */
    private final String attribute;

    /**
     * Value to replace.
     */
    private final String value;

    /**
     * Ctor.
     * @param delegate Underlying event reader
     * @param tag Tag to find
     * @param attribute Attribute to find
     * @param value Value to be replace
     * @checkstyle ParameterNumberCheck (6 lines)
     */
    AlterAttributeEventReader(
        final XMLEventReader delegate,
        final String tag,
        final String attribute,
        final String value) {
        super(delegate);
        this.tag = tag;
        this.attribute = attribute;
        this.value = value;
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public XMLEvent nextEvent() throws XMLStreamException {
        final XMLEvent original = super.nextEvent();
        if (!original.isStartElement()) {
            return original;
        }
        final StartElement element = original.asStartElement();
        if (!element.getName().getLocalPart().equals(this.tag)) {
            return original;
        }
        final List<Attribute> newattrs = new ArrayList<>(0);
        final XMLEventFactory events = XMLEventFactory.newFactory();
        boolean replaced = false;
        final Iterator<?> origattrs = element.getAttributes();
        while (origattrs.hasNext()) {
            final Attribute attr = (Attribute) origattrs.next();
            if (attr.getName().getLocalPart().equals(this.attribute)) {
                newattrs.add(events.createAttribute(attr.getName(), this.value));
                replaced = true;
            } else {
                newattrs.add(attr);
            }
        }
        if (!replaced) {
            return original;
        }
        return events.createStartElement(
            element.getName().getPrefix(),
            element.getName().getNamespaceURI(),
            element.getName().getLocalPart(),
            newattrs.iterator(),
            element.getNamespaces(),
            element.getNamespaceContext()
        );
    }
}
