/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
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
            throw new XmlException("Invalid primary file", ex);
        }
        return res;
    }
}
