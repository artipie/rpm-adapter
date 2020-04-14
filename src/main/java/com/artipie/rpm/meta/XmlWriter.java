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

import javax.xml.stream.XMLStreamException;

/**
 * Interface for wrapping needed functionality of xmlstreamwriter.
 * @since 0.7
 */
@SuppressWarnings("PMD.TooManyMethods")
public interface XmlWriter {
    /**
     * Writes a start tag to the output.  All writeStartElement methods.
     * open a new scope in the internal namespace context.  Writing the
     * corresponding EndElement causes the scope to be closed.
     * @param localname Local name of the tag, may not be null
     * @throws XMLStreamException On error
     */
    void writeStartElement(String localname) throws XMLStreamException;

    /**
     * Writes an attribute to the output stream without a prefix.
     * @param localname The local name of the attribute
     * @param value The value of the attribute
     * @throws IllegalStateException if the current state does not allow Attribute writing
     * @throws XMLStreamException On error
     */
    void writeAttribute(String localname, String value) throws XMLStreamException;

    /**
     * Writes an end tag to the output relying on the internal.
     * state of the writer to determine the prefix and local name
     * of the event.
     * @throws XMLStreamException On error
     */
    void writeEndElement() throws XMLStreamException;

    /**
     * Write the XML Declaration.  Note that the encoding parameter does.
     * not set the actual encoding of the underlying output.  That must
     * be set when the instance of the XMLStreamWriter is created using the
     * XMLOutputFactory
     * @param encoding Encoding of the xml declaration
     * @param version Version of the xml document
     * @throws XMLStreamException If given encoding does not match encoding
     *  of the underlying stream
     */
    void writeStartDocument(String encoding, String version) throws XMLStreamException;

    /**
     * Writes the default namespace to the stream.
     * @param namespaceuri The uri to bind the default namespace to
     * @throws IllegalStateException if the current state does not allow Namespace writing
     * @throws XMLStreamException On error
     */
    void writeDefaultNamespace(String namespaceuri) throws XMLStreamException;

    /**
     * Closes any start tags and writes corresponding end tags.
     * @throws XMLStreamException On error
     */
    void writeEndDocument() throws XMLStreamException;

    /**
     * Close this writer and free any resources associated with the.
     * writer.  This must not close the underlying output stream.
     * @throws XMLStreamException On error
     */
    void close() throws XMLStreamException;

    /**
     * Writes an empty element tag to the output.
     * @param localname Local name of the tag, may not be null
     * @throws XMLStreamException On error
     */
    void writeEmptyElement(String localname) throws XMLStreamException;

    /**
     * Write text to the output.
     * @param text The value to write
     * @throws XMLStreamException On error
     */
    void writeCharacters(String text) throws XMLStreamException;

    /**
     * Writes a namespace to the output stream.
     * If the prefix argument to this method is the empty string,
     * "xmlns", or null this method will delegate to writeDefaultNamespace
     *
     * @param prefix The prefix to bind this namespace to
     * @param namespaceuri The uri to bind the prefix to
     * @throws IllegalStateException if the current state does not allow Namespace writing
     * @throws XMLStreamException On error
     */
    void writeNamespace(String prefix, String namespaceuri) throws XMLStreamException;

    /**
     * Writes an empty element tag to the output.
     * @param namespaceuri The uri to bind the tag to, may not be null
     * @param localname Local name of the tag, may not be null
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
     *  javax.xml.stream.isRepairingNamespaces has not been set to true
     */
    void writeEmptyElement(String namespaceuri, String localname) throws XMLStreamException;

    /**
     * Writes a start tag to the output.
     * @param namespaceuri The namespaceURI of the prefix to use, may not be null
     * @param localname Local name of the tag, may not be null
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
     *  javax.xml.stream.isRepairingNamespaces has not been set to true
     */
    void writeStartElement(String namespaceuri, String localname) throws XMLStreamException;
}
