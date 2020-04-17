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

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Envelop for XmlFile Class.
 * @since 0.7
 * @checkstyle DesignForExtensionCheck (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
public class XmlWriterWrap implements XMLStreamWriter {
    /**
     * XML stream.
     */
    private final XMLStreamWriter xml;

    /**
     * Ctor.
     * @param xml XMLStreamWriter object.
     */
    public XmlWriterWrap(final XMLStreamWriter xml) {
        this.xml = xml;
    }

    @Override
    public void writeStartElement(
        final String localname
    ) throws XMLStreamException {
        this.xml.writeStartElement(localname);
    }

    @Override
    public void writeAttribute(
        final String localname,
        final String value
    ) throws XMLStreamException {
        this.xml.writeAttribute(localname, value);
    }

    // @checkstyle ParameterNumberCheck (4 lines)
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    @Override
    public void writeAttribute(
        final String prefix,
        final String namespaceuri,
        final String localname,
        final String value
    ) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void writeAttribute(
        final String namespaceuri,
        final String localname,
        final String value
    ) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        this.xml.writeEndElement();
    }

    @Override
    public void writeStartDocument(
        final String encoding,
        final String version
    ) throws XMLStreamException {
        this.xml.writeStartDocument(encoding, version);
    }

    @Override
    public void writeDefaultNamespace(
        final String namespaceuri
    ) throws XMLStreamException {
        this.xml.writeDefaultNamespace(namespaceuri);
    }

    @Override
    public void writeComment(final String data) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void writeProcessingInstruction(
        final String target
    ) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void writeProcessingInstruction(
        final String target,
        final String data
    ) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void writeCData(final String data) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void writeDTD(final String dtd) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void writeEntityRef(final String name) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void writeStartDocument(final String version) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        this.xml.writeEndDocument();
    }

    @Override
    public void close() throws XMLStreamException {
        this.xml.close();
    }

    @Override
    public void flush() throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void writeEmptyElement(final String localname) throws XMLStreamException {
        this.xml.writeEmptyElement(localname);
    }

    @Override
    public void writeCharacters(final String text) throws XMLStreamException {
        this.xml.writeCharacters(text);
    }

    @Override
    public void writeCharacters(
        final char[] text,
        final int start,
        final int len
    ) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public String getPrefix(final String uri) throws XMLStreamException {
        return null;
    }

    @Override
    public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void setDefaultNamespace(final String uri) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void setNamespaceContext(
        final NamespaceContext context
    ) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return null;
    }

    @Override
    public Object getProperty(final String name) throws IllegalArgumentException {
        return null;
    }

    @Override
    public void writeNamespace(
        final String prefix,
        final String namespaceuri
    ) throws XMLStreamException {
        this.xml.writeNamespace(prefix, namespaceuri);
    }

    @Override
    public void writeEmptyElement(
        final String namespaceuri,
        final String localname
    ) throws XMLStreamException {
        this.xml.writeEmptyElement(namespaceuri, localname);
    }

    @Override
    public void writeEmptyElement(
        final String prefix,
        final String localname,
        final String namespaceuri
    ) throws XMLStreamException {
        // Not implemeneted.
    }

    @Override
    public void writeStartElement(
        final String namespaceuri,
        final String localname
    ) throws XMLStreamException {
        this.xml.writeStartElement(namespaceuri, localname);
    }

    @Override
    public void writeStartElement(
        final String prefix,
        final String localname,
        final String namespaceuri
    ) throws XMLStreamException {
        // Not implemeneted.
    }
}
