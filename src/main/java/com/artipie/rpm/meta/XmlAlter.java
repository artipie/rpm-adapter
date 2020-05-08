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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Alter xml file.
 * @since 0.8
 * @todo #144:30min Create separate proper unit-test for this class, now it is tested in the
 *  XmlPackagesFileTest#writesCorrectPackageCount(java.nio.file.Path) method.
 */
public final class XmlAlter {

    /**
     * File to update.
     */
    private final Path file;

    /**
     * Ctor.
     * @param file File to update
     */
    public XmlAlter(final Path file) {
        this.file = file;
    }

    /**
     * Updates packages attribute of the given file.
     * @param tag Tag to change
     * @param value Value for the attribute
     * @throws IOException When error occurs
     */
    public void pkgAttr(final String tag, final String value) throws IOException {
        final Path trf = Files.createTempFile("", ".xml");
        try {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            try (
                InputStream input = Files.newInputStream(this.file);
                OutputStream out = Files.newOutputStream(trf)) {
                transformer.transform(
                    new StAXSource(
                        new AlterAttributeEventReader(
                            XMLInputFactory.newFactory().createXMLEventReader(input),
                            tag, "packages", value
                        )
                    ),
                    new StreamResult(out)
                );
            }
            Files.move(trf, this.file, StandardCopyOption.REPLACE_EXISTING);
        } catch (final XMLStreamException | TransformerException err) {
            throw new IOException("Failed to alter file", err);
        } finally {
            Files.deleteIfExists(trf);
        }
    }

}
