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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Extracts checksums from primary xml.
 * @since 0.8
 */
public final class XmlPrimaryChecksums {

    /**
     * File path.
     */
    private final Path path;

    /**
     * Ctor.
     * @param path Primary file path
     */
    public XmlPrimaryChecksums(final Path path) {
        this.path = path;
    }

    /**
     * Reads xml.
     * @return List of checksums.
     */
    public List<String> read() {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final List<String> res = new ArrayList<>(1);
        try {
            final Document document = factory.newDocumentBuilder().parse(this.path.toFile());
            document.getDocumentElement().normalize();
            document.getDocumentElement();
            final NodeList pkgs = document.getElementsByTagName("checksum");
            for (int idx = 0; idx < pkgs.getLength(); idx = idx + 1) {
                res.add(pkgs.item(idx).getTextContent());
            }
        } catch (final ParserConfigurationException | SAXException | IOException ex) {
            throw new IllegalStateException("Invalid primary file", ex);
        }
        return res;
    }
}
