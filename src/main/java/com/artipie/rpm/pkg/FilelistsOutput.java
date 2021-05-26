/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.rpm.meta.XmlException;
import com.artipie.rpm.meta.XmlFilelists;
import com.artipie.rpm.meta.XmlMaid;
import com.artipie.rpm.meta.XmlPackage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.xml.stream.XMLStreamException;

/**
 * Package output for {@code filelists.xml}.
 * @since 0.6
 */
public final class FilelistsOutput implements PackageOutput.FileOutput {

    /**
     * Path to write filelists.xml.
     */
    private final Path path;

    /**
     * Temporary file to modify.
     */
    private final Path tmp;

    /**
     * XML stream processor.
     */
    private final XmlFilelists xml;

    /**
     * Ctor.
     * @param file Path to write filelists.xml
     */
    public FilelistsOutput(final Path file) {
        this.path = file;
        this.tmp = file.getParent().resolve(
            String.format("%s.part", file.getFileName().toString())
        );
        this.xml = new XmlFilelists(this.tmp);
    }

    /**
     * Start packages.
     * @return Self
     * @throws IOException On failure
     */
    public FilelistsOutput start() {
        try {
            this.xml.startPackages();
        } catch (final XMLStreamException err) {
            throw new XmlException("Failed to start packages", err);
        }
        return this;
    }

    @Override
    public void accept(final Package.Meta meta) throws IOException {
        final HeaderTags tags = new HeaderTags(meta);
        try {
            this.xml.startPackage(
                tags.name(), tags.arch(), meta.checksum().hex()
            ).version(tags.epoch(), tags.version(), tags.release())
                .files(
                    tags.baseNames().toArray(new String[0]),
                    tags.dirNames().toArray(new String[0]),
                    tags.dirIndexes()
                ).close();
        } catch (final XMLStreamException err) {
            throw new XmlException("Failed to add package", err);
        }
    }

    @Override
    public void close() throws IOException {
        this.xml.close();
        Files.move(this.tmp, this.path, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path file() {
        return this.path;
    }

    @Override
    public XmlMaid maid() {
        return new XmlMaid.ByPkgidAttr(this.path);
    }

    @Override
    public String tag() {
        return XmlPackage.FILELISTS.tag();
    }
}
