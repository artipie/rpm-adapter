package com.artipie.rpm.meta;

import javax.xml.stream.XMLStreamException;

public interface XmlWriter {
    /**
     * Writes a start tag to the output.  All writeStartElement methods
     * open a new scope in the internal namespace context.  Writing the
     * corresponding EndElement causes the scope to be closed.
     * @param localName local name of the tag, may not be null
     * @throws XMLStreamException
     */
    public void writeStartElement(String localName)
            throws XMLStreamException;

    /**
     * Writes an attribute to the output stream without
     * a prefix.
     * @param localName the local name of the attribute
     * @param value the value of the attribute
     * @throws IllegalStateException if the current state does not allow Attribute writing
     * @throws XMLStreamException
     */
    public void writeAttribute(String localName, String value)
            throws XMLStreamException;

    /**
     * Writes an end tag to the output relying on the internal
     * state of the writer to determine the prefix and local name
     * of the event.
     * @throws XMLStreamException
     */
    public void writeEndElement()
            throws XMLStreamException;

    /**
     * Write the XML Declaration.  Note that the encoding parameter does
     * not set the actual encoding of the underlying output.  That must
     * be set when the instance of the XMLStreamWriter is created using the
     * XMLOutputFactory
     * @param encoding encoding of the xml declaration
     * @param version version of the xml document
     * @throws XMLStreamException If given encoding does not match encoding
     * of the underlying stream
     */
    public void writeStartDocument(String encoding,
                                   String version)
            throws XMLStreamException;

    /**
     * Writes the default namespace to the stream
     * @param namespaceURI the uri to bind the default namespace to
     * @throws IllegalStateException if the current state does not allow Namespace writing
     * @throws XMLStreamException
     */
    public void writeDefaultNamespace(String namespaceURI)
            throws XMLStreamException;

    /**
     * Closes any start tags and writes corresponding end tags.
     * @throws XMLStreamException
     */
    public void writeEndDocument()
            throws XMLStreamException;

    /**
     * Close this writer and free any resources associated with the
     * writer.  This must not close the underlying output stream.
     * @throws XMLStreamException
     */
    public void close()
            throws XMLStreamException;

    /**
     * Writes an empty element tag to the output
     * @param localName local name of the tag, may not be null
     * @throws XMLStreamException
     */
    public void writeEmptyElement(String localName)
            throws XMLStreamException;

    /**
     * Write text to the output
     * @param text the value to write
     * @throws XMLStreamException
     */
    public void writeCharacters(String text)
            throws XMLStreamException;

    /**
     * Writes a namespace to the output stream
     * If the prefix argument to this method is the empty string,
     * "xmlns", or null this method will delegate to writeDefaultNamespace
     *
     * @param prefix the prefix to bind this namespace to
     * @param namespaceURI the uri to bind the prefix to
     * @throws IllegalStateException if the current state does not allow Namespace writing
     * @throws XMLStreamException
     */
    public void writeNamespace(String prefix, String namespaceURI)
            throws XMLStreamException;

    /**
     * Writes an empty element tag to the output
     * @param namespaceURI the uri to bind the tag to, may not be null
     * @param localName local name of the tag, may not be null
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
     * javax.xml.stream.isRepairingNamespaces has not been set to true
     */
    public void writeEmptyElement(String namespaceURI, String localName)
            throws XMLStreamException;

    /**
     * Writes a start tag to the output
     * @param namespaceURI the namespaceURI of the prefix to use, may not be null
     * @param localName local name of the tag, may not be null
     * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
     * javax.xml.stream.isRepairingNamespaces has not been set to true
     */
    public void writeStartElement(String namespaceURI, String localName)
            throws XMLStreamException;
}
