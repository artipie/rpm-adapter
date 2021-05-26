/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.rpm.meta.XmlException;
import com.artipie.rpm.meta.XmlMaid;
import com.artipie.rpm.meta.XmlOthers;
import com.artipie.rpm.meta.XmlPackage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.xml.stream.XMLStreamException;

/**
 * Generates other.xml metadata file.
 * @since 0.4
 */
public final class OthersOutput implements PackageOutput.FileOutput {

    /**
     * Path to write other.xml.
     */
    private final Path path;

    /**
     * Temporary file to partly processed other.xml.
     */
    private final Path tmp;

    /**
     * XML of others.
     */
    private final XmlOthers xml;

    /**
     * Ctor.
     * @param file Path to write filelists.xml
     */
    public OthersOutput(final Path file) {
        this.path = file;
        this.tmp = file.getParent().resolve(
            String.format("%s.part", file.getFileName().toString())
        );
        this.xml = new XmlOthers(this.tmp);
    }

    /**
     * Starts processing of RPMs.
     * @return Self
     */
    public OthersOutput start() {
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
            this.xml.addPackage(
                tags.name(), tags.arch(),
                meta.checksum().hex()
            ).version(tags.epoch(), tags.version(), tags.release())
                .changelog(tags.changelog()).close();
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
        return XmlPackage.OTHER.tag();
    }
}
