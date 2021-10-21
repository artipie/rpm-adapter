/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.rpm.meta.XmlException;
import com.artipie.rpm.meta.XmlMaid;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlPrimary;
import com.artipie.rpm.meta.XmlPrimaryMaid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.xml.stream.XMLStreamException;

/**
 * Package output for {@code primary} metadata.
 * @since 0.4
 */
public final class PrimaryOutput implements PackageOutput.FileOutput {

    /**
     * Path to write primary.xml.
     */
    private final Path path;

    /**
     * Temporary file.
     */
    private final Path tmp;

    /**
     * XML primary file.
     */
    private final XmlPrimary xml;

    /**
     * Ctor.
     * @param file Path to write primary.xml
     */
    public PrimaryOutput(final Path file) {
        this.path = file;
        this.tmp = file.getParent().resolve(
            String.format("%s.part", file.getFileName().toString())
        );
        this.xml = new XmlPrimary(this.tmp);
    }

    /**
     * Starts processing of RPMs.
     * @return Self
     */
    public PrimaryOutput start() {
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
            this.xml.startPackage()
                .name(tags.name())
                .arch(tags.arch())
                .version(tags.epoch(), tags.version(), tags.release())
                .checksum(
                    meta.checksum().digest().type(), "YES",
                    meta.checksum().hex()
                ).summary(tags.summary())
                .description(tags.description())
                .packager(tags.packager())
                .url(tags.url())
                .time(tags.fileTimes(), tags.buildTime())
                .size(meta.size(), tags.installedSize(), tags.archiveSize())
                .location(meta.href())
                .startFormat()
                .license(tags.license())
                .vendor(tags.vendor())
                .group(tags.group())
                .buildHost(tags.buildHost())
                .sourceRpm(tags.sourceRmp())
                .headerRange(meta.range()[0], meta.range()[1])
                .provides(tags.providesNames(), tags.providesVer())
                .requires(tags.requires(), tags.requiresVer())
                .close()
                .files(
                    tags.baseNames().toArray(new String[0]),
                    tags.dirNames().toArray(new String[0]),
                    tags.dirIndexes()
                ).close();
        } catch (final XMLStreamException err) {
            throw new XmlException("Failed to update XML", err);
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
        return new XmlPrimaryMaid(this.path);
    }

    @Override
    public String tag() {
        return XmlPackage.PRIMARY.tag();
    }
}
