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
package com.artipie.rpm.pkg;

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
     * @throws IOException On errors
     */
    public PrimaryOutput start() throws IOException {
        try {
            this.xml.startPackages();
        } catch (final XMLStreamException err) {
            throw new IOException("Failed to start packages", err);
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
                .providers(tags.providerName(), tags.providerVer())
                .requires(tags.requires())
                .close()
                .files(
                    tags.baseNames().toArray(new String[0]),
                    tags.dirNames().toArray(new String[0]),
                    tags.dirIndexes()
                ).close();
        } catch (final XMLStreamException err) {
            throw new IOException("Failed to update XML", err);
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
